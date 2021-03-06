(ns harja.tiedot.navigaatio
  "Tämä nimiavaruus hallinnoi sovelluksen navigoinnin. Sisältää atomit, joilla eri sivuja ja polkua
  sovelluksessa ohjataan sekä kytkeytyy selaimen osoitepalkin #-polkuun ja historiaan. Tämä nimiavaruus
  ei viittaa itse näkymiin, vaan näkymät voivat hakea täältä tarvitsemansa navigointitiedot."

  (:require
    ;; Reititykset
    [goog.events :as events]
    [goog.Uri :as Uri]
    [goog.history.EventType :as EventType]
    [reagent.core :refer [atom wrap]]
    [cljs.core.async :refer [<! >! chan close!]]

    [harja.loki :refer [log tarkkaile!]]
    [harja.asiakas.tapahtumat :as t]
    [harja.tiedot.urakoitsijat :as urk]
    [harja.tiedot.hallintayksikot :as hy]
    [harja.tiedot.istunto :as istunto]
    [harja.tiedot.urakat :as ur]
    [harja.tiedot.raportit :as raportit]
    [harja.tiedot.navigaatio.reitit :as reitit]
    [harja.atom :refer-macros [reaction<! reaction-writable]]
    [harja.pvm :as pvm]
    [clojure.string :as str]
    [harja.geo :as geo]
    [harja.domain.oikeudet :as oikeudet])

  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction run!]])

  (:import goog.History))


(def valittu-valilehti reitit/valittu-valilehti)
(def valittu-valilehti-atom reitit/valittu-valilehti-atom)
(def aseta-valittu-valilehti! reitit/aseta-valittu-valilehti!)

(def valittu-sivu (reaction (get @reitit/url-navigaatio :sivu)))

(declare kasittele-url! paivita-url valitse-urakka!)

(defonce murupolku-nakyvissa? (reaction (and (not @raportit/raportit-nakymassa?)
                                             (not= @valittu-sivu :tilannekuva)
                                             (not= @valittu-sivu :about)
                                             (not= @valittu-sivu :hallinta))))





(defonce kartan-extent (atom nil))

(defonce kartalla-nakyva-alue
  ;; Näkyvä alue reaktoi siihen mihin zoomataan, mutta kun käyttäjä
  ;; muuttaa zoom-tasoa tai raahaa karttaa, se asetetaan näkyvään alueeseen.
  (atom
    (let [[minx miny maxx maxy] @kartan-extent]
      {:xmin minx :ymin miny
       :xmax maxx :ymax maxy})))

(def kartan-nakyvan-alueen-koko
  (reaction
    ((comp geo/extent-hypotenuusa (juxt :xmin :ymin :xmax :ymax))
      @kartalla-nakyva-alue)))

;; Kartan koko voi olla
;; :hidden (ei näy mitään)
;; :S (näkyy Näytä kartta -nappi)
;; :M (matalampi täysleveä)
;; :L (korkeampi täysleveä)
(def kartan-kokovalinta "Kartan koko" (atom :S))

(defn vaihda-kartan-koko! [uusi-koko]
  (let [vanha-koko @kartan-kokovalinta]
    (when uusi-koko
      (reset! kartan-kokovalinta uusi-koko)
      (t/julkaise! {:aihe :kartan-koko-vaihdettu
                    :vanha-koko vanha-koko
                    :uusi-koko uusi-koko}))))

;; I-vaiheessa aina :tie
(def valittu-vaylamuoto "Tällä hetkellä valittu väylämuoto" (atom :tie))

(def +urakkatyypit+
  [{:nimi "Hoito" :arvo :hoito}
   {:nimi "Tiemerkintä" :arvo :tiemerkinta}
   {:nimi "Päällystys" :arvo :paallystys}
   {:nimi "Paikkaus" :arvo :paikkaus}
   {:nimi "Valaistus" :arvo :valaistus}])

(defn urakkatyyppi-arvolle [tyyppi]
  (first (filter #(= tyyppi (:arvo %))
                 +urakkatyypit+)))

(defn nayta-urakkatyyppi [tyyppi]
  (:nimi (first
           (filter #(= tyyppi (:arvo %))
                   +urakkatyypit+))))

(def valittu-urakoitsija "Suodatusta varten valittu urakoitsija
                         tätä valintaa voi käyttää esim. alueurakoitden
                         urakoitsijakohtaiseen suodatukseen" (atom nil)) ;;(= nil kaikki)

;; Hallintayksikön valinta id:llä (URL parametrista)
(defonce valittu-hallintayksikko-id (atom nil))

;; Atomi, joka sisältää valitun hallintayksikön
(defonce valittu-hallintayksikko
  (reaction (let [id @valittu-hallintayksikko-id
                  yksikot @hy/hallintayksikot]
              (when (and id yksikot)
                (some #(and (= id (:id %)) %) yksikot)))))

;; Jos urakka valitaan id:n perusteella (url parametrilla), asetetaan se tänne
(defonce valittu-urakka-id (atom nil))

;; Atomi, joka sisältää valitun hallintayksikön urakat
(defonce hallintayksikon-urakkalista
  (reaction<! [yks @valittu-hallintayksikko]
              (when yks
                (ur/hae-hallintayksikon-urakat yks))))

;; Atomi, joka sisältää valitun urakan (tai nil)
;; Älä resetoi tätä suoraan, vaan urakan vaihtuessa resetoi valittu-urakka-id
(defonce valittu-urakka
  (reaction
    (let [id @valittu-urakka-id
          urakat @hallintayksikon-urakkalista]
      (when (and id urakat)
        (some #(when (= id (:id %)) %) urakat)))))


(defonce valittu-urakkatyyppi (atom nil))

;; Tällä hetkellä valittu väylämuodosta riippuvainen urakkatyyppi
;; Jos käyttäjällä urakkarooleja, valitaan urakoista yleisin urakkatyyppi
(defonce urakkatyyppi
  (reaction
    (let [oletus-urakkatyyppi (urakkatyyppi-arvolle (:urakkatyyppi @istunto/kayttaja))
          valittu-urakkatyyppi @valittu-urakkatyyppi
          urakan-urakkatyyppi (urakkatyyppi-arvolle (:tyyppi @valittu-urakka))]
      (or urakan-urakkatyyppi valittu-urakkatyyppi oletus-urakkatyyppi))))

(def tarvitsen-isoa-karttaa "Set käyttöliittymänäkymiä (keyword), jotka haluavat pakottaa kartan näkyviin.
  Jos tässä setissä on itemeitä, tulisi kartta pakottaa näkyviin :L kokoisena vaikka se ei olisikaan muuten näkyvissä."
  (atom #{}))

;; jos haluat palauttaa kartan edelliseen kokoon, säilö edellinen koko tähän (esim. Valitse kartalta -toiminto)
(def kartan-edellinen-koko (atom nil))

(def kartan-koko
  "Kartan laskettu koko riippuu kartan kokovalinnasta sekä kartan pakotteista."
  (reaction (let [valittu-koko @kartan-kokovalinta
                  sivu (valittu-valilehti :sivu)
                  v-ur @valittu-urakka
                  tarvitsen-isoa-karttaa @tarvitsen-isoa-karttaa]
              (if-not (empty? tarvitsen-isoa-karttaa)
                :L
                ;; Ei kartan pakotteita, tehdään sivukohtaisia special caseja
                ;; tai palautetaan käyttäjän valitsema koko
                (cond (= sivu :hallinta) :hidden
                      (= sivu :about) :hidden
                      (= sivu :tilannekuva) :XL
                      (and (= sivu :urakat)
                           (not v-ur)) :XL
                      :default valittu-koko)))))

(def kartta-nakyvissa?
  "Kartta ei piilotettu"
  (reaction (let [koko @kartan-koko]
              (and (not= :S koko)
                   (not= :hidden koko)))))


(def kartan-kontrollit-nakyvissa?
  (reaction
    (let [sivu (valittu-valilehti :sivu)]
      ;; Näytetään kartta jos karttaa ei ole pakotettu näkyviin,
      ;; JA ei olla tilannekuvassa, JA joko ei olla urakoissa TAI urakkaa ei ole valittu.
      (and
        (empty? @tarvitsen-isoa-karttaa)
        (not= sivu :tilannekuva)
        (or
          (not= sivu :urakat)
          (some? @valittu-urakka))))))

(defn aseta-hallintayksikko-ja-urakka [hy-id ur]
  (reset! valittu-hallintayksikko-id hy-id)
  (valitse-urakka! ur))

(defn valitse-urakoitsija! [u]
  (reset! valittu-urakoitsija u))

(defn vaihda-urakkatyyppi!
  "Vaihtaa urakkatyypin ja resetoi valitun urakoitsijan, jos kyseinen urakoitsija ei
   löydy valitun tyyppisten urakoitsijain listasta."
  [ut]
  (when (= @valittu-vaylamuoto :tie)
    (reset! valittu-urakkatyyppi ut)
    (swap! valittu-urakoitsija
           #(let [nykyisen-urakkatyypin-urakoitsijat (case (:arvo ut)
                                                       :hoito @urk/urakoitsijat-hoito
                                                       :paallystys @urk/urakoitsijat-paallystys
                                                       :paikkaus @urk/urakoitsijat-paikkaus
                                                       :tiemerkinta @urk/urakoitsijat-tiemerkinta
                                                       :valaistus @urk/urakoitsijat-valaistus)]
             (if (nykyisen-urakkatyypin-urakoitsijat (:id %))
               %
               nil)))))

;; Rajapinta hallintayksikön valitsemiseen, jota viewit voivat kutsua
(defn valitse-hallintayksikko [yks]
  (reset! valittu-hallintayksikko-id (:id yks))
  (reset! valittu-urakka-id nil)
  (paivita-url))

(defonce ilmoita-hallintayksikkovalinnasta
  (run! (let [yks @valittu-hallintayksikko]
          (if yks
            (t/julkaise! (assoc yks :aihe :hallintayksikko-valittu))
            (t/julkaise! {:aihe :hallintayksikkovalinta-poistettu})))))

(defn valitse-urakka! [ur]
  (reset! valittu-urakka-id (:id ur))
  (log "VALITTIIN URAKKA: " (pr-str (dissoc ur :alue)))
  (paivita-url))

(defonce ilmoita-urakkavalinnasta
  (run! (let [ur @valittu-urakka]
          (if ur
            (t/julkaise! (assoc ur :aihe :urakka-valittu))
            (t/julkaise! {:aihe :urakkavalinta-poistettu})))))

(defonce urakka-klikkaus-kuuntelija
  (t/kuuntele! :urakka-klikattu
               ;; FIXME: tämä pitäisi faktoroida elegantimmaksi
               ;; joku tapa pitää olla sanoa, mitä halutaan tapahtuvan kun urakkaa
               ;; klikataan
               ;;
               ;; Ehkä joku pino kartan valintatapahtumien kuuntelijoita, jonne voi lisätä
               ;; itsensä ja ne ajettaisiin uusin ensin. Jos palauttaa true, ei ajeta muita.
               ;; Silloin komponentti voisi ylikirjoittaa valintatapahtumien käsittelyn.

               (fn [urakka]
                 ;;(log "KLIKATTU URAKKAA: " (:nimi urakka))
                 (valitse-urakka! urakka))))

;; Quick and dirty history configuration.
(defonce historia (let [h (History. false)]
                    (events/listen h EventType/NAVIGATE
                                   #(kasittele-url! (.-token %)))
                    h))

(defn nykyinen-url []
  (str (reitit/muodosta-polku @reitit/url-navigaatio)
       "?"
       (when-let [hy @valittu-hallintayksikko] (str "&hy=" (:id hy)))
       (when-let [u @valittu-urakka] (str "&u=" (:id u)))))

;; asettaa oikean sisällön urliin ohjelman tilan perusteella
(defn paivita-url []
  (let [url (nykyinen-url)]
    (when (not= url (.-token historia))
      (log "URL != token :: " url " != " (.getToken historia))
      (.setToken historia url))))

(defn vaihda-sivu!
  "Vaihda nykyinen sivu haluttuun."
  [uusi-sivu]
  (when-not (= (valittu-valilehti :sivu) uusi-sivu)
    (reitit/aseta-valittu-valilehti! :sivu uusi-sivu)))

(def suodatettu-urakkalista "Urakat suodatettuna urakkatyypin ja urakoitsijan mukaan."
  (reaction
    (let [v-ur-tyyppi (:arvo @urakkatyyppi)
          v-urk @valittu-urakoitsija
          urakkalista @hallintayksikon-urakkalista]
      (into []
            (comp (filter #(= v-ur-tyyppi (:tyyppi %)))
                  (filter #(or (nil? v-urk) (= (:id v-urk) (:id (:urakoitsija %)))))
                  (filter #(oikeudet/voi-lukea? oikeudet/urakat (:id %) @istunto/kayttaja)))
            urakkalista))))

(def urakat-kartalla "Sisältää suodatetuista urakoista aktiiviset"
  (reaction (into []
                  (filter #(pvm/ennen? (pvm/nyt) (:loppupvm %)))
                  @suodatettu-urakkalista)))


(def render-lupa-hy? (reaction
                       (some? @hy/hallintayksikot)))

(def render-lupa-u? (reaction
                      (or (nil? @valittu-urakka-id) ;; urakkaa ei annettu urlissa, ei estetä latausta
                          (nil? @valittu-hallintayksikko) ;; hy:tä ei saatu asetettua -> ei estetä latausta
                          (some? @hallintayksikon-urakkalista))))

(def render-lupa-url-kasitelty? (atom false))

;; sulava ensi-render: evätään render-lupa? ennen kuin konteksti on valmiina
(def render-lupa? (reaction
                    (and @render-lupa-hy? @render-lupa-u?
                         @render-lupa-url-kasitelty?)))


(defn kasittele-url!
  "Käsittelee urlin (route) muutokset."
  [url]
  (let [uri (Uri/parse url)
        polku (.getPath uri)
        parametrit (.getQueryData uri)]
    (log "POLKU: " polku)
    (if-let [hy (some-> parametrit (.get "hy") js/parseInt)]
      (if-let [u (some-> parametrit (.get "u") js/parseInt)]
        (do (reset! valittu-hallintayksikko-id hy)
            (reset! valittu-urakka-id u))
        (do
          (reset! valittu-hallintayksikko-id hy)
          (reset! valittu-urakka-id nil))))

    (swap! reitit/url-navigaatio
           reitit/tulkitse-polku polku))
  (reset! render-lupa-url-kasitelty? true)
  (t/julkaise! {:aihe :url-muuttui :url url}))

(.setEnabled historia true)

(defonce paivita-url-navigaatiotilan-muuttuessa
  (add-watch reitit/url-navigaatio
             ::url-muutos
             (fn [_ _ vanha uusi]
               (when (not= vanha uusi)
                 (paivita-url)))))

(defn paivita-urakan-tiedot! [urakka-id funktio & args]
  (swap! hallintayksikon-urakkalista
         (fn [urakat]
           (mapv (fn [urakka]
                   (if (= (:id urakka) urakka-id)
                     (apply funktio urakka args)
                     urakka))
                 urakat))))

