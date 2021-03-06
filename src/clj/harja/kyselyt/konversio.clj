(ns harja.kyselyt.konversio
  "Usein resultsettien dataa joudutaan jotenkin muuntamaan sopivampaan muotoon Clojure maailmaa varten.
  Tähän nimiavaruuteen voi kerätä yleisiä. Yleisesti konversioiden tulee olla funktioita, jotka prosessoivat
  yhden rivin resultsetistä, mutta myös koko resultsetin konversiot ovat mahdollisia."
  (:require [cheshire.core :as cheshire]
            [clj-time.coerce :as coerce]
            [taoensso.timbre :as log]
            [clj-time.format :as format]
            [clojure.java.jdbc :as jdbc])
  (:import (clojure.lang Keyword)))


(defn yksi
  "Ottaa resultsetin ja palauttaa ensimmäisen rivin ensimmäisen arvon,
  tämä on tarkoitettu single-value queryille, kuten vaikka yhden COUNT arvon hakeminen."
  [rs]
  (second (ffirst rs)))

(defn organisaatio
  "Muuntaa (ja poistaa) :org_* kentät muotoon :organisaatio {:id ..., :nimi ..., ...}."
  [rivi]
  (-> rivi
      (assoc :organisaatio {:id (:org_id rivi)
                            :nimi (:org_nimi rivi)
                            :tyyppi (some-> rivi :org_tyyppi keyword)
                            :lyhenne (:org_lyhenne rivi)
                            :ytunnus (:org_ytunnus rivi)})
      (dissoc :org_id :org_nimi :org_tyyppi :org_lyhenne :org_ytunnus)))

(defn sarakkeet-vektoriin
  "Muuntaa muodon: [{:id 1 :juttu {:id 1}} {:id 1 :juttu {:id 2}}]
  muotoon:         [{:id 1 :jutut [{:id 1} {:id 2}]}]

  Usein tietokantahakuja tehdessä on tilanne, jolloin 'emorivi' sisältää useamman 'lapsirivin',
  esimerkiksi ilmoitus sisältää 0-n kuittausta. Suoraan tietokantahaussa näitä on kuitenkin
  vaikea yhdistää yhteen tietorakenteeseen. Tämän funktion avulla yhdistäminen onnistuu.
  Muunna aluksi rivien rakenne nested mapiksi alaviiva->rakenne funktiolla, ja syötä
  tulos tälle funktiolle.

  Rivien yhdistäminen tehdän oletusarvoisesti id:n perusteella, mutta 'emorivin' group-by funktion voi
  myös syöttää itse. Esim [{:toteuma {:id 1} :reittipiste {..}} ..] voidaan haluta groupata funktiolla
  #(get-in % [:toteuma :id])

  Parametrit:
  * kaikki-rivit: Vektori mäppejä, jossa yksi 'rivi' sisältää avaimen 'lapselle', joka on mäppi.
    * [{:ilmoitus-id 1 :kuittaus {:kuittaus-id 1}} {:ilmoitus-id 1 :kuittaus {:kuittaus-id 2}}]
  * sarake-vektori: Mäppi joka kertoo, mihin muotoon rivit muutetaan. Avain on
    yhden lapsirivin nimi, arvo on vektorin nimi, johon lapset tallennetaan.
      * (sarakkeet-vektoriin ilmoitukset {:kuittaus :kuittaukset})
  * group-fn: Funktio, joka ryhmittelee rivit ennen sarakkeiden yhdistämistä. Voidaan käyttää esim.
    saman id:n sisältävien rivien yhdistämiseen, koska kuvaavat loogisesti samaa asiaa.
  * lapsi-ok-fn: funktio, joka tarkistaa onko lapsi kelpo, oletuksena :id. Jos funktio palautta
    ei-truthy arvon, lasta ei lisätä vektoriin

  Funktio osaa käsitellä useamman 'lapsirivin' kerralla, tämä onnistuu yksinkertaisesti syöttämällä
  sarake-vektoriin useamman avain-arvo -parin.

  TÄRKEÄÄ! Jos lapsiriviä on useampia, konversio PITÄÄ tehdä yhdellä kutsulla (eli antamalla useampi
  avain-arvo -pari mäppiin). Funktio tunnistaa uniikit rivit kaikista-riveistä poistamalla lapsirivit,
  joten jos kaikkia lapsirivejä ei määrittele, ei konversio toimi oikein."
  ([kaikki-rivit sarake-vektori]
   (sarakkeet-vektoriin kaikki-rivit sarake-vektori :id :id))
  ([kaikki-rivit sarake-vektori group-fn]
   (sarakkeet-vektoriin kaikki-rivit sarake-vektori group-fn :id))
  ([kaikki-rivit sarake-vektori group-fn lapsi-ok-fn]
   (vec
     (for [[id rivit] (group-by group-fn kaikki-rivit)]
       (loop [rivi (first rivit)
              [[sarake vektori] & sarakkeet] (seq sarake-vektori)]
         (if-not sarake
           rivi
           (recur (-> rivi
                      (dissoc sarake)
                      (assoc vektori (vec (into #{}
                                                (keep #(when-let [lapsi (get % sarake)]
                                                        (when (lapsi-ok-fn lapsi)
                                                          lapsi))
                                                      rivit)))))
                  sarakkeet)))))))

(defn alaviiva->rakenne
  "Muuntaa mäpin avaimet alaviivalla sisäiseksi rakenteeksi, esim.
  {:id 1 :urakka_hallintayksikko_nimi \"POP ELY\"} => {:id 1 :urakka {:hallintayksikko {:nimi \"POP ELY\"}}}"
  [m]
  (let [ks (into []
                 (comp (map name)
                       (filter #(when (not= -1 (.indexOf % "_")) %))
                       (map (fn [k]
                              [(keyword k)
                               (into []
                                     (map keyword)
                                     (.split k "_"))])))
                 (keys m))]
    (loop [m m
           [[vanha-key uusi-key] & ks] ks]
      (if-not vanha-key
        m
        (let [arvo (get m vanha-key)]
          (recur (assoc-in (dissoc m vanha-key)
                           uusi-key arvo)
                 ks))))))

(defn vector-mappien-alaviiva->rakenne
  "Muuntaa vectorissa olevien mäppien avaimet alaviivalla sisäiseksi rakenteeksi."
  [vector]
  (mapv #(alaviiva->rakenne %) vector))

(defn muunna
  "Muuntaa mäpin annetut keyt muunnos-fn funktiolla. Nil arvot menevät läpi sellaisenaan ilman muunnosta."
  [rivi kentat muunnos-fn]
  (loop [rivi rivi
         [k & kentat] kentat]
    (if-not k
      rivi
      (if (vector? k)
        (let [arvo (get-in rivi k)]
          (recur (if arvo
                   (assoc-in rivi k (muunnos-fn arvo))
                   rivi)
                 kentat))
        (let [arvo (get rivi k)]
          (recur (if arvo
                   (assoc rivi k (muunnos-fn arvo))
                   rivi)
                 kentat))))))

(defn string->keyword
  "Muuttaa annetut kentät keywordeiksi, jos ne eivät ole NULL."
  [rivi & kentat]
  (muunna rivi kentat keyword))

(defn string-polusta->keyword
  "Muuntaa annetussa polussa olevan stringin Clojure-keywordiksi"
  [data avainpolku]
  (-> data
      (assoc-in avainpolku (keyword (get-in data avainpolku)))))

(defn string-poluista->keyword
  "Muuntaa annetuissa poluissa olevan stringin Clojure-keywordiksi"
  [data avainpolut]
  (reduce (fn [data polku]
            (assoc-in data polku (keyword (get-in data polku))))
          data
          avainpolut))

(defn decimal->double
  "Muuntaa postgresin tarkan numerotyypin doubleksi."
  [rivi & kentat]
  (muunna rivi kentat double))

(defn seq->array
  "Muuntaa yksittäisen arvon Clojure-sekvenssistä JDBC arrayksi."
  [seq]
  (let [kasittele #(if (or (= Keyword (type %)) (= String (type %)))
                    (name %)
                    (str %))]
    (str "{" (clojure.string/join "," (map kasittele seq)) "}")))

(defn string-vector->keyword-vector
  "Muuntaa mapin kentän vectorissa olevat stringit keywordeiksi."
  [rivi kentta]
  (assoc rivi kentta (mapv keyword (kentta rivi))))

(defn string-set->keyword-set
  "Muuntaa mapin kentän setissä olevat stringit keywordeiksi."
  [rivi kentta]
  (assoc rivi kentta (set (map keyword (kentta rivi)))))

(defn array->vec
  "Muuntaa rivin annetun kentän JDBC array tyypistä Clojure-vektoriksi."
  [rivi kentta]
  (assoc rivi
    kentta (if-let [a (get rivi kentta)]
             (vec (.getArray a))
             [])))

(defn array->set
  "Muuntaa rivin annetun kentän JDBC array tyypistä Clojure hash setiksi.
  Yhden arityn versio ottaa JDBC arrayn ja paluttaa setin ilman mäppiä."
  ([a]
   (into #{} (and a (.getArray a))))
  ([rivi kentta] (array->set rivi kentta identity))
  ([rivi kentta muunnos]
   (assoc rivi
     kentta (if-let [a (get rivi kentta)]
              (into #{} (map muunnos (.getArray a)))
              #{}))))

(defn array->keyword-set
  "Muuntaa rivin annentun kentän JDBC array tyypistä Clojure keyword hash setiksi."
  [rivi kentta]
  (array->set rivi kentta keyword))

(defn sql-date
  "Luo java.sql.Date objektin annetusta java.util.Date objektista."
  [^java.util.Date dt]
  (when dt
    (java.sql.Date. (.getTime dt))))

(defn sql-timestamp
  "Luo java.sql.Timestamp objektin annetusta java.util.Date objektista."
  [^java.util.Date dt]
  (when dt
    (java.sql.Timestamp. (.getTime dt))))

(defn java-date
  "Luo java.util.Date objektin annetusta java.sql.Date  objektista."
  [^java.sql.Date dt]
  (when dt
    (java.util.Date. (.getTime dt))))

(defn jsonb->clojuremap
  "Muuntaa JSONin Clojuremapiksi"
  ([json]
   (some-> json
           .getValue
           (cheshire/decode true)))
  ([json avain]
   (-> json
       (assoc avain
              (some-> json
                      avain
                      .getValue
                      (cheshire/decode true))))))


(extend-protocol jdbc/ISQLValue
  java.util.Date
  (sql-value [v]
    (sql-timestamp v)))
