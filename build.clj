(ns build
  (:require [clojure.tools.build.api :as build-api]))

;; ---------------------------------------------------------
;; Build configuration

(def config
  (let [library-name 'jhimanka/saa
        version (format "1.0.%s" (build-api/git-count-revs nil))]
    {:library-name    library-name
     :main-ns         library-name
     :version         "1.0.0"
     :class-directory "target/classes"
     :project-basis   (build-api/create-basis)
     :jar-file        (format "target/%s-%s.jar" (name library-name) version)
     :uberjar-file       (format "target/%s-%s-standalone.jar" (name library-name) version)}))

;; End of Build configuration

;; ---------------------------------------------------------
;; Build tasks

(defn clean
  "Remove the given directory.
  If directory path nil, delect `target` directory used to compile build artefacts"
  [directory]
  (build-api/delete {:path (or (:path directory) "target")}))

(defn jar [_]
  (let [{:keys [class-directory jar-file library-name project-basis version]} config]
    (build-api/write-pom {:class-dir class-directory
                          :lib       library-name
                          :version   version
                          :basis     project-basis
                          :src-dirs  ["src"]})
    (build-api/copy-dir {:src-dirs   ["src" "resources"]
                         :target-dir class-directory})
    (build-api/jar {:class-dir class-directory
                    :jar-file  jar-file})))

(defn uber [_]
  (let [{:keys [class-directory main-ns project-basis uberjar-file]} config]
    (clean nil)
    (build-api/copy-dir {:src-dirs   ["src" "resources"]
                         :target-dir class-directory})
    (build-api/compile-clj {:basis     project-basis
                            :src-dirs  ["src"]
                            :class-dir class-directory})
    (build-api/uber {:class-dir class-directory
                     :uber-file uberjar-file
                     :basis     project-basis
                     :main      main-ns})))

;; End of Build tasks
