(ns harja.palvelin.palvelut.tilannekuva
  (:require [com.stuartsierra.component :as component]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelu poista-palvelut]]
            [harja.kyselyt.konversio :as konv]
            [harja.domain.roolit :as roolit]

            [taoensso.timbre :as log]

            [harja.kyselyt.kayttajat :as kayttajat-q]
            [harja.kyselyt.urakat :as urakat-q]
            [harja.kyselyt.tilannekuva :as q]
            [harja.geo :as geo]))

(defn tulosta-virhe! [asiat e]
  (log/error (str "*** ERROR *** Yritettiin hakea tilannekuvaan " asiat ", mutta virhe tapahtui: " (.getMessage e))))

(defn tulosta-tulos! [asiaa tulos]
  (if (vector? tulos)
    (log/debug (str "  - " (count tulos) " " asiaa))
    (log/debug (str "  - " (count (keys tulos)) " " asiaa)))
  tulos)

(defn haettavat [s]
  (into #{} (keep (fn [[avain arvo]] (when arvo avain)) s)))

(defn kayttajan-urakoiden-idt
  [db user urakka-id urakoitsija urakkatyyppi hallintayksikko alku loppu]
  (roolit/vaadi-lukuoikeus-urakkaan user urakka-id)

  (cond
    (vector? urakka-id) urakka-id

    (not (nil? urakka-id)) [urakka-id]

    (get (:roolit user) "jarjestelmavastuuhenkilo")
    (mapv :id (urakat-q/hae-kaikki-urakat-aikavalilla db (konv/sql-date alku) (konv/sql-date loppu)
                                                      urakoitsija (name urakkatyyppi) hallintayksikko))

    :else (mapv :urakka_id (kayttajat-q/hae-kayttajan-urakat-aikavalilta db (:id user)
                                                                         (konv/sql-date alku) (konv/sql-date loppu)
                                                                         urakoitsija (name urakkatyyppi) hallintayksikko))))

(defn- hae-ilmoitukset
  [db user {{:keys [tyypit tilat]} :ilmoitukset :as tiedot} urakat]
  (let [haettavat (haettavat tyypit)]
    (when-not (empty? haettavat)
      (try
        (let [suljetut? (if (:suljetut tilat) true false)
              avoimet? (if (:avoimet tilat) true false)]
          (mapv
            #(assoc % :uusinkuittaus
                      (when-not (empty? (:kuittaukset %))
                        (:kuitattu (last (sort-by :kuitattu (:kuittaukset %))))))
            (konv/sarakkeet-vektoriin
              (into []
                    (comp
                      (geo/muunna-pg-tulokset :sijainti)
                      (map konv/alaviiva->rakenne)
                      (map #(assoc % :urakkatyyppi (keyword (:urakkatyyppi %))))
                      (map #(konv/array->vec % :selitteet))
                      (map #(assoc % :selitteet (mapv keyword (:selitteet %))))
                      (map #(assoc-in
                             %
                             [:kuittaus :kuittaustyyppi]
                             (keyword (get-in % [:kuittaus :kuittaustyyppi]))))
                      (map #(assoc % :ilmoitustyyppi (keyword (:ilmoitustyyppi %))))
                      (map #(assoc-in % [:ilmoittaja :tyyppi] (keyword (get-in % [:ilmoittaja :tyyppi])))))
                    (q/hae-ilmoitukset db urakat avoimet? suljetut? (mapv name haettavat)))
              {:kuittaus :kuittaukset})))
        (catch Exception e
          (tulosta-virhe! "ilmoituksia" e)
          nil)))))

(defn- hae-paallystystyot
  [db user {:keys [alku loppu yllapito]} urakat]
  (when (:paallystys yllapito)
    (try
      (into []
            (comp
              (geo/muunna-pg-tulokset :sijainti)
              (map konv/alaviiva->rakenne)
              (map #(konv/string->avain % [:tila])))
            (q/hae-paallystykset db))
      (catch Exception e
        (tulosta-virhe! "paallystyksia" e)
        nil))))

(defn- hae-paikkaustyot
  [db user {:keys [alku loppu yllapito]} urakat]
  (when (:paikkaus yllapito)
    (try
      (into []
            (comp
              (geo/muunna-pg-tulokset :sijainti)
              (map konv/alaviiva->rakenne)
              (map #(konv/string->avain % [:tila])))
            (q/hae-paikkaukset db))
      (catch Exception e
        (tulosta-virhe! "paikkauksia" e)
        nil))))

(defn- hae-laatupoikkeamat
  [db user {:keys [alku loppu laadunseuranta]} urakat]
  (when (:laatupoikkeamat laadunseuranta)
    (try
      (into []
            (comp
              (geo/muunna-pg-tulokset :sijainti)
              (map konv/alaviiva->rakenne)
              (map #(assoc % :selvitys-pyydetty (:selvityspyydetty %)))
              (map #(dissoc % :selvityspyydetty))
              (map #(assoc % :tekija (keyword (:tekija %))))
              (map #(update-in % [:paatos :paatos]
                               (fn [p]
                                 (when p (keyword p)))))
              (map #(update-in % [:paatos :kasittelytapa]
                               (fn [k]
                                 (when k (keyword k)))))
              (map #(if (nil? (:kasittelyaika (:paatos %)))
                     (dissoc % :paatos)
                     %)))
            (q/hae-laatupoikkeamat db urakat alku loppu))
      (catch Exception e
        (tulosta-virhe! "laatupoikkeamia" e)
        nil))))

(defn- hae-tarkastukset
  [db user {:keys [alku loppu laadunseuranta]} urakat]
  (when (:tarkastukset laadunseuranta)
    (try
      (into []
            (comp
              (geo/muunna-pg-tulokset :sijainti)
              (map konv/alaviiva->rakenne)
              (map #(konv/string->keyword % :tyyppi))
              (map (fn [tarkastus]
                     (condp = (:tyyppi tarkastus)
                       :talvihoito (dissoc tarkastus :soratiemittaus)
                       :soratie (dissoc tarkastus :talvihoitomittaus)
                       :tiesto (dissoc tarkastus :soratiemittaus :talvihoitomittaus)
                       :laatu (dissoc tarkastus :soratiemittaus :talvihoitomittaus)
                       :pistokoe (dissoc tarkastus :soratiemittaus :talvihoitomittaus)))))
            (q/hae-tarkastukset db urakat alku loppu))
      (catch Exception e
        (tulosta-virhe! "tarkastuksia" e)
        nil))))

(defn- hae-turvallisuuspoikkeamat
  [db user {:keys [alku loppu turvallisuus]} urakat]
  (when (:turvallisuuspoikkeamat turvallisuus)
    (try
      (konv/sarakkeet-vektoriin
        (into []
              (comp
                (map konv/alaviiva->rakenne)
                (geo/muunna-pg-tulokset :sijainti)
                (map #(konv/array->vec % :tyyppi))
                (map #(assoc % :tyyppi (mapv keyword (:tyyppi %)))))
              (q/hae-turvallisuuspoikkeamat db urakat alku loppu))
        {:korjaavatoimenpide :korjaavattoimenpiteet})
      (catch Exception e
        (tulosta-virhe! "turvallisuuspoikkeamia" e)
        nil))))

(defn- hae-tyokoneet
  [db user {:keys [alue alku loppu talvi kesa urakka-id]} urakat]
  (let [haettavat-toimenpiteet (haettavat (merge talvi kesa))
        tpi-str (str "{" (clojure.string/join "," haettavat-toimenpiteet) "}")]
    (when-not (empty? haettavat-toimenpiteet)
      (try
        (into {}
              (comp
                (map #(update-in % [:sijainti] (comp geo/piste-koordinaatit)))
                (map #(update-in % [:edellinensijainti] (fn [pos] (when pos
                                                                    (geo/piste-koordinaatit pos)))))
                (map #(assoc % :tyyppi :tyokone))
                (map #(konv/array->set % :tehtavat))
                (map (juxt :tyokoneid identity)))
              (q/hae-tyokoneet db (:xmin alue) (:ymin alue) (:xmax alue) (:ymax alue)
                               urakka-id tpi-str))
        (catch Exception e
          (tulosta-virhe! "tyokoneet" e)
          nil)))))

(defn- hae-toteumien-reitit
  [db user {:keys [alue alku loppu talvi kesa]} urakat]
  (let [haettavat-toimenpiteet (haettavat (merge talvi kesa))]
    (when-not (empty? haettavat-toimenpiteet)
      (try
        (let [toimenpidekoodit (map :id (q/hae-toimenpidekoodit db haettavat-toimenpiteet))]
          (when-not (empty? toimenpidekoodit)
            (konv/sarakkeet-vektoriin
              (into []
                    (comp
                      (harja.geo/muunna-pg-tulokset :reittipiste_sijainti)
                      (map konv/alaviiva->rakenne)
                      (map #(assoc % :tyyppi :toteuma)))
                    (q/hae-toteumat db alku loppu toimenpidekoodit
                                    (:xmin alue) (:ymin alue) (:xmax alue) (:ymax alue)
                                    urakat))
              {:tehtava     :tehtavat
               :materiaali  :materiaalit
               :reittipiste :reittipisteet})))
        (catch Exception e
          (tulosta-virhe! "toteumaa" e)
          nil)))))

(defn hae-tilannekuvaan
  [db user tiedot]
  (let [urakat (kayttajan-urakoiden-idt db user (:urakka-id tiedot) (:urakoitsija tiedot) (:urakkatyyppi tiedot)
                                        (:hallintayksikko tiedot) (:alku tiedot) (:loppu tiedot))]

    ;; Teoriassa on mahdollista, että käyttäjälle ei (näillä parametreilla) palauteta yhtään urakkaa.
    ;; Tällöin voitaisiin hakea kaikki "julkiset" asiat, esim ilmoitukset joita ei ole sidottu mihinkään
    ;; urakkaan. Käytännössä tästä syntyy ongelmia kyselyissä, sillä tuntuu olevan erittäin vaikeaa tehdä
    ;; kyselyä, joka esim palauttaa ilmoituksen jos a) ilmoitus ei kuulu mihinkään urakkaan TAI b) ilmoitus
    ;; kuuluu listassa olevaan urakkaan _jos lista urakoita ei ole tyhjä_. i.urakka IN (:urakat) epäonnistuu,
    ;; jos annettu lista on tyhjä.
    (when-not (empty? urakat)
      (log/debug "Löydettiin tilannekuvaan sisältöä urakoista: " (pr-str urakat))
      {:toteumat               (tulosta-tulos! "toteumaa"
                                               (hae-toteumien-reitit db user tiedot urakat))
       :tyokoneet              (tulosta-tulos! "tyokonetta"
                                               (hae-tyokoneet db user tiedot urakat))
       :turvallisuuspoikkeamat (tulosta-tulos! "turvallisuuspoikkeamaa"
                                               (hae-turvallisuuspoikkeamat db user tiedot urakat))
       :tarkastukset           (tulosta-tulos! "tarkastusta"
                                               (hae-tarkastukset db user tiedot urakat))
       :laatupoikkeamat        (tulosta-tulos! "laatupoikkeamaa"
                                               (hae-laatupoikkeamat db user tiedot urakat))
       :paikkaus               (tulosta-tulos! "paikkausta"
                                               (hae-paikkaustyot db user tiedot urakat))
       :paallystys             (tulosta-tulos! "paallystysta"
                                               (hae-paallystystyot db user tiedot urakat))
       :ilmoitukset            (tulosta-tulos! "ilmoitusta"
                                               (hae-ilmoitukset db user tiedot urakat))})))

(defrecord Tilannekuva []
  component/Lifecycle
  (start [this]
    (julkaise-palvelu (:http-palvelin this)
                      :hae-tilannekuvaan
                      (fn [user tiedot]
                        (hae-tilannekuvaan (:db this) user tiedot)))
    this)

  (stop [this]
    (poista-palvelut (:http-palvelin this)
                     :hae-tilannekuvaan)

    this))