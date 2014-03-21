(ns cse341.pages
  (:require [net.cgrand.enlive-html :as h]
            [ring.util.response :as response]
            [cemerick.friend :as friend]
            [me.raynes.fs :as fs]
            [cse341.utils :as utils]
            [cse341.model :as model]
            [cse341.forms :as forms]))

(h/deftemplate base-template "public/html/base.html" [title nav content]
  [:title] (h/content title)
  [:div#header-nav] (h/content nav)
  [:div#page-wrapper] (h/content content))

(h/defsnippet standard-nav "public/html/standard-nav.html"
  [:nav#navigation] []
  [:a#nav-login-out]
  (if (friend/current-authentication)
    (h/do->
     (h/set-attr :href "/logout")
     (h/content "Logout"))
    (h/content "Login"))
  [:a#nav-me]
  (when (friend/current-authentication)
    (h/do->
     (h/set-attr :href "/account")
     (h/content (:username (friend/current-authentication))))))

(def base-page #(base-template %1 (standard-nav) %2))

(defn greeting-page []
  (base-template "Hello!" (standard-nav) "Hello"))


(h/defsnippet notification-form "public/html/notification-form.html"
  [:div#content-bg] [& {:keys [title article action]}]
  [:form#notification] (h/set-attr :action (str "/admin/notifications/" action))
  [:input#input-title] (h/set-attr :value title)
  [:textarea#text-article] (h/content article))

(defn admin-add-notification [& {:keys [request article title tags]}]
  (if (= request :get)
    (base-page "Add Notification" (notification-form :action "add"))
    (let [slugger (utils/mk-slugger title)]
      (model/add-notification {:title title
                               :article article
                               :slugger slugger})
      (response/redirect (str "/notifications/view/" slugger)))))

(defn admin-edit-notification [slugger & {:keys [request article title tags]}]
  (if-let [n (model/get-notification slugger)]
    (if (= request :get)
      (base-page "Edit Notification" (notification-form :title (:title n)
                                                        :article (:article n)
                                                        :action (str "edit/" slugger)))
      (let [new-slugger (utils/mk-slugger title)]
        (model/update-notification n (merge n {:title title :article article
                                               :slugger new-slugger}))
        (response/redirect (str "/notifications/view/" new-slugger))))
    (response/not-found "Notification not found")))

(h/defsnippet notification-snippet "public/html/notification-view.html"
  [:div.notification-content] [title date content]
  [:div#notification-content-view] (h/content content)
  [:h1#notification-title] (h/content title)
  [:span#notification-date] (h/content date))

(defn view-notification [slugger]
  (if-let [n (model/get-notification slugger)]
    (->> (:article n) utils/md->snippet
         (notification-snippet (:title n) (:date n))
         (base-page (str "View | " (:title n))))
    (response/not-found "Notification not found")))

(h/defsnippet notification-list "public/html/notification-list.html"
  [:div.notification-wrapper] [notifications]
  [:div.notification-item]
  (h/clone-for [{:keys [title date article slugger]} notifications]
               [:a.notification-title]
               (h/do-> (h/content title)
                       (h/set-attr :href (str "/notifications/view/" slugger)))
               [:span.notification-date]
               (h/content date)
               [:div.notification-content]
               (h/content (utils/md->snippet (utils/first-n-lines article 3)))
               [:a.edit-link]
               (when (get (:roles (friend/current-authentication)) "admin")
                 (h/set-attr :href (str "/admin/notifications/edit/" slugger)))
               [:a.delete-link]
               (when (get (:roles (friend/current-authentication)) "admin")
                 (h/set-attr :href (str "/admin/notifications/delete/" slugger)))))

(defn notifications [& {:keys [cnt] :or {cnt 25}}]
  (base-page "Notifications" (-> cnt model/fetch-notifications
                                 notification-list)))

(h/defsnippet login-form "public/html/login-form.html"
  [:div#content-bg] [& {:keys [errors]}])

(defn login [& {:keys [request username password]}]
  (base-page "Login" (login-form)))

(defn register [request params]
  (let [x (forms/login-form
           params request
           (fn [{:keys [username firstname lastname
                        password person-number]}]
             (model/add-user {:username username
                              :password password
                              :person-number person-number
                              :firstname firstname
                              :lastname lastname})
             true))]
    (if (= x true)
      (response/redirect "/")
      (base-page "Register" x))))

(h/defsnippet exercise-form "public/html/exercise-form.html"
  [:div#content-bg] [& {:keys [errors]}])

(defn admin-add-exercise [request params multipart-params]
  (if (= request :get)
    (base-page "Add Exercise" (exercise-form))
    (let [num-questions (Integer/parseInt (:count params))
          name (:name params)
          due (utils/datetime-reformat-iso (:due params))
          slugger (utils/mk-slugger name)
          exercise-obj (model/add-exercise {:name name :due due
                                            :slugger slugger})
          exercise-id (:_id exercise-obj)
          exercise-dir (str "resources/public/exercises/" slugger)]
      (fs/mkdir exercise-dir)
      (doseq [i (range 1 (inc num-questions))]
        (let [question-file (get multipart-params (str "file-" i))
              question-spec (get multipart-params (str "spec-" i))
              target-file (->> (:filename question-file "")
                               utils/space->underscore
                               (str exercise-dir "/"))]
          (println question-file)
          (when (> (:size question-file) 0)
            (fs/copy (:tempfile question-file) target-file))
          (model/add-question {:spec question-spec
                               :attach (when (> (:size question-file) 0)
                                         {:filename target-file
                                          :name (:filename question-file)
                                          :link (str "/exercises/" slugger "/"
                                                     (utils/space->underscore
                                                      (:filename question-file)))
                                          :mime (:content-type question-file)})
                               :exercise exercise-id
                               :number i})))
      (response/redirect (str "/exercises/view/" slugger)))))

(h/defsnippet exercise-view "public/html/exercise-view.html"
  [:div#content-bg] [questions]
  [:div.list-group]
  (h/clone-for [q questions]
    [:h4] (h/content (str "Question " (:number q)))
    [:div.spec] (h/content (utils/md->snippet (:spec q)))
    [:a.attach] (when-let [{{name :name link :link} :attach} q]
                  (h/do-> (h/content name)
                          (h/set-attr :href link)))
    [:textarea.sol] (h/set-attr :name (str "sol-" (:number q)))
    [:img.question] (when (utils/image? (:mime (:attach q)))
                      (h/set-attr :src (:link (:attach q)))))
  [:input#exercise-id] (h/set-attr :value (str (:exercise (first questions))))
  [:input#exercise-count] (h/set-attr :value (count questions)))

(defn display-exercise [slugger]
  (let [exercise (model/fetch-exercise slugger)
        questions (model/fetch-questions exercise)]
    (base-page (:name exercise) (exercise-view questions))))

(defn submit-exercise [{:keys [count exercise] :as params}]
  (let [count (Integer/parseInt count)
        ex    (model/fetch-exercise exercise)
        curtime (utils/current-date-time)]
    (if (< curtime (:due ex))
      (do (model/write-submission
           {:user (:username (friend/current-authentication))
            :solutions (for [i (range 1 (inc count))]
                         {:question i  :answer (get params (str "sol-" i))})
            :submission-time (utils/current-date-time)
            :exercise (:_id ex)})
          (response/redirect "/exercises"))
      "Submission is closed!")))

(h/defsnippet exercise-list "public/html/exercise-list.html"
  [:div.exercises] [exercises]
  [:tbody :tr]
  (h/clone-for
   [{:keys [name due slugger sol]} exercises]
   [:td.name :a] (h/do-> (h/set-attr :href (str "/exercises/view/" slugger))
                         (h/content name))
   [:td.due] (h/content due)
   [:td.sol] (h/content (if sol "solution" "not provided"))
   [:td.grade] (h/content "Not implemented")))

(defn exercises []
  (base-page "Exercises" (exercise-list (model/fetch-exercises))))
