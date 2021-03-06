(ns harja.views.urakka.paallystyksen-kohdeluettelo
  "Päällystysurakan 'Kohdeluettelo' välilehti:"
  (:require [reagent.core :refer [atom] :as r]
            [harja.ui.bootstrap :as bs]
            [harja.ui.yleiset :refer [ajax-loader linkki livi-pudotusvalikko vihje] :as yleiset]
            [harja.views.urakka.paallystyskohteet :as paallystyskohteet]
            [harja.views.urakka.paallystysilmoitukset :as paallystysilmoitukset]
            [harja.views.kartta :as kartta]
            [harja.views.kartta.popupit :as popupit]

            [harja.ui.lomake :refer [lomake]]
            [harja.ui.komponentti :as komp]
            [harja.ui.ikonit :as ikonit]

            [harja.loki :refer [log logt]]
            [cljs.core.async :refer [<! >! chan]]
            [harja.ui.protokollat :refer [Haku hae]]
            [harja.domain.skeema :refer [+tyotyypit+]]
            [harja.asiakas.tapahtumat :as tapahtumat]
            [harja.tiedot.navigaatio :as nav]
            [harja.domain.oikeudet :as oikeudet]
            [harja.tiedot.urakka.paallystys :as paallystys])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction run!]]
                   [harja.atom :refer [reaction<!]]))

(defn kohdeosan-reitti-klikattu [_ kohde]
  (let [paallystyskohde-id (:paallystyskohde-id kohde)]
    (popupit/nayta-popup
      (assoc kohde
        :aihe :paallystys-klikattu
        :kohde {:nimi (get-in kohde [:kohde :nimi])}
        :kohdeosa {:nimi (get-in kohde [:osa :nimi])}
        :nykyinen-paallyste (get-in kohde [:osa :nykyinen-paallyste])
        :toimenpide (get-in kohde [:osa :toimenpide])
        :paallystysilmoitus {:tila (:tila kohde)}
        :tr {:numero (get-in kohde [:osa :tr-numero])
             :alkuosa (get-in kohde [:osa :tr-alkuosa])
             :alkuetaisyys (get-in kohde [:osa :tr-alkuetaisyys])
             :loppuosa (get-in kohde [:osa :tr-loppuosa])
             :loppuetaisyys (get-in kohde [:osa :tr-loppuetaisyys])}
        :kohde-click #(do (kartta/poista-popup!)
                          (nav/aseta-valittu-valilehti! :kohdeluettelo-paallystys :paallystysilmoitukset)
                          (tapahtumat/julkaise! {:aihe :avaa-paallystysilmoitus :paallystyskohde-id paallystyskohde-id}))))))

(defn kohdeluettelo
  "Kohdeluettelo-pääkomponentti"
  [ur]
  (komp/luo
    (komp/ulos #(kartta/poista-popup!))
    (komp/kuuntelija :paallystys-klikattu kohdeosan-reitti-klikattu)
    (komp/lippu paallystys/karttataso-paallystyskohteet)
    (fn [ur]
      (if (:yhatiedot ur)
        [:span.kohdeluettelo
         [bs/tabs {:style :tabs :classes "tabs-taso2"
                   :active (nav/valittu-valilehti-atom :kohdeluettelo-paallystys)}

          "Päällystyskohteet"
          :paallystyskohteet
          (when (oikeudet/urakat-kohdeluettelo-paallystyskohteet (:id ur))
            [paallystyskohteet/paallystyskohteet ur])

          "Päällystysilmoitukset"
          :paallystysilmoitukset
          (when (oikeudet/urakat-kohdeluettelo-paallystysilmoitukset (:id ur))
            [paallystysilmoitukset/paallystysilmoitukset])]]
        [vihje "Päällystysurakka täytyy sitoa YHA-urakkaan ennen kuin sen kohteita voi hallita."]))))
