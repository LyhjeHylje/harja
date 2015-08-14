(ns harja.views.urakka.toteumat
  "Urakan 'Toteumat' välilehti:"
  (:require [reagent.core :refer [atom] :as r]
            [bootstrap :as bs]
            [harja.ui.yleiset :refer [ajax-loader kuuntelija linkki sisalla? raksiboksi
                                      livi-pudotusvalikko]]
            [harja.tiedot.urakka :as u]
            [harja.views.urakka.toteumat.lampotilat :refer [suolasakot]]
            [harja.views.urakka.toteumat.yksikkohintaiset-tyot :as yks-hint-tyot]
            [harja.views.urakka.toteumat.muut-tyot :as muut-tyot]
            [harja.views.urakka.toteumat.erilliskustannukset :as erilliskustannukset]
            [harja.views.urakka.toteumat.materiaalit :refer [materiaalit-nakyma]]

            [harja.ui.lomake :refer [lomake]]
            [harja.loki :refer [log logt]]
            [cljs.core.async :refer [<! >! chan]]
            [harja.ui.protokollat :refer [Haku hae]]
            [harja.domain.skeema :refer [+tyotyypit+]])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction run!]]
                   [harja.atom :refer [reaction<!]]))



(defn toteumat
  "Toteumien pääkomponentti"
  [ur]
  [bs/tabs {:active u/toteumat-valilehti}

   "Yksikköhintaiset työt" :yksikkohintaiset-tyot
   [yks-hint-tyot/yksikkohintaisten-toteumat]

   "Muut työt" :muut-tyot
   [muut-tyot/muut-tyot-toteumat]

   "Materiaalit" :materiaalit
   [materiaalit-nakyma ur]

   "Erilliskustannukset" :erilliskustannukset
   [erilliskustannukset/erilliskustannusten-toteumat]
   
   "Suolasakot" :suolasakot
   [suolasakot]])

