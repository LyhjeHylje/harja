(ns harja-laadunseuranta.utils-test
  (:require [cljs.test :as t :refer-macros [deftest is testing]]
            [harja-laadunseuranta.utils :as utils]))

(deftest kaynnistysparametrien-parsinta-test
  (testing "Tyhjät parametrit"
    (is (= {} (utils/parsi-kaynnistysparametrit "")))
    (is (= {} (utils/parsi-kaynnistysparametrit "?"))))
  (testing "Avaimet ja arvot tulevat oikein"
    (is (= {"foo" "2" "bar" "3"} (utils/parsi-kaynnistysparametrit "?foo=2&bar=3")))))

(deftest mapin-keywordize-test
  (is (= (utils/keywordize-map {"foo" 1 "bar" 2}) {:foo 1 :bar 2})))

(deftest mittausten-ja-havaintojen-erotus-test
  (let [havainnot {:lampotila 12
                   :lumisuus 5
                   :tasaisuus 2
                   :kitkamittaus 0.45
                   :polyavyys 3
                   :kiinteys 1
                   :sivukaltevuus 3
                   :liukasta true
                   :lumista true}]
    (is (= [:lumista :liukasta] (utils/erota-havainnot havainnot)))
    (is (= {:lampotila 12
            :lumisuus 5
            :tasaisuus 2
            :kitkamittaus 0.45
            :polyavyys 3
            :kiinteys 1
            :sivukaltevuus 3} (utils/erota-mittaukset havainnot)))))
