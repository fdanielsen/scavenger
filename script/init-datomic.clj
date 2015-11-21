(use '[datomic.api :only [q db] :as d])

; Create items database
(def uri "datomic:free://localhost:4334/items")
(d/create-database uri)

; Connect to database
(def conn (d/connect uri))

; Read in schema EDN and run a transaction with it on the database
(def schema-tx (read-string (slurp "data/items-schema.edn")))
@(d/transact conn schema-tx)

; Load initial data into the database
(def data-tx (read-string (slurp "data/items-data.edn")))
@(d/transact conn data-tx)

(System/exit 0)
