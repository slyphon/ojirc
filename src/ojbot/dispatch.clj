(ns ojbot.dispatch
  (:use
     [clojure.contrib.except    :only (throw-if)]
     [ojbot.common]
     [ojbot.responses           :only (REPLY_CODE CODE_REPLY rpl-code=)]
     [clojure.contrib.logging])
  (:require 
     [ojbot.input :as input])
)


(defmulti dispatch (fn [bot {:keys [tag cmd] :as msg}] [tag cmd]))

(defmethod dispatch [::input/Message :PRIVMSG] [bot msg])
(defmethod dispatch [::input/Message :PING] [bot msg])


(defn dispatch-msgs [{:keys [dispatchq] :as bot} & messages]
  "add messages to bot's dispatch queue"
  (send dispatchq (fn [dq msgs] (doseq [msg msgs] (.put dq msg)) dq) messages))

(defn stop-dispatch-loop [{:keys [dispatchq dispatch-future] :as bot}]
  (await
    (send dispatchq 
          (fn [dq] (doto dq (.clear) (.put *kill-token*)) dq)))
  @dispatch-future)

(defn dispatch-loop [{:keys [dispatchq listeners] :as bot}]
  (debug "entering dispatch-loop")
  (doseq [msg (lazify-q @dispatchq)]
    (dispatch bot msg)))



