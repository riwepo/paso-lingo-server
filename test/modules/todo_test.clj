(ns modules.todo-test
  (:use [clojure.test]
        [com.rpl.rama]
        [com.rpl.rama path])
  (:require [com.rpl.rama.test :as rtest]
            [modules.todo2 :as todo]))

(deftest todo-module-test
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc todo/TodoAppModule {:tasks 4 :threads 2})
    (let [module-name (get-module-name todo/TodoAppModule)
          todo-depot (foreign-depot ipc module-name "*todo-depot")
          todos (foreign-pstate ipc module-name "$$todos")
          completed-stats (foreign-pstate ipc module-name "$$completed-stats")]
      (foreign-append! todo-depot (todo/->NewTodo "alice" "todo1"))
      (foreign-append! todo-depot (todo/->NewTodo "alice" "todo2"))
      (foreign-append! todo-depot (todo/->NewTodo "alice" "todo3"))
      (foreign-append! todo-depot (todo/->CompleteTodo "alice" 1 1000))
      (is (= [{:todo "todo1"} {:todo "todo2" :completed-at 1000} {:todo "todo3"}]
             (foreign-select-one (keypath "alice") todos)))
      (is (= 1 (foreign-select-one (keypath "alice") completed-stats)))

      (foreign-append! todo-depot (todo/->ReorderTodo "alice" 2 0))
      (foreign-append! todo-depot (todo/->CompleteTodo "alice" 0 2000))
      (is (= [{:todo "todo3" :completed-at 2000} {:todo "todo1"} {:todo "todo2" :completed-at 1000}]
             (foreign-select-one (keypath "alice") todos)))
      (is (= 2 (foreign-select-one (keypath "alice") completed-stats)))

      (foreign-append! todo-depot (todo/->ReorderTodo "alice" 10 0))
      (is (= [{:todo "todo3" :completed-at 2000} {:todo "todo1"} {:todo "todo2" :completed-at 1000}]
             (foreign-select-one (keypath "alice") todos))))))
