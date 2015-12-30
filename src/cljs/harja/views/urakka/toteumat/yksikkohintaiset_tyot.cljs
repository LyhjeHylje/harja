(ns harja.views.urakka.toteumat.yksikkohintaiset-tyot
  "Urakan 'Toteumat' välilehden Yksikköhintaist työt osio"
  (:require [reagent.core :refer [atom]]
            [harja.domain.roolit :as roolit]
            [harja.ui.grid :as grid]
            [harja.ui.ikonit :as ikonit]
            [harja.ui.yleiset :refer [ajax-loader kuuntelija linkki sisalla? raksiboksi
                                      livi-pudotusvalikko]]
            [harja.ui.viesti :as viesti]
            [harja.ui.komponentti :as komp]
            [harja.tiedot.navigaatio :as nav]
            [harja.tiedot.urakka :as u]
            [harja.tiedot.urakka.toteumat :as toteumat]
            [harja.views.urakka.valinnat :as valinnat]
            [harja.pvm :as pvm]
            [harja.ui.lomake :refer [lomake]]
            [harja.loki :refer [log logt tarkkaile!]]

            [cljs.core.async :refer [<! >! chan]]
            [harja.ui.protokollat :refer [Haku hae]]
            [harja.domain.skeema :refer [+tyotyypit+]]
            [harja.fmt :as fmt]
            [harja.tiedot.urakka.urakan-toimenpiteet :as urakan-toimenpiteet]
            [harja.tiedot.urakka.toteumat.yksikkohintaiset-tyot :as yksikkohintaiset-tyot]
            [harja.views.kartta :as kartta]
            [harja.asiakas.kommunikaatio :as k]
            [harja.ui.napit :as napit])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction run!]]))

(defn tallenna-toteuma
  "Ottaa lomakkeen ja tehtävät siinä muodossa kuin ne ovat lomake-komponentissa ja muodostaa palvelimelle lähetettävän payloadin."
  [lomakkeen-toteuma lomakkeen-tehtavat]
  (let [lahetettava-toteuma (->
                              (assoc lomakkeen-toteuma
                                :tyyppi :yksikkohintainen
                                :urakka-id (:id @nav/valittu-urakka)
                                :sopimus-id (first @u/valittu-sopimusnumero)
                                :tehtavat (mapv
                                            (fn [rivi]
                                              {:toimenpidekoodi (:tehtava rivi)
                                               :maara           (:maara rivi)
                                               :tehtava-id      (:tehtava-id rivi)
                                               :poistettu       (:poistettu rivi)
                                               })
                                            (grid/filteroi-uudet-poistetut lomakkeen-tehtavat))
                                :hoitokausi-aloituspvm (first @u/valittu-hoitokausi)
                                :hoitokausi-lopetuspvm (second @u/valittu-hoitokausi)))]
    (log "Tallennetaan toteuma: " (pr-str lahetettava-toteuma))
    (toteumat/tallenna-toteuma-ja-yksikkohintaiset-tehtavat lahetettava-toteuma)))

(defn tehtavat-ja-maarat [tehtavat jarjestelman-lisaama-toteuma? tehtavat-virheet]
  (let [tehtavat-tasoineen @u/urakan-toimenpiteet-ja-tehtavat
        nelostason-tehtavat (map #(nth % 3) tehtavat-tasoineen)
        toimenpideinstanssit @u/urakan-toimenpideinstanssit]

    [grid/muokkaus-grid
     {:tyhja        "Ei töitä."
      :voi-muokata? (not jarjestelman-lisaama-toteuma?)
      :muutos       #(reset! tehtavat-virheet (grid/hae-virheet %))}
     [{:otsikko       "Toimenpide" :nimi :toimenpideinstanssi
       :tyyppi        :valinta
       :fmt           #(:tpi_nimi (urakan-toimenpiteet/toimenpideinstanssi-idlla % toimenpideinstanssit))
       :valinta-arvo  :tpi_id
       :valinta-nayta #(if % (:tpi_nimi %) "- Valitse toimenpide -")
       :valinnat      toimenpideinstanssit
       :leveys        "30%"
       :validoi       [[:ei-tyhja "Valitse työ"]]
       :aseta         #(assoc %1 :toimenpideinstanssi %2
                                 :tehtava nil)}
      {:otsikko       "Tehtävä" :nimi :tehtava
       :tyyppi        :valinta
       :valinta-arvo  #(:id (nth % 3))
       :valinta-nayta #(if % (:nimi (nth % 3)) "- Valitse tehtävä -")
       :valinnat-fn   #(let [urakan-tpi-tehtavat (urakan-toimenpiteet/toimenpideinstanssin-tehtavat
                                                   (:toimenpideinstanssi %)
                                                   toimenpideinstanssit tehtavat-tasoineen)
                             urakan-hoitokauden-yks-hint-tyot (filter
                                                                (fn [tyo]
                                                                  (pvm/sama-pvm? (:alkupvm tyo) (first @u/valittu-hoitokausi)))
                                                                @u/urakan-yks-hint-tyot)
                             yksikkohintaiset-tehtavat (filter
                                                         (fn [tehtava]
                                                           (let [tehtavan-tiedot (first (filter
                                                                                          (fn [tiedot]
                                                                                            (= (:tehtavan_id tiedot) (:id (nth tehtava 3))))
                                                                                          urakan-hoitokauden-yks-hint-tyot))]
                                                             (> (:yksikkohinta tehtavan-tiedot) 0)))
                                                         urakan-tpi-tehtavat)]
                        yksikkohintaiset-tehtavat)
       :leveys        "45%"
       :validoi       [[:ei-tyhja "Valitse tehtävä"]]
       :aseta         (fn [rivi arvo] (assoc rivi
                                        :tehtava arvo
                                        :yksikko (:yksikko (urakan-toimenpiteet/tehtava-idlla arvo nelostason-tehtavat))))}
      {:otsikko "Määrä" :nimi :maara :tyyppi :positiivinen-numero :leveys "25%" :validoi [[:ei-tyhja "Anna määrä"]]}
      {:otsikko "Yks." :nimi :yksikko :tyyppi :string :muokattava? (constantly false) :leveys "15%"}]
     tehtavat]))

(defn yksikkohintainen-toteumalomake
  "Valmiin kohteen tietoja tarkasteltaessa tiedot annetaan valittu-yksikkohintainen-toteuma atomille.
  Lomakkeen käsittelyn ajaksi tiedot haetaan tästä atomista kahteen eri atomiin käsittelyn ajaksi:
  yksikköhintaiset tehtävät ovat omassa ja muut tiedot omassa atomissa.
  Kun lomake tallennetaan, tiedot yhdistetään näistä atomeista yhdeksi kokonaisuudeksi."
  []
  (let [lomake-toteuma (atom (if (empty? @yksikkohintaiset-tyot/valittu-yksikkohintainen-toteuma)
                               (if @u/urakan-organisaatio
                                 (-> (assoc @yksikkohintaiset-tyot/valittu-yksikkohintainen-toteuma :suorittajan-nimi (:nimi @u/urakan-organisaatio))
                                     (assoc :suorittajan-ytunnus (:ytunnus @u/urakan-organisaatio)))
                                 @yksikkohintaiset-tyot/valittu-yksikkohintainen-toteuma)
                               @yksikkohintaiset-tyot/valittu-yksikkohintainen-toteuma))
        lomake-tehtavat (atom (into {}
                                    (map (fn [[id tehtava]]
                                           [id (assoc tehtava :tehtava
                                                              (:id (:tehtava tehtava)))])
                                         (:tehtavat @yksikkohintaiset-tyot/valittu-yksikkohintainen-toteuma))))
        tehtavat-virheet (atom nil)
        jarjestelman-lisaama-toteuma? (true? (:jarjestelman-lisaama @lomake-toteuma))
        valmis-tallennettavaksi? (reaction
                                   (and
                                     ; Validoi toteuma
                                     (not jarjestelman-lisaama-toteuma?)
                                     (not (nil? (:alkanut @lomake-toteuma)))
                                     (not (nil? (:paattynyt @lomake-toteuma)))
                                     (not (pvm/ennen? (:paattynyt @lomake-toteuma) (:alkanut @lomake-toteuma)))
                                     ; Validoi tehtävät
                                     (not (empty? (filter #(not (true? (:poistettu %))) (vals @lomake-tehtavat))))
                                     (empty? @tehtavat-virheet)))]

    (log "Lomake-toteuma: " (pr-str @lomake-toteuma))
    (log "Lomake tehtävät: " (pr-str @lomake-tehtavat))
    (komp/luo
      (fn [ur]
        [:div.toteuman-tiedot
         [napit/takaisin "Takaisin toteumaluetteloon" #(reset! yksikkohintaiset-tyot/valittu-yksikkohintainen-toteuma nil)]
         (if (:toteuma-id @yksikkohintaiset-tyot/valittu-yksikkohintainen-toteuma)
           (if jarjestelman-lisaama-toteuma?
             [:h3 "Tarkastele toteumaa"]
             [:h3 "Muokkaa toteumaa"])
           [:h3 "Luo uusi toteuma"])

         [lomake {:luokka       :horizontal
                  :voi-muokata? (and (roolit/rooli-urakassa? roolit/toteumien-kirjaus (:id @nav/valittu-urakka))
                                     (not jarjestelman-lisaama-toteuma?))
                  :muokkaa!     (fn [uusi]
                                  (log "Muokataan toteumaa: " (pr-str uusi))
                                  (reset! lomake-toteuma uusi))
                  :footer       (when (roolit/rooli-urakassa? roolit/toteumien-kirjaus (:id @nav/valittu-urakka))
                                  [harja.ui.napit/palvelinkutsu-nappi
                                   "Tallenna toteuma"
                                   #(tallenna-toteuma @lomake-toteuma @lomake-tehtavat)
                                   {:luokka       "nappi-ensisijainen"
                                    :disabled     (false? @valmis-tallennettavaksi?)
                                    :kun-onnistuu (fn [vastaus]
                                                    (log "Tehtävät tallennettu, vastaus: " (pr-str vastaus))
                                                    (reset! yksikkohintaiset-tyot/yks-hint-tehtavien-summat (:tehtavien-summat vastaus))
                                                    (reset! lomake-tehtavat nil)
                                                    (reset! lomake-toteuma nil)
                                                    (reset! yksikkohintaiset-tyot/valittu-yksikkohintainen-toteuma nil))}])}
          [(when jarjestelman-lisaama-toteuma?
             {:otsikko     "Lähde" :nimi :luoja :tyyppi :string
              :hae         (fn [rivi] (str "Järjestelmä (" (:luoja rivi) " / " (:organisaatio rivi) ")"))
              :muokattava? (constantly false)
              :vihje       "Tietojärjestelmästä tulleen toteuman muokkaus ei ole sallittu."})
           {:otsikko "Sopimus" :nimi :sopimus :hae (fn [_] (second @u/valittu-sopimusnumero)) :muokattava? (constantly false)}
           {:otsikko "Aloitus" :nimi :alkanut :pakollinen? true :tyyppi :pvm :leveys-col 2 :muokattava? (constantly (not jarjestelman-lisaama-toteuma?))
            :aseta   (fn [rivi arvo]
                       (assoc
                         (if
                           (or
                             (not (:paattynyt rivi))
                             (pvm/jalkeen? arvo (:paattynyt rivi)))
                           (assoc rivi :paattynyt arvo)
                           rivi)
                         :alkanut
                         arvo))
            :validoi [[:ei-tyhja "Valitse päivämäärä"]]
            :varoita [[:urakan-aikana-ja-hoitokaudella]]}
           {:otsikko "Lopetus" :nimi :paattynyt :pakollinen? true :tyyppi :pvm :muokattava? (constantly (not jarjestelman-lisaama-toteuma?)) :validoi [[:ei-tyhja "Valitse päivämäärä"]
                                                                                                                                                       [:pvm-kentan-jalkeen :alkanut "Lopetuksen pitää olla aloituksen jälkeen"]] :leveys-col 2}
           {:otsikko "Tehtävät" :nimi :tehtavat :pakollinen? true :leveys "20%" :tyyppi :komponentti :komponentti [tehtavat-ja-maarat lomake-tehtavat jarjestelman-lisaama-toteuma? tehtavat-virheet]}
           {:otsikko "Suorittaja" :nimi :suorittajan-nimi :pituus-max 256 :tyyppi :string :muokattava? (constantly (not jarjestelman-lisaama-toteuma?))}
           {:otsikko "Suorittajan Y-tunnus" :nimi :suorittajan-ytunnus :pituus-max 256 :tyyppi :string :muokattava? (constantly (not jarjestelman-lisaama-toteuma?))}
           {:otsikko "Lisätieto" :nimi :lisatieto :pituus-max 256 :tyyppi :text :muokattava? (constantly (not jarjestelman-lisaama-toteuma?)) :koko [80 :auto]}]
          @lomake-toteuma]
         (when-not (roolit/rooli-urakassa? roolit/toteumien-kirjaus (:id @nav/valittu-urakka))
           "Käyttäjäroolillasi ei ole oikeutta muokata tätä toteumaa.")]))))

(defn yksiloidyt-tehtavat [rivi tehtavien-summat]
  (let [urakka-id (:id @nav/valittu-urakka)
        [sopimus-id _] @u/valittu-sopimusnumero
        aikavali [(first @u/valittu-hoitokausi) (second @u/valittu-hoitokausi)]
        toteutuneet-tehtavat (atom nil)]
    (go (reset! toteutuneet-tehtavat
                (<! (toteumat/hae-urakan-toteutuneet-tehtavat-toimenpidekoodilla urakka-id sopimus-id aikavali
                                                                                 :yksikkohintainen (:id rivi)))))

    (fn [toteuma-rivi]
      [:div
       [grid/grid
        {:otsikko     (str "Yksilöidyt tehtävät: " (:nimi toteuma-rivi))
         :tyhja       (if (nil? @toteutuneet-tehtavat) [ajax-loader "Haetaan..."] "Toteumia ei löydy")
         :tallenna    #(go (let [vastaus (<! (toteumat/paivita-yk-hint-toteumien-tehtavat urakka-id sopimus-id aikavali :yksikkohintainen %))]
                             (log "Tehtävät tallennettu: " (pr-str vastaus))
                             (reset! toteutuneet-tehtavat (:tehtavat vastaus))
                             (reset! tehtavien-summat (:tehtavien-summat vastaus))))
         :voi-lisata? false
         :tunniste    :tehtava_id}
        [{:otsikko "Päivämäärä" :nimi :alkanut :muokattava? (constantly false) :tyyppi :pvm :hae (comp pvm/pvm :alkanut) :leveys "20%"}
         {:otsikko "Määrä" :nimi :maara :muokattava? (fn [rivi] (not (:jarjestelmanlisaama rivi))) :tyyppi :positiivinen-numero :leveys "20%"}
         {:otsikko "Suorittaja" :nimi :suorittajan_nimi :muokattava? (constantly false) :tyyppi :string :leveys "20%"}
         {:otsikko "Lisätieto" :nimi :lisatieto :muokattava? (constantly false) :tyyppi :string :leveys "20%"}
         {:otsikko "Tarkastele koko toteumaa" :nimi :tarkastele-toteumaa :muokattava? (constantly false) :tyyppi :komponentti :leveys "20%"
          :komponentti (fn [rivi]
                         [:button.nappi-toissijainen.nappi-grid
                          {:on-click
                           #(go (let [toteuma (<! (toteumat/hae-urakan-toteuma urakka-id (:toteuma_id rivi)))]
                                  (log "toteuma: " (pr-str toteuma))
                                  (if-not (k/virhe? toteuma)
                                    (let [lomake-tiedot {:toteuma-id           (:id toteuma)
                                                         :tehtavat             (zipmap (iterate inc 1)
                                                                                       (mapv (fn [tehtava]
                                                                                               (let [tehtava-urakassa (get (first (filter (fn [tehtavat]
                                                                                                                                            (= (:id (get tehtavat 3)) (:tpk-id tehtava)))
                                                                                                                                          @u/urakan-toimenpiteet-ja-tehtavat)) 3)
                                                                                                     emo (get (first (filter (fn [tehtavat]
                                                                                                                               (= (:id (get tehtavat 3)) (:tpk-id tehtava)))
                                                                                                                             @u/urakan-toimenpiteet-ja-tehtavat)) 2)
                                                                                                     tpi (first (filter (fn [tpi] (= (:t3_koodi tpi) (:koodi emo))) @u/urakan-toimenpideinstanssit))]
                                                                                                 (log "Toteuman 4. tason tehtävän 3. tason emo selvitetty: " (pr-str emo))
                                                                                                 (log "Toteuman 4. tason tehtävän toimenpideinstanssi selvitetty: " (pr-str tpi))
                                                                                                 {:tehtava             {:id (:tpk-id tehtava)}
                                                                                                  :maara               (:maara tehtava)
                                                                                                  :tehtava-id          (:tehtava-id tehtava)
                                                                                                  :toimenpideinstanssi (:tpi_id tpi)
                                                                                                  :yksikko             (:yksikko tehtava-urakassa)}))
                                                                                             (:tehtavat toteuma)))
                                                         :alkanut              (:alkanut toteuma)
                                                         :paattynyt            (:paattynyt toteuma)
                                                         :lisatieto            (:lisatieto toteuma)
                                                         :suorittajan-nimi     (:nimi (:suorittaja toteuma))
                                                         :suorittajan-ytunnus  (:ytunnus (:suorittaja toteuma))
                                                         :jarjestelman-lisaama (:jarjestelmanlisaama toteuma)
                                                         :luoja                (:kayttajanimi toteuma)
                                                         :reittipisteet        (:reittipisteet toteuma)
                                                         :organisaatio         (:organisaatio toteuma)}]
                                      (reset! yksikkohintaiset-tyot/valittu-yksikkohintainen-toteuma lomake-tiedot)))))}
                          (ikonit/eye-open) " Toteuma"])}]
        (sort
          (fn [eka toka] (pvm/ennen? (:alkanut eka) (:alkanut toka)))
          (filter (fn [tehtava] (= (:toimenpidekoodi tehtava) (:id toteuma-rivi))) @toteutuneet-tehtavat))]])))

(defn yksikkohintaisten-toteumalistaus
  "Yksikköhintaisten töiden toteumat tehtävittäin"
  []
  (komp/luo
      (fn []
        [:div
         [valinnat/urakan-sopimus-ja-hoitokausi-ja-toimenpide @nav/valittu-urakka]
         [valinnat/urakan-yksikkohintainen-tehtava+kaikki]

         [:button.nappi-ensisijainen {:on-click #(reset! yksikkohintaiset-tyot/valittu-yksikkohintainen-toteuma {})
                                      :disabled (not (roolit/rooli-urakassa? roolit/toteumien-kirjaus (:id @nav/valittu-urakka)))}
          (ikonit/plus) " Lisää toteuma"]

         [grid/grid
          {:otsikko      (str "Yksikköhintaisten töiden toteumat")
           :tyhja        (if (nil? @yksikkohintaiset-tyot/yks-hint-tyot-tehtavittain) [ajax-loader "Haetaan yksikköhintaisten töiden toteumia..."] "Ei yksikköhintaisten töiden toteumia")
           :luokat       ["toteumat-paasisalto"]
           :vetolaatikot (into {} (map (juxt :id (fn [rivi] [yksiloidyt-tehtavat rivi yksikkohintaiset-tyot/yks-hint-tehtavien-summat]))
                                       (filter (fn [rivi]
                                                 (> (:hoitokauden-toteutunut-maara rivi) 0))
                                               @yksikkohintaiset-tyot/yks-hint-tyot-tehtavittain)))
           }
          [{:tyyppi :vetolaatikon-tila :leveys "5%"}
           {:otsikko "Tehtävä" :nimi :nimi :muokattava? (constantly false) :tyyppi :numero :leveys "25%"}
           {:otsikko "Yksikkö" :nimi :yksikko :muokattava? (constantly false) :tyyppi :numero :leveys "10%"}
           {:otsikko "Yksikköhinta" :nimi :yksikkohinta :muokattava? (constantly false) :tyyppi :numero :leveys "10%"}
           {:otsikko "Suunniteltu määrä" :nimi :hoitokauden-suunniteltu-maara :muokattava? (constantly false) :tyyppi :numero :leveys "10%"}
           {:otsikko "Toteutunut määrä" :nimi :hoitokauden-toteutunut-maara :muokattava? (constantly false) :tyyppi :numero :leveys "10%"}
           {:otsikko "Suunnitellut kustannukset" :nimi :hoitokauden-suunnitellut-kustannukset :fmt fmt/euro-opt :muokattava? (constantly false) :tyyppi :numero :leveys "10%"}
           {:otsikko "Toteutuneet kustannukset" :nimi :hoitokauden-toteutuneet-kustannukset :fmt fmt/euro-opt :muokattava? (constantly false) :tyyppi :numero :leveys "10%"}
           {:otsikko "Budjettia jäljellä" :nimi :kustannuserotus :muokattava? (constantly false) :tyyppi :komponentti :komponentti
            (fn [rivi] (if (>= (:kustannuserotus rivi) 0)
                         [:span.kustannuserotus.kustannuserotus-positiivinen (fmt/euro-opt (:kustannuserotus rivi))]
                         [:span.kustannuserotus.kustannuserotus-negatiivinen (fmt/euro-opt (:kustannuserotus rivi))])) :leveys "10%"}]
          @yksikkohintaiset-tyot/yks-hint-tyot-tehtavittain]])))

(defn yksikkohintaisten-toteumat []
  (komp/luo
    (komp/lippu yksikkohintaiset-tyot/yksikkohintaiset-tyot-nakymassa? yksikkohintaiset-tyot/karttataso-yksikkohintainen-toteuma)
    (fn []
      [:span
       [kartta/kartan-paikka]
       (if @yksikkohintaiset-tyot/valittu-yksikkohintainen-toteuma
         [yksikkohintainen-toteumalomake]
         [yksikkohintaisten-toteumalistaus])])))