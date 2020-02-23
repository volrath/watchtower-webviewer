(ns vlt.jdbc-watchtower.webviewer.app
  (:require [clojure.edn :as edn]
            ["date-fns" :as date-fns]
            [goog.dom :as gdom]
            [reagent.core :as r]))


;; UI
;; -----------------------------------------------------------------------------

(defonce db
  (r/atom {:limit 100
           :queries '()}))


(def colors
  {:bg1 "#0B0C10"
   :bg2 "#1F2833"
   :fg  "#C5C6C7"
   :hl1 "#45A29E"
   :hl2 "#66FCF1"})


(defn query-entry
  [props {:keys [execution-info logged-at] :as _query}]
  (let [{:keys [statement elapsed-time]} execution-info]
    [:tr props
     [:td.statement {:style {:padding ".5rem 0"}} statement]
     [:td.duration  {:style {:padding ".5rem 0" :text-align "center"}} elapsed-time]
     [:td.logged-at {:style {:padding ".5rem 0" :text-align "center"}} (date-fns/format logged-at "eee, yyyy-MM-dd, h:mm:ss aa")]]))


(defn app
  []
  (let [{:keys [limit queries]} @db]
    [:div#watchtower {:style {:background (:bg1 colors)
                              :color      (:fg colors)
                              :height     "100%"}}
     [:h1 {:style {:text-align "center"
                   :color      (:hl2 colors)}}
      "All along the Watchtower, Princess kept the view..."]
     [:div#control-panel {:style {:margin-bottom "1.5rem"}}
      [:button {:on-click #(swap! db assoc :queries '())}
       "Clear"]
      [:span
       "Show only"]
      [:input {:type          "text"
               :default-value limit
               :on-change     (fn [e]
                                (let [new-limit (js/parseInt (.. e -target -value))]
                                  (swap! db assoc :limit (if (js/isNaN new-limit) 0 new-limit))
                                  (swap! db update :queries (partial take new-limit))))}]]
     [:table {:style {:width "100%"
                      :color (:fg colors)}}
      [:thead {:style {:background (:bg2 colors)}}
       [:tr
        [:th {:style {:padding ".8rem 0"}} (str "Query (Showing: " (count queries) " / " limit ")")]
        [:th {:style {:padding ".8rem 0"}} "Duration (ms)"]
        [:th {:style {:padding ".8rem 0"}} "Logged at"]]]
      [:tbody
       (let [colors (sequence cat (repeat [(:bg1 colors) (:bg2 colors)]))]
         (for [[idx query] (map-indexed vector (take limit queries))
               :let        [background-color (nth colors idx)]]
           ^{:key idx} [query-entry {:style {:background background-color}} query]))]]]))


(r/render [app] (gdom/getElement "app-wrapper"))


;; WebSocket
;; -----------------------------------------------------------------------------

(defn on-receive
  [^js/MessageEvent evt]
  (let [query     (-> evt
                      .-data
                      edn/read-string
                      (assoc :logged-at (js/Date.)))
        add-query (fn [queries]
                    (->> query
                         (conj queries)
                         (take (:limit @db))))]
    (swap! db update :queries add-query)))


(defonce ^:dynamic *socket* nil)
(when-not *socket*
  (let [ws-url "ws://localhost:8080"]
    (set! *socket* (js/WebSocket. ws-url))
    (if-not *socket*
      (throw (ex-info "Could not connect to Watchtower server." *socket*))
      (do
        (set! (.-onmessage *socket*) on-receive)
        (set! (.-onopen *socket*)    #(js/console.log "Watchtower connected to" ws-url))
        (set! (.-onclose *socket*)   #(js/console.warn "Disconnected from Watchtower."))))))
