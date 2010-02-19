(ns clojbot
  (:import 
     [java.net Socket InetSocketAddress]
     [java.util.concurrent LinkedBlockingQueue])
  (:use 
     [clojure.contrib.except :only (throw-if)]
     [clojure.contrib.logging]
     [clojure.contrib.seq-utils :only (flatten)]))

(def FREENODE "irc.freenode.net")

(def config-defaults 
  {:tag ::Config
   :hostname "localhost" 
   :port 6667
   :nick "clojbot"
   :login "clojbot"
   :finger "don't finger me!"})


(defstruct message    :tag :type :channel :sender :login :hostname :message :target :action)
(defstruct config     :tag :hostname :port :nick :login :finger)
(defstruct bot        :tag :config :socket :listeners :channels)
(defstruct net-state  :tag :socket :outq :writer :reader)

(defn bot? [x]
  (and (map? x)
       (= (x :tag) ::Bot)))

(defn closed? [sock]
  (.isClosed sock))

(defmacro #^{:private true} struct-hash-map [s hm]
  `(apply struct-map ~s (flatten (vec ~hm))))

(defn create-config 
  "creates a config struct for the bot. with no args returns a default config,
  otherwise expects key/value pairs that will be used to override default values
  for the config"
  ([]
   (struct-hash-map config config-defaults))
  ([& kvpairs] 
   (struct-hash-map config (merge config-defaults (apply hash-map kvpairs)))))

(defn create-net-state []
  (struct-map net-state 
              :tag        ::NetState
              :connected  (atom false)
              :socket     (Socket.)
              :outq       (LinkedBlockingQueue.)
              :out-thread (atom nil)
              :in-thread  (atom nil))) 

; simple for now
(defn create-bot 
  ([] 
   (create-bot (create-config)))
  ([conf] 
   (struct-map bot 
               :tag       ::Bot
               :config    (ref conf)
               :net       (net-state)
               :listeners (ref {})
               :channels  (ref {}))))

(defn- inet-sock-address [conf]
  (let [hostname (get conf :hostname) port (get conf :port)]
    (throw-if (nil? hostname) IllegalArgumentException "hostname must be set for bot")
    (throw-if (nil? port)     IllegalArgumentException "port must be set for bot")
    (InetSocketAddress. hostname port)))

(defn- connect-sock [socket conf]
  (.connect socket (inet-sock-address conf))
  socket)

(defn- do-login-stuff [bot])

(defn connect [bot]
  (connect-sock (bot :socket) @(bot :config))
  bot)

(defn disconnect [bot]
  (let [sock @(bot :socket)] 
    (if (and (instance? Socket sock) 
             (not (.isClosed sock))) (.close sock)))
  bot)

