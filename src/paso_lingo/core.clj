(ns paso-lingo.core
  (:require [rama.depot :as depot]
            [rama.server :as server]))

;; Define a depot for user memberships
(def memberships-depot
  (depot/create-depot
    {:name "memberships"
     :tables {:users {:primary-key :id
                      :columns {:id :uuid
                                :email :string
                                :role :string}}}
     :streams {:events {:columns {:user-id :uuid
                                  :event-type :string
                                  :timestamp :instant}}}}))

;; Define Rama app with depots
(def app
  (server/create-app
    {:depots [memberships-depot]}))

(defn -main [& _]
  (server/start app {:port 8080})
  (println "Paso Lingo server running on port 8080"))