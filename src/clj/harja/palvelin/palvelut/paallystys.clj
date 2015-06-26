(ns harja.palvelin.palvelut.paallystys
  (:require [com.stuartsierra.component :as component]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelu poista-palvelut]]
            [harja.palvelin.oikeudet :as oik]
            [harja.kyselyt.konversio :as konv]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [harja.domain.skeema :refer [Toteuma validoi]]
            [harja.domain.roolit :as roolit]
            [clojure.java.jdbc :as jdbc]

            [harja.kyselyt.paallystys :as q]
            [harja.kyselyt.materiaalit :as materiaalit-q]

            [harja.palvelin.palvelut.materiaalit :as materiaalipalvelut]
            [cheshire.core :as cheshire]))

(def muunna-desimaaliluvut-xf
  (map #(-> %
            (assoc-in [:bitumi_indeksi]
                      (or (some-> % :bitumi_indeksi double) 0))
            (assoc-in [:sopimuksen_mukaiset_tyot]
                      (or (some-> % :sopimuksen_mukaiset_tyot double) 0))
            (assoc-in [:arvonvahennykset]
                      (or (some-> % :arvonvahennykset double) 0))
            (assoc-in [:lisatyot]
                      (or (some-> % :lisatyot double) 0))
            (assoc-in [:muutoshinta]
                      (or (some-> % :muutoshinta double) 0))
            (assoc-in [:kaasuindeksi]
                      (or (some-> % :kaasuindeksi double) 0)))))


(def jsonb->clojuremap
  (map #(-> %
            (assoc-in [:ilmoitustiedot]
                      (let [ilmoitustiedot (:ilmoitustiedot %)]
                        (log/debug "Ilmoitustiedot on:" ilmoitustiedot)
                        (or ilmoitustiedot
                              (let [json (.getValue ilmoitustiedot)] ; FIXME Tämä kaatuu jostain kummasta syystä :(
                                (log/debug "JSON on:" json)
                                (cheshire/decode json))
                            ""))))))

(defn hae-urakan-paallystyskohteet [db user {:keys [urakka-id sopimus-id]}]
  (log/debug "Haetaan urakan päällystyskohteet. Urakka-id " urakka-id ", sopimus-id: " sopimus-id)
  (oik/vaadi-lukuoikeus-urakkaan user urakka-id)
  (let [vastaus (into []
                      muunna-desimaaliluvut-xf
                      (q/hae-urakan-paallystyskohteet db urakka-id sopimus-id))]
    (log/debug "Päällystyskohteet saatu: " (pr-str vastaus))
    vastaus))

(defn hae-urakan-paallystyskohdeosat [db user {:keys [urakka-id sopimus-id paallystyskohde-id]}]
  (log/debug "Haetaan urakan päällystyskohdeosat. Urakka-id " urakka-id ", sopimus-id: " sopimus-id ", paallystyskohde-id: " paallystyskohde-id)
  (oik/vaadi-lukuoikeus-urakkaan user urakka-id)
  (let [vastaus (into []
                      muunna-desimaaliluvut-xf
                      (q/hae-urakan-paallystyskohteen-paallystyskohdeosat db urakka-id sopimus-id paallystyskohde-id))]
    (log/debug "Päällystyskohdeosat saatu: " (pr-str vastaus))
    vastaus))

(defn hae-urakan-paallystystoteumat [db user {:keys [urakka-id sopimus-id]}]
  (log/debug "Haetaan urakan päällystystoteumat. Urakka-id " urakka-id ", sopimus-id: " sopimus-id)
  (oik/vaadi-lukuoikeus-urakkaan user urakka-id)
  (let [vastaus (into []
                      muunna-desimaaliluvut-xf
                      (q/hae-urakan-paallystystoteumat db urakka-id sopimus-id))]
    (log/debug "Päällystystoteumat saatu: " (pr-str vastaus))
    vastaus))

(defn hae-urakan-paallystysilmoitus-paallystyskohteella [db user {:keys [urakka-id sopimus-id paallystyskohde-id]}]
  (log/debug "Haetaan urakan päällystysilmoitus, jonka päällystyskohde-id " paallystyskohde-id ". Urakka-id " urakka-id ", sopimus-id: " sopimus-id)
  (oik/vaadi-lukuoikeus-urakkaan user urakka-id)
  (let [vastaus (into []
                      jsonb->clojuremap
                      (q/hae-urakan-paallystysilmoitus-paallystyskohteella db urakka-id sopimus-id paallystyskohde-id))]
    (log/debug "Päällystysilmoitus saatu: " (pr-str vastaus))
    vastaus))

(defn tallenna-paallystysilmoitus [db user {:keys [urakka-id sopimus-id paallytyskohde-id lomakedata]}]
  (log/debug "Käsitellään päällystysilmoitus: " lomakedata ". Urakka-id " urakka-id ", sopimus-id: " sopimus-id)
  (oik/vaadi-rooli-urakassa user roolit/toteumien-kirjaus urakka-id) ; FIXME Onko rooli oikein?
  ; FIXME Vaadi skeema
  (let [muutoshinta 0] ; FIXME Laske lomakedatasta tämä
    ; FIXME Kanta ei huoli JSON-stringiä vaikka normaalisti huolii?
    ;(q/luo-paallystysilmoitus<! db paallytyskohde-id (cheshire/encode lomakedata) muutoshinta (:id user))))
    ))

(defrecord Paallystys []
  component/Lifecycle
  (start [this]
    (let [http (:http-palvelin this)
          db (:db this)]
      (julkaise-palvelu http :urakan-paallystyskohteet
                        (fn [user tiedot]
                          (hae-urakan-paallystyskohteet db user tiedot)))
      (julkaise-palvelu http :urakan-paallystyskohdeosat
                        (fn [user tiedot]
                          (hae-urakan-paallystyskohdeosat db user tiedot)))
      (julkaise-palvelu http :urakan-paallystystoteumat
                        (fn [user tiedot]
                          (hae-urakan-paallystystoteumat db user tiedot)))
      (julkaise-palvelu http :urakan-paallystysilmoitus-paallystyskohteella
                        (fn [user tiedot]
                          (hae-urakan-paallystysilmoitus-paallystyskohteella db user tiedot)))
      (julkaise-palvelu http :tallenna-paallystysilmoitus
                        (fn [user tiedot]
                          (tallenna-paallystysilmoitus db user tiedot)))
      this))

  (stop [this]
    (poista-palvelut
      (:http-palvelin this)
      :urakan-paallystyskohteet)
    this))
