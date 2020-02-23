(ns vlt.jdbc-watchtower.webviewer.core
  (:require [clojure.core.async :as a]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [ring.middleware.resource :refer [wrap-resource]]
            [org.httpkit.server :as httpkit]
            [clojure.string :as str]))

(defmulti process-ws-message
  ""
  :type)


(defmethod process-ws-message :default
  [msg]
  (prn msg))


(def ^:private connections
  (atom #{}))


(def ^:private static-root-path
  "public")


(defn web-handler
  "If we receive a websocket request, we accept it. Otherwise we serve our
  index.html file."
  [request]
  (if (:websocket? request)
    (httpkit/as-channel request
                        {:on-open    (partial swap! connections conj)
                         :on-receive (comp process-ws-message edn/read-string)})
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (io/input-stream (io/resource (str static-root-path "/index.html")))}))


(defn wrap-default-index
  [next-handler]
  (fn [request]
    (let [{:keys [uri]} request]
      (if (or (str/starts-with? uri "/css/")
              (str/starts-with? uri "/js/"))
        (next-handler request)
        (web-handler request)))))


(def app
  (-> (fn [_] {:status 404 :body "static asset not found"})
      (wrap-resource static-root-path)
      (wrap-default-index)))


(defn start!
  ""
  ([watchtower-ch]
   (start! watchtower-ch {}))
  ([watchtower-ch
    {:keys [port]
     :or   {port 8080}}]
   (let [stop-webserver-fn (httpkit/run-server #'app {:port port})
         listener-running? (volatile! (some? watchtower-ch))
         stop!             (fn []
                             (stop-webserver-fn)
                             (vreset! listener-running? false))]
     ;; Constantly listen to `watchtower-ch` and report to clients whatever we
     ;; get from it.
     (when watchtower-ch
       (a/go-loop [query (a/<! watchtower-ch)]
         (when (and @listener-running? query)
           (run! #(httpkit/send! % (pr-str query)) @connections)
           (recur (a/<! watchtower-ch)))))
     ;; Return a function to stop everything
     stop!)))


(comment
  (def stop! (start! {}))
  (stop!)
  )
