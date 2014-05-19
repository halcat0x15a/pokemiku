(defproject pokemiku "0.1.0-SNAPSHOT"
  :repositories [["atilika" "http://www.atilika.org/nexus/content/repositories/atilika-snapshots"]]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [org.twitter4j/twitter4j-stream "3.0.6"]
                 [com.atilika.kuromoji/kuromoji-unidic-extended "0.8-SNAPSHOT"]]
  :global-vars {*warn-on-reflection* true}
  :aot :all
  :main pokemiku)
