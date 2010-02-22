(ns ojbot.input
  (:use
     [clojure.contrib.except        :only (throw-if)]
     [clojure.contrib.logging]
     [clojure.contrib.seq-utils     :only (flatten)]))


(defonce *max-line-length* 512)

(defn- prefix? [s]
  (.startsWith s ":"))

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
 
