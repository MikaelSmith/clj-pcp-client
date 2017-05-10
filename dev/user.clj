(ns user
  (:require [puppetlabs.pcp.client :as pcp]
            [puppetlabs.kitchensink.core :as ks]))

(def test-parameters
  {:server "wss://broker.example.com:8142/pcp2"
   :ssl-context {
                 :cacert "../pcp-broker/test-resources/ssl/certs/ca.pem"
                 :cert "../pcp-broker/test-resources/ssl/certs/client01.example.com.pem"
                 :private-key "../pcp-broker/test-resources/ssl/private_keys/client01.example.com.pem"}
   :type "test-client"
   :max-message-size 67108864})

(defn connect
  [params]
  (-> (pcp/connect params {"http://puppetlabs.com/rpc_streaming_update"
                           (fn [_ message]
                             (println "Streaming update:")
                             (println (-> (get-in message [:data :update]))))
                           "http://puppetlabs.com/rpc_provisional_response"
                           (fn [_ message]
                             (println "Received provisional response from" (:sender message)))
                           "http://puppetlabs.com/rpc_non_blocking_response"
                           (fn [_ message]
                             (println "Received final response from" (:sender message))
                             (println (:data message)))
                           "http://puppetlabs.com/inventory_response"
                           (fn [_ message]
                             (println "Inventory:" (get-in message [:data :uris])))
                           :default (fn [_ message] (println message))})
      (pcp/wait-for-connection 5000)))

(defn ping
  [client target]
  (pcp/send! client {:id (ks/uuid)
                     :message_type "http://puppetlabs.com/rpc_blocking_request"
                     :target target
                     :data {:transaction_id (ks/uuid)
                            :module "ping"
                            :action "ping"}}))

(defn inventory
  ([client]
   (inventory client false))
  ([client subscribe]
   (pcp/send! client {:id (ks/uuid)
                      :message_type "http://puppetlabs.com/inventory_request"
                      :data {:query ["pcp://*/*"] :subscribe subscribe}})))

(defn run-puppet
  ([client target]
   (pcp/send! client {:id (ks/uuid)
                      :message_type "http://puppetlabs.com/rpc_non_blocking_request"
                      :target target
                      :data {:transaction_id (ks/uuid)
                             :notify_outcome true
                             :module "pxp-module-puppet"
                             :action "run"
                             :params {:flags []}}})))
