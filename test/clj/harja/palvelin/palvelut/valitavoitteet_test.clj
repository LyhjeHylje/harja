(ns harja.palvelin.palvelut.valitavoitteet-test
  (:require [clojure.test :refer :all]
            [taoensso.timbre :as log]
            [harja.palvelin.komponentit.tietokanta :as tietokanta]
            [harja.palvelin.palvelut.valitavoitteet :refer :all]
            [harja.testi :refer :all]
            [com.stuartsierra.component :as component]
            [harja.testi :as testi]
            [clj-time.core :as t]
            [clj-time.coerce :as c]))


(defn jarjestelma-fixture [testit]
  (alter-var-root #'jarjestelma
                  (fn [_]
                    (component/start
                      (component/system-map
                        :db (tietokanta/luo-tietokanta testitietokanta)
                        :http-palvelin (testi-http-palvelin)
                        :hae (component/using
                               (->Valitavoitteet)
                               [:http-palvelin :db])))))

  (testit)
  (alter-var-root #'jarjestelma component/stop))


(use-fixtures :once jarjestelma-fixture)

(deftest urakan-valitavoitteiden-haku-toimii
  (let [vastaus (kutsu-palvelua (:http-palvelin jarjestelma)
                                :hae-urakan-valitavoitteet +kayttaja-jvh+ (hae-oulun-alueurakan-2014-2019-id))]

    (log/debug "Vastaus: " vastaus)
    (is (>= (count vastaus) 4))))


(deftest urakan-valitavoitteiden-haku-toimii
  (let [oulun-urakan-valitavoitteet (kutsu-palvelua (:http-palvelin jarjestelma)
                                                    :hae-urakan-valitavoitteet +kayttaja-jvh+
                                                    (hae-oulun-alueurakan-2014-2019-id))
        muhoksen-urakan-valitavoitteet (kutsu-palvelua (:http-palvelin jarjestelma)
                                                       :hae-urakan-valitavoitteet +kayttaja-jvh+
                                                       (hae-muhoksen-paallystysurakan-id))
        lisatyt-valtakunnalliset
        (kutsu-palvelua
          (:http-palvelin jarjestelma)
          :tallenna-valtakunnalliset-valitavoitteet
          +kayttaja-jvh+
          {:valitavoitteet [{:id -2, :nimi "Kertaluontoinen",
                             :takaraja (c/to-date (t/plus (t/now) (t/years 5))),
                             :tyyppi :kertaluontoinen, :urakkatyyppi :hoito,
                             :takaraja-toistopaiva nil, :takaraja-toistokuukausi nil}
                            {:id -5, :nimi "Sepon mökkitien vuosittainen auraus",
                             :takaraja nil, :tyyppi :toistuva,
                             :urakkatyyppi :hoito, :takaraja-toistopaiva 1,
                             :takaraja-toistokuukausi 7}]})
        oulun-urakan-paivitetyt-valitavoitteet (kutsu-palvelua (:http-palvelin jarjestelma)
                                                               :hae-urakan-valitavoitteet +kayttaja-jvh+
                                                               (hae-oulun-alueurakan-2014-2019-id))
        muhoksen-urakan-paivitetyt-valitavoitteet (kutsu-palvelua (:http-palvelin jarjestelma)
                                                                  :hae-urakan-valitavoitteet +kayttaja-jvh+
                                                                  (hae-muhoksen-paallystysurakan-id))]

    ;; Uudet valtakunnalliset lisätty ok
    (is (= (count lisatyt-valtakunnalliset) 2))
    (is (= (count (filter #(= (:tyyppi %) :kertaluontoinen) lisatyt-valtakunnalliset)) 1))
    (is (= (count (filter #(= (:tyyppi %) :toistuva) lisatyt-valtakunnalliset)) 1))

    ;; Oulun hoidon urakalle tuli lisää välitavoitteita
    (is (> (count oulun-urakan-paivitetyt-valitavoitteet) (count oulun-urakan-valitavoitteet)))
    (is (some :valtakunnallinen-id oulun-urakan-paivitetyt-valitavoitteet))
    ;; Muhokselle ei tullut, koska oli eri urakkatyyppi
    (is (= (count muhoksen-urakan-paivitetyt-valitavoitteet) (count muhoksen-urakan-valitavoitteet)))

    ;; Päivitä urakkakohtaista tavoitetta ja sen jälkeen valtakunnallista
    (let [random-tavoite-id-urakassa (first (first (q (str
                                                        "SELECT id FROM valitavoite
                                                         WHERE urakka = 4
                                                         AND valtakunnallinen_valitavoite IS NOT NULL
                                                         AND poistettu IS NOT TRUE
                                                         LIMIT 1;"))))
          _ (is (integer? random-tavoite-id-urakassa))
          _ (u (str "UPDATE valitavoite set muokattu = NOW() WHERE id = " random-tavoite-id-urakassa))
          _ (kutsu-palvelua
              (:http-palvelin jarjestelma)
              :tallenna-valtakunnalliset-valitavoitteet
              +kayttaja-jvh+
              {:valitavoitteet (mapv
                                 #(assoc % :nimi "PÄIVITÄ")
                                 lisatyt-valtakunnalliset)})
          oulun-urakan-paivitetyt-valitavoitteet (kutsu-palvelua (:http-palvelin jarjestelma)
                                                                 :hae-urakan-valitavoitteet +kayttaja-jvh+
                                                                 (hae-oulun-alueurakan-2014-2019-id))]
      ;; Ei muokatut välitavoitteet päivittyivät myös urakkaan
      (is (every? #(= (:nimi %) "PÄIVITÄ")
                  (filter #(and (:valtakunnallinen-id %)
                                (nil? (:muokattu %)))
                          oulun-urakan-paivitetyt-valitavoitteet)))

      ;; Muokatut välitavoitteet eivät päivittyneet
      (is (every? #(not= (:nimi %) "PÄIVITÄ")
                  (filter #(and (:valtakunnallinen-id %)
                                (some? (:muokattu %)))
                          oulun-urakan-paivitetyt-valitavoitteet)))

      ;; Kaikkien linkitettyjen välitavoitteiden "emo" näkyy kuitenkin päivitettynä
      (is (every? #(= (:valtakunnallinen-nimi %) "PÄIVITÄ")
                  (filter :valtakunnallinen-id oulun-urakan-paivitetyt-valitavoitteet)))

      (u (str "DELETE FROM valitavoite WHERE valtakunnallinen_valitavoite IS NOT NULL"))
      (u (str "DELETE FROM valitavoite WHERE urakka IS NULL")))))
