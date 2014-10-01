(ns data-info.routes.filesystem
  (:use [compojure.core]
        [data-info.util])
  (:require [data-info.services.filesystem.create :as create]
            [data-info.services.filesystem.directory :as dir]
            [data-info.services.filesystem.exists :as exists]
            [data-info.services.filesystem.home :as home]
            [data-info.services.filesystem.metadata :as meta]
            [data-info.services.filesystem.move :as move]
            [data-info.services.filesystem.page-csv :as csv]
            [data-info.services.filesystem.page-file :as file]
            [data-info.services.filesystem.preview :as preview]
            [data-info.services.filesystem.rename :as rename]
            [data-info.services.filesystem.sharing :as sharing]
            [data-info.services.filesystem.space-handling :as sh]
            [data-info.services.filesystem.stat :as stat]
            [data-info.services.filesystem.trash :as trash]
            [data-info.services.filesystem.updown :as ud]
            [data-info.services.filesystem.users :as user]
            [data-info.services.filesystem.uuids :as uuid]))


(defn filesystem-routes
  "The routes for file IO endpoints."
  []
  (GET "/filesystem/home" [:as req]
    (controller req home/do-homedir :params))

  (POST "/filesystem/exists" [:as req]
    (controller req exists/do-exists :params :body))

  (POST "/filesystem/stat" [:as req]
    (controller req stat/do-stat :params :body))

  (POST "/filesystem/download" [:as req]
    (controller req ud/do-download :params :body))

  (POST "/filesystem/download-contents" [:as req]
    (controller req ud/do-download-contents :params :body))

  (GET "/filesystem/display-download" [:as req]
    (controller req ud/do-special-download :params))

  (GET "/filesystem/upload" [:as req]
    (controller req ud/do-upload :params))

  (GET "/filesystem/directory" [:as req]
    (controller req dir/do-directory :params))

  (GET "/filesystem/paged-directory" [:as req]
    (controller req dir/do-paged-listing :params))

  (POST "/filesystem/directory/create" [:as req]
    (controller req create/do-create :params :body))

  (POST "/filesystem/rename" [:as req]
    (controller req rename/do-rename :params :body))

  (POST "/filesystem/delete" [:as req]
    (controller req trash/do-delete :params :body))

  (POST "/filesystem/delete-contents" [:as req]
    (controller req trash/do-delete-contents :params :body))

  (POST "/filesystem/move" [:as req]
    (controller req move/do-move :params :body))

  (POST "/filesystem/move-contents" [:as req]
    (controller req move/do-move-contents :params :body))

  (GET "/filesystem/file/preview" [:as req]
    (controller req preview/do-preview :params))

  (GET "/filesystem/metadata" [:as req]
    (controller req meta/do-metadata-get :params))

  (POST "/filesystem/metadata" [:as req]
    (controller req meta/do-metadata-set :params :body))

  (DELETE "/filesystem/metadata" [:as req]
    (controller req meta/do-metadata-delete :params))

  (POST "/filesystem/metadata-batch" [:as req]
    (controller req meta/do-metadata-batch-set :params :body))

  (POST "/filesystem/share" [:as req]
    (controller req sharing/do-share :params :body))

  (POST "/filesystem/unshare" [:as req]
    (controller req sharing/do-unshare :params :body))

  (POST "/filesystem/user-permissions" [:as req]
    (controller req user/do-user-permissions :params :body))

  (GET "/filesystem/groups" [:as req]
    (controller req user/do-groups :params))

  (GET "/filesystem/quota" [:as req]
    (controller req user/do-quota :params))

  (POST "/filesystem/restore" [:as req]
    (controller req trash/do-restore :params :body))

  (POST "/filesystem/restore-all" [:as req]
    (controller req trash/do-restore-all :params))

  (GET "/filesystem/user-trash-dir" [:as req]
    (controller req trash/do-user-trash :params))

  (POST "/filesystem/paths-contain-space" [:as req]
    (controller req sh/do-paths-contain-space :params :body))

  (POST "/filesystem/replace-spaces" [:as req]
    (controller req sh/do-replace-spaces :params :body))

  (DELETE "/filesystem/trash" [:as req]
    (controller req trash/do-delete-trash :params))

  (POST "/filesystem/read-chunk" [:as req]
    (controller req file/do-read-chunk :params :body))

  (POST "/filesystem/overwrite-chunk" [:as req]
    (controller req file/do-overwrite-chunk :params :body))

  (POST "/filesystem/read-csv-chunk" [:as req]
    (controller req csv/do-read-csv-chunk :params :body))

  (POST "/filesystem/anon-files" [:as req]
    (controller req sharing/do-anon-files :params :body))

  (POST "/filesystem/paths-for-uuids" [:as req]
    (controller req uuid/do-paths-for-uuids :params :body))

  (POST "/filesystem/uuids-for-paths" [:as req]
    (controller req uuid/do-uuids-for-paths :params :body)))