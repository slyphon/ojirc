(ns ojbot.errors)


(gen-class 
  :name ojbot.errors.OJBotError
  :extends java.lang.RuntimeException)

(gen-class 
  :name ojbot.errors.UnparseableMessageError
  :extends java.lang.RuntimeException)

