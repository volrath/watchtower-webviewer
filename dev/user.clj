(ns user
  (:require [shadow.cljs.devtools.api :as shadow]
            [shadow.cljs.devtools.server :as shadow.server]
            [vlt.jdbc-watchtower.webviewer.core :as watchtower.webviewer]))


(defn stop-frontend!
  []
  (shadow/stop-worker :app)
  (shadow.server/stop!))


(defn start-frontend!
  []
  (shadow.server/start!)
  (shadow/watch :app))


(def server-stop-fn (volatile! nil))


(defn start!
  []
  (start-frontend!)
  (vreset! server-stop-fn (watchtower.webviewer/start! nil)))


(defn stop!
  []
  (stop-frontend!)
  (when (fn? @server-stop-fn)
    (@server-stop-fn))
  (vreset! server-stop-fn nil))


(comment
  (do
    (stop!)
    (start!)
    )
  )
