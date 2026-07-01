(ns forestry.governor
  "ForestryGovernor — the independent safety/traceability layer for the
  ISCO-08 6210 independent forestry actor. The Forestry Advisor proposes
  actions (fell, replant); it has no notion of permit provenance or
  protected-habitat risk, so this MUST be a separate system able to
  *reject* a proposal and fall back to HOLD — the itonami-actor pattern
  (independent Governor gates a proposing actor) applied to this
  occupation.

  Charter (mirrors ADR-2607011000 robotics premise + ADR-2607012000
  cloud-itonami-isco): the actor never dispatches a robot action or writes an
  operating record the governor refuses. Felling on a protected stand
  ALWAYS requires human sign-off — it can never be auto-approved.

  HARD invariants for :forestry/propose:
    1. Permit provenance        — a felling event must reference a
       registered permit on a registered stand.
    2. No-actuation             — the proposal must not directly mutate a
       felling or replanting record outside the record-felling!/
       record-replanting! path (effect must be :propose, never a raw
       store write).
    3. Protected-habitat safety — a felling on a stand with `protected?
       true` always requires :high or higher safety-class, forcing human
       sign-off; it is never auto-approved regardless of confidence.
  SOFT:
    4. Confidence floor → escalate."
  (:require [forestry.store :as store]))

(def confidence-floor 0.6)
(def safety-classes [:none :low :medium :high :safety-critical])

(defn- safety-rank [safety-class]
  (let [idx (.indexOf safety-classes safety-class)]
    (if (neg? idx) 0 idx)))

(defn- hard-violations [{:keys [permit-fn stand-fn]} proposal]
  (let [{:keys [permit-id safety-class effect]} proposal
        found-permit (permit-fn permit-id)
        found-stand  (when found-permit (stand-fn (:stand-id found-permit)))]
    (cond-> []
      (nil? found-permit)
      (conj {:rule :no-permit :detail (str "未登録 permit " permit-id)})

      (not= :propose effect)
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"})

      (and found-stand (:protected? found-stand)
           (< (safety-rank (or safety-class :none)) (safety-rank :high)))
      (conj {:rule :protected-habitat-safety
             :detail "protected? stand への felling は :high 以上の safety-class が必須"}))))

(defn assess
  "Assess a proposal against `env` (a map with `:permit-fn`/`:stand-fn`
  lookups, decoupled from any concrete Store so this stays pure). Returns
  `{:decision :proceed|:hold|:human-approval :violations [...] :confidence n}`."
  [env proposal]
  (let [violations (hard-violations env proposal)
        safety-class (or (:safety-class proposal) :none)
        confidence (or (:confidence proposal) 1.0)]
    (cond
      (seq violations)
      {:decision :hold :violations violations :confidence confidence}

      (>= (safety-rank safety-class) (safety-rank :high))
      {:decision :human-approval :violations [] :confidence confidence}

      (< confidence confidence-floor)
      {:decision :human-approval :violations [] :confidence confidence
       :reason :low-confidence}

      :else
      {:decision :proceed :violations [] :confidence confidence})))

(defn env-for-store
  "Build the decoupled env map `assess` needs from a concrete
  `forestry.store/Store` implementation."
  [store]
  {:permit-fn #(store/permit store %)
   :stand-fn  #(store/stand store %)})
