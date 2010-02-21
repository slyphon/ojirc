(ns ojbot.core
  (:import 
     [java.net Socket InetSocketAddress]
     [java.util.concurrent LinkedBlockingQueue])
  (:use 
     [ojbot.input]
     [clojure.contrib.except        :only (throw-if)]
     [clojure.contrib.duck-streams  :only (reader writer)]
     [clojure.contrib.str-utils     :only (re-split)]
     [clojure.contrib.logging]
     [clojure.contrib.seq-utils     :only (flatten)]))

(defonce *kill-token* ::KILL-YOURSELF)
(defonce *CRLF* "\r\n")
(defonce *bot-version* "ojbot-0.0.1")

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
  (let [sock (Socket.) lbq (LinkedBlockingQueue.)]
    (struct-map net-state 
                :tag        ::NetState
                :connected  (ref false)
                :socket     (ref sock)
                :local-addr (ref nil)
                :outq       lbq 
                :writer     (ref nil)
                :reader     (ref nil)
                :out-future (ref nil)
                :in-future  (ref nil))))


; simple for now
(defn create-bot 
  ([] 
   (create-bot (create-config)))
  ([conf] 
   (struct-map bot-struct
               :tag       ::Bot
               :config    (ref conf)
               :net       (create-net-state)
               :listeners (ref {})
               :channels  (ref {}))))

(defn- inet-sock-address [{:keys [hostname port]}]
  (throw-if (nil? hostname) IllegalArgumentException "hostname must be set for bot")
  (throw-if (nil? port)     IllegalArgumentException "port must be set for bot")
  (InetSocketAddress. hostname port))

(defn- connect-sock [socket conf]
  (locking socket
    (.connect socket (inet-sock-address conf)))
  socket)

; takes a j.u.c.BlockingQueue and turns it into a lazy seq. if the
; token pulled off the queue is the *kill-token*, then the seq ends
(defn- lazify-q [q]
  (take-while #(not= *kill-token* %) (repeatedly #(.take q))))

(defn- trim-line [line]
  (let [maxlen (- ojbot.input/*max-line-length* 2)]
  (if (> (.length line) maxlen)
    (.substring line 0 maxlen)
    line)))

(defn- output-loop [{wrtr :writer :keys [outq] :as net}]
  (binding [*out* @wrtr]
    (doseq [line (lazify-q outq)]
      (print (str (trim-line line) *CRLF*))
      (flush)
      (debug (str ">>> " line)))))

(defn send-lines [{{:keys [outq]} :net :as bot} & lines]
  (doseq [line lines] (.put outq line)))

(defn- split-spaces [s] (re-split #" " s))

(defn- handle-login [{:keys [hostpass nick login finger] :as config} {:keys [net] :as bot}]
  (when-not (nil? hostpass) (send-lines (str "PASS " hostpass)))
  (send-lines bot (str "NICK " nick)
                  (str "USER " login " 8 * :" *bot-version*))

  (let [r @(net :reader)]
    (debug (str "reader: " r))
    (loop [line (.readLine r)]
      (info (str "<<< " line))
      (let [[prefix code & more] (split-spaces line)]
        (cond
          (= code "004") true
          (= code "433") (throw RuntimeException "nick already in use")
          true (recur (.readLine r)))))))

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
      (ref-set out-future (future (output-loop net)))))
  ; pircbot handles the login chat before starting the input/output threads
  ; we start the output thread before
  (handle-login @config bot) 
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

     

