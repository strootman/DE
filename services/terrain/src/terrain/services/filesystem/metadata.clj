(ns terrain.services.filesystem.metadata
  (:use [clojure-commons.error-codes]
        [clojure-commons.validators]
        [terrain.services.filesystem.common-paths]
        [terrain.services.filesystem.validators]
        [kameleon.uuids :only [uuidify]]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.item-ops :only [input-stream]]
        [clj-jargon.metadata]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure-commons.file-utils :as ft]
            [cemerick.url :as url]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [terrain.clients.metadata.raw :as metadata-client]
            [terrain.services.filesystem.icat :as icat]
            [terrain.services.filesystem.uuids :as uuids]
            [terrain.services.filesystem.validators :as validators]
            [terrain.util.config :as cfg]
            [terrain.util.service :as service]
            [ring.middleware.multipart-params :as multipart])
  (:import [au.com.bytecode.opencsv CSVReader]
           [java.io InputStream]))

(defn- fix-unit
  "Used to replace the IPCRESERVED unit with an empty string."
  [avu]
  (if (= (:unit avu) IPCRESERVED)
    (assoc avu :unit "")
    avu))

(def ^:private ipc-regex #"(?i)^ipc")

(defn- ipc-avu?
  "Returns a truthy value if the AVU map passed in is reserved for the DE's use."
  [avu]
  (re-find ipc-regex (:attr avu)))

(defn- authorized-avus
  "Validation to make sure the AVUs aren't system AVUs. Throws a slingshot error
   map if the validation fails."
  [avus]
  (when (some ipc-avu? avus)
    (throw+ {:type :clojure-commons.exception/not-authorized
             :avus avus})))

(defn service-response->json
  [response]
  (->> response :body service/decode-json))

(defn- get-readable-path
  [cm user data-id]
  (let [path (:path (uuids/path-for-uuid cm user data-id))]
    (validators/path-readable cm user path)
    path))

(defn- list-path-metadata
  "Returns the metadata for a path. Passes all AVUs to (fix-unit).
   AVUs with a unit matching IPCSYSTEM are filtered out."
  [cm path]
  (remove
   ipc-avu?
   (map fix-unit (get-metadata cm (ft/rm-last-slash path)))))

(defn- reserved-unit
  "Turns a blank unit into a reserved unit."
  [avu-map]
  (if (string/blank? (:unit avu-map))
    IPCRESERVED
    (:unit avu-map)))

(defn metadata-get
  "Returns the metadata for a path. Filters out system AVUs and replaces
   units set to ipc-reserved with an empty string."
  [user data-id]
  (with-jargon (icat/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (let [path (get-readable-path cm user data-id)
          template-avus (service-response->json (metadata-client/list-metadata-avus data-id))]
      {:irods-avus (list-path-metadata cm path)
       :metadata template-avus})))

(defn- common-metadata-add
  "Adds an AVU to 'path'. The AVU is passed in as a map in the format:
   {
      :attr attr-string
      :value value-string
      :unit unit-string
   }
   It's a no-op if an AVU with the same attribute and value is already
   associated with the path."
  [cm path avu-map]
  (let [fixed-path (ft/rm-last-slash path)
        new-unit   (reserved-unit avu-map)
        attr       (:attr avu-map)
        value      (:value avu-map)]
    (log/warn "Fixed Path:" fixed-path)
    (log/warn "check" (true? (attr-value? cm fixed-path attr value)))
    (when-not (attr-value? cm fixed-path attr value)
      (log/warn "Adding " attr value "to" fixed-path)
      (add-metadata cm fixed-path attr value new-unit))
    fixed-path))

(defn metadata-add
  "Allows user to set metadata on a path. The user must exist in iRODS
   and have write permissions on the path. The path must exist. The
   avu-map parameter must be in this format:
   {
      :attr attr-string
      :value value-string
      :unit unit-string
   }"
  [user path avu-map]
  (with-jargon (icat/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (when (= "failure" (:status avu-map))
      (throw+ {:error_code ERR_INVALID_JSON}))
    (validators/path-exists cm path)
    (validators/path-writeable cm user path)
    (authorized-avus [avu-map])
    {:path (common-metadata-add cm path avu-map)
     :user user}))

(defn admin-metadata-add
  "Adds the AVU to path, bypassing user permission checks. See (metadata-set)
   for the AVU map format."
  [path avu-map]
  (with-jargon (icat/jargon-cfg) [cm]
    (when (= "failure" (:status avu-map))
      (throw+ {:error_code ERR_INVALID_JSON}))
    (validators/path-exists cm path)
    (validators/path-writeable cm (cfg/irods-user) path)
    (common-metadata-add cm path avu-map)))

(defn metadata-set
  "Allows user to set metadata on an item with the given data-id. The user must exist in iRODS and have
   write permissions on the data item. The request parameter is a map with :irods-avus and :metadata keys
   in the following format:
   {
     :irods-avus [{:attr string :value string :unit string}]
     :metadata {:template_id UUID :avus [{:attr string :value string :unit string}]}
   }"
  [data-id user {:keys [irods-avus metadata]}]
  (with-jargon (icat/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (let [{:keys [path type] :as data-item} (uuids/path-for-uuid cm user data-id)
          data-type (metadata-client/resolve-data-type type)
          irods-avus (set (map #(select-keys % [:attr :value :unit]) irods-avus))
          current-avus (set (list-path-metadata cm path))
          delete-irods-avus (clojure.set/difference current-avus irods-avus)]
      (validators/path-writeable cm user path)
      (authorized-avus irods-avus)

      (metadata-client/set-metadata-template-avus data-id data-type (or metadata {}))

      (doseq [del-avu delete-irods-avus]
        (let [attr  (:attr del-avu)
              value (:value del-avu)]
          (if (attr-value? cm path attr value)
            (delete-metadata cm path attr value))))
      (doseq [avu irods-avus]
        (let [new-unit (reserved-unit avu)
              attr     (:attr avu)
              value    (:value avu)]
          (if-not (attr-value? cm path attr value)
            (add-metadata cm path attr value new-unit))))

      {:path path
       :user user})))

(defn- find-attributes
  [cm attrs path]
  (let [matching-avus (get-attributes cm attrs path)]
    (if-not (empty? matching-avus)
      {:path path
       :avus matching-avus}
      nil)))

(defn- validate-batch-add-attrs
  "Throws an error if any of the given paths already have metadata set with any of the given attrs."
  [cm paths attrs]
  (let [duplicates (remove nil? (map (partial find-attributes cm attrs) paths))]
    (when-not (empty? duplicates)
      (validators/duplicate-attrs-error duplicates))))

(defn- metadata-batch-add
  "Adds metadata to the given path. If the destination path already has an AVU with the same attr
   and value as one from the given avus list, that AVU is not added."
  [cm path avus]
  (loop [avus-current (get-metadata cm path)
         avus-to-add  avus]
    (when-not (empty? avus-to-add)
      (let [{:keys [attr value] :as avu} (first avus-to-add)
            new-unit (reserved-unit avu)]
        (if-not (attr-value? avus-current attr value)
          (add-metadata cm path attr value new-unit))
        (recur (conj avus-current {:attr attr :value value :unit new-unit}) (rest avus-to-add))))))

(defn- format-copy-dest-item
  [{:keys [id type]}]
  {:id   id
   :type (metadata-client/resolve-data-type type)})

(defn- metadata-copy
  "Copies all IRODS AVUs visible to the client, and Metadata Template AVUs, from the data item with
   src-id to the items with dest-ids. When the 'force?' parameter is false or not set, additional
   validation is performed."
  [user force? src-id dest-ids]
  (with-jargon (icat/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (let [src-path (get-readable-path cm user src-id)
          dest-items (map (partial uuids/path-for-uuid cm user) dest-ids)
          dest-paths (map :path dest-items)
          irods-avus (list-path-metadata cm src-path)
          attrs (set (map :attr irods-avus))]
      (validators/all-paths-writeable cm user dest-paths)
      (if-not force?
        (validate-batch-add-attrs cm dest-paths attrs))
      (metadata-client/copy-metadata-template-avus src-id force? (map format-copy-dest-item dest-items))
      (doseq [path dest-paths]
        (metadata-batch-add cm path irods-avus))
      {:user  user
       :src   src-path
       :paths dest-paths})))

(defn metadata-delete
  "Deletes an AVU from path on behalf of a user. attr and value should be strings."
  [user path attr value]
  (with-jargon (icat/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (validators/path-exists cm path)
    (validators/path-writeable cm user path)
    (authorized-avus [{:attr attr :value value :unit ""}])
    (delete-metadata cm path attr value)
    {:path path :user user}))

(defn- check-avus
  [avus]
  (mapv
   #(and (map? %1)
         (contains? %1 :attr)
         (contains? %1 :value)
         (contains? %1 :unit))
    avus))

(defn do-metadata-get
  "Entrypoint for the API. Calls (metadata-get). Parameter should be a map
   with :user and :path as keys. Values are strings."
  [{user :user} data-id]
  (metadata-get user (uuidify data-id)))

(with-pre-hook! #'do-metadata-get
  (fn [params data-id]
    (log-call "do-metadata-get" params)
    (validate-map params {:user string?})))

(with-post-hook! #'do-metadata-get (log-func "do-metadata-get"))

(defn do-metadata-add
  "Entrypoint for the API. Calls (metadata-add). Parameter should be a map
   with :user and :path as keys. Values are strings."
  [{user :user path :path} body]
  (metadata-add user path body))

(with-pre-hook! #'do-metadata-add
  (fn [params body]
    (log-call "do-metadata-add" params body)
    (validate-map params {:user string? :path string?})
    (validate-map body {:attr string? :value string? :unit string?})))

(with-post-hook! #'do-metadata-add (log-func "do-metadata-add"))

(defn do-metadata-set
  "Entrypoint for the API that calls (metadata-set).
   Body is a map with :irods-avus and :metadata keys."
  [data-id {user :user} body]
  (metadata-set (uuidify data-id) user body))

(with-pre-hook! #'do-metadata-set
  (fn [data-id params {:keys [irods-avus metadata] :as body}]
    (log-call "do-metadata-set" params body)
    (validate-map params {:user string?})
    (when (pos? (count irods-avus))
      (validate-field :irods-avus irods-avus (comp (partial every? true?) check-avus)))))

(with-post-hook! #'do-metadata-set (log-func "do-metadata-set"))

(defn do-metadata-delete
  "Entrypoint for the API that calls (metadata-delete). Parameter is a map
   with :user, :path, :attr, :value as keys. Values are strings."
  [{user :user path :path attr :attr value :value}]
  (metadata-delete user path attr value))

(with-pre-hook! #'do-metadata-delete
  (fn [params]
    (log-call "do-metadata-delete" params)
    (validate-map params {:user string?
                          :path string?
                          :attr string?
                          :value string?})))

(with-post-hook! #'do-metadata-delete (log-func "do-metadata-delete"))

(defn do-metadata-copy
  "Entrypoint for the API that calls (metadata-copy)."
  [{:keys [user]} data-id force {dest-ids :destination_ids}]
  (metadata-copy user (Boolean/parseBoolean force) (uuidify data-id) (map uuidify dest-ids)))

(with-pre-hook! #'do-metadata-copy
  (fn [{:keys [user] :as params} data-id force {dest-ids :destination_ids :as body}]
    (log-call "do-metadata-copy" params data-id body)
    (validate-map params {:user string?})
    (validate-map body {:destination_ids sequential?})
    (validate-num-paths dest-ids)))

(with-post-hook! #'do-metadata-copy (log-func "do-metadata-copy"))

(defn do-metadata-save
  "Forwards request to data-info service."
  [data-id params body]
  (let [url (url/url (cfg/data-info-base-url) "data" data-id "metadata" "save")
        req-map {:query-params (select-keys params [:user])
                 :content-type :json
                 :body         (json/encode body)}]
    (http/post (str url) req-map)))

(with-pre-hook! #'do-metadata-save
  (fn [data-id params body]
    (log-call "do-metadata-save" params body)
    (validate-map params {:user string?})))

(with-post-hook! #'do-metadata-save (log-func "do-metadata-save"))

(defn- add-metadata-template-avus
  "Adds or Updates AVUs associated with a Metadata Template for the given user's data item."
  [cm user path template-id avus]
  (validators/path-exists cm path)
  (validators/path-writeable cm user path)
  (let [{:keys [id type]} (uuids/uuid-for-path cm user path)
        data-id (uuidify id)
        data-type (metadata-client/resolve-data-type type)]
    (metadata-client/set-metadata-template-avus data-id data-type template-id {:avus avus})))

(defn- bulk-add-file-avus
  "Applies metadata from a list of attributes and values to the given path.
   If an AVU's attribute is found in the given template-attrs map, then that AVU is stored in the
   metadata db; all other AVUs are stored in IRODS."
  [cm user dest-dir template-id template-attrs attrs path values]
  (let [avus (map (partial zipmap [:attr :value :unit]) (map vector attrs values (repeat "")))
        template-attr? #(contains? template-attrs (:attr %))
        [template-avus irods-avus] ((juxt filter remove) template-attr? avus)]
    (when-not (empty? template-avus)
      (add-metadata-template-avus cm user path template-id template-avus))
    (when-not (empty? irods-avus)
      (authorized-avus irods-avus)
      (metadata-batch-add cm path irods-avus))
    {:path path
     :metadata template-avus
     :irods-avus irods-avus}))

(defn- parse-template-attrs
  "Fetches Metadata Template attributes from the metadata service if given a template-id UUID.
   Returns a map with the attribute names as keys, or nil."
  [template-id]
  (when template-id
    (->> (metadata-client/get-template template-id)
         :body
         service/decode-json
         :attributes
         (group-by :name))))

(defn- format-csv-metadata-filename
  [dest-dir ^String filename]
  (ft/rm-last-slash
    (if (.startsWith filename "/")
      filename
      (ft/path-join dest-dir filename))))

(defn- bulk-add-avus
  "Applies metadata from a list of attributes and filename/values to those files found under
   dest-dir."
  [cm user dest-dir force? template-id template-attrs attrs csv-filename-values]
  (let [format-path (partial format-csv-metadata-filename dest-dir)
        paths (map (comp format-path first) csv-filename-values)
        value-lists (map rest csv-filename-values)
        irods-attrs (clojure.set/difference (set attrs) (set (keys template-attrs)))]
    (validators/all-paths-exist cm paths)
    (validators/all-paths-writeable cm user paths)
    (if-not force?
      (validate-batch-add-attrs cm paths irods-attrs))
  (mapv (partial bulk-add-file-avus cm user dest-dir template-id template-attrs attrs)
    paths value-lists)))

(defn- parse-metadata-csv
  "Parses filenames and metadata to apply from a CSV file input stream.
   If a template-id is provided, then AVUs with template attributes are stored in the metadata db,
   and all other AVUs are stored in IRODS."
  [cm user dest-dir force? template-id ^String separator ^InputStream stream]
  (let [stream-reader (java.io.InputStreamReader. stream "UTF-8")
        csv (mapv (partial mapv string/trim) (.readAll (CSVReader. stream-reader (.charAt separator 0))))
        attrs (-> csv first rest)
        csv-filename-values (rest csv)
        template-attrs (parse-template-attrs template-id)]
    {:path-metadata
     (bulk-add-avus cm user dest-dir force? template-id template-attrs attrs csv-filename-values)}))

(defn parse-metadata-csv-file
  "Parses filenames and metadata to apply from a source CSV file in the data store"
  [{:keys [user]} {:keys [src dest force template-id separator] :or {separator "%2C"}}]
  (with-jargon (icat/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (validators/path-exists cm src)
    (validators/path-readable cm user src)
    (service/success-response
      (parse-metadata-csv cm user dest
        (Boolean/parseBoolean force)
        (uuidify template-id)
        (url/url-decode separator)
        (input-stream cm src)))))

(with-pre-hook! #'parse-metadata-csv-file
  (fn [user-info params]
    (log-call "parse-metadata-csv-file" user-info params)
    (validate-map user-info {:user string?})
    (validate-map params {:src string?
                          :dest string?})))

(with-post-hook! #'parse-metadata-csv-file (log-func "parse-metadata-csv-file"))
