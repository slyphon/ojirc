(ns ojbot.output
  (:use 
     [clojure.contrib.duck-streams  :only (reader writer)]
     [clojure.contrib.except        :only (throw-if)]
     [clojure.contrib.str-utils     :only (re-split)]
     [clojure.contrib.seq-utils     :only (flatten)]
     [clojure.contrib.logging]
     [ojbot.common]))
     
(defn send-lines [{{:keys [outq]} :net :as bot} & lines]
  (debug (str "adding lines to outq: " lines))
  (doseq [line lines] (.put outq line)))

(defn- trim-line [line]
  (let [maxlen (- ojbot.common/*max-line-length* 2)]
  (if (> (.length line) maxlen)
    (.substring line 0 maxlen)
    line)))

(defn output-loop [{wrtr :writer :keys [outq] :as net}]
  (debug "entering output loop")
  (binding [*out* @wrtr]
    (doseq [line (lazify-q outq)]
      (print (str (trim-line line) *CRLF*))
      (flush)
      (info (str ">>> " line)))))


