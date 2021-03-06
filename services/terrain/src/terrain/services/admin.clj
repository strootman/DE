(ns terrain.services.admin
  (:use [terrain.util.service :only [success-response]])
  (:require [clojure.tools.logging :as log]
            [cemerick.url :as url]
            [cheshire.core :as json]
            [clojure-commons.config :as cc]
            [clojure-commons.error-codes :as ce]
            [terrain.util.config :as config]
            [clj-http.client :as client]
            [terrain.clients.data-info :as data]))


(defn config
  "Returns JSON containing Terrain's configuration, passwords filtered out."
  []
  (success-response (config/masked-config)))


(defn- check-irods?
  "Returns true if the iRODS settings should be checked."
  []
  (or (config/data-routes-enabled)
      (config/filesystem-routes-enabled)
      (config/fileio-routes-enabled)))

(defn check-jex?
  "Returns true if the JEX settings should be checked."
  []
  (config/app-routes-enabled))

(defn check-apps?
  "Returns true if the apps settings should be checked."
  []
  (config/app-routes-enabled))

(defn check-notificationagent?
  "Returns true if the notification agent settings should be checked."
  []
  (config/notification-routes-enabled))

(defn scrub-url
  [url-to-scrub]
  (str (url/url url-to-scrub :path "/")))


(defn perform-jex-check
  []
  (try
    (let [s (:status (client/get (config/jex-base-url)))]
      (log/info "HTTP Status from JEX: " s)
      (<= 200 s 299))
    (catch Exception e
      (log/error "Error performing JEX status check:")
      (log/error (ce/format-exception e))
      false)))

(defn perform-apps-check
  []
  (try
    (let [base-url (scrub-url (config/apps-base))
          s        (:status (client/get base-url))]
      (log/info "HTTP Status from Apps: " s)
      (<= 200 s 299))
    (catch Exception e
      (log/error "Error performing Apps status check:")
      (log/error (ce/format-exception e))
      false)))

(defn perform-notificationagent-check
  []
  (try
    (let [base-url (scrub-url (config/notificationagent-base))
          s        (:status (client/get base-url))]
      (log/info "HTTP Status from NotificationAgent: " s)
      (<= 200 s 299))
    (catch Exception e
      (log/error "Error performing NotificationAgent status check:")
      (log/error (ce/format-exception e))
      false)))


(defn- status-irods
  [status]
  (if (check-irods?)
    (merge status {:iRODS (data/irods-running?)})
    status))

(defn status-jex
  [status]
  (if (check-jex?)
    (merge status {:jex (perform-jex-check)})
    status))

(defn status-apps
  [status]
  (if (check-apps?)
    (merge status {:apps (perform-apps-check)})
    status))

(defn status-notificationagent
  [status]
  (if (check-notificationagent?)
    (merge status {:notificationagent (perform-notificationagent-check)})
    status))

(defn status
  "Returns JSON containing the Terrain's status."
  [request]
  (-> {}
    (status-irods)
    (status-jex)
    (status-apps)
    (status-notificationagent)
    success-response))

