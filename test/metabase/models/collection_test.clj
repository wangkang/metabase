(ns metabase.models.collection-test
  (:require [expectations :refer :all]
            [metabase.db :as db]
            [metabase.models.collection :refer [Collection]]
            [metabase.test.util :as tu]))

;; test that we can create a new Collection with valid inputs
(expect
  {:name        "My Favorite Cards"
   :slug        "my_favorite_cards"
   :description nil
   :color       "ABCDEF"
   :archived    false}
  (tu/with-temp Collection [collection {:name "My Favorite Cards", :color "ABCDEF"}]
    (dissoc collection :id)))

;; check that the color is validated
(expect Exception (db/insert! Collection {:name "My Favorite Cards"}))                   ; missing color
(expect Exception (db/insert! Collection {:name "My Favorite Cards", :color "ABC"}))     ; too short
(expect Exception (db/insert! Collection {:name "My Favorite Cards", :color "BCDEFG"}))  ; invalid chars
(expect Exception (db/insert! Collection {:name "My Favorite Cards", :color "ABCDEFF"})) ; too long

;; double-check that `with-temp-defaults` are working correctly for Collection
(expect
  :ok
  (tu/with-temp* [Collection [_]]
    :ok))

;; test that duplicate names aren't allowed
(expect
  Exception
  (tu/with-temp* [Collection [_ {:name "My Favorite Cards"}]
                  Collection [_ {:name "My Favorite Cards"}]]
    :ok))

;; things with different names that would cause the same slug shouldn't be allowed either
(expect
  Exception
  (tu/with-temp* [Collection [_ {:name "My Favorite Cards"}]
                  Collection [_ {:name "my_favorite Cards"}]]
    :ok))
