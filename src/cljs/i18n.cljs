(ns cljs.i18n
  (:require-macros [cljs.i18n]))

(defprotocol ITranslate
  (-get-text [this context key args]))

(defonce translate-ref (atom nil))

(defn set-translator! [tx]
  {:pre [(instance? ITranslate tx)]}
  (reset! translate-ref tx))

(defn get-text
  "do not use directly, accessed via the `tr` macro"
  [context msg args]
  (let [tx @translate-ref]
    (if-not tx
      msg
      (-get-text tx context msg args)
      )))

(defn tr [msg & args]
  (throw (ex-info "can only use tr as a macro" {})))
