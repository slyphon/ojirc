(ns clojbot
  (:import (org.jibble.pircbot PircBot DccChat DccFileTransfer DccManager 
                               IrcException NickAlreadyInUseException 
                               ReplyConstants User)
           (java.util.concurrent Executors))
  (:gen-class
     :name clojbot.Bot
     :extends org.jibble.pircbot.PircBot
     :state state
     :init init
     :methods [[setName [String] Void]]
     :exposes-methods {setName    setNameSuper
                       setVersion setVersionSuper
                       setLogin   setLoginSuper
                       setFinger  setFingerSuper}))

(def FREENODE "irc.freenode.net")

(defstruct message :bot :type :channel :sender :login :hostname :message :target :action)

(defn private? [msg]
  (nil? (msg :channel)))

(defn create-listeners-registry []
  (let [reg {}]
    (assoc reg 
           ::Join       (ref [])
           ::Message    (ref [])
           ::Action     (ref [])
           ::Connect    (ref [])
           ::Disconnect (ref []))))

(defn -init []
  [[] (ref (create-listeners-registry))])

(defn -setName [this s] (. this setNameSuper s))

(defn -onJoin [this channel sender login hostname]
  (struct message 
          :bot this
          :type ::Join 
          :channel channel 
          :sender sender 
          :login login 
          :hostname hostname))

(defn -onMessage [this channel sender login hostname message] 
  (struct message 
          :bot this
          :type ::Message 
          :channel channel 
          :sender sender 
          :login login 
          :hostname hostname 
          :message message))

(defn -onAction [this sender login hostname target action]
  (struct message
          :bot this
          :type ::Action 
          :sender sender 
          :login login 
          :hostname hostname 
          :target target 
          :action action))

(defn -onConnect [this]
  (struct message
          :bot this
          :type ::Connect))

(defn -onDisconnect [this]
  (struct message
          :bot this
          :type ::Disconnect))

(defn create-freenode-bot []
  (let [bot (clojbot.Bot.)]
    (assoc (bean bot)
           :name "clojbot"
           :finger "where has that finger been!?"
           :login "clojbot")
    bot))
           

(comment
(defn create-bot []
  (proxy [PircBot] []
    (onChannelInfo    [channel user-count topic])
    (onDeop           [channel source-nick source-login source-hostname recipient])
    (onDeVoice        [channel source-nick source-login source-hostname recipient])
    (onFileTransferFinished [transfer e])
    (onFinger         [source-nick source-login source-hostname target])
    (onIncomingChatRequest  [chat])
    (onIncomingFileTransfer [transfer])
    (onInvite         [target-nick source-nick source-login source-hostname channel])
    (onKick           [channel kicker-nick kicker-login kicker-hostname recipient-nick reason])
    (onMode           [channel source-nick source-login source-hostname mode])
    (onNickChange     [channel old-nick login hostname new-nick])
    (onNotice         [source-nick source-login source-hostname target notice])
    (onOp             [channel source-nick source-login source-hostname recipient])
    (onPart           [channel sender login hostname])
    (onPrivateMessage [sender login hostname message])
    (onQuit           [source-nick source-login source-hostname reason])
    (onTopic          [channel topic setBy date changed])
    (onUnknown        [line])
    (onUserList       [channel users])
    (onUserMode       [target-nick source-nick source-login source-hostname mode])
    (onVersion        [source-nick source-login source-hostname target])
    (onVoice          [channel source-nick source-login source-hostname mode])))

)

