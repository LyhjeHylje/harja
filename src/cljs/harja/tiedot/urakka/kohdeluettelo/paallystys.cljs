(ns harja.tiedot.urakka.kohdeluettelo.paallystys
  "Tämä nimiavaruus hallinnoi urakan päällystystietoja."
  (:require [reagent.core :refer [atom] :as r]
            [harja.asiakas.kommunikaatio :as k]
            [harja.asiakas.tapahtumat :as t]
            [cljs.core.async :refer [<! >! chan]]
            [harja.loki :refer [log logt]]
            [harja.pvm :as pvm]
            [harja.ui.protokollat :refer [Haku hae]]
            [harja.tiedot.navigaatio :as nav]
            [harja.tiedot.urakka :as u])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [harja.atom :refer [reaction<!]]
                   [reagent.ratom :refer [reaction]]))

(defonce paallystyskohteet-nakymassa? (atom false))
(defonce paallystysilmoitukset-nakymassa? (atom false))

(defonce paallystys-tai-paikkausnakymassa? (atom false))




(defn hae-paallystyskohteet [urakka-id sopimus-id]
  (k/post! :urakan-paallystyskohteet {:urakka-id urakka-id
                                          :sopimus-id sopimus-id}))

(defn hae-paallystyskohdeosat [urakka-id sopimus-id paallystyskohde-id]
  (k/post! :urakan-paallystyskohdeosat {:urakka-id urakka-id
                                      :sopimus-id sopimus-id
                                      :paallystyskohde-id paallystyskohde-id}))

(defn hae-paallystystoteumat [urakka-id sopimus-id]
  (k/post! :urakan-paallystystoteumat {:urakka-id urakka-id
                                      :sopimus-id sopimus-id}))

(defn hae-paallystysilmoitus-paallystyskohteella [urakka-id sopimus-id paallystyskohde-id]
  (k/post! :urakan-paallystysilmoitus-paallystyskohteella {:urakka-id urakka-id
                                       :sopimus-id sopimus-id
                                       :paallystyskohde-id paallystyskohde-id}))

(defn tallenna-paallystysilmoitus [urakka-id sopimus-id lomakedata]
  (k/post! :tallenna-paallystysilmoitus {:urakka-id urakka-id
                                         :sopimus-id sopimus-id
                                         :paallystysilmoitus lomakedata}))

(defn tallenna-paallystyskohteet [urakka-id sopimus-id kohteet]
  (k/post! :tallenna-paallystyskohteet {:urakka-id urakka-id
                                         :sopimus-id sopimus-id
                                         :kohteet kohteet}))

(defn tallenna-paallystyskohdeosat [urakka-id sopimus-id paallystyskohde-id osat]
  (k/post! :tallenna-paallystyskohdeosat {:urakka-id urakka-id
                                        :sopimus-id sopimus-id
                                        :paallystyskohde-id paallystyskohde-id
                                        :osat osat}))

(defonce kohderivit (reaction<! [valittu-urakka-id (:id @nav/valittu-urakka)
                                 [valittu-sopimus-id _] @u/valittu-sopimusnumero
                                 nakymassa? @paallystys-tai-paikkausnakymassa?]
                                (when (and valittu-urakka-id valittu-sopimus-id nakymassa?)
                                  (log "PÄÄ Haetaan päällystyskohteet.")
                                  (let [vastaus (hae-paallystyskohteet valittu-urakka-id valittu-sopimus-id)]
                                    (log "PÄÄ Vastaus saatu: " vastaus)
                                    vastaus))))

(defn paivita-kohde! [id funktio & argumentit]
  (swap! kohderivit
         (fn [kohderivit]
           (into []
                 (map (fn [kohderivi]
                        (if (= id (:id kohderivi))
                          (apply funktio kohderivi argumentit)
                          kohderivi)))
                 kohderivit))))

(defonce karttataso-paallystyskohteet (atom false))

(defonce toteumarivit (reaction<! [valittu-urakka-id (:id @nav/valittu-urakka)
                                         [valittu-sopimus-id _] @u/valittu-sopimusnumero
                                         nakymassa? @paallystysilmoitukset-nakymassa?]
                                        (when (and valittu-urakka-id valittu-sopimus-id nakymassa?)
                                          (log "PÄÄ Haetaan päällystystoteumat.")
                                          (hae-paallystystoteumat valittu-urakka-id valittu-sopimus-id))))

(defonce lomakedata (atom nil)) ; Vastaa rakenteeltaan päällystysilmoitus-taulun sisältöä

(defonce paallystyskohteet-kartalla
  (reaction (let [taso @karttataso-paallystyskohteet
                  kohderivit @kohderivit
                  toteumarivit @toteumarivit
                  avoin-paallystysilmoitus (:paallystyskohde-id @lomakedata)]
              (when taso
                (into []
                      (mapcat #(map (fn [{sij :sijainti nimi :nimi :as osa}]
                                      (let [paallystyskohde-id (:paallystyskohde_id %)]
                                        {:type :paallystyskohde
                                         :kohde %
                                         :paallystyskohde-id paallystyskohde-id
                                         :tila (:tila %)
                                         :nimi (str (:nimi %) ": " nimi)
                                         :osa osa
                                         :alue (assoc sij
                                                      :stroke {:color (case (:tila %)
                                                                        :aloitettu "blue"
                                                                        :valmis "green"
                                                                        "orange")
                                                               :width (if (= paallystyskohde-id avoin-paallystysilmoitus) 8 6)})}))
                                    (:kohdeosat %)))
                      (concat (map #(assoc % :paallystyskohde_id (:id %)) ;; yhtenäistä id kohde ja toteumariveille
                                   kohderivit)
                              toteumarivit))))))

  
                  
