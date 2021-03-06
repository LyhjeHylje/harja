(ns harja.views.urakka.paikkausilmoitukset-test
  (:require
    [cljs-time.core :as t]
    [cljs.test :as test :refer-macros [deftest is]]

    [harja.pvm :refer [->pvm]]
    [harja.domain.paikkausilmoitus :as minipot]
    [harja.loki :refer [log]]
    [harja.views.urakka.paikkausilmoitukset :as paikkausilmoitukset]))


(deftest hinta-alv-laskettu-oikein
  (is (= (.toFixed (paikkausilmoitukset/laske-tyon-alv 10 24) 2) "12.40"))
  (is (= (.toFixed (paikkausilmoitukset/laske-tyon-alv 6 10) 2) "6.60"))
  (is (= (.toFixed (paikkausilmoitukset/laske-tyon-alv 3.2 50) 2) "4.80"))
  (is (= (.toFixed (paikkausilmoitukset/laske-tyon-alv 4.5 100) 2) "9.00")))

(deftest laskee-minipotin-kokonaishinnan-oikein
  (let [tyot [{:yks-hint-alv-0 10 :maara 15}
              {:yks-hint-alv-0 7 :maara 4}
              {:yks-hint-alv-0 2 :maara 2}]
        tyot2 [{:yks-hint-alv-0 2.4 :maara 6.5}
               {:yks-hint-alv-0 3.3 :maara 2.5}]]
    (is (= (minipot/laske-kokonaishinta tyot) 182))
    (is (= (.toFixed (minipot/laske-kokonaishinta tyot2) 2) "23.85"))))