(ns ojbot.main
  (:gen-class)
  (:use [ojbot common core]))

(defn -main [& args]
  (let [bot (create-bot)]
    (connect bot)))

