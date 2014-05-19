(ns pokemiku
  (:gen-class)
  (:require [pokemiku.core :as core]
            [clojure.java.io :refer [reader]]
            [clojure.core.async :refer [go <! >! chan]])
  (:import [java.io BufferedReader]
           [java.net InetSocketAddress InetAddress ServerSocket Socket]))

(defn -main []
  (core/print-devices)
  (let [n (read-string (read-line))
        text (chan)
        server (doto (ServerSocket.)
                 (.bind (InetSocketAddress. 7888)))]
    (core/play "サーバーを起動します" n)
    (go (while true (core/play (<! text) n)))
    (while true
      (let [client (.accept server)]
        (core/play "接続しました" n)
        (go
          (with-open [^Socket client client]
            (binding [*in* (reader (.getInputStream client) :encoding "UTF-8")]
              (while true (>! text (read-line))))))))))
