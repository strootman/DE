(ns fishy.routes.domain.params
  (:use [compojure.api.sweet :only [describe]])
  (:require [clojure.string :as string]
            [schema.core :as s]))

(def NonBlankString
  (describe
   (s/both String (s/pred (complement string/blank?) `non-blank?))
   "A non-blank string."))

(s/defschema SecuredQueryParams
  {:user (describe NonBlankString "The short version of the username")})