(ns ojbot.input
  (:require [ojbot errors output])
  (:use
     [clojure.contrib.duck-streams  :only (reader writer)]
     [clojure.contrib.except        :only (throw-if)]
     [clojure.contrib.str-utils     :only (re-split)]
     [clojure.contrib.seq-utils     :only (flatten)]
     [clojure.contrib.logging]

     [ojbot.common     :only (*CRLF* split-spaces)]
     [ojbot.responses  :only (REPLY_CODE CODE_REPLY rpl-code=)]))

(defstruct nick-info-struct   :tag :nick :login :hostname)
(defstruct message-struct     :tag :source :cmd :target :params :raw-msg)

; (re-matches #"^([^:]+)?(?:[:](.*))?$" s)

(defn parse-leading-params [s]
  (let [ntp (rest (re-find #"^([^:]+):?" s))]
    (if (seq ntp) (vec (split-spaces (first ntp))) []))) ; (seq ntp) is equivalent to (not (empty? x))

(defn parse-trailing-params [s]
  (vec (rest (re-matches #":(.*)$" s))))

(defn parse-params
  "takes a param string, possibly containing a 'trailing' param, and returns a vector
  of the params"
  ([s] (into (parse-leading-params s) (parse-trailing-params s))))

(defn- to-i [s]
  (try
    (Integer/parseInt s 10)
    (catch NumberFormatException _ nil)))

(defn- to-cmd-kw [s]
  (let [code-int (to-i s)
        code-kw  (CODE_REPLY code-int)]
    (if code-int
      code-kw
      (keyword (.toUpperCase s)))))

(defn create-nick-info-struct 
  ([nick login host] (struct nick-info-struct ::NickInfo nick login host))
  ([host] (struct nick-info-struct ::NickInfo nil nil host)))

(defn create-message-struct
  "when called with all args, creates a ::Message tagged struct with all relevant information,
  when called with only a single arg, creates an ::UnparseableMessage tagged struct
  with only the :raw-msg attribute set
  "
  ([source cmd target params raw-msg] 
    (struct message-struct ::Message source cmd target params raw-msg))
  ([raw-msg] 
    (struct-map message-struct :tag ::UnparseableMessage :raw-msg raw-msg)))

(defn parse-nick-info 
  "parses a nick-info string in the form ':nick!~login@hostname' and returns a
  nick-info-struct with the appropriate values set returns nil if the string doesn't
  fit the pattern"
  ([s] (let [[_ nick login host] (re-find #"^(?:([^ ]+)!~([^ ]+)@)?([^ ]+)" s)] 
         (create-nick-info-struct nick login host))))

(defn- do-auto-nick-change [{:keys [nick] :as config} bot]
  (dosync (ojbot.output/send-lines (str "NICK " (alter nick #(str % "_"))))))

(defn handle-login [{:keys [hostpass nick login finger] :as config} {:keys [net] :as bot}]
  (when-not (nil? hostpass) (ojbot.output/send-lines (str "PASS " hostpass)))
  (ojbot.output/send-lines bot (str "NICK " nick)
                  (str "USER " login " 8 * :" ojbot.common/*bot-version*))

  (let [r @(net :reader)]
    (debug (str "reader: " r))
    (loop [line (.readLine r)]
      (throw-if (nil? line) RuntimeException "disconnected while trying to log in")

      (info (str "<<< " line))
      (let [[prefix code-str & more] (split-spaces line)
            code (Integer/decode code-str)]
        (cond
          (rpl-code= :RPL_MYINFO code) true
          ; XXX: need to handle this nick situation, make sure we don't add thousands of '_'s
          (rpl-code= :ERR_NICKNAMEINUSE code) (do-auto-nick-change config bot) 
          true (recur (.readLine r)))))))


(defn parse-line 
  "parses the responses from the server and returns the appropriate
   message struct "
  ([line]
    (let [[_ sender-str cmd-str target param-str] (re-matches #"^:?([^ ]+) ([0-9A-Z]+) ([^ ]+) (.*)$" line)] 
      (spy (prn-str sender-str cmd-str target param-str))
      (if sender-str
        (create-message-struct (spy (parse-nick-info sender-str)) (to-cmd-kw cmd-str) target (parse-params param-str) line)
        (create-message-struct line)))))

(defn mainloop 
  "iterates over reponses from the server in a loop and takes care of dispatching
  the resulting messages"
  [{:keys [net] :as bot}] 

  (let [r @(:reader net)]
    (debug (str "reader: " r))
    (loop [line (.readLine r)]
      (if (not (nil? line))
        (do
          (info (str "<<< " line))
          (parse-line line)
          (recur (.readline r)))
        ;; XXX: Handle disconnect case here!
        ))))
 
