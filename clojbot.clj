(ns clojbot
  (:import [java.net Socket])
  (:use [clojure.contrib.except :only (throw-if)]
        [clojure.contrib.logging]))

(def FREENODE "irc.freenode.net")
(def default-port 6667)

(defstruct message :type :channel :sender :login :hostname :message :target :action)
(defstruct bot :hostname :port :nick :login :finger)

(defn connection-args [bot]
  (let [hostname (get bot :hostname) port (get bot :port default-port)]
    (throw-if (nil? hostname) IllegalArgumentException "hostname must be set for bot")
    [hostname port]))

(defn connect [bot]
  (let [[hostname port] (connection-args bot)]
    (debug (format "hostname: %s, port %d" hostname port))
    (new Socket hostname port)))
  
(defn- default-config []
  (struct bot "localhost" default-port "clojbot" "clojbot" "don't finger me!"))


