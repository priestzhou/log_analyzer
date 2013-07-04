(ns web.main
    (:use ring.adapter.jetty)
    (:gen-class)
)

(defn app
    [{:keys [uri]}]
    {:body (format "You requested %s" uri)}
)

(defn -main []
    (run-jetty #'app {:port 8085 :join? false})
)
