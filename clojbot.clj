(ns clojbot
  (:import [java.net Socket])
  (:use [clojure.contrib.except :only (throw-if)]
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


(defstruct message  :tag :type :channel :sender :login :hostname :message :target :action)
(defstruct config   :tag :hostname :port :nick :login :finger)
(defstruct bot      :tag :config :socket :listeners :channels)

(defn bot? [x]
  (and (map? x)
       (= (x :tag) ::Bot)))

; simple for now
(defn create-bot [conf]
  (struct-map bot :tag       ::Bot
                  :config    conf
                  :listeners (ref {})
                  :channels  (ref {})))

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

(defn- connection-args [conf]
  (let [hostname (get conf :hostname) port (get conf :port default-port)]
    (throw-if (nil? hostname) IllegalArgumentException "hostname must be set for bot")
    [hostname port]))
 
(defmulti connect "connect the bot to its configured server" :tag)

(defmethod #^{:private true} connect ::Config [conf]
  (let [[hostname port] (connection-args conf)]
    (debug (format "hostname: %s, port %d" hostname port))
    (new Socket hostname port)))

(defmethod connect ::Bot [b]
  (assoc b :socket (connect (b :config))))

(defmethod connect :default [a]
  (throw IllegalArgumentException (str "don't know how to connect " a)))


(defmulti disconnect "disconnect the bot from its server" class)

(defmethod disconnect clojure.lang.PersistentStructMap [b])

