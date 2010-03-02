(ns user
  (:use 
     [clojure.stacktrace]
     [clojure.contrib.repl-utils]
     [clojure.contrib.pprint  :only (pprint pp)]
     [clojure.stacktrace      :only (print-cause-trace)]
     [clojure.test            :only (run-tests)])
  (:require
     [ojbot.common  :as common]
     [ojbot.core    :as core]
     [ojbot.input   :as input]))


(defn reload []
  (use :reload-all 'user))

