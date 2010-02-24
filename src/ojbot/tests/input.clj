(ns ojbot.tests.input
  (:use 
     [ojbot input]
     [clojure.test]))

(def server-msg-luserclient ":verne.freenode.net 251 fydeaux :There are 785 users and 49775 invisible on 23 servers")


(deftest test-parse-trailing-params-when-trailing-param-present
  (is (= (parse-trailing-params ":trailing params") ["trailing params"])))

(deftest test-parse-trailing-params-when-no-trailing-param-present
  (is (= (parse-trailing-params "no trailing params") [])))

(deftest test-parse-leading-params
  (is (= (parse-leading-params "no trailing params") ["no" "trailing" "params"]))
  (is (= (parse-leading-params ":only trailing params") []))
  (is (= (parse-leading-params "mixed :with trailing") ["mixed"])))

(let [server-nick-str "verne.freenode.net"
      server-nick-info (struct-map nick-info-struct 
                                   :tag :ojbot.input/NickInfo
                                   :hostname "verne.freenode.net")
      nick-str "ojbot-nick!~ojbot-login@unaffiliated/ojbot"
      user-nick-info (struct-map nick-info-struct
                                 :tag :ojbot.input/NickInfo
                                 :nick "ojbot-nick"
                                 :login "ojbot-login"
                                 :hostname "unaffiliated/ojbot")]

  (deftest test-parse-nick-info
    (is (= (parse-nick-info server-nick-str) server-nick-info)
    (is (= (parse-nick-info nick-str) user-nick-info))))

  (deftest test-parse-line-with-server-message
    (let [expected-struct (struct message-struct 
                  :ojbot.input/Message server-nick-info :RPL_LUSERCLIENT 
                  "fydeaux" ["There are 785 users and 49775 invisible on 23 servers"] server-msg-luserclient)]
      (is (= (parse-line server-msg-luserclient) expected-struct))))
)

(deftest test-parse-line-unparseable-message
  (let [line "whatthehellisthis"
        r (parse-line line)]
    (is (= (:tag r) :ojbot.input/UnparseableMessage))
    (is (= (every? nil? (vals (select-keys r '(:source :cmd :target :params))))))
    (is (= line (r :raw-msg)) )))

