(ns harja.palvelin.integraatiot.api.siltatarkastukset
  "Siltatarkstuksien API-kutsut"
  (:require [com.stuartsierra.component :as component]
            [compojure.core :refer [POST GET DELETE PUT]]
            [taoensso.timbre :as log]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-reitti poista-palvelut]]
            [harja.palvelin.integraatiot.api.tyokalut.kutsukasittely :refer [tee-sisainen-kasittelyvirhevastaus tee-viallinen-kutsu-virhevastaus tee-vastaus]]
            [harja.palvelin.integraatiot.api.tyokalut.json-skeemat :as json-skeemat]
            [harja.palvelin.integraatiot.api.tyokalut.kutsukasittely :refer [kasittele-kutsu]]
            [harja.kyselyt.siltatarkastukset :as silta-q]
            [clojure.java.jdbc :as jdbc]
            [harja.palvelin.integraatiot.api.tyokalut.validointi :as validointi])
  (:use [slingshot.slingshot :only [try+ throw+]]))

(defn api-tulos->kirjain [tulos-nimi]
  (case tulos-nimi
    "eiToimenpiteita" "A"
    "puhdistettava" "B"
    "urakanKunnostettava" "C"
    "korjausOhjelmoitava" "D"))

(defn luo-siltatarkastus [ulkoinen-id urakka-id tarkastus silta kayttaja db]
  (log/debug "Luodaan uusi siltarkastus")
  (silta-q/luo-siltatarkastus<!
    db
    (:id silta)
    urakka-id
    (:tarkastusaika tarkastus)
    (str (get-in tarkastus [:tarkastaja :etunimi]) " " (get-in tarkastus [:tarkastaja :sukunimi]))
    (:id kayttaja)
    false
    ulkoinen-id))

(defn paivita-siltatarkastus [ulkoinen-id urakka-id silta tarkastus kayttaja db]
  (log/debug "Päivitetään vanha siltarkastus")
  (silta-q/paivita-siltatarkastus<!
    db
    silta
    urakka-id
    (:tarkastusaika tarkastus)
    (str (get-in tarkastus [:tarkastaja :etunimi]) " " (get-in tarkastus [:tarkastaja :sukunimi]))
    (:id kayttaja)
    false
    ulkoinen-id))

(defn luo-tai-paivita-siltatarkastus [ulkoinen-id urakka-id tarkastus silta kayttaja db]
  (let [siltatarkastus-kannassa (first (silta-q/hae-siltatarkastus-ulkoisella-idlla-ja-luojalla db ulkoinen-id (:id kayttaja)))]
    (if siltatarkastus-kannassa
      (paivita-siltatarkastus ulkoinen-id urakka-id tarkastus silta kayttaja db)
      (luo-siltatarkastus ulkoinen-id urakka-id tarkastus silta kayttaja db))))

#_(defn lisaa-siltatarkastuskohteet [ulkoinen-id tarkastus silta kayttaja db]
  (let [kohteet (:sillantarkastuskohteet tarkastus)
        aluerakenne (:aluerakenne kohteet)
        paallysrakenne (:paallystysrakenne kohteet)
        varusteet-ja-laitteet (:varusteetJaLaitteet kohteet)
        siltapaikan-rakenteet (:siltapaikanRakenteet kohteet)]
    (silta-q/poista-siltatarkastuskohteet! siltatarkastus-id)
    ;; Aluerakenne
    (silta-q/luo-siltatarkastuksen-kohde<! (api-tulos->kirjain (:maatukienSiisteysJaKunto aluerakenne))
                                           lisatieto
                                           siltatarkastus-id
                                           kohde)
    ;; Päällystysrakenne
    ;; TODO
    ;; Varusteet ja laitteet
    ;; TODO
    ;; Siltapaikan rakenteet
    ;; TODO
    ))

(defn lisaa-siltatarkastus [{id :id} data kayttaja db]
  (log/debug "DB on " (pr-str db))
  (log/info "Kirjataan siltatarkastus käyttäjältä: " kayttaja)
  (let [urakka-id (Integer/parseInt id)]
    (validointi/tarkista-urakka-ja-kayttaja db urakka-id kayttaja)
    (jdbc/with-db-transaction [transaktio db]
                              (let [ulkoinen-id (get-in [:siltatarkastus :tunniste :id] data)
                                    silta (silta-q/hae-silta-numerolla (get-in [:siltatarkastus :siltanumero] data))]
                                (log/debug "Siltanumerolla löydetty silta: " (pr-str silta))
                                (if silta
                                  (do
                                    (luo-tai-paivita-siltatarkastus ulkoinen-id urakka-id (get-in [:siltatarkastus :siltanumero] data) silta kayttaja transaktio)
                                    #_(lisaa-siltatarkastuskohteet ulkoinen-id (get-in [:siltatarkastus :siltanumero] data) silta kayttaja transaktio))
                                  ; FIXME Virhe: siltaa ei löydy
                                  )))))

(defrecord Siltatarkastukset []
  component/Lifecycle
  (start [{http :http-palvelin db :db integraatioloki :integraatioloki :as this}]
    (julkaise-reitti
      http :lisaa-siltatarkastus
      (POST "/api/urakat/:id/tarkastus/siltatarkastus" request
        (kasittele-kutsu db integraatioloki :lisaa-siltatarkastus request json-skeemat/+siltatarkastuksen-kirjaus+ nil
                         (fn [parametrit data kayttaja db]
                           (lisaa-siltatarkastus parametrit data kayttaja db)))))
    this)

  (stop [{http :http-palvelin :as this}]
    (poista-palvelut http
                     :lisaa-siltatarkastus)
    this))