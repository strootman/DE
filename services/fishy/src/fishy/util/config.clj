(ns fishy.util.config
  (:use [slingshot.slingshot :only [throw+]])
  (:require [clojure-commons.config :as cc]
            [clojure-commons.error-codes :as ce]))

(def docs-uri "/docs")

(def svc-info
  {:desc     "RESTful facade for the Grouper API."
   :app-name "fishy"
   :group-id "org.iplantc"
   :art-id   "fishy"
   :service  "fishy"})

(def ^:private props
  "A ref for storing the configuration properties."
  (ref nil))

(def ^:private config-valid
  "A ref for storing a configuration validity flag."
  (ref true))

(def ^:private configs
  "A ref for storing the symbols used to get configuration settings."
  (ref []))

(cc/defprop-int listen-port
  "The port that fishy listens to."
  [props config-valid configs]
  "fishy.app.listen-port")

(cc/defprop-str grouper-base
  "The base URL to use when connecting to the Grouper API."
  [props config-valid configs]
  "fishy.grouper.base-url")

(cc/defprop-str grouper-api-version
  "The Grouper REST API version used by this facade."
  [props config-valid configs]
  "fishy.grouper.api-version")

(defn- validate-config
  "Validates the configuration settings after they've been loaded."
  []
  (when-not (cc/validate-config configs config-valid)
    (throw+ {:error_code ce/ERR_CONFIG_INVALID})))

(defn load-config-from-file
  "Loads the configuration settings from a file."
  [cfg-path]
  (cc/load-config-from-file cfg-path props)
  (cc/log-config props)
  (validate-config))