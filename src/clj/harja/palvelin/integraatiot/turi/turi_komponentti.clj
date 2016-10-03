(ns harja.palvelin.integraatiot.turi.turi-komponentti
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [harja.palvelin.integraatiot.integraatioloki :as integraatioloki]
            [harja.kyselyt.turvallisuuspoikkeamat :as q]
            [harja.palvelin.integraatiot.turi.turvallisuuspoikkeamasanoma :as sanoma]
            [harja.palvelin.komponentit.liitteet :as liitteet]
            [harja.palvelin.integraatiot.integraatiotapahtuma :as integraatiotapahtuma]
            [harja.palvelin.tyokalut.ajastettu-tehtava :as ajastettu-tehtava]
            [harja.kyselyt.konversio :as konv])
  (:use [slingshot.slingshot :only [throw+]]))

(defprotocol TurvallisuusPoikkeamanLahetys
  (laheta-turvallisuuspoikkeama [this id]))

(defn tee-lokittaja [this]
  (integraatioloki/lokittaja (:integraatioloki this) (:db this) "turi" "laheta-turvallisuuspoikkeama"))

(defn kasittele-turin-vastaus [db id]
  (q/lokita-lahetys<! db true id)

  ;; TODO Tallenna turi-id, tarvitaan vastaus-skeema tai esimerkki
  (q/tallenna-turvallisuuspoikkeaman-turi-id db turi-id id))

(defn hae-liitteiden-sisallot [liitteiden-hallinta turvallisuuspoikkeama]
  (let [liitteet (:liitteet turvallisuuspoikkeama)]
    (mapv
      (fn [liite]
        (assoc liite
          :data
          (:data
            (liitteet/lataa-liite liitteiden-hallinta (:id liite)))))
      liitteet)))

(defn hae-turvallisuuspoikkeama [liitteiden-hallinta db id]
  (let [turvallisuuspoikkeama (first (konv/sarakkeet-vektoriin
                                       (into []
                                             q/turvallisuuspoikkeama-xf
                                             (q/hae-turvallisuuspoikkeama-lahetettavaksi-turiin db id))
                                       {:korjaavatoimenpide :korjaavattoimenpiteet
                                        :liite :liitteet
                                        :kommentti :kommentit}))]
    (if turvallisuuspoikkeama
      (let [turvallisuuspoikkeama (assoc turvallisuuspoikkeama
                                    :liitteet
                                    (concat (:liitteet turvallisuuspoikkeama)
                                            (mapv :liite (:kommentit turvallisuuspoikkeama))))
            liitteet (hae-liitteiden-sisallot liitteiden-hallinta turvallisuuspoikkeama)]
        (assoc turvallisuuspoikkeama
          :liitteet liitteet))
      (let [virhe (format "Id:llä %s ei löydy turvallisuuspoikkeamaa" id)]
        (log/error virhe)
        (throw+ {:type :tuntematon-turvallisuuspoikkeama
                 :error virhe})))))

(defn laheta-turvallisuuspoikkeama-turiin [{:keys [db integraatioloki liitteiden-hallinta
                                                   url kayttajatunnus salasana]} id]
  (when-not (empty? url)
    (log/debug (format "Lähetetään turvallisuuspoikkeama (id: %s) TURI:n" id))
    (try
      (integraatiotapahtuma/suorita-integraatio
        db integraatioloki "turi" "laheta-turvallisuuspoikkeama" nil
        (fn [konteksti]
          (->> id
               (hae-turvallisuuspoikkeama liitteiden-hallinta db)
               sanoma/muodosta
               (integraatiotapahtuma/laheta
                 konteksti :http {:metodi :POST
                                  :url url
                                  :kayttajatunnus kayttajatunnus
                                  :salasana salasana
                                  :otsikot {"Content-Type" "text/xml"}})
               (kasittele-turin-vastaus db)))
        {:virhekasittelija (fn [_ _] (q/lokita-lahetys<! db false id))})
      (catch Throwable t
        (log/error t (format "Turvallisuuspoikkeaman (id: %s) lähetyksessä TURI:n tapahtui poikkeus" id))))))

(defn laheta-turvallisuuspoikkeamat-turiin [this]
  (let [idt (q/hae-lahettamattomat-turvallisuuspoikkeamat (:db this))]
    (doseq [id idt]
      (laheta-turvallisuuspoikkeama this id))))

(defn tee-paivittainen-lahetys-tehtava [this paivittainen-lahetysaika]
  (if paivittainen-lahetysaika
    (do
      (log/debug "Ajastetaan turvallisuuspoikkeamien lähettäminen joka päivä kello: " paivittainen-lahetysaika)
      (ajastettu-tehtava/ajasta-paivittain
        paivittainen-lahetysaika
        (fn [_] (laheta-turvallisuuspoikkeamat-turiin this))))
    (fn [])))

(defrecord Turi [asetukset]
  component/Lifecycle
  (start [this]
    (let [{url :url kayttajatunnus :kayttajatunnus
           salasana :salasana paivittainen-lahetysaika :paivittainen-lahetysaika} asetukset]
      (log/debug (format "Käynnistetään TURI-komponentti (URL: %s)" url))
      (assoc
        (assoc this
          :url url
          :kayttajatunnus kayttajatunnus
          :salasana salasana)
        :paivittainen-lahetys-tehtava (tee-paivittainen-lahetys-tehtava this paivittainen-lahetysaika))))

  (stop [this]
    (:paivittainen-lahetys-tehtava this)
    this)

  TurvallisuusPoikkeamanLahetys
  (laheta-turvallisuuspoikkeama [this id]
    (laheta-turvallisuuspoikkeama-turiin this id)))

