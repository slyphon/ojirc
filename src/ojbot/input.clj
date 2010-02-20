(ns ojbot.input
  (:use
     [clojure.contrib.except        :only (throw-if)]
     [clojure.contrib.logging]
     [clojure.contrib.seq-utils     :only (flatten)]))

(defonce *max-line-length* 512)

(defn handle-line
  "parses the responses from the server and returns the appropriate
   message struct "
  ([line]
   (info (str "<<< " line))


)

