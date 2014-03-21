(ns cse341.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            [ring.util.response :as response]
            [cse341.pages :as pages]
            [cse341.model :as model]))

(defroutes admin-routes
;  (GET "/notifications" [] (pages/admin-all-notifications))
  (GET "/notifications/add" [] (pages/admin-add-notification :request :get))
  (POST "/notifications/add" {{title :title article :article tags :tags} :params}
        (pages/admin-add-notification :request :post :title title
                                      :article article :tags tags))
  (GET "/notifications/edit/:slugger" [slugger]
       (pages/admin-edit-notification slugger :request :get))
  (POST "/notifications/edit/:slugger" {{title :title article :article tags :tags slugger :slugger} :params}
        (pages/admin-edit-notification slugger :request :post :title title :article article
                                       :tags tags))
  (GET "/notifications/delete/:slugger" [slugger] (do (model/delete-notification slugger)
                                                      (response/redirect "/notifications")))
  (GET "/exercises/add" {:keys [params multipart-params]}
       (pages/admin-add-exercise :get params multipart-params))
  (POST "/exercises/add" {:keys [params multipart-params]}
        (pages/admin-add-exercise :post params multipart-params))
  (GET "/exercises/list" [] ))

(defroutes user-routes
  ;("/")
  )

(defroutes exercise-routes
  (GET "/" [] (pages/exercises))
  (GET "/view/:slugger" [slugger] (pages/display-exercise slugger))
  (POST "/submit" params (pages/submit-exercise params)))

(defroutes app-routes
  (context "/admin" []
           (friend/wrap-authorize admin-routes #{"admin"}))
  (context "/user" request
           (friend/wrap-authorize user-routes #{"user" "admin"}))
  (context "/exercises" request
           (friend/wrap-authorize exercise-routes #{"user" "admin"}))
  (GET "/notifications/view/:slugger" [slugger]
       (pages/view-notification slugger))
  (GET "/" [] (pages/notifications :cnt 10))
  (GET "/notifications" [] (pages/notifications :cnt 100))
  (GET "/register" params (pages/register :get params))
  (POST "/register" params (pages/register :post params))
  (GET "/login" [] (pages/login))
   (GET "/logout" req
    (friend/logout* (response/redirect (str (:context req) "/"))))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site
   (friend/authenticate
    app-routes
    {:allow-anon? true
     :login-uri "/login"
     :default-landing-uri "/"
     :credential-fn #(when-let [u (model/get-user %)]
                       (when (creds/bcrypt-verify (:password %) (:password u))
                         {:username (:username u) :roles (set (:role u))}))
     :workflows [(workflows/interactive-form)]})))
