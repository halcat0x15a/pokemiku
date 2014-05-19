(ns pokemiku.core
  (:require [clojure.string :as string]
            [clojure.java.io :refer [reader writer]])
  (:import [javax.sound.midi MidiSystem MidiDevice$Info ShortMessage SysexMessage MetaMessage Sequence Track MidiEvent]
           [javax.xml.bind DatatypeConverter]
           [com.atilika.kuromoji Token]
           [com.atilika.kuromoji.unidic.extended Tokenizer Tokenizer$Builder]))

(defn print-devices []
  (->> (MidiSystem/getMidiDeviceInfo)
       (map-indexed (fn [i ^MidiDevice$Info info]
                      (println (format "[%d] %s" i (.getName info)))))
       dorun))

(def syllable #".[ァィゥェォャュョ]?ー?")

(defn syllabicate [s]
  (let [m (re-matcher syllable s)]
    (take-while identity (repeatedly #(when (re-find m) (re-groups m))))))

(def exdata (map #(DatatypeConverter/parseHexBinary (format "F0437909110A00%02XF7" %)) (range)))

(def syllabary
  (merge (zipmap (syllabicate "アイウエオカキクケコガギグゲゴキャキュキョギャギュギョサスィスセソザズィズゼゾシャシシュシェショジャジジュジェジョタティトゥテトダディドゥデドテュデュチャチチュチェチョツァツィツツェツォナニヌネノニャニュニョハヒフヘホバビブベボパピプペポヒャヒュヒョビャビュビョピャピュピョファフィフュフェフォマミムメモミャミュミョヤユヨラリルレロリャリュリョワウィウェウォン") exdata)
         (zipmap (syllabicate "ヅァヅィヅヅェヅォ") (drop 26 exdata))
         (zipmap (syllabicate "ヰヱヲ") (drop 120 exdata))))

(defonce ^Tokenizer tokenizer (.build (Tokenizer$Builder.)))

(defn add [^Track track ^Token token pitch volume tempo start]
  (let [features (vec (.getAllFeaturesArray token))
        syllables (syllabicate (get features 9 ""))
        accents (->> (string/split (get features 23 "") #",")
                     (map read-string)
                     (set))]
    (reduce (fn [n [^String s i]]
              (let [pitch (if (accents (inc i)) (inc pitch) pitch)
                    long? (.contains s "ー")
                    n' (+ n tempo (if long? tempo 0))]
                (when-let [data (syllabary (if long? (subs s 0 (dec (count s))) s))]
                  (doto track
                    (.add (MidiEvent. (SysexMessage. data (count data)) n))
                    (.add (MidiEvent. (ShortMessage. ShortMessage/NOTE_ON pitch volume) (inc n)))
                    (.add (MidiEvent. (ShortMessage. ShortMessage/NOTE_OFF pitch volume) n'))))
                n'))
            start
            (map vector syllables (range)))))

(defn play [text n]
  (with-open [device (MidiSystem/getMidiDevice (aget (MidiSystem/getMidiDeviceInfo) n))
              sequencer (MidiSystem/getSequencer false)]
    (.open device)
    (.open sequencer)
    (let [sequence (Sequence. Sequence/PPQ 240)
          track (.createTrack sequence)]
      (.setReceiver (.getTransmitter sequencer) (.getReceiver device))
      (.add track (MidiEvent. (MetaMessage. 0x51 (byte-array [0x07 0xa1 0x20]) 3) 0))
      (reduce (fn [n token] (add track token 69 127 180 n)) 0 (.tokenize tokenizer text))
      (doto sequencer
        (.setSequence sequence)
        (.start)))
    (while (.isRunning sequencer) (Thread/sleep 100))))

(defn kana [c]
  (let [i (int c)]
    (if (<= (int \ぁ) i (int \ん))
      (char (+ i (- (int \ァ) (int \ぁ))))
      c)))
