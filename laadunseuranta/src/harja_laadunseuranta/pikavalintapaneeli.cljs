(ns harja-laadunseuranta.pikavalintapaneeli
  (:require [reagent.core :as reagent :refer [atom]]
            [devcards.core :as devcards]
            [harja-laadunseuranta.kuvat :as kuvat]
            [harja-laadunseuranta.utils :refer [avg kahdella-desimaalilla]]
            [harja-laadunseuranta.kitkamittaus :as kitkamittaus]
            [harja-laadunseuranta.arviokomponentti :as arviokomponentti]
            [harja-laadunseuranta.painike :as painike]
            [harja-laadunseuranta.schemas :as schemas])
  (:require-macros [devcards.core :refer [defcard]]))

(defn on-painike [otsikko havainnot avain]
  [:nav.pikavalintapainike
   {:on-click (fn [_]
                (swap! havainnot #(assoc % avain true)))} otsikko])

(defn off-painike [otsikko havainnot avain & {:keys [on-click validi?]
                                              :or {on-click #()
                                                   validi? true}}]
  [:nav.pikavalintapainike.painike-aktiivinen
   {:on-click (fn [_]
                (when validi?
                  (when havainnot
                    (swap! havainnot #(assoc % avain false)))
                  (on-click)))}
   otsikko])

(defn toggle-painike [otsikko havainnot avain & {:keys [on-click ikoni]
                                                 :or {on-click #()}}]
  [:nav.pikavalintapainike
   {:class (when (avain @havainnot) "painike-aktiivinen")
    :on-click (fn [_]
                (swap! havainnot #(update % avain not))
                (on-click))}
   (cond (string? ikoni)
         [:img.pikavalintaikoni {:src ikoni}]
         (vector? ikoni)
         [:span.pikavalintaikoni ikoni])
   [:span.pikavalintaotsikko otsikko]
   (when (avain @havainnot)
         [:span.rec "REC"])])

(defn kertapainike [otsikko toiminto-fn]
  [:nav.pikavalintapainike {:on-click toiminto-fn} otsikko])

(defn avattu-nuoli []
  [:div.avattu [:img {:src kuvat/+avattu+}]])

(defn- kitkamittaustiedot [mittaukset]
  [:div.kitkamittaustiedot
   [:span (str "Mittauksia: " (count @mittaukset))]
   [:span (str "Keskiarvo: " (if (empty? @mittaukset)
                               "-"
                               (kahdella-desimaalilla (avg @mittaukset))))]])

(defn liukkaus-paalla [havainnot kitkamittaus-kirjattu keskiarvo-atom]
  [:div.sidepanel-box
   [off-painike "Liukkaus päättyy" havainnot :liukasta :on-click #(reset! keskiarvo-atom nil)]
   [avattu-nuoli]
   [kitkamittaustiedot keskiarvo-atom]
   [kitkamittaus/kitkamittauskomponentti (fn [mittaus]
                                           (swap! keskiarvo-atom #(conj % mittaus))
                                           (kitkamittaus-kirjattu mittaus))]])

(defn- turn-off [m key]
  (swap! m #(assoc % key false)))

(defn- tee-kirjaus-fn [alivalikot valikko-key kertakirjaus-kirjattu]
  (fn [kertakirjaus]
    (when valikko-key
      (turn-off alivalikot valikko-key))
    (kertakirjaus-kirjattu kertakirjaus)))

(defn lumisuus-paalla [havainnot lumisuus-kirjattu lumimaara]
  [:div.sidepanel-box
   [off-painike "Lumisuus päättyy" havainnot :lumista :on-click #(lumisuus-kirjattu @lumimaara)
                                             :validi? (schemas/validi-lumisuus? @lumimaara)]
   [avattu-nuoli]
   [:div.lumisuustiedot
    (if (schemas/validi-lumisuus? @lumimaara)
      [:span (str (or @lumimaara "-") " cm")]
      [:span.virhe "Virhe: 0-100cm sallittu"])]
   [kitkamittaus/kitkamittaus (atom {:desimaalit ""
                                     :min-length 0
                                     :max-length 3}) #(reset! lumimaara %)]])

(defn tasauspuute-paalla [alivalikot tasaisuus-kirjattu tasaisuus]
  [:div.sidepanel-box
   [off-painike "TASAUSPUUTE päättyy" alivalikot :tasauspuute :on-click #(tasaisuus-kirjattu @tasaisuus)
                                                 :validi? (schemas/validi-tasaisuus? @tasaisuus)]
   [avattu-nuoli]
   [:div.tasaisuustiedot
    (if (schemas/validi-tasaisuus? @tasaisuus)
      [:span (str (or @tasaisuus "-") " cm")]
      [:span.virhe "Virhe: 0-100cm sallittu"])]
   [kitkamittaus/kitkamittaus (atom {:desimaalit ""
                                     :min-length 0
                                     :max-length 3}) #(reset! tasaisuus %)]])

(defn pl-alueet [alivalikot kertakirjaus-kirjattu]
  (let [kirjaa (tee-kirjaus-fn alivalikot :pl-alueet kertakirjaus-kirjattu)]
    [:div.sidepanel-box
     [off-painike "P- & L-alueet" alivalikot :pl-alueet]
     [avattu-nuoli]
     [kertapainike "Alue auraamatta" #(kirjaa :pl-alue-auraamatta)]
     [kertapainike "Alue hiekoittamatta" #(kirjaa :pl-alue-hiekoittamatta)]
     [kertapainike "Epätasainen polanne" #(kirjaa :pl-epatasainen-polanne)]]))

(defn pysakit [alivalikot kertakirjaus-kirjattu]
  (let [kirjaa (tee-kirjaus-fn alivalikot :pysakit kertakirjaus-kirjattu)]
    [:div.sidepanel-box
     [off-painike "Pysäkit" alivalikot :pysakit]
     [avattu-nuoli]
     [kertapainike "Epätasainen polanne" #(kirjaa :pysakilla-epatasainen-polanne)]
     [kertapainike "Auraamatta" #(kirjaa :pysakki-auraamatta)]
     [kertapainike "Hiekoittamatta" #(kirjaa :pysakki-hiekoittamatta)]]))

(defn liikennemerkit [alivalikot kertakirjaus-kirjattu]
  (let [kirjaa (tee-kirjaus-fn alivalikot :liikennemerkit kertakirjaus-kirjattu)]
    [:div.sidepanel-box
     [off-painike "Liikennemerkit" alivalikot :liikennemerkit]
     [avattu-nuoli]
     [kertapainike "Likainen/luminen" #(kirjaa :liikennemerkki-luminen)]]))

(defn soratiet [alivalikot soratiehavainto-kirjattu tasaisuus kiinteys polyavyys]
  [:div.sidepanel-box
   [off-painike "Soratie päättyy" alivalikot :soratie :on-click #(soratiehavainto-kirjattu @tasaisuus @kiinteys @polyavyys)]
   [avattu-nuoli]
   [:div.arviot
    [:div.tasaisuusarvio
     [:span "Tasaisuus"]
     [arviokomponentti/arviokomponentti tasaisuus]]
    [:div.kiinteysarvio
     [:span "Kiinteys"]
     [arviokomponentti/arviokomponentti kiinteys]]
    [:div.polyavyysarvio
     [:span "Pölyävyys"]
     [arviokomponentti/arviokomponentti polyavyys]]]
   [:div.keskenerainen "Koekäytössä"]])

(defn- liikenneymparisto [alivalikot kertakirjaus-kirjattu]
  (let [submenu (atom nil)
        alivalikkoon (fn [key] (reset! submenu key))
        kirjaa (tee-kirjaus-fn alivalikot :liikenneymparisto kertakirjaus-kirjattu)]
    (fn [alivalikot kertakirjaus-kirjattu]
      (condp = @submenu
        :liikennemerkit
        [:div.sidepanel-box
         [kertapainike "Likainen" #(kirjaa :liikennemerkki-luminen)]
         [kertapainike "Vinossa" #(kirjaa :liikennemerkki-vinossa)]]

        :reunapaalut
        [:div.sidepanel-box
         [kertapainike "Likainen" #(kirjaa :reunapaalut-likaisia)]
         [kertapainike "Vinossa" #(kirjaa :reunapaalut-vinossa)]]

        :pl-alueet
        [:div.sidepanel-box
         [kertapainike "Puhdistettava" #(kirjaa :pl-alue-puhdistettava)]
         [kertapainike "Korjattavaa" #(kirjaa :pl-alue-korjattavaa)]
         [kertapainike "Viheralueet hoitamatta" #(kirjaa :viheralueet-hoitamatta)]]

        :rummut
        [:div.sidepanel-box
         [kertapainike "Tukossa" #(kirjaa :rumpu-tukossa)]
         [kertapainike "Liettynyt" #(kirjaa :rumpu-liettynyt)]
         [kertapainike "Rikki" #(kirjaa :rumpu-rikki)]]

        :kaiteet-kiveykset
        [:div.sidepanel-box
         [kertapainike "Kaidevaurio" #(kirjaa :kaidevaurio)]
         [kertapainike "Kiveysvaurio" #(kirjaa :kiveysvaurio)]]

        [:div.sidepanel-box
         [kertapainike "Liikennemerkit" #(alivalikkoon :liikennemerkit)]
         [kertapainike "Reunapaalut" #(alivalikkoon :reunapaalut)]
         [kertapainike "P- ja L-alueet" #(alivalikkoon :pl-alueet)]
         [kertapainike "Rummut" #(alivalikkoon :rummut)]
         [kertapainike "Kaiteet/kiveykset" #(alivalikkoon :kaiteet-kiveykset)]]))))

(defn- viherhoito [alivalikot kertakirjaus-kirjattu]
  (let [kirjaa (tee-kirjaus-fn alivalikot :viherhoito kertakirjaus-kirjattu)]
    [:div.sidepanel-box
     [kertapainike "Vesakko raivaamatta" #(kirjaa :vesakko-raivaamatta)]
     [kertapainike "Niittämättä" #(kirjaa :niittamatta)]
     [kertapainike "Näkemäalue raivaamatta" #(kirjaa :nakemaalue-raivaamatta)]
     [kertapainike "Liikennetila hoitamatta" #(kirjaa :liikennetila-hoitamatta)]
     [kertapainike "Istutukset hoitamatta" #(kirjaa :istutukset-hoitamatta)]]))

(defn- muut [alivalikot kertakirjaus-kirjattu]
  (let [submenu (atom nil)
        alivalikkoon (fn [key] (reset! submenu key))
        kirjaa (tee-kirjaus-fn alivalikot :muut kertakirjaus-kirjattu)]
    (fn [alivalikot kertakirjaus-kirjattu]
      (condp = @submenu
        :sorapientareet
        [:div.sidepanel-box
         [kertapainike "Reunapalletta" #(kirjaa :reunapalletta)]
         [kertapainike "Reunatäyttö puutteellinen" #(kirjaa :reunataytto-puutteellinen)]
         [kertapainike "Luiskavaurio" #(kirjaa :luiskavaurio)]]

        :ojat
        [:div.sidepanel-box
         [kertapainike "Oja tukossa" #(kirjaa :oja-tukossa)]
         [kertapainike "Ylijäämäm. tasattu huonosti" #(kirjaa :ylijaamamassa-tasattu-huonosti)]
         [kertapainike "Kiviä poistamatta" #(kirjaa :ojat-kivia-poistamatta)]]

        :sillat
        [:div.sidepanel-box
         [kertapainike "Puhdistamatta" #(kirjaa :silta-puhdistamatta)]
         [kertapainike "Vaurioita" #(kirjaa :siltavaurioita)]
         [kertapainike "Saumoissa puutteita" #(kirjaa :siltasaumoissa-puutteita)]]

        [:div.sidepanel-box
         [kertapainike "Sorapientareet" #(alivalikkoon :sorapientareet)]
         [kertapainike "Ojat" #(alivalikkoon :ojat)]
         [kertapainike "Sillat" #(alivalikkoon :sillat)]]))))

(defn- paallystyksen-tyovirheluettelo [alivalikot havainnot]
  (let [ikoni #(kuvat/paallystys-tyovirheet %)
        tallennuksia-kaynnissa? (some #(= true (val %)) @havainnot)]
    [:div.painikelaatikko
     [:div.painikerivi
      [toggle-painike "Saumavirhe" havainnot :saumavirhe
       :ikoni (ikoni :saumavirhe)]
      [toggle-painike "Lajittuma" havainnot :lajittuma
       :ikoni (ikoni :lajittuma)]
      [toggle-painike "Epätasaisuus" havainnot :epatasaisuus
       :ikoni (ikoni :epatasaisuus)]]
     [:div.painikerivi
      [toggle-painike "Halkeamat" havainnot :halkeamat
       :ikoni (ikoni :halkeamat)]
      [toggle-painike "Vesilammikot" havainnot :vesilammikot
       :ikoni (ikoni :vesilammikot)]
      [toggle-painike "Epätasaiset reunat" havainnot :epatasaisetreunat
       :ikoni (ikoni :epatasaisetreunat)]]
     [:div.painikerivi
      [toggle-painike "Jyrän jälkiä" havainnot :jyranjalkia
       :ikoni (ikoni :jyranjalkia)]
      [toggle-painike "Sideaineläikkiä" havainnot :sideainelaikkia
       :ikoni (ikoni :sideainelaikkia)]
      [toggle-painike "Väärä korkeusasema" havainnot :vaarakorkeusasema
       :ikoni (ikoni :vaarakorkeusasema)]]
     [:div.painikerivi
      [toggle-painike "Pinta harva" havainnot :pintaharva
       :ikoni (ikoni :pintaharva)]
      [toggle-painike "Pintakuivatus puutteellinen" havainnot :pintakuivatuspuute
       :ikoni (ikoni :pintakuivatuspuute)]
      [toggle-painike "Kaivojen korkeusasema" havainnot :kaivojenkorkeusasema
       :ikoni (ikoni :kaivojenkorkeusasema)]]
     [:div
      [:span.tyhja-nappi]
      [:span.tyhja-nappi]
      [:button.peruuta.nappi-toissijainen {:on-click #(turn-off alivalikot :tyovirheluettelo)} "Sulje"]]]))


(defn- tiemerkinta [alivalikot]
  [:div.painikelaatikko
   [:div "1"]
   [:div.peruuta.nappi-toissijainen {:on-click #(turn-off alivalikot :tiemerkinta)} "Sulje"]])

(defn pikavalintapaneeli [tr-osoite moodi havainnot alivalikot kitkamittaus-kirjattu kertakirjaus-kirjattu yleishavainto-kirjattu
                          lumisuus-kirjattu tasaisuus-kirjattu soratiehavainto-kirjattu keskiarvo-atom lumimaara-atom
                          tasaisuus-atom kiinteys-atom polyavyys-atom]
  (condp = @moodi
    :kelitarkastus
    [:div.sidepanel
     (cond (:liukasta @havainnot)
           [liukkaus-paalla havainnot kitkamittaus-kirjattu keskiarvo-atom]

           (:lumista @alivalikot)
           [lumisuus-paalla alivalikot lumisuus-kirjattu lumimaara-atom]

           (:tasauspuute @alivalikot)
           [tasauspuute-paalla alivalikot tasaisuus-kirjattu tasaisuus-atom]

           (:pl-alueet @alivalikot)
           [pl-alueet alivalikot kertakirjaus-kirjattu]

           (:pysakit @alivalikot)
           [pysakit alivalikot kertakirjaus-kirjattu]

           (:liikennemerkit @alivalikot)
           [liikennemerkit alivalikot kertakirjaus-kirjattu]

           :default
           [:div.sidepanel-box
            [toggle-painike "Liukkaus alkaa" havainnot :liukasta]
            [toggle-painike "Lumisuus alkaa" alivalikot :lumista :on-click #(swap! havainnot assoc :lumista true)]
            [toggle-painike "Tasauspuute alkaa" alivalikot :tasauspuute :on-click #(swap! havainnot assoc :tasauspuute true)]
            [on-painike "P- & L-alueet" alivalikot :pl-alueet]
            [on-painike "Pysäkit" alivalikot :pysakit]
            [on-painike "Liikennemerkit" alivalikot :liikennemerkit]])]

    :soratietarkastus
    [:div.sidepanel
     (cond (:soratie @alivalikot)
           [soratiet alivalikot soratiehavainto-kirjattu tasaisuus-atom kiinteys-atom polyavyys-atom]

           (:liikenneymparisto @alivalikot)
           [liikenneymparisto alivalikot kertakirjaus-kirjattu]

           (:viherhoito @alivalikot)
           [viherhoito alivalikot kertakirjaus-kirjattu]

           (:muut @alivalikot)
           [muut alivalikot kertakirjaus-kirjattu]

           :default
           [:div.sidepanel-box
            [toggle-painike "Soratie alkaa" alivalikot :soratie :on-click #(swap! havainnot assoc :soratie true)]
            [on-painike "Liikenneympäristö" alivalikot :liikenneymparisto]
            [on-painike "Viherhoito" alivalikot :viherhoito]
            [on-painike "Muut" alivalikot :muut]])]

    :paallystys
    [:div.sidepanel
     (cond (:tyovirheluettelo @alivalikot)
           [paallystyksen-tyovirheluettelo alivalikot havainnot]

           (:tiemerkinta @alivalikot)
           [tiemerkinta alivalikot]

           :default
           [:div.sidepanel-box
            [on-painike "Työvirheluettelo" alivalikot :tyovirheluettelo]
            ;[on-painike "Tiemerkintä" alivalikot :tiemerkinta]
            ])]))

(defn lisaa-havainto [aktiivinen on-click on-press]
  [:div.sidepanel-box
   [:nav.pikavalintapainike {:on-click (if aktiivinen
                                         #(on-click true)
                                         on-press)
                             :class (when aktiivinen "painike-aktiivinen")}
    (if aktiivinen
      "Lopeta havaintoväli"
      "Aloita havaintoväli")]
   [:nav.pikavalintapainike {:on-click #(on-click false)}
    [:i.livicon-pen " "]
    "Lisää havainto"]])

(def testihavainnot (atom {:liukasta false}))

(defcard kesatarkastuspaneeli
  (reagent/as-element
   [:div {:style {:width "400px"
                  :height "600px"
                  :border "1px solid black"}}
    [pikavalintapaneeli (atom {}) (atom :kesatarkastus) testihavainnot (atom {}) #() #() #() #() #() #() (atom nil) (atom nil)
     (atom nil) (atom nil) (atom nil)]]))

(defcard talvitarkastuspaneeli
  (reagent/as-element
   [:div {:style {:width "400px"
                  :height "600px"
                  :border "1px solid black"}}
    [pikavalintapaneeli (atom {}) (atom :kelitarkastus) testihavainnot (atom {}) #() #() #() #() #() #() (atom nil) (atom nil)
     (atom nil) (atom nil) (atom nil)]]))
