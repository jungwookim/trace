; Copyright (c) 2022-present Howard Lewis Ship.
;
; Licensed under the Apache License, Version 2.0 (the "License")
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;     http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.

(ns net.lewisship.trace-test
  (:require
   [clojure.test :refer [deftest is]]
   [net.lewisship.trace :as trace
    :refer [trace trace> trace>> *compile-trace* *enable-trace*]]))

;; Note: these tests may fail if executed from REPL (and trace compilation
;; is enabled).

(deftest trace-uncompiled-is-nil
  (binding [*compile-trace* false]
    (is (= nil
          (macroexpand-1 `(trace :foo 1 :bar 2))))))

;; Because line numbers are embedded, any changes above this line may make the lines below fail.

(deftest trace-with-compile-enabled
  (binding [*compile-trace* true]
    (is (= '(net.lewisship.trace/emit-trace 34 :foo 1 :bar 2)
          (macroexpand-1 '(net.lewisship.trace/trace :foo 1 :bar 2))))

    (is (= '(clojure.core/let [% n] (net.lewisship.trace/emit-trace 37 :value % :foo 1) %)
          (macroexpand-1 '(net.lewisship.trace/trace> n :value % :foo 1))))

    (is (= '(clojure.core/let [% n] (net.lewisship.trace/emit-trace 40 :value % :bar 2) %)
          (macroexpand-1 '(net.lewisship.trace/trace>> :value % :bar 2 n))))))

(deftest emit-trace-expansion
  (binding [*compile-trace* true]
    (is (= '(clojure.core/when net.lewisship.trace/*enable-trace*
              (clojure.core/tap>
                (clojure.core/array-map
                  :in (net.lewisship.trace/extract-in)
                  :line 99
                  :thread (.getName (java.lang.Thread/currentThread))
                  :x 1
                  :y 2))
              nil)
          (macroexpand-1 '(net.lewisship.trace/emit-trace 99 :x 1 :y 2))))

    (is (= '(clojure.core/when net.lewisship.trace/*enable-trace*
              (clojure.core/tap>
                (clojure.core/array-map
                  :in (net.lewisship.trace/extract-in)
                  :thread (.getName (java.lang.Thread/currentThread))
                  :x 1
                  :y 2))
              nil)
          (macroexpand-1 '(net.lewisship.trace/emit-trace nil :x 1 :y 2))))))

;; The rest are just experiments used to manually test the macro expansions.

(defn calls-trace
  []
  (trace :msg "called"))

(defn calls-trace>
  []
  (-> {:value 1}
    (update :value inc)
    (trace> :data % :label :post-inc)
    (assoc :after true)))

(defn calls-trace>>
  []
  (->> (range 10)
    (map inc)
    (trace>> :values % :label :post-inc)
    (partition 2)))

(defn calls-extract-in
  []
  (trace/extract-in))

(deftest identifies-trace-location
  (is (= 'net.lewisship.trace-test/calls-extract-in
        (calls-extract-in))))

(comment

  ;; Rest of this is very tricky to automated test due to dynamic nature of the macros.

  (calls-trace)
  ;; no output

  (trace/setup-default)
  ;; Reload this NS to test the remainder:

  (clojure.walk/macroexpand-all '(trace :msg "hello"))

  (calls-trace) ; => nil
  ;; {:in net.lewisship.trace-test/calls-trace,
  ;;  :line 23,
  ;;  :thread "nREPL-session-e439a250-d27a-474b-a694-69a97dbe5572",
  ;;  :msg "called"}

  (calls-trace>) ; => {:value 2, :after true }
  ;; {:in net.lewisship.trace-test/calls-trace>,
  ;;  :line 25,
  ;;  :thread "nREPL-session-e439a250-d27a-474b-a694-69a97dbe5572",
  ;;  :%value% {:value 2},
  ;;  :data {:value 2},
  ;;  :label :post-inc}

  (calls-trace>>) ; => ((1 2) (3 4) (5 6) (7 8) (9 10))
  ;; {:in net.lewisship.trace-test/calls-trace>>,
  ;;  :line 32,
  ;;  :thread "nREPL-session-e439a250-d27a-474b-a694-69a97dbe5572",
  ;;  :%value% (1 2 3 4 5 6 7 8 9 10),
  ;;  :values (1 2 3 4 5 6 7 8 9 10),
  ;;  :label :post-inc}

  (calls-extract-in)
  )
