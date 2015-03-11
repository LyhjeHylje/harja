(ns harja.palvelin.komponentit.todennus
  "Tämä namespace määrittelee käyttäjäidentiteetin todentamisen. Käyttäjän todentaminen WWW-palvelussa tehdään KOKA ympäristön antamilla header tiedoilla. Tämä komponentti ei huolehdi käyttöoikeuksista, vaan pelkästään tarkistaa käyttäjän identiteetin."
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [clojure.core.cache :as cache]
            [harja.kyselyt.kayttajat :as q]))


;; Pidetään käyttäjätietoja muistissa vartti, jotta ei tarvitse koko ajan hakea tietokannasta uudestaan.
;; KOKA->käyttäjätiedot pitää hakea joka ikiselle HTTP pyynnölle.
(def kayttajatiedot (atom (cache/ttl-cache-factory {} :ttl (* 15 60 1000))))

(defn koka-remote-id->kayttajatiedot [db koka-remote-id]
  (get (swap! kayttajatiedot
              #(cache/through (fn [id]
                                (let [kt (first  (q/hae-kirjautumistiedot db id))]
                                  (if (nil? kt)
                                    nil
                                    (-> kt
                                        (assoc :organisaatio {:id (:org_id kt)
                                                              :nimi (:org_nimi kt)
                                                              :tyyppi (:org_tyyppi kt)})
                                        (dissoc :org_id :org_nimi :org_tyyppi)))))
                              %
                              koka-remote-id))
       koka-remote-id))

  
(defprotocol Todennus
  "Protokolla HTTP pyyntöjen käyttäjäidentiteetin todentamiseen."
  (todenna-pyynto [this req] "Todenna annetun HTTP-pyynnön käyttäjätiedot, palauttaa uuden req mäpin, jossa käyttäjän tiedot on lisätty avaimella :kayttaja."))

(defrecord HttpTodennus []
  component/Lifecycle
  (start [this]
    (log/info "Todennetaan HTTP käyttäjä KOKA headereista.")
    this)
  (stop [this]
    this)

  Todennus
  (todenna-pyynto [this req]
    (let [headerit (:headers req)
          kayttaja-id (headerit "oam_remote_user")]
      
      ;;(log/info "KOKA: " kayttaja-id)
      (assoc req
        :kayttaja (koka-remote-id->kayttajatiedot (:db this) kayttaja-id)))))

(defrecord FeikkiHttpTodennus [kayttaja]
  component/Lifecycle
  (start [this]
    (log/warn "Käytetään FEIKKI käyttäjätodennusta, käyttäjä = " (pr-str kayttaja))
    this)
  (stop [this]
    this)

  Todennus
  (todenna-pyynto [this req]
    (assoc req
      :kayttaja kayttaja)))

(defn http-todennus []
  (->HttpTodennus))

(defn feikki-http-todennus [kayttaja]
  (->FeikkiHttpTodennus kayttaja))


