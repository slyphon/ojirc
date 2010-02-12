(ns com.slyphon.clojbot.bot
  (:gen-class
     :name com.slyphon.clojbot.ClojBot
     :extends org.jibble.pircbot.PircBot
     :state state
     :init init)
  (:use [clojure.core])
  (:import [org.jibble.pircbot PircBot User IrcException NickAlreadyInUseException]))


(defn -init []
  [[] (atom [])])



