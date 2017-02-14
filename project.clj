(defproject tcp-driver "0.1.1-SNAPSHOT"
  :description "Java/Clojure TCP Connections done right"
  :url "https://github.com/gerritjvv/tcp-driver"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :global-vars {*warn-on-reflection* true
                *assert* true}

  :javac-options ["-target" "1.8" "-source" "1.8" "-Xlint:-options"]
  :jvm-opts ["-Xmx1g"]

  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :dependencies [
                 [org.clojure/clojure "1.8.0"]

                 [fun-utils "0.6.2"]
                 [org.apache.commons/commons-pool2 "2.4.2"]
                 [prismatic/schema "1.1.3"]]

  :plugins [[jonase/eastwood "0.2.3"]])
