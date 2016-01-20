(ns harja.ui.tierekisteri
  "Tierekisteriosoitteiden näyttämiseen, muokkaamiseen ja karttavalintaan liittyvät komponentit."
  (:require [reagent.core :refer [atom] :as r]
            [harja.loki :refer [log logt tarkkaile!]]
            [harja.ui.komponentti :as komp]
            [harja.views.kartta :as kartta]
            [harja.views.kartta.tasot :as karttatasot]
            [harja.tiedot.navigaatio :as nav]
            [harja.tyokalut.vkm :as vkm]
            [harja.tiedot.tierekisteri :as tierekisteri]
            [cljs.core.async :refer [>! <! alts! chan] :as async]
            [harja.geo :as geo]
            [harja.asiakas.kommunikaatio :as k])

  (:require-macros
    [reagent.ratom :refer [reaction run!]]
    [harja.makrot :refer [nappaa-virhe]]
    [cljs.core.async.macros :refer [go go-loop]]))

(defn laske-tien-pituus [{alkuet :aet loppuet :let}]
  (if (and alkuet loppuet)
    (Math/abs (- loppuet alkuet))))

(defn tieosoite
  "Näyttää tieosoitteen muodossa tienumero/tieosa/alkuosa/alkuetäisyys - tienumero//loppuosa/loppuetäisyys.
  Jos ei kaikkia kenttiä ole saatavilla, palauttaa 'ei saatavilla' -viestin"
  [numero aosa aet losa lopet]
  (let [laita (fn [arvo]
                (if (or
                      (and (number? arvo) (not (nil? arvo)))
                      (not (empty? arvo))) arvo "?"))]
    (if (and numero aosa aet losa lopet)
      [:span (str (laita numero) " / " (laita aosa) " / " (laita aet) " - " (laita losa) " / " (laita lopet))]
      ;; mahdollistetaan pistemisen sijainnin näyttäminen
      (if (and numero aosa aet)
        [:span (str (laita numero) " / " (laita aosa) " / " (laita aet))]
        [:span "Tieosoitetta ei saatavilla"]))))

(defn luo-tooltip [tila-teksti]
  [:div.tr-valitsin-hover
   [:div.tr-valitsin-tila tila-teksti]
   [:div.tr-valitsin-peruuta-esc "Peruuta painamalla ESC."]])

(defn poistu-tr-valinnasta []
  (karttatasot/taso-pois! :tr-alkupiste)
  (kartta/tyhjenna-ohjelaatikko))

(defn pisteelle-ei-loydy-tieta-ilmoitus []
  (kartta/aseta-ohjelaatikon-sisalto [:span
                                      [:span.tr-valitsin-virhe vkm/pisteelle-ei-loydy-tieta]
                                      " "
                                      [:span.tr-valitsin-ohje vkm/vihje-zoomaa-lahemmas]]))

(defn konvertoi-tr-osoitteeksi [osoite]
  {:numero (:tie osoite)
   :alkuosa (:aosa osoite)
   :alkuetaisyys (:aet osoite)
   :geometria (:geometria osoite)
   :loppuosa (:losa osoite)
   :loppuetaisyys (:let osoite)})

(defn konvertoi-pistemaiseksi-tr-osoitteeksi [osoite]
  {:numero (:tie osoite)
   :alkuosa (:aosa osoite)
   :alkuetaisyys (:aet osoite)
   :geometria (:geometria osoite)})

(defn nayta-alkupiste-ohjelaatikossa [osoite]
  (kartta/aseta-ohjelaatikon-sisalto [:span.tr-valitsin-ohje
                                      (str "Valittu alkupiste: "
                                           (:numero osoite) " / "
                                           (:alkuosa osoite) " / "
                                           (:alkuetaisyys osoite))]))

(defn karttavalitsin
  "Komponentti TR-osoitteen (pistemäisen tai välin) valitsemiseen kartalta.
  Asettaa kartan näkyviin, jos se ei ole jo näkyvissä, ja keskittää sen
  löytyneeseen pisteeseen.

  Optiot on mäppi parametreja, jossa seuraavat avaimet:

  :kun-valmis  Funktio, jota kutsutaan viimeisenä kun käyttäjän valinta on valmis.
               Parametrina valittu osoite mäppi, jossa avaimet:
               :numero, :alkuosa, :alkuetaisyys, :loppuosa, :loppuetaisyys
               Jos käyttäjä valitsi pistemäisen osoitteen, loppuosa ja -etäisyys
               avaimia ei ole mäpissä.

  :kun-peruttu Funktio, jota kutsutaan, jos käyttäjä haluaa perua karttavalinnan
               ilman TR-osoitteen päivittämistä. Ei parametrejä.

  :paivita     Funktio, jota kutsutaan kun valittu osoite muuttuu. Esim.
               kun käyttäjä valitsee alkupisteen, kutsutaan tätä funktiota
               osoitteella, jossa ei ole vielä loppupistettä."
  [optiot]
  (let [tapahtumat (chan)
        vkm-haku (chan)
        tila (atom :ei-valittu)
        alkupiste (atom nil)
        tr-osoite (atom {})
        optiot (cljs.core/atom optiot)]

    (go-loop [arvo (<! tapahtumat)]
      (when-let [{:keys [tyyppi sijainti x y]} arvo]
        (case tyyppi
          ;; Hiirtä liikutellaan kartan yllä, aseta tilan mukainen tooltip
          :hover
          (kartta/aseta-tooltip! x y
                                 (luo-tooltip (case @tila
                                                :ei-valittu "Klikkaa alkupiste"
                                                :alku-valittu "Klikkaa loppupiste tai hyväksy pistemäinen painamalla Enter")))

          ;; Enter näppäimellä voi hyväksyä pistemäisen osoitteen
          :enter
          (when (= @tila :alku-valittu)
            ((:kun-valmis @optiot) @tr-osoite)
            (poistu-tr-valinnasta))

          :click
          (if (= :alku-valittu @tila)
            (>! vkm-haku (<! (vkm/koordinaatti->trosoite-kahdella @alkupiste sijainti)))
            (do
              (reset! alkupiste sijainti)
              (>! vkm-haku (<! (vkm/koordinaatti->trosoite sijainti))))))
        
        (recur (<! tapahtumat))))

    (go-loop [osoite (<! vkm-haku)]
      (when osoite
        (if (vkm/virhe? osoite)
          (pisteelle-ei-loydy-tieta-ilmoitus)          
          (let [{:keys [kun-valmis paivita]} @optiot]
            (kartta/tyhjenna-ohjelaatikko)
            (case @tila
              :ei-valittu
              (let [osoite (reset! tr-osoite (konvertoi-pistemaiseksi-tr-osoitteeksi osoite))]
                (paivita osoite)
                (karttatasot/taso-paalle! :tr-alkupiste)
                (reset! tila :alku-valittu) 
                (reset! tierekisteri/valittu-alkupiste (:geometria osoite))
                (nayta-alkupiste-ohjelaatikossa osoite))

              :alku-valittu
              (let [osoite (reset! tr-osoite (konvertoi-tr-osoitteeksi osoite))]
                (poistu-tr-valinnasta)
                (kun-valmis osoite)))))
        
        (recur (<! vkm-haku))))

    (let [kartan-koko @nav/kartan-koko]
      (komp/luo
       {:component-will-receive-props
        (fn [_ _ uudet-optiot]
          (reset! optiot uudet-optiot))}

       (komp/sisaan-ulos #(do
                            (reset! kartta/pida-geometriat-nakyvilla? false) ; Emme halua, että zoom-taso muuttuu kun TR:ää valitaan
                            (reset! nav/kartan-edellinen-koko kartan-koko)
                            (when-not (= :XL kartan-koko) ;;ei syytä pienentää karttaa
                              (nav/vaihda-kartan-koko! :L))
                            (kartta/aseta-kursori! :crosshair))
                         #(do
                            (nav/vaihda-kartan-koko! @nav/kartan-edellinen-koko)
                            (reset! nav/kartan-edellinen-koko nil)
                            (poistu-tr-valinnasta)
                            (kartta/aseta-kursori! nil)))
       (komp/ulos (kartta/kaappaa-hiiri tapahtumat))
       (komp/kuuntelija :esc-painettu
                        (fn [_]
                          (log "optiot: " @optiot)
                          ((:kun-peruttu @optiot))
                          (poistu-tr-valinnasta))
                        :enter-painettu
                        #(go (>! tapahtumat {:tyyppi :enter})))
       (fn [_]                                             ;; suljetaan kun-peruttu ja kun-valittu yli
         [:div.tr-valitsin-teksti.form-control
          [:div (case @tila
                  :ei-valittu "Valitse alkupiste"
                  :alku-valittu "Valitse loppupiste"
                  "")]])))))
