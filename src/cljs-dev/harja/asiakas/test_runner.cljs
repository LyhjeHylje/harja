(ns ^:figwheel-always harja.asiakas.test-runner
    "Juoksuttaa testit aina."
  (:require
   [harja.ui.viesti :as viesti]
   [harja.loki :refer [log]]
   ;; require kaikki testit
   [cljs.test :as test]
   [harja.app-test]
   [harja.tiedot.muokkauslukko-test]
   [harja.tiedot.urakka.suunnittelu-test]
   [harja.tiedot.urakka.yhatuonti-test]
   [harja.views.urakka.siltatarkastukset-test]
   [harja.views.urakka.paallystysilmoitukset-test]
   [harja.views.urakka.paikkausilmoitukset-test]
   [harja.views.urakka.yllapitokohteet-test]
   [harja.pvm-test]
   [harja.ui.dom-test]))


(def +virheviestin-nayttoaika+ 5000)

(defmethod test/report [:harja :begin-test-ns] [event]
  (.log js/console "Testataan: " (:ns event)))

(defmethod test/report [:harja :begin-test-var] [event]
  (.log js/console "TEST: " (test/testing-vars-str (:var event))))

(defmethod test/report [:harja :fail] [event]
  (.log js/console "FAIL: " (pr-str event))
  (viesti/nayta! [:div.testfail
                  [:h3 "Testi epäonnistui:"]
                  [:div.expected "Odotettu: " (pr-str (:expected event))]
                  [:div.actual "Saatu: " (pr-str (:actual event))]
                  (when-let [m (:message event)]
                    [:div.testmessage "Viesti: " m])]
                 :danger
                 +virheviestin-nayttoaika+))

(defmethod test/report [:harja :error] [event]
  (.log js/console "ERROR:" (pr-str event))
  (viesti/nayta! [:div.testfail
                  [:h3 "Virhe testin suorituksessa"
                   [:div.expected "Odotettu: " (pr-str (:expected event))]
                   [:div.actual "Saatu: " (pr-str (:actual event))]
                   (when-let [m (:message event)]
                     [:div.testmessage "Viesti: " m])]]
                 :danger
                 +virheviestin-nayttoaika+))

(defmethod test/report [:harja :summary] [event]
  (.log js/console "Testejä ajettu: " (:test event)))

(defn aja-testit []
  (test/run-tests (merge (test/empty-env)
                         {:reporter :harja})
                  'harja.app-test
                  'harja.tiedot.urakka.suunnittelu-test
                  'harja.tiedot.muokkauslukko-test
                  'harja.views.urakka.siltatarkastukset-test
                  'harja.views.urakka.paallystysilmoitukset-test
                  'harja.views.urakka.paikkausilmoitukset-test
                  'harja.pvm-test
                  'harja.ui.dom-test
                  'harja.tiedot.urakka.yhatuonti-test
                  'harja.views.urakka.yllapitokohteet-test))

(defn change-favicon [ico]
  (let [link (.createElement js/document "link")
        oldlink (.getElementById js/document "dynamic-favicon")]
    (set! (.-id link) "dynamic-favicon")
    (set! (.-rel link) "shortcut icon")
    (set! (.-type link) "image/ico")
    (set! (.-href link) ico)
    (when oldlink
      (.removeChild (.-head js/document) oldlink))
    (.appendChild (.-head js/document) link)))
