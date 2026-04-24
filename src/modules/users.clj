(ns modules.users
  (:use [com.rpl.rama]
        [com.rpl.rama path]))

;; Event records
(defrecord NewUser [user-id role preferences])
(defrecord UpdateUserRole [user-id role])
(defrecord UpdateUserPreferences [user-id preferences])

(defmodule UsersModule
  [setup topologies]
  ;; Depot partitioned by user-id
  (declare-depot setup *users-depot (hash-by :user-id))

  (let [s (stream-topology topologies "users")]
    ;; Persistent state: map from user-id -> fixed record
    (declare-pstate
      s
      $$users
      {String (fixed-keys-schema
                {:role #{:guest :member :admin}
                 :preferences (fixed-keys-schema
                                {:language String
                                 :questions Boolean})})})

    ;; Sources: consume depot events
    (<<sources s
      (source> *users-depot :> *data)

      ;; New user creation
      (<<subsource *data
                   (case> NewUser :> {:keys [*user-id *role *preferences]})
                   (local-transform> [(keypath *user-id)
                                      (termval {:role *role
                                                :preferences *preferences})]
                                $$users)

                   ;; Role update
                   (case> UpdateUserRole :> {:keys [*user-id *role]})
                   (local-transform> [(must *user-id) :role (termval *role)]
                                $$users)

                   ;; Preferences update (merge map)
                   (case> UpdateUserPreferences :> {:keys [*user-id *preferences]})
                   (local-transform> [(must *user-id) :preferences (mergeval *preferences)]
                                $$users)))))