(ns harja.palvelin.integraatiot.tierekisteri.sanomat.tietolajin-hakukutsu-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [harja.tyokalut.xml :as xml]
            [harja.palvelin.integraatiot.tierekisteri.sanomat.tietolajin-hakukutsu :as tietolajin-hakukutsu])
  (:import (java.text SimpleDateFormat)))

(def +xsd-polku+ "xsd/tierekisteri/skeemat/")

(def +testi-xml+ "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<haeTietolaji><tietolajitunniste>tl506</tietolajitunniste></haeTietolaji>")

(deftest tarkista-kutsu
  (let [kutsu-xml (tietolajin-hakukutsu/muodosta-kutsu "tl506" nil)]
    (is (= +testi-xml+ kutsu-xml) "Muodostettu kutsu on oletetun muotoinen")))

(deftest tarkista-kutsun-validius
  (let [kutsu-xml (tietolajin-hakukutsu/muodosta-kutsu "tl506" nil)
        xsd "haeTietolaji.xsd"]
    (is (xml/validi-xml? +xsd-polku+ xsd kutsu-xml) "Muodostettu kutsu on XSD-skeeman mukainen")
    (is (not (.contains kutsu-xml "muutospvm")))))

(deftest tarkista-paivamaaran-kasittely
  (let [muutospaivamaara (.parse (SimpleDateFormat. "dd.MM.yyyy") "1.1.2015")
        kutsu-xml (tietolajin-hakukutsu/muodosta-kutsu "tl506" muutospaivamaara)]
    (is (.contains kutsu-xml "2015-01-01") "Kutsu sisältää oikein formatoidun muutospäivämäärän")))


