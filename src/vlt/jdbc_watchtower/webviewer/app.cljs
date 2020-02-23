(ns vlt.jdbc-watchtower.webviewer.app
  (:require [reagent.core :as reagent]))


(defn app
  []
  [:div#watchtower
   [:p "There must be some kind of way outta here..."]])


(reagent/render [app] js/document.body)
