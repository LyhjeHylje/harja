(ns harja.palvelin.palvelut.maksuerat-test
  (:require [clojure.test :refer :all]
            [harja.palvelin.komponentit.tietokanta :as tietokanta]
            [harja.palvelin.palvelut.maksuerat :refer :all]
            [harja.palvelin.palvelut.toimenpidekoodit :refer :all]
            [harja.testi :refer :all]
            [taoensso.timbre :as log]
            [com.stuartsierra.component :as component]))


(defn jarjestelma-fixture [testit]
  (alter-var-root #'jarjestelma
                  (fn [_]
                    (component/start
                      (component/system-map
                        :db (apply tietokanta/luo-tietokanta testitietokanta)
                        :http-palvelin (testi-http-palvelin)
                        :hae-urakan-maksuerat (component/using
                                                (->Maksuerat)
                                                [:http-palvelin :db])))))

  (testit)
  (alter-var-root #'jarjestelma component/stop))


(use-fixtures :once jarjestelma-fixture)

(deftest urakan-maksuerat-haettu-okein-urakalle-1
  (let [maksuerat (kutsu-palvelua (:http-palvelin jarjestelma)
                                  :hae-urakan-maksuerat +kayttaja-jvh+ 1)]
    (is (= 16 (count maksuerat)))
    (is (= (count (filter #(= :kokonaishintainen (:tyyppi %)) maksuerat)) 2))
    (is (= (count (filter #(= :yksikkohintainen (:tyyppi %)) maksuerat)) 2))
    (is (= (count (filter #(= :bonus (:tyyppi %)) maksuerat)) 2))
    (is (= (count (filter #(= :akillinen_hoitotyo (:tyyppi %)) maksuerat)) 2))
    (is (= (count (filter #(= :lisatyo (:tyyppi %)) maksuerat)) 2))
    (is (= (count (filter #(= :sakko (:tyyppi %)) maksuerat)) 2))
    (is (= (count (filter #(= :indeksi (:tyyppi %)) maksuerat)) 2))
    (is (= (count (filter #(= :muu (:tyyppi %)) maksuerat)) 2))))


