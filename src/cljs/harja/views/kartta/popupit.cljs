(ns harja.views.kartta.popupit
  "Yleinen nayta-popup multimetodi, joka osaa eri tyyppisistä asioista tehdä popupin."
  (:require [harja.loki :refer [log tarkkaile!]]
            [harja.pvm :as pvm]
            [harja.views.kartta :as kartta]
            [harja.tiedot.ilmoitukset :as ilmoitukset]
            [harja.views.ilmoituksen-tiedot :as ilmoituksen-tiedot]
            [harja.tiedot.urakka.laadunseuranta.laatupoikkeamat :as laatupoikkeamat]
            [clojure.string :as str]
            [harja.ui.ikonit :as ikonit]
            [harja.ui.yleiset :as yleiset]
            [harja.domain.laadunseuranta.tarkastukset :as tarkastukset]
            [harja.tiedot.urakka.yllapitokohteet :as yllapitokohteet]
            [harja.domain.turvallisuuspoikkeamat :as turpodomain]
            [harja.ui.modal :as modal]
            [harja.domain.paallystys-ja-paikkaus :as paallystys-ja-paikkaus]
            [harja.domain.tierekisteri :as tierekisteri]))

(def klikattu-tyokone (atom nil))

(defn- map->hiccup [kartta]
  "Funktio, joka rakentaa mapistä hiccup-rakenteen. Ei ole tarkoituskaan olla kovin älykäs, vaan helpottaa lähinnä
  kehitystyötä."
  (log (pr-str kartta))
  (for [avain (keys kartta)]
    (cond
      (map? (get kartta avain)) (into [:div [:b (pr-str avain)]] (map->hiccup (get kartta avain)))

      (or (set? (get kartta avain)) (vector? (get kartta avain)))
      [:div [:b (pr-str avain)] (clojure.string/join ", " (get kartta avain))]

      :else [:div [:b (pr-str avain)] (pr-str (get kartta avain))])))

(defn tee-arvolistaus-popup
  ([otsikko nimi-arvo-parit] (tee-arvolistaus-popup otsikko nimi-arvo-parit nil))
  ([otsikko nimi-arvo-parit {:keys [paaluokka linkki nappi]}]
   [:div {:class (str "kartta-popup " (when paaluokka
                                        paaluokka))}
    [:h2 [:b otsikko]]

    [:table.otsikot-ja-arvot
     [:tbody
      (for [[nimi arvo] nimi-arvo-parit]
        (when-not (nil? arvo)
          ^{:key (str nimi arvo)}
          [:tr
           [:td.otsikko [:b nimi]]
           [:td.arvo arvo]]))]]

    (when linkki
      [:a.arvolistaus-linkki.klikattava
       (select-keys linkki [:on-click :href :target])
       (:nimi linkki)])

    (when (and (:nimi nappi) (:on-click nappi))
      (let [nimi (:nimi nappi)
            on-click (:on-click nappi)]
        [:button.arvolistaus-nappi.nappi-ensisijainen {:on-click on-click}
         (ikonit/eye-open) " " nimi]))]))

(defmulti nayta-popup :aihe)

(defn geometrian-koordinaatti [tapahtuma]
  (if-let [piste (get-in tapahtuma [:sijainti :coordinates])]
    piste
    ;; PENDING: tässä on epäselvyyttä mikä on optimaalisin tapa valita
    ;; viivan-keskella on huono, koska pitkässä reitissä se voi olla
    ;; näkyvän ruudun ulkopuolellakin.
    ;; Toisaalta klikkauspisteen sijainti voi zoomatessa olla jossain ihan muualla.
    ;; Parasta olisi kai projisoida klikkauskoordinaatti lähimpään geometrian pisteeseen.
    (:klikkaus-koordinaatit tapahtuma)))

(defmethod nayta-popup :toteuma-klikattu [tapahtuma]
  (log "Näytetään popuppi" (pr-str tapahtuma))
  (kartta/nayta-popup! (geometrian-koordinaatti tapahtuma)
                       (tee-arvolistaus-popup "Toteuma"
                                              [["Aika" (pvm/pvm-aika (:alkanut tapahtuma)) "-" (pvm/pvm-aika (:paattynyt tapahtuma))]
                                               ["Suorittaja" (get-in tapahtuma [:suorittaja :nimi])]
                                               ["Tehtävät" (when (not-empty (:tehtavat tapahtuma))
                                                             (for [tehtava (:tehtavat tapahtuma)]
                                                               ^{:key tehtava}
                                                               [:div.toteuma-tehtavat
                                                                [:div "Toimenpide: " (:toimenpide tehtava)]
                                                                [:div "Määrä: " (:maara tehtava)]
                                                                (when (:paivanhinta tehtava)
                                                                  [:div "Päivän hinta: " (:paivanhinta tehtava)])
                                                                (when (:lisatieto tehtava)
                                                                  [:div "Lisätieto: " (:lisatieto tehtava)])]))]
                                               ["Materiaalit" (when (not-empty (:materiaalit tapahtuma))
                                                                (for [toteuma (:materiaalit tapahtuma)]
                                                                  ^{:key toteuma}
                                                                  [:div.toteuma-materiaalit
                                                                   [:div "Materiaali: " (get-in toteuma [:materiaali :nimi])]
                                                                   [:div "Määrä: " (:maara toteuma)]]))]
                                               ["Lisätieto" (when (:lisatieto tapahtuma)
                                                              (:lisatieto tapahtuma))]])))

(defmethod nayta-popup :ilmoitus-klikattu [tapahtuma]
  (let [sulje-fn (fn []
                   (ilmoitukset/sulje-ilmoitus!))]
    (kartta/nayta-popup!
     (geometrian-koordinaatti tapahtuma)
     (tee-arvolistaus-popup
       (condp = (:ilmoitustyyppi tapahtuma)
         :toimenpidepyynto "Toimenpidepyyntö"
         :tiedoitus "Tiedotus"
         (str/capitalize (name (:ilmoitustyyppi tapahtuma))))
       [["Ilmoitettu" (pvm/pvm-aika-sek (:ilmoitettu tapahtuma))]
        ["Otsikko" (:otsikko tapahtuma)]
        ["Paikan kuvaus" (:paikankuvaus tapahtuma)]
        ["Lisätietoja" (:lisatieto tapahtuma)]
        ["Kuittaukset" (count (:kuittaukset tapahtuma))]]
       {:linkki {:nimi     "Tarkemmat tiedot"
                 :on-click #(do
                             (.preventDefault %)
                             (ilmoitukset/avaa-ilmoitus! (dissoc tapahtuma :type :alue))
                             (modal/nayta!
                               {:otsikko "Ilmoituksen tiedot"
                                :leveys  "1000px"
                                :sulje   sulje-fn
                                :footer  [:button.nappi-toissijainen {:on-click (fn [e]
                                                                                  (.preventDefault e)
                                                                                  (modal/piilota!))}
                                          "Takaisin tilannekuvaan"]}
                               [ilmoituksen-tiedot/ilmoitus nil (dissoc tapahtuma :type :alue)]))}}))))

(defmethod nayta-popup :suljettu-tieosuus-klikattu [tapahtuma]
  (kartta/nayta-popup! (geometrian-koordinaatti tapahtuma)
                       (tee-arvolistaus-popup "Suljettu tieosuus"
                                              [["Ylläpitokohde" (str (:yllapitokohteen-nimi tapahtuma) " (" (:yllapitokohteen-numero tapahtuma) ")")]
                                               ["Aika" (pvm/pvm-aika(:aika tapahtuma))]
                                               ["Osoite" (tierekisteri/tierekisteriosoite-tekstina tapahtuma {:teksti-tie? false})]
                                               ["Kaistat" (clojure.string/join ", " (map str (:kaistat tapahtuma)))]
                                               ["Ajoradat" (clojure.string/join ", " (map str (:ajoradat tapahtuma)))]])))

(defmethod nayta-popup :tyokone-klikattu [tapahtuma]
  (reset! klikattu-tyokone (:tyokoneid tapahtuma))
  (kartta/keskita-kartta-pisteeseen (:sijainti tapahtuma))

  (kartta/nayta-popup! (:sijainti tapahtuma)
                       (tee-arvolistaus-popup "Työkone"
                                              [["Tyyppi" (:tyokonetyyppi tapahtuma)]
                                               ["Viimeisin paikka\u00ADtieto" (pvm/pvm-aika-sek (:lahetysaika tapahtuma))]
                                               ["Organisaatio" (or (:organisaationimi tapahtuma) "Ei organisaatiotietoja")]
                                               ["Urakka" (or (:urakkanimi tapahtuma) "Ei urakkatietoja")]
                                               ["Tehtävät" (let [tehtavat (str/join ", " (:tehtavat tapahtuma))]
                                                             [:span tehtavat])]])))

(defmethod nayta-popup :uusi-tyokonedata [data]
  (when-let [tk @klikattu-tyokone]
    (when-let [haettu (first (filter #(= tk (:tyokoneid %))
                                     (:tyokoneet data)))]
      (kartta/keskita-kartta-pisteeseen (:sijainti haettu))
      (kartta/poista-popup-ilman-eventtia!)
      (nayta-popup (assoc haettu :aihe :tyokone-klikattu)))))


(defmethod nayta-popup :laatupoikkeama-klikattu [tapahtuma]
  (kartta/nayta-popup! (geometrian-koordinaatti tapahtuma)
                       (let [paatos (get-in tapahtuma [:paatos :paatos])
                             kasittelyaika (get-in tapahtuma [:paatos :kasittelyaika])]
                         (tee-arvolistaus-popup "Laatupoikkeama"
                                                [["Aika" (pvm/pvm-aika-sek (:aika tapahtuma))]
                                                 ["Tekijä" (:tekijanimi tapahtuma) ", " (name (:tekija tapahtuma))]
                                                 (when (and paatos kasittelyaika)
                                                   ["Päätös" (str (laatupoikkeamat/kuvaile-paatostyyppi paatos)
                                                                  " (" (pvm/pvm-aika kasittelyaika) ")")])]))))

(defmethod nayta-popup :tarkastus-klikattu [tapahtuma]
  (let [havainnot (cond
                    (and (:havainnot tapahtuma) (not-empty (:vakiohavainnot tapahtuma)))
                    (str (:havainnot tapahtuma) " & " (str/join ", " (:vakiohavainnot tapahtuma)))

                    (:havainnot tapahtuma)
                    (:havainnot tapahtuma)

                    (not-empty (:vakiohavainnot tapahtuma))
                    (str/join ", " (:vakiohavainnot tapahtuma))

                    :default nil)]
    (kartta/nayta-popup! (geometrian-koordinaatti tapahtuma)
                        (tee-arvolistaus-popup (tarkastukset/+tarkastustyyppi->nimi+ (:tyyppi tapahtuma))
                                               [["Aika" (pvm/pvm-aika-sek (:aika tapahtuma))]
                                                ["Tarkastaja" (:tarkastaja tapahtuma)]
                                                ["Havainnot" havainnot]]))))

(defmethod nayta-popup :turvallisuuspoikkeama-klikattu [tapahtuma]
  (kartta/nayta-popup! (geometrian-koordinaatti tapahtuma)
                       (let [tapahtunut (:tapahtunut tapahtuma)
                             paattynyt (:paattynyt tapahtuma)
                             kasitelty (:kasitelty tapahtuma)]
                         (tee-arvolistaus-popup "Turvallisuuspoikkeama"
                                                [(when tapahtunut
                                                   ["Tapahtunut" (pvm/pvm-aika tapahtunut)])
                                                 (when kasitelty
                                                   ["Käsitelty" (pvm/pvm-aika kasitelty)])
                                                 ["Työn\u00ADtekijä" (turpodomain/kuvaile-tyontekijan-ammatti tapahtuma)]
                                                 ["Vammat" (str/join ", " (map turpodomain/vammat (:vammat tapahtuma)))]
                                                 ["Sairaala\u00ADvuorokaudet" (:sairaalavuorokaudet tapahtuma)]
                                                 ["Sairaus\u00ADpoissaolo\u00ADpäivät" (:sairauspoissaolopaivat tapahtuma)]
                                                 ["Vakavuus\u00ADaste" (turpodomain/turpo-vakavuusasteet (:vakavuusaste tapahtuma))]
                                                 ["Kuvaus" (:kuvaus tapahtuma)]
                                                 ["Korjaavat toimen\u00ADpiteet" (count (filter :suoritettu (:korjaavattoimenpiteet tapahtuma)))
                                                  "/" (count (:korjaavattoimenpiteet tapahtuma))]]))))

(defmethod nayta-popup :paallystys-klikattu [tapahtuma]
  (kartta/nayta-popup! (geometrian-koordinaatti tapahtuma)
                       (let [aloitettu (:aloituspvm tapahtuma)
                             paallystys-valmis (:paallystysvalmispvm tapahtuma)
                             kohde-valmis (:kohdevalmispvm tapahtuma)]
                         (tee-arvolistaus-popup "Päällystyskohde"
                                                [["Nimi" (get-in tapahtuma [:kohde :nimi])]
                                                 ["Tie\u00ADrekisteri\u00ADkohde" (get-in tapahtuma [:kohdeosa :nimi])]
                                                 ["Osoite" (tierekisteri/tierekisteriosoite-tekstina
                                                             {:tr-numero (get-in tapahtuma [:tr :numero])
                                                              :tr-alkuosa (get-in tapahtuma [:tr :alkuosa])
                                                              :tr-alkuetaisyys (get-in tapahtuma [:tr :alkuetaisyys])
                                                              :tr-loppuosa (get-in tapahtuma [:tr :loppuosa])
                                                              :tr-loppuetaisyys (get-in tapahtuma [:tr :loppuetaisyys])})]
                                                 ["Nykyinen päällyste: " (paallystys-ja-paikkaus/hae-paallyste-koodilla (:nykyinen-paallyste tapahtuma))]
                                                 ["Toimenpide" (:toimenpide tapahtuma)]
                                                 ["Tila" (yllapitokohteet/kuvaile-kohteen-tila (get-in tapahtuma [:paallystysilmoitus :tila]))]
                                                 (when aloitettu
                                                   ["Aloitettu" (pvm/pvm-aika aloitettu)])
                                                 (when paallystys-valmis
                                                   ["Päällystys valmistunut" (pvm/pvm-aika paallystys-valmis)])
                                                 (when kohde-valmis
                                                   ["Kohde valmistunut" (pvm/pvm-aika kohde-valmis)])]
                                                {:nappi {:nimi     (if (get-in tapahtuma [:paallystysilmoitus :tila])
                                                                     "Päällystysilmoitus"
                                                                     "Aloita päällystysilmoitus")
                                                         :on-click (:kohde-click tapahtuma)}}))))

(defmethod nayta-popup :paikkaus-klikattu [tapahtuma]
  (kartta/nayta-popup! (geometrian-koordinaatti tapahtuma)
                       (let [aloitettu (:aloituspvm tapahtuma)
                             paikkaus-valmis (:paikkausvalmispvm tapahtuma)
                             kohde-valmis (:kohdevalmispvm tapahtuma)]
                         (tee-arvolistaus-popup "Paikkauskohde"
                                                [["Nimi" (get-in tapahtuma [:kohde :nimi])]
                                                 ["Tie\u00ADrekisteri\u00ADkohde" (get-in tapahtuma [:kohdeosa :nimi])]
                                                 ["Osoite" (tierekisteri/tierekisteriosoite-tekstina
                                                             {:tr-numero (get-in tapahtuma [:tr :numero])
                                                              :tr-alkuosa (get-in tapahtuma [:tr :alkuosa])
                                                              :tr-alkuetaisyys (get-in tapahtuma [:tr :alkuetaisyys])
                                                              :tr-loppuosa (get-in tapahtuma [:tr :loppuosa])
                                                              :tr-loppuetaisyys (get-in tapahtuma [:tr :loppuetaisyys])})]
                                                 ["Nykyinen päällyste: " (paallystys-ja-paikkaus/hae-paallyste-koodilla (:nykyinen-paallyste tapahtuma))]
                                                 ["Toimenpide" (:toimenpide tapahtuma)]
                                                 ["Tila" (yllapitokohteet/kuvaile-kohteen-tila (get-in tapahtuma [:paikkausilmoitus :tila]))]
                                                 (when aloitettu
                                                   ["Aloitettu" (pvm/pvm-aika aloitettu)])
                                                 (when paikkaus-valmis
                                                   ["Paikkaus valmistunut" (pvm/pvm-aika paikkaus-valmis)])
                                                 (when kohde-valmis
                                                   ["Kohde valmistunut" (pvm/pvm-aika kohde-valmis)])]
                                                {:nappi {:nimi     (if (get-in tapahtuma [:paikkausilmoitus :tila])
                                                                     "Paikkausilmoitus"
                                                                     "Aloita paikkausilmoitus")
                                                         :on-click (:kohde-click tapahtuma)}}))))

(defmethod nayta-popup :varustetoteuma-klikattu [tapahtuma]
  (kartta/nayta-popup! (geometrian-koordinaatti tapahtuma)
                       (tee-arvolistaus-popup "Varustetoteuma"
                                              [["Päivämäärä: " (pvm/pvm (:alkupvm tapahtuma))]
                                               ["Tunniste: " (:tunniste tapahtuma)]
                                               ["Tietolaji: " (:tietolaji tapahtuma)]
                                               ["Toimenpide: " (:toimenpide tapahtuma)]
                                               ["Kuntoluokka" (:kuntoluokka tapahtuma)]]
                                              {:linkki {:nimi   "Avaa varustekortti"
                                                        :href   (:varustekortti-url tapahtuma)
                                                        :target "_blank"}})))

