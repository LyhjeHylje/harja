(ns harja.palvelin.integraatiot.integraatiotapahtuma
  (:require [taoensso.timbre :as log]
            [harja.palvelin.integraatiot.integraatiopisteet.http :as http]
            [harja.palvelin.integraatiot.integraatioloki :as integraatioloki]
            [clojure.string :as str]))

(defmulti laheta-integraatioviesti (fn [konteksti tyyppi asetukset payload] tyyppi))
(defmethod laheta-integraatioviesti :http [{:keys [lokittaja tapahtuma-id] :as konteksti} _
                                           {:keys [metodi url otsikot parametrit] :as asetukset} payload]
  (let [asetukset (dissoc asetukset :metodi :url :otsikot :parametrit)
        http-piste (http/luo-integraatiopiste lokittaja tapahtuma-id asetukset)]
    (case metodi
      :GET (http/GET http-piste url otsikot parametrit)
      :POST (http/POST http-piste url otsikot parametrit payload)
      :HEAD (http/HEAD http-piste url otsikot parametrit))))

(defprotocol Integraatiotapahtuma
  (laheta
    [this pistetyyppi asetukset]
    [this pistetyyppi asetukset payload])

  (vastaanota
    [this pistetyyppi asetukset])

  (lisaa-tietoja
    [this & tiedot]))

(defrecord IntegraatioTapahtumaKonteksti [tapahtuma-id lokittaja ulkoinen-id lisatiedot]
  Integraatiotapahtuma

  (laheta [this pistetyyppi asetukset]
    (laheta-integraatioviesti this pistetyyppi asetukset nil))

  (laheta [this pistetyyppi asetukset payload]
    (laheta-integraatioviesti this pistetyyppi asetukset payload))

  (vastaanota [this pistetyyppi asetukset]
    ;; todo: toteuta!
    )

  (lisaa-tietoja [_ & tiedot]
    (swap! lisatiedot conj (str/join " " tiedot))))

(defn tee-tapahtuma
  ([db integraatioloki jarjestelma integraatio]
   (tee-tapahtuma db integraatioloki jarjestelma integraatio nil))
  ([db integraatioloki jarjestelma integraatio ulkoinen-id]
   (let [tapahtuma-id (integraatioloki/luo-alkanut-integraatio db jarjestelma integraatio ulkoinen-id nil)
         lokittaja (integraatioloki/lokittaja integraatioloki db jarjestelma integraatio)]
     (->IntegraatioTapahtumaKonteksti tapahtuma-id lokittaja ulkoinen-id (atom [])))))

(defn suorita-integraatio
  ([db integraatioloki jarjestelma integraatio tyonkulku-fn]
   (suorita-integraatio db integraatioloki jarjestelma integraatio nil tyonkulku-fn))
  ([db integraatioloki jarjestelma integraatio ulkoinen-id tyonkulku-fn]
   (suorita-integraatio db integraatioloki jarjestelma integraatio ulkoinen-id tyonkulku-fn {}))
  ([db integraatioloki jarjestelma integraatio ulkoinen-id tyonkulku-fn {:keys [virhekasittelija]}]
   (let [{lokittaja :lokittaja tapahtuma-id :tapahtuma-id ulkoinen-id :ulkoinen-id lisatietoja :lisatietoja :as konteksti}
         (tee-tapahtuma db integraatioloki jarjestelma integraatio ulkoinen-id)
         lisatietoja (when lisatietoja (str/join "\n" @lisatietoja))]
     (try
       (let [vastaus (tyonkulku-fn konteksti)]
         (lokittaja :onnistunut nil lisatietoja tapahtuma-id ulkoinen-id)
         vastaus)

       (catch Throwable t
         (log/error t (format "Integraatiotapahtuman (järjestelmä: %s, integraatio: %s) suorituksessa tapahtui poikkeus"
                              jarjestelma integraatio))
         (when virhekasittelija
           (virhekasittelija konteksti t))
         (lokittaja :epaonnistunut nil (str lisatietoja " " t) tapahtuma-id ulkoinen-id)
         (throw t))))))