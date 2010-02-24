(ns ojbot.core
  (:import 
     [java.net Socket InetSocketAddress]
     [java.util.concurrent LinkedBlockingQueue])
  (:require
     [ojbot input output])
  (:use 
     [clojure.contrib.duck-streams  :only (reader writer)]
     [clojure.contrib.except        :only (throw-if)]
     [clojure.contrib.str-utils     :only (re-split)]
     [clojure.contrib.seq-utils     :only (flatten)]
     [clojure.contrib.logging]

     [ojbot common]))

(def config-defaults 
  {:tag ::Config
   :hostname "localhost" 
   :port 6667
   :nick "ojbot"
   :login "ojbot"
   :finger "don't finger me!"})

(defstruct message    :tag :type :channel :sender :login :hostname :message :target :action)
(defstruct config     :tag :hostname :hostpass :port :nick :login :finger)
(defstruct bot-struct :tag :config :socket :listeners :channels)
(defstruct net-state  :tag :socket :outq :outq-fill :writer :reader)

(defn bot? [x]
  (and (map? x)
       (= (x :tag) ::Bot)))

(defn connected? [{:keys [net] :as bot}]
  (@net :connected))


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
  (let [sock (Socket.) outq (LinkedBlockingQueue.)]
    (struct-map net-state 
                :tag        ::NetState
                :connected  (ref false)
                :socket     (ref sock)
                :local-addr (ref nil)
                :outq       outq
                :writer     (ref nil)
                :reader     (ref nil)
                :out-future (ref nil))))


; simple for now
(defn create-bot 
  ([] 
   (create-bot (create-config)))
  ([conf] 
   (struct-map bot-struct
               :tag               ::Bot
               :config            (ref conf)
               :net               (create-net-state)
               :dispatchq         (agent nil)
               :dispatch-future   (ref   nil)
               :listeners         (agent {})
               :channels          (agent {}))))

(defn- inet-sock-address [{:keys [hostname port]}]
  (throw-if (nil? hostname) IllegalArgumentException "hostname must be set for bot")
  (throw-if (nil? port)     IllegalArgumentException "port must be set for bot")
  (InetSocketAddress. hostname port))

(defn- connect-sock [socket conf]
  (locking socket
    (.connect socket (inet-sock-address conf)))
  socket)

(defn connect [{:keys [net config] :as bot}]
  (debug (str "net: " net " config " config))
  (dosync
    (let [{:keys [socket connected local-addr out-future]} net
          {rdr :reader wrtr :writer} net]
      (connect-sock @socket @config)
      (ref-set connected  true)
      (ref-set rdr        (reader @socket))
      (ref-set wrtr       (writer @socket))
      (ref-set local-addr (.getLocalAddress @socket))
      (ref-set out-future (future (ojbot.output/output-loop net)))))
  ; pircbot handles the login chat before starting the input/output threads
  ; we start the output thread before
  (ojbot.input/handle-login @config bot) 
  bot)

(defn- disconnect-sock [sock]
  (debug (str "socket: " sock))
  (when-not (nil? sock)
    (locking sock
      (if (and (instance? Socket sock)
                  (not (.isClosed sock)))
            (do
              (debug (str "disconnecting sock: " sock))
              (.close sock)))))
  sock)

(defn- stop-outputter [{:keys [outq out-future]}]
  (doto outq
    (.clear)
    (.put *kill-token*))
  @out-future)  ; wait for future to stop

(defn disconnect [{:keys [net] :as bot}]
  (dosync
    (let [{:keys [socket connected local-addr out-future outq]} net
          {rdr :reader wrtr :writer} net]
      (debug (str "disconnect sock: " @socket))

      (disconnect-sock @socket)
      (stop-outputter net)

      (.close  @rdr)
      (.close  @wrtr)
      (.clear  outq)
      (ref-set rdr        nil)
      (ref-set wrtr       nil)
      (ref-set socket     (Socket.))
      (ref-set local-addr nil)
      (ref-set connected  false)))
  bot)

     

