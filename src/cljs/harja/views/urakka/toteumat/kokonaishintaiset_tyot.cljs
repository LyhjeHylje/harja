(ns harja.views.urakka.toteumat.kokonaishintaiset-tyot
  "Urakan 'Toteumat' välilehden 'Kokonaishintaiset työt' osio"
  (:require [reagent.core :refer [atom] :as r]
            [cljs.core.async :refer [<! >! chan timeout]]
            [harja.atom :refer [paivita!] :refer-macros [reaction<!]]
            [harja.ui.grid :as grid]
            [harja.ui.yleiset :refer [ajax-loader]]
            [harja.ui.protokollat :refer [Haku hae]]
            [harja.views.kartta.popupit :as popupit]
            [harja.tiedot.navigaatio :as navigaatio]
            [harja.tiedot.urakka.toteumat.kokonaishintaiset-tyot :as tiedot]
            [harja.loki :refer [log logt tarkkaile!]]
            [harja.domain.skeema :refer [+tyotyypit+]]
            [harja.views.kartta :as kartta]
            [harja.views.urakka.valinnat :as urakka-valinnat]
            [harja.ui.komponentti :as komp]
            [harja.pvm :as pvm]
            [harja.fmt :as fmt]
            [harja.tiedot.navigaatio :as nav]
            [harja.ui.lomake :as lomake]
            [harja.ui.ikonit :as ikonit]
            [harja.ui.napit :as napit]
            [harja.tiedot.urakka :as u]
            [harja.tiedot.urakka.urakan-toimenpiteet :as urakan-toimenpiteet]
            [harja.domain.oikeudet :as oikeudet]
            [harja.tiedot.urakka.toteumat :as toteumat]
            [harja.ui.yleiset :as yleiset])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [harja.makrot :refer [defc fnc]]
                   [reagent.ratom :refer [reaction run!]]
                   [harja.atom :refer [reaction-writable]]))

(defn kokonaishintainen-reitti-klikattu [_ toteuma]
  (popupit/nayta-popup (assoc toteuma :aihe :toteuma-klikattu)))

(defn tehtavan-paivakohtaiset-tiedot [pvm toimenpidekoodi]
  (let [tiedot (atom nil)]
    (go (reset! tiedot
                (<! (tiedot/hae-kokonaishintaisen-toteuman-tiedot (:id @nav/valittu-urakka) pvm toimenpidekoodi))))
    (fn [pvm toimenpidekoodi]
      [grid/grid {:otsikko  "Päivän toteumat"
                  :tunniste :id
                  :tyhja    (if (nil? @tiedot) [ajax-loader "Haetaan tehtävän päiväkohtaisia tietoja..."]
                                               "Tietoja ei löytynyt")}
       [{:otsikko "Suorittaja" :nimi :suorittaja :hae (comp :nimi :suorittaja) :leveys 3}
        {:otsikko "Alkanut" :nimi :alkanut :leveys 2 :fmt pvm/aika}
        {:otsikko "Päättynyt" :nimi :paattynyt :leveys 2 :fmt pvm/aika}
        {:otsikko "Pituus" :nimi :pituus :leveys 3 :fmt fmt/pituus-opt :tasaa :oikea}
        {:otsikko "Lisätietoja" :nimi :lisatieto :leveys 3}
        {:otsikko     "Tarkastele koko toteumaa"
         :nimi        :tarkastele-toteumaa
         :muokattava? (constantly false)
         :tyyppi      :komponentti
         :leveys      2
         :komponentti (fn [rivi]
                        [:div
                         [:button.nappi-toissijainen.nappi-grid
                          {:on-click #(tiedot/valitse-toteuma! rivi)}
                          (ikonit/eye-open) " Toteuma"]])}]
       (sort-by :alkanut @tiedot)])))

(defn tee-taulukko []
  (let [toteumat @tiedot/haetut-toteumat
        tunniste (juxt :pvm :toimenpidekoodi :jarjestelmanlisaama)]
    [:span
     [grid/grid
      {:otsikko                   "Kokonaishintaisten töiden toteumat"
       :tyhja                     (if @tiedot/haetut-toteumat "Toteumia ei löytynyt" [ajax-loader "Haetaan toteumia."])
       :rivi-klikattu             #(do
                                    (nav/vaihda-kartan-koko! :L)
                                    (reset! tiedot/valittu-paivakohtainen-tehtava %))
       :rivi-valinta-peruttu      #(do (reset! tiedot/valittu-paivakohtainen-tehtava nil))
       :mahdollista-rivin-valinta true
       :max-rivimaara 500
       :max-rivimaaran-ylitys-viesti "Toteumia löytyi yli 500. Tarkenna hakurajausta."
       :tunniste tunniste
       :vetolaatikot (into {}
                           (map (juxt
                                 tunniste
                                 (fn [{:keys [pvm toimenpidekoodi]}]
                                   [tehtavan-paivakohtaiset-tiedot pvm toimenpidekoodi])))
                           toteumat)}
      [{:nimi :tarkemmat-tiedot :tyyppi :vetolaatikon-tila :leveys "3%"}
       {:otsikko "Pvm" :tyyppi :pvm :fmt pvm/pvm :nimi :pvm :leveys "19%"}
       {:otsikko "Tehtävä" :tyyppi :string :nimi :nimi :leveys "38%"}
       {:otsikko "Määrä" :tyyppi :numero :nimi :maara :leveys "10%" :fmt #(fmt/desimaaliluku-opt % 1) :tasaa :oikea}
       {:otsikko "Yksikkö" :tyyppi :numero :nimi :yksikko :leveys "10%"}
       {:otsikko "Lähde" :nimi :lahde :hae #(if (:jarjestelmanlisaama %) "Urak. järj." "Harja") :tyyppi :string :leveys "20%"}]
      toteumat]]))

(defn tee-valinnat []
  [urakka-valinnat/urakan-sopimus-ja-hoitokausi-ja-toimenpide @navigaatio/valittu-urakka]
  (let [urakka @navigaatio/valittu-urakka]
    [:span
     (urakka-valinnat/urakan-sopimus urakka)
     (urakka-valinnat/urakan-hoitokausi urakka)
     (urakka-valinnat/aikavali)
     (urakka-valinnat/urakan-toimenpide+kaikki)
     (urakka-valinnat/urakan-kokonaishintainen-tehtava+kaikki)]))

(defn kokonaishintaisten-toteumien-listaus
  "Kokonaishintaisten töiden toteumat"
  []
  [:div
   (tee-valinnat)
   (let [oikeus? (oikeudet/voi-kirjoittaa?
                  oikeudet/urakat-toteumat-kokonaishintaisettyot
                  (:id @nav/valittu-urakka))]
     (yleiset/wrap-if
      (not oikeus?)
      [yleiset/tooltip {} :%
       (oikeudet/oikeuden-puute-kuvaus :kirjoitus
                                       oikeudet/urakat-toteumat-kokonaishintaisettyot)]
      [napit/uusi "Lisää toteuma" #(reset! tiedot/valittu-kokonaishintainen-toteuma
                                           (tiedot/uusi-kokonaishintainen-toteuma))
       {:disabled (not oikeus?)}]))
   (tee-taulukko)
   [yleiset/vihje "Näet työn kartalla klikkaamalla riviä."]])

(defn kokonaishintainen-toteuma-lomake []
  (let [muokattu (reaction-writable @tiedot/valittu-kokonaishintainen-toteuma)
        jarjestelman-lisaama-toteuma? (true? (:jarjestelma @muokattu))
        nelostason-tehtavat (map #(nth % 3) @u/urakan-toimenpiteet-ja-tehtavat)
        toimenpideinstanssit u/urakan-toimenpideinstanssit
        ;; Tehtävät pitää kasata tässä erikseen, jotta kun lomakkeessa vaihtaa toimenpideinstanssia,
        ;; myös tehtävät haetaan uudelleen..
        tehtavat (reaction (let [valittu-tpi-id (get-in @muokattu [:tehtava :toimenpideinstanssi :id])
                                 tpi-tiedot (some #(when (= valittu-tpi-id (:tpi_id %)) %) @u/urakan-toimenpideinstanssit)
                                 kaikki-tehtavat @u/urakan-kokonaishintaiset-toimenpiteet-ja-tehtavat-tehtavat
                                 tpin-tehtavat (into [] (keep (fn [[_ _ t3 t4]]
                                                                (when (= (:koodi t3) (:t3_koodi tpi-tiedot))
                                                                  t4))
                                                              kaikki-tehtavat))]
                             (sort-by :nimi tpin-tehtavat)))]
    (fnc []
         [:div
          [napit/takaisin "Takaisin luetteloon" #(reset! tiedot/valittu-kokonaishintainen-toteuma nil)]

          [lomake/lomake
           {:otsikko (if (:id @muokattu)
                       "Muokkaa kokonaishintaista toteumaa"
                       "Luo uusi kokonaishintainen toteuma")
            :muokkaa! #(do (reset! muokattu %))
            :voi-muokata? (oikeudet/voi-kirjoittaa? oikeudet/urakat-toteumat-kokonaishintaisettyot (:id @nav/valittu-urakka))
            :footer [napit/palvelinkutsu-nappi
                     "Tallenna toteuma"
                     #(tiedot/tallenna-kokonaishintainen-toteuma! @muokattu)
                     {:luokka "nappi-ensisijainen"
                      :ikoni (ikonit/tallenna)
                      :kun-onnistuu #(do
                                      (tiedot/toteuman-tallennus-onnistui %)
                                      (reset! tiedot/valittu-kokonaishintainen-toteuma nil))
                      :disabled (or (not (lomake/voi-tallentaa? @muokattu))
                                    jarjestelman-lisaama-toteuma?
                                    (not (oikeudet/voi-kirjoittaa? oikeudet/urakat-toteumat-kokonaishintaisettyot (:id @nav/valittu-urakka))))}]}
           ;; lisatieto, suorittaja {ytunnus, nimi}, pituus
           ;; reitti!
           [(when jarjestelman-lisaama-toteuma?
              {:otsikko "Lähde" :nimi :luoja :tyyppi :string
               :hae (fn [rivi]
                      (str "Järjestelmä (" (get-in rivi [:suorittaja :nimi]) ")"))
               :muokattava? (constantly false)
               :vihje toteumat/ilmoitus-jarjestelman-muokkaama-toteuma})
            {:otsikko     "Päivämäärä"
             :nimi        :alkanut
             :pakollinen? true
             :tyyppi      :pvm-aika
             :uusi-rivi?  true
             :aseta (fn [rivi arvo]
                      (-> rivi
                          (assoc :paattynyt arvo)
                          (assoc :alkanut arvo)))
             :muokattava? (constantly (not jarjestelman-lisaama-toteuma?))
             :validoi     [[:ei-tyhja "Valitse päivämäärä"]]
             :huomauta     [[:urakan-aikana-ja-hoitokaudella]]}
           (if (:jarjestelma @muokattu)
              {:tyyppi :string
               :otsikko "Pituus"
               :fmt fmt/pituus-opt
               :nimi :pituus
               :muokattava? (constantly (not jarjestelman-lisaama-toteuma?))}
              (if-not (= (:reitti @muokattu) :hakee)
               {:tyyppi              :tierekisteriosoite
                :nimi                :tr
                :pakollinen?         true
                :sijainti            (r/wrap (:reitti @muokattu)
                                             #(swap! muokattu assoc :reitti %))}
               {:tyyppi :spinner
                :nimi :spinner
                :viesti "Haetaan reittiä"}))
            {:otsikko "Suorittaja"
             :uusi-rivi? true
             :nimi :suorittajan-nimi
             :hae (comp :nimi :suorittaja)
             :aseta (fn [rivi arvo] (assoc-in rivi [:suorittaja :nimi] arvo))
             :pituus-max 256
             :tyyppi :string
             :muokattava? (constantly (not jarjestelman-lisaama-toteuma?))}
            {:otsikko "Suorittajan Y-tunnus"
             :nimi :suorittajan-ytunnus
             :hae (comp :ytunnus :suorittaja)
             :aseta (fn [rivi arvo] (assoc-in rivi [:suorittaja :ytunnus] arvo))
             :pituus-max 256
             :tyyppi :string
             :muokattava? (constantly (not jarjestelman-lisaama-toteuma?))}
            (lomake/ryhma
              {:otsikko "Tehty työ"
               :leveys-col 3}
              {:otsikko       "Toimenpide"
               :nimi          :toimenpide
               :pakollinen?   true
               :muokattava?   (constantly (not jarjestelman-lisaama-toteuma?))
               :tyyppi        :valinta
               :valinnat      @toimenpideinstanssit
               :fmt           #(:tpi_nimi
                                (urakan-toimenpiteet/toimenpideinstanssi-idlla % @toimenpideinstanssit))
               :valinta-arvo  :tpi_id
               :valinta-nayta #(if % (:tpi_nimi %) "- Valitse toimenpide -")
               :hae (comp :id :toimenpideinstanssi :tehtava)
               :aseta (fn [rivi arvo]
                        (-> rivi
                            (assoc-in [:tehtava :toimenpideinstanssi :id] arvo)
                            (assoc-in [:tehtava :toimenpidekoodi :id] nil)
                            (assoc-in [:tehtava :yksikko] nil)))
               :leveys-col    3}
              {:otsikko       "Tehtävä"
               :nimi          :tehtava
               :pakollinen?   true
               :muokattava?   (constantly (not jarjestelman-lisaama-toteuma?))
               :tyyppi        :valinta
               :valinnat      @tehtavat
               :valinta-arvo  :id
               :valinta-nayta #(if % (:nimi %) "- Valitse tehtävä -")
               :hae           (comp :id :toimenpidekoodi :tehtava)
               :aseta         (fn [rivi arvo]
                                (-> rivi
                                    (assoc-in [:tehtava :toimenpidekoodi :id] arvo)
                                    (assoc-in [:tehtava :yksikko] (:yksikko
                                                                    (urakan-toimenpiteet/tehtava-idlla
                                                                     arvo nelostason-tehtavat)))))
               :leveys-col    3}
              {:otsikko "Määrä"
               :nimi :maara
               :pakollinen?   true
               :muokattava? (constantly (not jarjestelman-lisaama-toteuma?))
               :tyyppi :positiivinen-numero
               :hae (comp :maara :tehtava)
               :aseta (fn [rivi arvo]
                        (assoc-in rivi [:tehtava :maara] arvo))
               :leveys-col 3}
              {:otsikko "Yksikkö"
               :nimi :yksikko
               :muokattava? (constantly false)
               :tyyppi :string
               :hae (comp :yksikko :tehtava)
               :leveys-col 3})
            {:otsikko "Lisätieto"
             :nimi :lisatieto
             :pituus-max 256
             :tyyppi :text
             :uusi-rivi? true
             :muokattava? (constantly (not jarjestelman-lisaama-toteuma?))
             :koko [80 :auto]
             :palstoja 2}]
           @muokattu]])))

(defn kokonaishintaiset-toteumat []
  (komp/luo
    (komp/kuuntelija :toteuma-klikattu kokonaishintainen-reitti-klikattu)
    (komp/lippu tiedot/nakymassa? tiedot/karttataso-kokonaishintainen-toteuma)

    (fn []
      [:span
       [kartta/kartan-paikka]
       (if @tiedot/valittu-kokonaishintainen-toteuma
         [kokonaishintainen-toteuma-lomake]
         [kokonaishintaisten-toteumien-listaus])])))

(def tyhjenna-popupit-kun-filtterit-muuttuu (run!
                                              @tiedot/haetut-toteumat
                                              (kartta/poista-popup!)))
