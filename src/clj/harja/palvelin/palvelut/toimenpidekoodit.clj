(ns harja.palvelin.palvelut.toimenpidekoodit
  (:require [com.stuartsierra.component :as component]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelu poista-palvelu]]
            [harja.kyselyt.toimenpidekoodit :as q]
            [clojure.java.jdbc :as jdbc]
            [harja.kyselyt.konversio :as konv]
            [harja.domain.oikeudet :as oikeudet]
            [cheshire.core :as cheshire]
            [clojure.java.io :as io]))

(defn hae-toimenpidekoodit
  "Palauttaa toimenpidekoodit listana"
  [db kayttaja]
  (into []
        (map #(konv/array->vec % :hinnoittelu))
        (q/hae-kaikki-toimenpidekoodit db)))

(defn lisaa-toimenpidekoodi
  "Lisää toimenpidekoodin, sisään tulevassa koodissa on oltava :nimi, :emo ja :yksikko. Emon on oltava 3. tason koodi."
  [db user {:keys [nimi emo yksikko hinnoittelu api-seuranta] :as rivi}]
  (let [luotu (q/lisaa-toimenpidekoodi<! db nimi emo yksikko (konv/seq->array hinnoittelu) api-seuranta (:id user))]
    {:taso 4
     :emo emo
     :nimi nimi
     :yksikko yksikko
     :hinnoittelu hinnoittelu
     :apiseuranta api-seuranta
     :id (:id luotu)}))

(defn poista-toimenpidekoodi
  "Merkitsee toimenpidekoodin poistetuksi. Palauttaa true jos koodi merkittiin poistetuksi, false muuten."
  [db user id]
  (= 1 (q/poista-toimenpidekoodi! db (:id user) id)))

(defn muokkaa-toimenpidekoodi
  "Muokkaa toimenpidekoodin nimeä ja yksikköä. Palauttaa true jos muokkaus tehtiin, false muuten."

  [db user {:keys [nimi emo yksikko id hinnoittelu api-seuranta] :as rivi}]
  (= 1 (q/muokkaa-toimenpidekoodi! db (:id user) nimi yksikko (konv/seq->array hinnoittelu) api-seuranta id)))

(defn tallenna-tehtavat [db user {:keys [lisattavat muokattavat poistettavat]}]
  (oikeudet/vaadi-kirjoitusoikeus oikeudet/hallinta-tehtavat user)
  (jdbc/with-db-transaction [c db]
    (doseq [rivi lisattavat]
      (lisaa-toimenpidekoodi c user rivi))
    (doseq [rivi muokattavat]
      (muokkaa-toimenpidekoodi c user rivi))
    (doseq [id poistettavat]
      (poista-toimenpidekoodi c user id))
    (hae-toimenpidekoodit c user)))

(defn hae-reaaliaikaseurannan-tehtavat []
  (let [json (clojure.walk/keywordize-keys
               (cheshire/decode
                 (slurp (io/resource "api/schemas/tyokoneenseurannan-kirjaus-request.schema.json"))))
        tehtavat (get-in json [:properties :havainnot :items :properties :havainto :properties :suoritettavatTehtavat
                               :items :enum])]
    (mapv #(hash-map :nimi %) tehtavat)))

(defrecord Toimenpidekoodit []
  component/Lifecycle
  (start [this]
    (doto (:http-palvelin this)
      (julkaise-palvelu
        :hae-toimenpidekoodit
        (fn [kayttaja]
          (hae-toimenpidekoodit (:db this) kayttaja))
        {:last-modified (fn [user]
                          (:muokattu (first (q/viimeisin-muokkauspvm (:db this)))))})
      (julkaise-palvelu
        :tallenna-tehtavat
        (fn [user tiedot]
          (tallenna-tehtavat (:db this) user tiedot)))
      (julkaise-palvelu
        :hae-reaaliaikaseurannan-tehtavat
        (fn [_]
          (hae-reaaliaikaseurannan-tehtavat))))
    this)

  (stop [this]
    (doseq [p [:hae-toimenpidekoodit :tallenna-tehtavat :hae-reaaliaikaseurannan-tehtavat]]
      (poista-palvelu (:http-palvelin this) p))
    this))