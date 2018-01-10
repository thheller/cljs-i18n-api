# cljs.i18n - API proposal

fulcro provides an i18n api which currently requires a separate CLJS build and involves a whole bunch of `gettext` related tooling to extract strings for translation.

This could be significantly easier when using the `tr` macro itself to collect all relevant strings and being able to extract them on demand via the CLJS analyzer data.

## User API

The user API is simple and relies on a single `tr` macro.

```clojure
(ns demo.i18n
  (:require [cljs.i18n :refer (tr)]))

;; simple translation
(tr "translate me plz")

;; translation with dynamic replacements, FormatJS?
(tr "foo {thing} bar" :thing something-dynamic)

;; translation with context
;; when the same string is used in different contexts
(tr ["foo?" "foo"])
(tr ["foo?" "bar"])

(tr ["foo {thing} bar" "some-context"] :thing something-dynamic)
```

## Providing Translations

The translations are provided via a simple protocol with no default implementation. Keeping track of the users locale and available translations is not part of the API.

```clojure
(ns demo.i18n)

(defprotocol ITranslate
  (-get-text [this context key args]))
```

- `context` is either `nil` or a `"string"`
- `key` is the original text or key used in `tr`
- `args` is `nil` or a JS Object (not a clojure map, avoids a call to `clj->js` when passing it to a JS impl, eg. FormatJS)


At runtime the user (or build tool) can set the translation implementation. A toy implementation that just upper cases all strings could look like this.

```clojure
(cljs.i18n/set-translator!
  (reify cljs.i18n/ITranslate
    (-get-text [this context key args]
      (clojure.string/upper-case key))))
```

Libraries can provide different `ITranslate` implementations that handle the actual loading/formatting of the translation strings.

## Extracting Strings

The `cljs.i18n/tr` macro collects all used strings per namespace. It is added to the `cljs.env/*compiler*` atom on compile under the `[:cljs.analyzer/namespaces demo.i18n :cljs.i18n/strings]` key. It is a vector containing simple maps.

```clojure
[{:ns demo.i18n
  :msg "foo {thing} bar"
  :context nil
  :line 5
  :column 4
  :args [:thing]}]
```

Tools can extract this data and generate `.pot` files or any other translation format. The information is collected per namespace so it works properly with compiler caching enabled. It should be de-duped before passing it to other tools but AFTER compilation.
