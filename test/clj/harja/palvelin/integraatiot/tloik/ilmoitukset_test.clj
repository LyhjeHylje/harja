(ns harja.palvelin.integraatiot.tloik.ilmoitukset-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clojure.xml :refer [parse]]
            [clojure.zip :refer [xml-zip]]
            [clojure.data.zip.xml :as z]
            [hiccup.core :refer [html]]
            [harja.testi :refer :all]
            [com.stuartsierra.component :as component]
            [harja.palvelin.komponentit.sonja :as sonja]
            [org.httpkit.fake :refer [with-fake-http]]
            [harja.palvelin.integraatiot.tloik.tloik-komponentti :refer [->Tloik]]
            [harja.palvelin.integraatiot.integraatioloki :refer [->Integraatioloki]]
            [harja.jms-test :refer [feikki-sonja]]
            [harja.tyokalut.xml :as xml]
            [harja.palvelin.integraatiot.tloik.tyokalut :refer :all]
            [harja.palvelin.integraatiot.api.ilmoitukset :as api-ilmoitukset]
            [harja.palvelin.integraatiot.api.tyokalut :as api-tyokalut]
            [harja.palvelin.integraatiot.labyrintti.sms :refer [->Labyrintti]]
            [harja.palvelin.integraatiot.labyrintti.sms :as labyrintti]
            [harja.palvelin.integraatiot.sonja.sahkoposti :as sahkoposti]
            [cheshire.core :as cheshire]))

(def kayttaja "yit-rakennus")

(def jarjestelma-fixture
  (laajenna-integraatiojarjestelmafixturea
    kayttaja
    :api-ilmoitukset (component/using
                       (api-ilmoitukset/->Ilmoitukset)
                       [:http-palvelin :db :integraatioloki :klusterin-tapahtumat])
    :sonja (feikki-sonja)
    :sonja-sahkoposti (component/using
                        (sahkoposti/luo-sahkoposti "foo@example.com"
                                                   {:sahkoposti-sisaan-jono "email-to-harja"
                                                    :sahkoposti-sisaan-kuittausjono "email-to-harja-ack"
                                                    :sahkoposti-ulos-jono "harja-to-email"
                                                    :sahkoposti-ulos-kuittausjono "harja-to-email-ack"})
                        [:sonja :db :integraatioloki])
    :labyrintti (component/using
                  (labyrintti/luo-labyrintti
                    {:url "http://localhost:28080/sendsms"
                     :kayttajatunnus "solita-2" :salasana "ne8aCrasesev"})
                  [:db :http-palvelin :integraatioloki])
    :tloik (component/using
             (luo-tloik-komponentti)
             [:db :sonja :integraatioloki :klusterin-tapahtumat :labyrintti])))

(use-fixtures :once jarjestelma-fixture)

(deftest tarkista-uuden-ilmoituksen-tallennus
  (tuo-ilmoitus)
  (let [ilmoitukset (hae-ilmoitus)]
    (is (= 1 (count ilmoitukset)) "Viesti on käsitelty ja tietokannasta löytyy ilmoitus T-LOIK:n id:llä.")
    (is (= "2015-09-29 17:49:45.0" (str (nth (first ilmoitukset) 3))) "Ilmoitusaika on parsittu oikein"))
  (poista-ilmoitus))

(deftest tarkista-ilmoituksen-paivitys
  (tuo-ilmoitus)
  (is (= 1 (count (hae-ilmoitus)))
      "Viesti on käsitelty ja tietokannasta löytyy ilmoitus T-LOIK:n id:llä.")
  (tuo-ilmoitus)
  (is (= 1 (count (hae-ilmoitus)))
      "Kun viesti on tuotu toiseen kertaan, on päivitetty olemassa olevaa ilmoitusta eikä luotu uutta.")
  (poista-ilmoitus))

(deftest tarkista-ilmoituksen-urakan-paattely
  (tuo-ilmoitus)
  (is (= (first (q "select id from urakka where nimi = 'Oulun alueurakka 2014-2019';"))
         (first (q "select urakka from ilmoitus where ilmoitusid = 123456789;")))
      "Urakka on asetettu tyypin ja sijainnin mukaan oikein käynnissäolevaksi Oulun alueurakaksi 2014-2019.")
  (poista-ilmoitus)

  (tuo-paallystysilmoitus)
  (is (= (first (q "select id from urakka where nimi = 'Oulun alueurakka 2014-2019';"))
         (first (q "select urakka from ilmoitus where ilmoitusid = 123456789;")))
      "Urakka on asetettu oletuksena hoidon alueurakalle, kun sijainnissa ei ole käynnissä päällystysurakkaa.")
  (poista-ilmoitus))

(deftest tarkista-viestin-kasittely-ja-kuittaukset
  "Tarkistaa että ilmoituksen saapuessa data on käsitelty oikein, että ilmoituksia API:n kautta kuuntelevat tahot saavat
   viestit ja että kuittaukset on välitetty oikein Tieliikennekeskukseen"
  (let [viestit (atom [])]
    (sonja/kuuntele (:sonja jarjestelma) +tloik-ilmoituskuittausjono+
                    #(swap! viestit conj (.getText %)))

    ;; Ilmoitushausta tehdään future, jotta HTTP long poll on jo käynnissä, kun uusi ilmoitus vastaanotetaan
    (let [ilmoitushaku (future (api-tyokalut/get-kutsu ["/api/urakat/4/ilmoitukset?odotaUusia=true"]
                                                       kayttaja portti))]
      (sonja/laheta (:sonja jarjestelma) +tloik-ilmoitusviestijono+ +testi-ilmoitus-sanoma+)

      (odota-ehdon-tayttymista #(= 1 (count @viestit)) "Kuittaus on vastaanotettu." 100000)

      (let [xml (first @viestit)
            data (xml/lue xml)]
        (is (xml/validi-xml? +xsd-polku+ "harja-tloik.xsd" xml) "Kuittaus on validia XML:ää.")
        (is (= "10a24e56-d7d4-4b23-9776-2a5a12f254af" (z/xml1-> data :viestiId z/text))
            "Kuittauksen on tehty oikeaan viestiin.")
        (is (= "valitetty" (z/xml1-> data :kuittaustyyppi z/text)) "Kuittauksen tyyppi on oikea.")
        (is (empty? (z/xml1-> data :virhe z/text)) "Virheitä ei ole raportoitu."))

      (is (= 1 (count (hae-ilmoitus)))
          "Viesti on käsitelty ja tietokannasta löytyy ilmoitus T-LOIK:n id:llä")

      (let [{:keys [status body]} @ilmoitushaku]
        (is (= 200 status) "Ilmoituksen haku APIsta onnistuu")
        (is (= (-> (cheshire/decode body)
                   (get "ilmoitukset")
                   count) 1) "Ilmoituksia on vastauksessa yksi")))
    (poista-ilmoitus)))

(deftest tarkista-viestin-kasittely-kun-urakkaa-ei-loydy
  (let [sanoma +ilmoitus-ruotsissa+
        viestit (atom [])]
    (sonja/kuuntele (:sonja jarjestelma) +tloik-ilmoituskuittausjono+
                    #(swap! viestit conj (.getText %)))
    (sonja/laheta (:sonja jarjestelma) +tloik-ilmoitusviestijono+ sanoma)

    (odota-ehdon-tayttymista #(= 1 (count @viestit)) "Kuittaus on vastaanotettu." 10000)

    (let [xml (first @viestit)
          data (xml/lue xml)]
      (is (xml/validi-xml? +xsd-polku+ "harja-tloik.xsd" xml) "Kuittaus on validia XML:ää.")
      (is (= "10a24e56-d7d4-4b23-9776-2a5a12f254af" (z/xml1-> data :viestiId z/text))
          "Kuittauksen on tehty oikeaan viestiin.")
      (is (= "virhe" (z/xml1-> data :kuittaustyyppi z/text)) "Kuittauksen tyyppi on oikea.")
      (is (= "Tiedoilla ei voitu päätellä urakkaa." (z/xml1-> data :virhe z/text))
          "Virheitä ei ole raportoitu."))

    (is (= 0 (count (hae-ilmoitus))) "Tietokannasta ei löydy ilmoitusta T-LOIK:n id:llä")
    (poista-ilmoitus)))

(deftest ilmoittaja-kuuluu-urakoitsijan-organisaatioon-merkitaan-vastaanotetuksi
  (try
    (with-fake-http []
      (let [kuittausviestit (atom [])]
        (sonja/kuuntele (:sonja jarjestelma) +tloik-ilmoituskuittausjono+
                        #(swap! kuittausviestit conj (.getText %)))

        (sonja/laheta (:sonja jarjestelma)
                      +tloik-ilmoitusviestijono+
                      +testi-ilmoitus-sanoma-jossa-ilmoittaja-urakoitsija+)

        (odota-ehdon-tayttymista #(= 1 (count @kuittausviestit)) "Kuittaus ilmoitukseen vastaanotettu." 10000)

        (is (= 1 (count (hae-ilmoitustoimenpide))) "Viestille löytyy ilmoitustoimenpide")
        (is (= (ffirst (hae-ilmoitustoimenpide)) "vastaanotto")) "Viesti on käsitelty ja merkitty vastaanotetuksi"

        (poista-ilmoitus)))
    (catch IllegalArgumentException e
      (is false "Lähetystä Labyrintin SMS-Gatewayhyn ei yritetty."))))


(deftest tarkista-ilmoituksen-lahettaminen-valaistusurakalle
  "Tarkistaa että ilmoitus ohjataan oikein valaistusurakalle"
  (tuo-valaistusilmoitus)

  (is (= (first (q "select id from urakka where nimi = 'Oulun valaistuksen palvelusopimus 2013-2018';"))
         (first (q "select urakka from ilmoitus where ilmoitusid = 987654321;")))
      "Urakka on asetettu oletuksena hoidon alueurakalle, kun sijainnissa ei ole käynnissä päällystysurakkaa.")
  (poista-valaistusilmoitus))
