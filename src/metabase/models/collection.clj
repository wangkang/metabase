(ns metabase.models.collection
  (:require [metabase.db :as db]
            (metabase.models [interface :as i]
                             [permissions :as perms])
            [metabase.util :as u]))

(def ^:private ^:const collection-slug-max-length
  "Maximum number of characters allowed in a Collection `slug`."
  254)

(i/defentity Collection :collection)

(defn- assert-unique-slug [slug]
  (when (db/exists? Collection :slug slug)
    (throw (ex-info "Name already taken"
             {:status-code 400, :errors {:name "A collection with this name already exists"}}))))

(defn- assert-valid-hex-color [^String hex-color]
  (when (or (not (string? hex-color))
            (not (re-matches #"[0-9A-Fa-f]{6}" hex-color)))
    (throw (ex-info "Invalid color"
             {:status-code 400, :erros {:color "must be a valid 6-character hex color code"}}))))

(defn- pre-insert [{collection-name :name, color :color, :as collection}]
  (assert-valid-hex-color color)
  (assoc collection :slug (u/prog1 (u/slugify collection-name collection-slug-max-length)
                            (assert-unique-slug <>))))

(defn- pre-update [{collection-name :name, id :id, color :color, :as collection}]
  (when (contains? collection :color)
    (assert-valid-hex-color color))
  (if-not collection-name
    collection
    (assoc collection :slug (u/prog1 (u/slugify collection-name collection-slug-max-length)
                              (or (db/exists? Collection, :slug <>, :id id) ; if slug hasn't changed no need to check for uniqueness
                                  (assert-unique-slug <>))))))              ; otherwise check to make sure the new slug is unique

(defn- pre-cascade-delete [collection]
  ;; unset the collection_id for Cards in this collection. This is mostly for the sake of tests since IRL we shouldn't be deleting collections, but rather archiving them instead
  (db/update-where! 'Card {:collection_id (u/get-id collection)}
    :collection_id nil))

(defn perms-objects-set
  "Return the required set of permissions to READ-OR-WRITE COLLECTION-OR-ID."
  [collection-or-id read-or-write]
  ;; This is not entirely accurate as you need to be a superuser to modifiy a collection itself (e.g., changing its name) but if you have write perms you can add/remove cards
  #{(case read-or-write
      :read  (perms/collection-read-path collection-or-id)
      :write (perms/collection-readwrite-path collection-or-id))})


(u/strict-extend (class Collection)
  i/IEntity
  (merge i/IEntityDefaults
         {:hydration-keys     (constantly [:collection])
          :types              (constantly {:name :clob, :description :clob})
          :pre-insert         pre-insert
          :pre-update         pre-update
          :pre-cascade-delete pre-cascade-delete
          :can-read?          (partial i/current-user-has-full-permissions? :read)
          :can-write?         (partial i/current-user-has-full-permissions? :write)
          :perms-objects-set  perms-objects-set}))
