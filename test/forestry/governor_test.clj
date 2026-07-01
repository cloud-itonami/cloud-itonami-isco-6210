(ns forestry.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [forestry.store :as store]
            [forestry.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-stand! st {:stand-id "stand-1" :protected? false})
    (store/register-stand! st {:stand-id "stand-2" :protected? true})
    (store/register-permit! st {:permit-id "permit-1" :stand-id "stand-1" :scope "selective-cut"})
    (store/register-permit! st {:permit-id "permit-2" :stand-id "stand-2" :scope "selective-cut"})
    st))

(deftest proceeds-on-clean-unprotected-felling
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :standard :permit-id "permit-1" :safety-class :low
                   :effect :propose :confidence 0.9}]
    (is (= :proceed (:decision (governor/assess env proposal))))))

(deftest holds-on-unregistered-permit
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :standard :permit-id "no-such-permit" :safety-class :low
                   :effect :propose :confidence 0.9}
        result (governor/assess env proposal)]
    (is (= :hold (:decision result)))
    (is (some #(= :no-permit (:rule %)) (:violations result)))))

(deftest holds-on-no-actuation-violation
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :standard :permit-id "permit-1" :safety-class :low
                   :effect :direct-write :confidence 0.9}
        result (governor/assess env proposal)]
    (is (= :hold (:decision result)))
    (is (some #(= :no-actuation (:rule %)) (:violations result)))))

(deftest holds-on-protected-stand-felling-without-high-safety-class
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :standard :permit-id "permit-2" :safety-class :medium
                   :effect :propose :confidence 0.9}
        result (governor/assess env proposal)]
    (is (= :hold (:decision result)))
    (is (some #(= :protected-habitat-safety (:rule %)) (:violations result)))))

(deftest human-approval-on-protected-stand-felling-with-high-safety-class
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :standard :permit-id "permit-2" :safety-class :high
                   :effect :propose :confidence 0.9}]
    (is (= :human-approval (:decision (governor/assess env proposal))))))

(deftest human-approval-on-low-confidence
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :standard :permit-id "permit-1" :safety-class :none
                   :effect :propose :confidence 0.2}
        result (governor/assess env proposal)]
    (is (= :human-approval (:decision result)))
    (is (= :low-confidence (:reason result)))))

(deftest store-records-append-only
  (let [st (fresh-store)]
    (store/record-felling! st {:felling-id "f1" :permit-id "permit-1" :kind :standard})
    (store/record-replanting! st {:replant-id "r1" :stand-id "stand-1" :saplings 40})
    (is (= 1 (count (store/fellings-of st "permit-1"))))
    (is (= 1 (count (store/replantings-of st "stand-1"))))
    (is (= 1 (count (store/permits-of st "stand-1"))))))
