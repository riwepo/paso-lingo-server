(ns modules.todo
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer [termval merge-termval must keypath local-transform>]]))


(defrecord NewTodo [user-id text])
(defrecord CompleteTodo [user-id index time-millis])

(defmodule TodoAppModule
  [setup topologies]
  (declare-depot setup *todo-depot (hash-by :user-id))

  (let [s (stream-topology topologies "todos")]
    (declare-pstate
      s
      $$todos
      {String [(fixed-keys-schema
                 {:todo         String
                  :completed-at Long})]})
    (declare-pstate s $$completed-stats {String Long})

    (<<sources s
      (source> *todo-depot :> *data)
      (<<subsource *data
                   (case> NewTodo :> {:keys [*user-id *text]})
                   (local-transform> [(keypath *user-id) NIL->VECTOR AFTER-ELEM (termval {:todo *text})]
                     $$todos)

                   (case> CompleteTodo :> {:keys [*user-id *index *time-millis]})
                   (local-transform> [(must *user-id *index) :completed-at (termval *time-millis)]
                     $$todos)
                   (local-transform> [(keypath *user-id) (nil->val 0) (term inc)] $$completed-stats)))))


(comment
  (require '[clojure.repl :refer [source]])
  (require 'com.rpl.rama)
  (source declare-depot)

  (+ 1 2)
  (doc declare-depot))
