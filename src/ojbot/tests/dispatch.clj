(ns ojbot.tests.dispatch
  (:use 
     [clojure.test]
     [ojbot dispatch])
  (:require 
     [ojbot.input    :as input]))

; (fn [_] (dosync (alter cb-called conj :default)))

(let [cb-called (atom [])
      empty-hash {}
      existing-hash { [:PING nil] [:foo]}]
  (letfn [(cb-fn [_] (dosync (alter cb-called conj :cb-fn)))]
    (deftest test-update-listeners-hash
      (is (= (update-listeners-hash empty-hash [:PING nil] cb-fn) {[:PING nil] [cb-fn]}))
      (is (= (update-listeners-hash existing-hash [:PING nil] cb-fn)
             {[:PING nil] [:foo cb-fn]}))
    )))


