(ns ojbot.dispatch
  (:use
     [clojure.contrib.except    :only (throw-if)]
     [ojbot.common]
     [ojbot.responses           :only (REPLY_CODE CODE_REPLY rpl-code=)]
     [clojure.contrib.logging]
))


(defn dispatch-loop [{:keys [dispatchq] :as bot}]
  (debug "entering dispatch-loop")
  (doseq [msg (lazify-q @dispatchq)]))


