(ns cse341.forms
  (:require  [formative.core :as f]
             [formative.parse :as fp]
             [hiccup.core :as hi]
             [net.cgrand.enlive-html :as html]
             [cse341.model :as model]))

(defn form->snippet [form params & {:keys [problems]}]
  (-> (assoc form :values params :problems problems)
      f/render-form hi/html html/html-snippet))

(defmacro def-form-action [form-submit form-spec]
  `(do
     (let [form-name# ~form-spec
           form-snippet# (fn [params# & [problems#]]
                           (form->snippet form-name# params# :problems problems#))]
       (defn ~form-submit [params# method# success-fn#]
         (if (= method# :get)
           (form-snippet# params#)
           (fp/with-fallback #(form-snippet# params# %)
             (success-fn# (fp/parse-params form-name#
                                           (:form-params params#)))))))))

(def-form-action login-form
  {:method "post"
   :action "/register"
   :renderer :bootstrap3-stacked
   :fields [{:name :username :type :input}
            {:name :password :type :password}
            {:name :password-2 :type :password}
            {:name :firstname :type :input}
            {:name :lastname :type :input}
            {:name :person-number :type :input}
            {:name :email :type :email}]
   :validations [[:required [:username :password :firstname :lastname
                             :person-number :email]]
                 [:min-length 6 :password]
                 [:equal [:password :password-2]]
                 [:matches #"([a-z]|[A-Z])+" :username "Must contain only characters"]
                 [:matches #"([a-z]|[A-Z])+" :firstname "Must contain only characters"]
                 [:matches #"([a-z]|[A-Z])+" :lastname "Must contain only characters"]
                 [:matches #"([0-9])+" :person-number "Must be all digits"]]
   :validator (fn [values]
                (when (model/get-user {:username (:username values)})
                  {:keys [:username] :msg (str "Username " (:username values)
                                               " has been used!")}))})
