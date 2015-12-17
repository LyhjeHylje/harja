(ns harja.palvelin.integraatiot.api.ilmoitusten-haku-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [com.stuartsierra.component :as component]
            [harja.testi :refer :all]
            [harja.jms :refer [feikki-sonja]]
            [harja.palvelin.integraatiot.tloik.tyokalut :refer :all]
            [harja.palvelin.integraatiot.tloik.tloik-komponentti :refer [->Tloik]]
            [harja.palvelin.integraatiot.api.tyokalut :as api-tyokalut]
            [cheshire.core :as cheshire]
            [harja.palvelin.komponentit.sonja :as sonja]
            [harja.palvelin.integraatiot.api.ilmoitukset :as api-ilmoitukset]))

(def kayttaja "jvh")

(def jarjestelma-fixture
  (laajenna-integraatiojarjestelmafixturea
    kayttaja
    :api-ilmoitukset (component/using
                       (api-ilmoitukset/->Ilmoitukset)
                       [:http-palvelin :db :integraatioloki :klusterin-tapahtumat])
    :sonja (feikki-sonja)
    :tloik (component/using
             (->Tloik +tloik-ilmoitusviestijono+
                      +tloik-ilmoituskuittausjono+
                      +tloik-ilmoitustoimenpideviestijono+
                      +tloik-ilmoitustoimenpidekuittausjono+)
             [:db :sonja :integraatioloki :klusterin-tapahtumat])))

(use-fixtures :once jarjestelma-fixture)

(def odotettu-ilmoitus
  {"ilmoittaja"
                        {"sukunimi"           "Meikäläinen",
                         "etunimi"            "Matti",
                         "matkapuhelinnumero" "08023394852",
                         "tyopuhelinnumero"   nil,
                         "email"              "matti.meikalainen@palvelu.fi"},
   "ilmoitustyyppi"     "toimenpidepyynto",
   "otsikko"            "Korkeat vallit",
   "yhteydenottopyynto" false,
   "sijainti"           {"koordinaatit" {"x" 452935.0, "y" 7186873.0}},
   "lahettaja"
                        {"etunimi"            "Pekka",
                         "sukunimi"           "Päivystäjä",
                         "matkapuhelinnumero" nil,
                         "tyopuhelinnumero"   nil,
                         "email"              "pekka.paivystaja@livi.fi"},
   "ilmoitettu"         "2015-09-29T11:49:45Z",
   "ilmoitusid"         123456789,
   "selitteet"
                        [{"selite" "auraustarve"} {"selite" "aurausvallitNakemaesteena"}],
   "tienumero"          4,
   "lyhytselite"        nil,
   "pitkaselite"
                        "Vanhat vallit ovat liian korkeat ja uutta lunta on satanut reippaasti."})

(deftest kuuntele-urakan-ilmoituksia
  (let [vastaus (future (api-tyokalut/get-kutsu ["/api/urakat/4/ilmoitukset"] kayttaja portti))]
    (sonja/laheta (:sonja jarjestelma) +tloik-ilmoitusviestijono+ +testi-ilmoitus-sanoma+)
    (odota #(not (nil? @vastaus)) "Saatiin vastaus ilmoitushakuun." 10000)
    (is (= 200 (:status @vastaus)))

    (let [vastausdata (cheshire/decode (:body @vastaus))
          ilmoitus (get (first (get vastausdata "ilmoitukset")) "ilmoitus")]
      (is (= 1 (count (get vastausdata "ilmoitukset"))))
      (is (= odotettu-ilmoitus ilmoitus)))

    (poista-ilmoitus)))

(deftest hae-muuttuneet-ilmoitukset
  (let [nyt (.format (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ssX") (java.util.Date.))
        _ (do (Thread/sleep 10) ; varmista, että on kulunut joitain millisekunteja
              (u (str "UPDATE ilmoitus SET muokattu = NOW() WHERE urakka = 4 AND id IN (SELECT id FROM ilmoitus WHERE urakka = 4 LIMIT 1)")))
        vastaus (api-tyokalut/get-kutsu ["/api/urakat/4/ilmoitukset?muuttunutJalkeen="
                                         (java.net.URLEncoder/encode nyt)] kayttaja portti)
        kaikkien-ilmoitusten-maara-suoraan-kannasta (ffirst (q
                                                             (str "SELECT count(*) FROM ilmoitus where urakka = 4;")))]
    (println "%%%% VASTAUS: " vastaus)
    (is (= 200 (:status vastaus)))
    (let [vastausdata (cheshire/decode (:body vastaus))
          ilmoitukset (get vastausdata "ilmoitukset")
          ilmoituksia (count ilmoitukset)]
      (is (> kaikkien-ilmoitusten-maara-suoraan-kannasta ilmoituksia))
      (is (= 1 ilmoituksia)))

    (poista-ilmoitus)))