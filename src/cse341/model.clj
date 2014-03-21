(ns cse341.model
  (:require [somnium.congomongo :as m]
            [cemerick.friend.credentials :as creds]
            [cse341.utils :as utils]))

(def conn
  (m/make-connection "test" :host "127.0.0.1" :port 27017))

(m/set-connection! conn)

(defn add-user [{:keys [username password role firstname person-number lastname email] :or {role "user"}}]
  (m/insert! :users {:username username
                     :password (creds/hash-bcrypt password)
                     :role role
                     :firstname firstname
                     :lastname lastname
                     :person-number person-number
                     :email email}))

(defn get-user [{:keys [username]}]
  (m/fetch-one :users :where {:username username}))

(defn add-notification [n]
  (m/insert! :notifications (assoc n :date (utils/current-date-time))))

(defn update-notification [n new-n]
  (m/update! :notifications n new-n))

(defn delete-notification [slugger]
  (m/destroy! :notifications {:slugger slugger}))

(defn fetch-notifications [cnt]
  (m/fetch :notifications :limit cnt :sort {:date -1}))

(defn get-notification [slugger]
  (m/fetch-one :notifications :where {:slugger slugger}))

(defn add-exercise [ex]
  (m/insert! :exercises ex))

(defn add-question [q]
  (m/insert! :questions q))

(defn fetch-exercises []
  (m/fetch :exercises :sort {:_id -1}))

(defn fetch-exercise [slugger]
  (m/fetch-one :exercises :where {:slugger slugger}))

(defn fetch-exercise-by-id [id]
  (m/fetch-one :exercises :where {:_id (m/object-id id)}))

(defn fetch-questions [{:keys [_id]}]
  (m/fetch :questions :where {:exercise _id}))

(defn write-submission [sol]
  (m/insert! :submissions sol))
