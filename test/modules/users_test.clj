(ns modules.users-test
  (:use [clojure.test]
        [com.rpl.rama]
        [com.rpl.rama path])
  (:require [com.rpl.rama.test :as rtest]
            [modules.users :as users]))

(deftest users-module-test
  (with-open [ipc (rtest/create-ipc)]
    ;; Launch the module with some parallelism
    (rtest/launch-module! ipc users/UsersModule {:tasks 4 :threads 2})
    (let [module-name (get-module-name users/UsersModule)
          users-depot (foreign-depot ipc module-name "*users-depot")
          users-pstate (foreign-pstate ipc module-name "$$users")]

      ;; Insert a new user
      (foreign-append! users-depot
                       (users/->NewUser "alice" :member {:language "en" :questions true}))

      ;; Verify initial state
      (is (= {:role :member
              :preferences {:language "en" :questions true}}
             (foreign-select-one (keypath "alice") users-pstate)))

      ;; Update role
      (foreign-append! users-depot
                       (users/->UpdateUserRole "alice" :admin))
      (is (= {:role :admin
              :preferences {:language "en" :questions true}}
             (foreign-select-one (keypath "alice") users-pstate)))

      ;; Update preferences
      (foreign-append! users-depot
                       (users/->UpdateUserPreferences "alice" {:language "es"}))
      (is (= {:role :admin
              :preferences {:language "es" :questions true}}
             (foreign-select-one (keypath "alice") users-pstate))))))