(ns ojbot.dispatch
  (:use
     [clojure.contrib.except    :only (throw-if)]
     [ojbot.common]
     [ojbot.responses           :only (REPLY_CODE CODE_REPLY rpl-code=)]
     [clojure.contrib.logging])
  (:require 
     [ojbot.input :as input])
)


(defmulti dispatch (fn [bot {:keys [tag cmd ctcp-cmd] :as msg}] [tag cmd ctcp-cmd]))

; CTCP protocol is embeddeed in PRIVMSG commands (yeah, thanks)

(defmethod dispatch [::input/Message :PRIVMSG nil]
  "a non-CTCP PRIVMSG"
  [bot msg])

(defmethod dispatch [::input/Message :PRIVMSG :ACTION] 
  "A CTCP ACTION request"
  [bot msg])

(defmethod dispatch [::input/Message :PRIVMSG :PING]
 "a CTCP PING request" 
 [bot msg])

(defmethod dispatch [::input/Message :PRIVMSG :TIME] 
  "a CTCP TIME request"
  [bot msg])

(defmethod dispatch [::input/Message :PRIVMSG :FINGER] 
  "a CTCP FINGER request"
  [bot msg])

(defmethod dispatch [::input/Message :PING nil]   [bot msg])
(defmethod dispatch [::input/Message :JOIN nil]   [bot msg])
(defmethod dispatch [::input/Message :PART nil]   [bot msg])
(defmethod dispatch [::input/Message :NICK nil]   [bot msg])
(defmethod dispatch [::input/Message :QUIT nil]   [bot msg])
(defmethod dispatch [::input/Message :KICK nil]   [bot msg])
(defmethod dispatch [::input/Message :MODE nil]   [bot msg])
(defmethod dispatch [::input/Message :TOPIC nil]  [bot msg])
(defmethod dispatch [::input/Message :NOTICE nil] [bot msg])
(defmethod dispatch [::input/Message :INVITE nil] [bot msg])

(defmethod dispatch [::input/UnparseableMessage nil nil] [bot msg])

(defmethod dispatch :default [bot msg])

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



