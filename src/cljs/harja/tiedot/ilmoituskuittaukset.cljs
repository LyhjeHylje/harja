(ns harja.tiedot.ilmoituskuittaukset
  (:require [reagent.core :refer [atom]]
            [harja.domain.ilmoitusapurit :refer [+ilmoitustyypit+ ilmoitustyypin-nimi +ilmoitustilat+]]
            [harja.asiakas.kommunikaatio :as k]
            [harja.loki :refer [log tarkkaile!]]
            [cljs.core.async :refer [<!]]
            [harja.atom :refer [paivita-periodisesti] :refer-macros [reaction<!]]
            [harja.ui.kartta.esitettavat-asiat :refer [kartalla-esitettavaan-muotoon]]
            [harja.tiedot.istunto :as istunto])

  (:require-macros [reagent.ratom :refer [reaction run!]]
                   [cljs.core.async.macros :refer [go]]))

(defonce uusi-kuittaus (atom nil))

(defn tallenna-uusi-kuittaus [kuittaus]
  (k/post! :tallenna-ilmoitustoimenpide kuittaus))

(defn alusta-uusi-kuittaus [valittu-ilmoitus]
  (let [kayttaja @istunto/kayttaja
        organisaatio (:organisaatio kayttaja)]
    (reset! uusi-kuittaus
            {:ilmoitusid              (:ilmoitusid valittu-ilmoitus)
             :tyyppi                  :vastaanotettu
             :ilmoittaja-etunimi      (:etunimi kayttaja)
             :ilmoittaja-sukunimi     (:sukunimi kayttaja)
             :ilmoittaja-matkapuhelin (:puhelin kayttaja)
             :ilmoittaja-tyopuhelin   (:puhelin kayttaja)
             :ilmoittaja-sahkoposti   (:sahkoposti kayttaja)
             :ilmoittaja-organisaatio (:nimi organisaatio)
             :ilmoittaja-ytunnus      (:ytunnus organisaatio)})))
