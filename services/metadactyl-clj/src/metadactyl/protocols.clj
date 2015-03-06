(ns metadactyl.protocols)

(defprotocol Apps
  "A protocol used to provide an abstraction layer for dealing with app metadata."
  (getClientName [_])
  (listAppCategories [_ params])
  (hasCategory [_ category-id])
  (listAppsInCategory [_ category-id params])
  (searchApps [_ search-term params])
  (canEditApps [_])
  (addApp [_ app])
  (previewCommandLine [_ app])
  (listAppIds [_])
  (deleteApps [_ deletion-request])
  (getAppJobView [_ app-id])
  (deleteApp [_ app-id])
  (relabelApp [_ app]))
