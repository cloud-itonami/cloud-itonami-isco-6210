(ns forestry.store
  "SSoT for the ISCO-08 6210 independent forestry sole-proprietor actor,
  behind a `Store` protocol so the backend is a swap (MemStore default ‖ a
  real Datomic/kotoba-server backend, per the itonami actor pattern).

  Domain = independent forestry operations:

    stand        — a forest stand (standId, protected? boolean)
    permit       — a harvest permit scoped to a stand (permitId, standId,
                   scope)
    felling      — a felling event under a permit (fellingId, permitId,
                   kind #{:standard :near-habitat})
    replanting   — a replanting event on a stand (replantId, standId,
                   saplings)

  The append-only records are the operating ledger: a felling or
  replanting event must reference a registered permit/stand, and
  fellings/replantings are never mutated in place, only appended.")

(defprotocol Store
  (stand [st stand-id])
  (permit [st permit-id])
  (permits-of [st stand-id])
  (fellings-of [st permit-id])
  (replantings-of [st stand-id])
  (register-stand! [st stand])
  (register-permit! [st permit])
  (record-felling! [st felling])
  (record-replanting! [st replanting]))

(defrecord MemStore [state]
  Store
  (stand [_ stand-id]
    (get-in @state [:stands stand-id]))
  (permit [_ permit-id]
    (get-in @state [:permits permit-id]))
  (permits-of [_ stand-id]
    (filter #(= stand-id (:stand-id %)) (vals (:permits @state))))
  (fellings-of [_ permit-id]
    (filter #(= permit-id (:permit-id %)) (:fellings @state)))
  (replantings-of [_ stand-id]
    (filter #(= stand-id (:stand-id %)) (:replantings @state)))
  (register-stand! [_ stand]
    (swap! state assoc-in [:stands (:stand-id stand)] stand))
  (register-permit! [_ permit]
    (swap! state assoc-in [:permits (:permit-id permit)] permit))
  (record-felling! [_ felling]
    (swap! state update :fellings (fnil conj []) felling))
  (record-replanting! [_ replanting]
    (swap! state update :replantings (fnil conj []) replanting)))

(defn mem-store
  ([] (mem-store {}))
  ([seed]
   (->MemStore (atom (merge {:stands {} :permits {} :fellings [] :replantings []} seed)))))
