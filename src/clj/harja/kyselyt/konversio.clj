(ns harja.kyselyt.konversio
  "Usein resultsettien dataa joudutaan jotenkin muuntamaan sopivampaan muotoon Clojure maailmaa varten.
Tähän nimiavaruuteen voi kerätä yleisiä. Yleisesti konversioiden tulee olla funktioita, jotka prosessoivat
yhden rivin resultsetistä, mutta myös koko resultsetin konversiot ovat mahdollisia."
  (:require [cheshire.core :as cheshire]
            [clj-time.coerce :as coerce]
            [clj-time.format :as format]))


(defn yksi
  "Ottaa resultsetin ja palauttaa ensimmäisen rivin ensimmäisen arvon, 
  tämä on tarkoitettu single-value queryille, kuten vaikka yhden COUNT arvon hakeminen."
  [rs]
  (second (ffirst rs)))

(defn organisaatio
  "Muuntaa (ja poistaa) :org_* kentät muotoon :organisaatio {:id ..., :nimi ..., ...}."
  [rivi]
  (-> rivi
      (assoc :organisaatio {:id      (:org_id rivi)
                            :nimi    (:org_nimi rivi)
                            :tyyppi  (some-> rivi :org_tyyppi keyword)
                            :lyhenne (:org_lyhenne rivi)
                            :ytunnus (:org_ytunnus rivi)})
      (dissoc :org_id :org_nimi :org_tyyppi :org_lyhenne :org_ytunnus)))

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

(defn muunna
  "Muuntaa mäpin annetut keyt muunnos-fn funktiolla. Nil arvot menevät läpi sellaisenaan ilman muunnosta."
  [rivi kentat muunnos-fn]
  (loop [rivi rivi
         [k & kentat] kentat]
    (if-not k
      rivi
      (let [arvo (get rivi k)]
        (recur (if arvo
                 (assoc rivi k (muunnos-fn arvo))
                 rivi)
               kentat)))))

(defn string->keyword
  "Muuttaa annetut kentät keywordeiksi, jos ne eivät ole NULL."
  [rivi & kentat]
  (muunna rivi kentat keyword))

(defn decimal->double
  "Muuntaa postgresin tarkan numerotyypin doubleksi."
  [rivi & kentat]
  (muunna rivi kentat double))


(defn array->vec
  "Muuntaa rivin annetun kentän JDBC array tyypistä Clojure vektoriksi."
  [rivi kentta]
  (assoc rivi
    kentta (if-let [a (get rivi kentta)]
             (vec (.getArray a))
             [])))

(defn array->set
  "Muuntaa rivin annetun kentän JDBC array tyypistä Clojure hash setiksi."
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

(defn jsonb->clojuremap [json avain]
  "Muuntaa JSONin Clojuremapiksi"
  (-> json
      (assoc avain
             (some-> json
                     avain
                     .getValue
                     (cheshire/decode true)))))

(defn parsi-json-pvm
  "Muuntaa JSONissa olevan pvm:n Clojurelle sopivaan muotoon."
  [json avainpolku]
  (-> json
      (assoc-in avainpolku
                (when-let [dt (some-> json (get-in avainpolku))]
                  (coerce/to-date (format/parse (format/formatters :date-time) dt))))))

(defn string->avain [data avainpolku]
  "Muuntaa annetussa polussa olevan stringin Clojure-keywordiksi"
  (-> data
      (assoc-in avainpolku (keyword (get-in data avainpolku)))))
