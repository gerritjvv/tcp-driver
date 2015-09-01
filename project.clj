(defproject tcp-driver "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :global-vars {*warn-on-reflection* true
                *assert* true}

  :jvm-opts ["-Xmx1g"]

  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :dependencies [
                 [org.clojure/clojure "1.7.0"]
                 [org.apache.commons/commons-pool2 "2.4.2"]])
