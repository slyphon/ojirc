(ns ojbot.common
  (:use 
     [clojure.contrib.except        :only (throw-if)]
     [clojure.contrib.logging]
     [clojure.contrib.str-utils     :only (re-split)]
     [clojure.contrib.seq-utils     :only (flatten)]))

(defonce *max-line-length* 512)

(defn split-spaces [s] (re-split #" " s))

(defonce *kill-token* ::KILL-YOURSELF)
(defonce *CRLF* "\r\n")
(defonce *bot-version* "ojbot-0.0.1")


