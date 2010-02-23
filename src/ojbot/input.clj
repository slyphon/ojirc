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

(defn handle-line
  "parses the responses from the server and returns the appropriate
   message struct "
  ([line]

   ))

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
          (handle-line line)
          (recur (.readline r)))
        ;; XXX: Handle disconnect case here!
        ))))
 
