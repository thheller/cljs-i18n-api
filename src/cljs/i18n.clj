(ns cljs.i18n
  (:require [cljs.env :as env]
            [cljs.analyzer :as ana]
            [cljs.tagged-literals :as tlit]))

(def vec-conj (fnil conj []))

(defmacro tr
  "msg is a literal string
   followed by :key/value pairs (optional) to inject into the translated string
   if the the argument count is uneven the last argument will be treated as a dynamic map

   (tr \"Hello World\")
   (tr \"Hello World\" :cljs.i18n/context \"foo-ctx\"])
   (tr \"{name} owes {amount, currency}\" :name who :amount amt)
   (tr \"{name} owes {amount, currency}\" :cljs.i18n/context \"foo-ctx\" :name who :amount amt)
   (tr \"{name} owes {amount, currency}\" a-map)
   (tr \"{name} owes {amount, currency}\" :name who a-map)
   (tr \"{name} owes {amount, currency}\" :cljs.i18n/context \"foo-ctx\" a-map)
   "
  [msg & args]
  (let [kv-args
        (if (even? (count args))
          args
          (butlast args))

        map-arg
        (when (odd? (count args))
          (last args))

        {::keys [context] :as static-args}
        (-> (apply hash-map kv-args)
            (cond->
              (map? map-arg)
              (merge map-arg)))

        {:keys [line column]}
        (meta &form)

        current-ns
        (-> &env :ns :name)

        string-data
        {:msg msg
         :ns current-ns
         :args (into [] (keys static-args))
         :context context
         :line line
         :column column}

        static-args
        (dissoc static-args ::context)]

    ;; FIXME: could also record this for clojure somewhere?
    (when env/*compiler*
      (swap! env/*compiler* update-in [::ana/namespaces current-ns ::strings] vec-conj string-data))

    `(cljs.i18n/get-text ~context ~msg
       ~(cond
          (or (nil? map-arg)
              (= map-arg static-args))
          static-args

          (and (empty? static-args)
               (some? map-arg))
          map-arg

          :else
          `(merge ~static-args ~map-arg)))
    ))


(comment
  (defn get-text [context msg args])
  (prn (macroexpand `(tr "hello world")))
  (prn (macroexpand `(tr "hello world" :one 1 :two 2 a-map)))
  (prn (macroexpand `(tr "hello world" :cljs.i18n/context "foo" :two 2)))
  (prn (macroexpand `(tr "hello world" :cljs.i18n/context "foo" a-map)))
  (prn (macroexpand `(tr "hello world" :one 1 :two 2)))
  (prn (macroexpand `(tr "hello world" {:one 1})))
  (prn (macroexpand `(tr "hello world" a-map))))

