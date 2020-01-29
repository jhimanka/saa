(ns saa
  (:require
   [fipp.edn :refer (pprint) :rename {pprint fipp}]
   [clojure.java.io :as io]
   [clojure.tools.cli :refer [parse-opts]]
   [aero.core :refer (read-config)]
   [clj-http.client :as client])
  (:use [clojure.data.xml])
  (:gen-class))

(defn config []
  (read-config "config.edn"))

(defn apikey [conf]
  (get-in conf [:secrets :apikey])) 

(def measurements #{"GeopHeight" "Temperature" "Pressure" "Humidity" "WindDirection" "WindSpeedMS" "WindUMS" "WindVMS" "MaximumWind" "WindGust" "DewPoint" "TotalCloudCover" "WeatherSymbol3" "LowCloudCover" "MediumCloudCover" "HighCloudCover" "Precipitation1h" "PrecipitationAmount" "RadiationGlobalAccumulation" "RadiationLWAccumulation" "RadiationNetSurfaceLWAccumulation" "RadiationNetSurfaceSWAccumulation" "RadiationDiffuseAccumulation"})

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

(def text (comp (mapcat :content) (filter string?)))

(def firstcontent (comp first :content))
(def secondcontent (comp second :content))

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
        measurement (str "mts-1-1-" (:measurement cliopts))
        filename (str "/tmp/weatherdata-" (:location cliopts) ".xml")
        ;; datauri (str "http://data.fmi.fi/fmi-apikey/"
        ;;              (apikey (config))
        ;;              "/wfs?request=getFeature&storedquery_id=fmi::forecast::hirlam::surface::point::timevaluepair&place="
        ;;              (:location cliopts))
        datauri (str "http://opendata.fmi.fi/wfs?request=getFeature&storedquery_id=fmi::forecast::hirlam::surface::point::timevaluepair&place="
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
        path
        [(tag= :member) (tag= :PointTimeSeriesObservation)
         (tag= :result) (tag= :MeasurementTimeseries)
         (attr= :gml/id measurement) (tag= :point) (tag= :MeasurementTVP)]
        points (eduction (apply comp path) [ennuste])]
    (cleanup)
    (cond
      (:help cliopts)
      (println (str "-l, --location PLACE [" (:defaultlocation (config)) "]\n-m --measurement [" (:defaultmeasurement (config)) "]\n\nPossible values for measurement are:\n" (clojure.string/join " " (sort measurements))))
      (:errors climap)
      (println (:errors climap))
      :else
      (fipp (map (juxt (comp firstcontent firstcontent)
                       (comp firstcontent secondcontent)) points)))))
  


