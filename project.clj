(defproject saa "0.1.0"
  :description "Finnish Meterological Institute weather forecasts in an intelligible format"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :jvm-opts ["-Dclojure.compiler.direct-linking=true"]
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/data.xml "0.0.8"]
                 [fipp "0.6.14"]
                 [aero "1.1.3"]
                 [org.clojure/tools.cli "0.4.1"]
                 [clj-http "3.9.1"]]
  :main saa.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
