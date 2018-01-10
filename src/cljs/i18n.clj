(ns cljs.i18n
  (:require [cljs.env :as env]
            [cljs.analyzer :as ana]
            [cljs.tagged-literals :as tlit]))

(def vec-conj (fnil conj []))

(defmacro tr
  "msg is either a literal string or [string context]
   followed by :key/value pairs (optional) to inject into the translated string

   (tr \"Hello World\")
   (tr [\"Hello World\" \"foo-ctx\"])
   (tr \"{name} owes {amount, currency)\" :name who :amount amt)
   (tr [\"{name} owes {amount, currency)\" \"foo-ctx\"] :name who :amount amt)"
  [msg & {:as args}]
  (let [{:keys [line column]}
        (meta &form)

        [msg context]
        (cond
          (string? msg)
          [msg nil]

          (and (vector? msg)
               (= 2 (count msg))
               (string? (nth msg 0))
               (string? (nth msg 1)))
          msg

          :else
          (throw (ex-info "invalid tr message, must be a string or [string context]" {:form &form})))

        ;; FIXME: validate that all arg keys are present in the msg string

        current-ns
        (-> &env :ns :name)

        string-data
        {:msg msg
         :ns current-ns
         :args (into [] (keys args))
         :context context
         :line line
         :column column}]

    (swap! env/*compiler* update-in [::ana/namespaces current-ns ::strings] vec-conj string-data)

    `(get-text ~context ~msg ~(when args (tlit/->JSValue args)))
    ))
