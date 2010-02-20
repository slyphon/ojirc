(ns ojbot.core
  (:import 
     [java.net Socket InetSocketAddress]
     [java.util.concurrent LinkedBlockingQueue])
  (:use 
     [clojure.contrib.except        :only (throw-if)]
     [clojure.contrib.duck-streams  :only (reader writer)]
     [clojure.contrib.logging]
     [clojure.contrib.seq-utils     :only (flatten)]))

(defonce *kill-token* ::KILL-YOURSELF)
(defonce *CRLF* "\r\n")
(defonce *bot-version* "ojbot-0.0.1")

(def config-defaults 
  {:tag ::Config
   :hostname "localhost" 
   :port 6667
   :nick "clojbot"
   :login "clojbot"
   :finger "don't finger me!"})


(defstruct message    :tag :type :channel :sender :login :hostname :message :target :action)
(defstruct config     :tag :hostname :hostpass :port :nick :login :finger)
(defstruct bot-struct :tag :config :socket :listeners :channels)
(defstruct net-state  :tag :socket :outq :outq-fill :writer :reader)

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
  (let [sock (Socket.) lbq (LinkedBlockingQueue.)]
    (struct-map net-state 
                :tag        ::NetState
                :connected  (atom false)
                :socket     (atom sock)
                :local-addr (atom nil)
                :outq       lbq 
                :writer     (atom nil)
                :reader     (atom nil)
                :out-future (atom nil)
                :in-future  (atom nil)))) 


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
  (.connect socket (inet-sock-address conf))
  socket)

; takes a j.u.c.BlockingQueue and turns it into a lazy seq. if the
; token pulled off the queue is the *kill-token*, then the seq ends
(defn- lazify-q [q]
  (take-while #(not= *kill-token* %) (repeatedly #(.take q))))

(defn- output-loop [{wrtr :writer :keys [outq] :as net}]
  (binding [*out* @wrtr]
    (doseq [line (lazify-q outq)]
      ; XXX: trim lines that are over-length
      ; (if (> (.length line) clojbot.input/*max-line-length*)
      (print (str line *CRLF*))
      (flush)
      (debug (str ">>> " line)))))


(defn send-lines [{{:keys [outq]} :net :as bot} & lines]
  (doseq [line lines] (.put outq line)))

(defn- handle-login [{:keys [hostpass nick login finger]} bot]
  (when-not (nil? hostpass) (send-lines (str "PASS " hostpass)))
  (send-lines bot (str "NICK " nick)
                  (str "USER " login " 8 * :" *bot-version*)))

(defn connect [{:keys [net config] :as bot}]
  (let [socket (net :socket)]
    (debug (str "_net_ " net " _config_ " config " _socket_ " @socket))
    (connect-sock @socket @config)
    (reset! (net :connected) true)
    (reset! (net :reader) (reader @socket))
    (reset! (net :writer) (writer @socket))
    (reset! (net :local-addr) (.getLocalAddress @socket))
    (reset! (net :out-future) (future (output-loop net))))
  (handle-login config bot)
  bot)

(defn- disconnect-sock [sock]
  (if (and (instance? Socket sock) 
              (not (.isClosed sock))) 
        (do
          (debug (str "disconnecting sock: " sock))
          (.close sock))))

(defn- stop-outputter [{:keys [outq out-future]}]
  (doto outq
    (.clear)
    (.put *kill-token*))
  @out-future)  ; wait for future to stop

(defn disconnect [{net :net sock @(net :socket) :as bot}]
  (disconnect-sock sock)
  (stop-outputter net)
  (reset! (net :socket) nil)
  (reset! (net :connected) false)
  bot)

(defn mainloop 
  "iterates over reponses from the server in a loop and takes care of dispatching
  the resulting messages"
  ([{{:keys [reader] :as net} :net :as bot}] ))

