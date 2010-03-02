(ns ojbot.tests.input
  (:use 
     [ojbot input]
     [clojure.test])
  (:require [ojbot.input :as input]))

(def server-msg-luserclient ":verne.freenode.net 251 fydeaux :this is trailing")


(deftest test-parse-trailing-params-when-trailing-param-present
  (is (= (parse-trailing-params ":trailing params") "trailing params")))

(deftest test-parse-trailing-params-when-no-trailing-param-present
  (is (= (parse-trailing-params "no trailing params") nil)))

(deftest test-parse-leading-params
  (is (= (parse-leading-params "no trailing params") ["no" "trailing" "params"]))
  (is (= (parse-leading-params ":only trailing params") []))
  (is (= (parse-leading-params "mixed :with trailing") ["mixed"])))

(let [ctcp-str      ":VERSION ctcpparam"
      command       "PRIVMSG"
      ctcp-cmd      "VERSION"
      ctcp-params   ["ctcpparam"]
      rval          (input/create-params-struct ctcp-str)]

  (deftest test-create-params-struct-with-ctcp-command
           (is (= (:ctcp-cmd rval)    ctcp-cmd))
           (is (= (:ctcp-params rval) ctcp-params))))

(let [server-nick-str "verne.freenode.net"
      server-nick-info (struct-map nick-info-struct 
                                   :tag ::input/NickInfo
                                   :hostname "verne.freenode.net")
      nick-str "ojbot-nick!~ojbot-login@unaffiliated/ojbot"
      user-nick-info (struct-map nick-info-struct
                                 :tag ::input/NickInfo
                                 :nick "ojbot-nick"
                                 :login "ojbot-login"
                                 :hostname "unaffiliated/ojbot")
      params (struct-map params-struct
                         :tag       ::input/Params
                         :leading   []
                         :trailing  "this is trailing"
                         :all       ["this is trailing"]) ]

  (deftest test-parse-nick-info
    (is (= (parse-nick-info server-nick-str) server-nick-info)
    (is (= (parse-nick-info nick-str) user-nick-info))))

  (deftest test-parse-line-with-server-message
    (let [expected-struct (struct message-struct 
                                  ::input/Message server-nick-info :RPL_LUSERCLIENT 
                                  "fydeaux" params server-msg-luserclient)]
      (is (= (parse-line server-msg-luserclient) expected-struct))))
)

(deftest test-parse-line-unparseable-message
  (let [line "whatthehellisthis"
        r (parse-line line)]
    (is (= (:tag r) ::input/UnparseableMessage))
    (is (= (every? nil? (vals (select-keys r '(:source :cmd :target :params))))))
    (is (= line (r :raw-msg)) )))


(let [ctcp-param-str  "\u0001   \u0001"
      plain-str       "not a ctcp param"
      incomplete-str  "\u0001   "]

  (deftest test-string-ctcp-params?
    (is      (ctcp-params?  ctcp-param-str  ))
    (is (not (ctcp-params?  plain-str       )))
    (is (not (ctcp-params?  incomplete-str  ))))

  (letfn [(params [t] (struct-map params-struct :tag ::input/Params :trailing t))
          (msg [t] (create-message-struct nil nil nil (params t) (str ":" t)))]

    (deftest test-params-ctcp-params?
      (is      (ctcp-params? (params ctcp-param-str  )))
      (is (not (ctcp-params? (params plain-str       ))))
      (is (not (ctcp-params? (params incomplete-str  )))))

    (deftest test-message-ctcp-params?
      (is      (ctcp-params? (msg ctcp-param-str  )))
      (is (not (ctcp-params? (msg plain-str       ))))
      (is (not (ctcp-params? (msg incomplete-str  )))))

    (deftest test-error-ctcp-params?
      (is (thrown? RuntimeException (ctcp-params? :not-ok))) )))

