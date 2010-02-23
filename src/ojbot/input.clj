(ns ojbot.input
  (:require [ojbot output])
  (:use
     [clojure.contrib.duck-streams  :only (reader writer)]
     [clojure.contrib.except        :only (throw-if)]
     [clojure.contrib.str-utils     :only (re-split)]
     [clojure.contrib.seq-utils     :only (flatten)]
     [clojure.contrib.logging]

     [ojbot.common     :only (*CRLF* split-spaces)]
     [ojbot.responses  :only (REPLY_CODE CODE_REPLY rpl-code=)]))

(defstruct nick-info-struct       :tag :nick :login :hostname)
(defstruct client-message-struct  :tag :nick-info :command :params)

(defn parse-nick-info 
  "parses a nick-info string in the form ':nick!~login@hostname' and returns a
  nick-info-struct with the appropriate values set returns nil if the string doesn't
  fit the pattern"
  [s]
  (let [[_ nick login host] (first (re-seq #"^:([^ ]+)!~([^ ]+)@([^ ]+)" s))]
    (if (nil? nick)
      nil
      (struct nick-info-struct ::NickInfo nick login host))))

(defn- parse-client-command [nick-str cmd-str trailing]
  (let [[command & params] (split-spaces cmd-str)
        params             (conj (vec params) trailing)
        command            (.toUpperCase command)]
    (struct-map client-message-struct
                :tag        ::ClientMessage
                :nick-info  (parse-nick-info nick-str)
                :command    command
                :params     params)))

;(defn- parse-server-command [

(defn parse-command
  "splits a line received from the server into the nick-info-string, the 
  command proper, and any params the command might have"
  [s]
  (let [[_ nick-str cmd-str trailing] (first (re-seq #"^:([^:]+):(.*)$" s))]
    (if nick-str
      (parse-client-command nick-str cmd-str trailing))))


(defn- prefix? [s]
  (.startsWith s ":"))

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
    (let [[sender-info command] (take 2 (split-spaces line))
          nick-info (parse-nick-info sender-info)]
;      (if nick-info


    
   )))

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
 
