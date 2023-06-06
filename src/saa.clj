(ns saa
  (:require
   [fipp.edn :refer (pprint) :rename {pprint fipp}]
   [clojure.string :refer [split join trim]]
   [clojure.java.io :as io]
   [clojure.tools.cli :refer [parse-opts]]
   [aero.core :refer (read-config)]
   [clj-http.client :as client]
   [java-time :as jt])
  (:use [clojure.data.xml])
  (:import [java.io  BufferedReader StringReader])
  (:gen-class))

(defn config []
  (read-config "config.edn"))

(defn apikey [conf]
  (get-in conf [:secrets :apikey]))

(def measurements #{"GeopHeight" "Temperature" "Pressure" "Humidity" "WindDirection" "WindSpeedMS" "WindUMS" "WindVMS"  "WindGust" "DewPoint" "TotalCloudCover" "LowCloudCover" "MediumCloudCover" "HighCloudCover" "PrecipitationAmount" "RadiationGlobalAccumulation" "RadiationNetSurfaceLWAccumulation" "RadiationNetSurfaceSWAccumulation" "RadiationGlobal" "Visibility"})

(def cli-options
  ;; An option with a required argument
  [["-l" "--location PLACE" "Location"
    :default (:defaultlocation (config))
    :validate [#(string? %) "Must be a string"]]
   ;; A non-idempotent option
   ["-m" "--measurement MEASUREMENT" "Measurement"
    :default (:defaultmeasurement (config))
    :validate [#(contains? measurements %) "Not a valid measurement"]]
   ;; A boolean option defaulting to nil
   ["-h" "--help"]])

(def children (mapcat :content))

(defn tagp [pred]
  (comp children (filter (comp pred :tag))))

(defn tag= [tag]
  (tagp (partial = tag)))

(defn attr-accessor [a]
  (comp a :attrs))

(defn attrp [a pred]
  (filter (comp pred (attr-accessor a))))

(defn attr= [a v]
  (attrp a (partial = v)))

(defn olderthan
  "Is this file older than given time?"
  [suspect age]
  (> (- (.getTime (java.util.Date.))
        (.lastModified suspect))
     age))

(defn cleanup
  "Delete datafiles older than 30 minutes"
  []
  (let [dir (io/file "/tmp/.")
        files (.listFiles dir)
        datafiles (filter #(re-find #"^weatherdata-.+xml$" (.getName %)) files)]
    (doall
     (map (fn [x]
            (when
             (olderthan x 1800000)
              (.delete x)))
          datafiles))))

(defn -main
  [& args]
  (let [climap (parse-opts args cli-options)
        cliopts (:options climap)
        measurement-name  (:measurement cliopts)
        filename (str "/tmp/weatherdata-" (:location cliopts) ".xml")
        datauri (str "https://opendata.fmi.fi/wfs?service=WFS&version=2.0.0&request=getFeature&storedquery_id=fmi::forecast::harmonie::surface::point::multipointcoverage&place="
                     (:location cliopts))
        initfile (when (or ; fetch the XML if a cached copy doesn't exist or is over 15 mins old
                        (not (.exists (io/file filename)))
                        (olderthan (io/file filename) 900000))
                   (spit filename
                         (:body
                          (client/get datauri))))
        ennuste (->
                 (slurp filename)
                 parse-str)
        positions_path [(tag= :member) (tag= :GridSeriesObservation)
                        (tag= :result) (tag= :MultiPointCoverage)
                        (tag= :domainSet) (tag= :SimpleMultiPoint) (tag= :positions)]
        times_positions (eduction (apply comp positions_path) [ennuste])
        times_positions_rows (line-seq (BufferedReader. (StringReader. (apply str (:content (first times_positions))))))
        results_path [(tag= :member) (tag= :GridSeriesObservation)
                      (tag= :result) (tag= :MultiPointCoverage) (tag= :rangeSet)
                      (tag= :DataBlock) (tag= :doubleOrNilReasonTupleList)]
        results (eduction (apply comp results_path) [ennuste])
        resultrows (line-seq (BufferedReader. (StringReader. (apply str (:content (first results))))))
        fields_path [(tag= :member) (tag= :GridSeriesObservation)
                     (tag= :result) (tag= :MultiPointCoverage) (tag= :rangeType) (tag= :DataRecord)]
        fields (eduction (apply comp fields_path) [ennuste])
        fieldindex (reduce (fn mapitus [acc pari] (assoc acc (key (first pari)) (val (first  pari)))) {}
                           (map-indexed (fn mapsahdus [index item] {(get-in item [:attrs :name]) index})
                                        (:content (first fields))))
        slice (fn [measurement coll]
                (map (fn [rivi]
                       (-> (trim rivi)
                           (split #"\s+")
                           (nth (get fieldindex measurement)))) coll))
        time-extract (fn [coll]
                       (map (fn [rivi]
                              (-> (trim rivi)
                                  (split #"\s+")
                                  (last)
                                  (parse-long)
                                  (* 1000)
                                  (jt/java-date)
                                  (str))) coll))]
    (cleanup)
    (cond
      (:help cliopts)
      (println (str "-l, --location PLACE [" (:defaultlocation (config)) "]\n-m --measurement [" (:defaultmeasurement (config)) "]\n\nPossible values for measurement are:\n" (join " " (sort measurements))))
      (:errors climap)
      (println (:errors climap))
      :else
      (->> (interleave
            (time-extract (-> times_positions_rows (rest) (butlast)))
            (slice measurement-name (-> resultrows (rest) (butlast))))
           (partition 2)
           (fipp)))))

(comment
  (require '[portal.api :as portal])
  (portal/open)
  (portal/tap)
  (def ennuste (parse-str (slurp "/tmp/weatherdata-juhannuskylä,tampere.xml"))))
