(ns harja.palvelin.palvelut.api-jarjestelmatunnukset
  (:require [harja.kyselyt.konversio :as konv]
            [harja.kyselyt.api-jarjestelmatunnukset :as q]
            [com.stuartsierra.component :as component]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelu poista-palvelut]]
            [harja.domain.oikeudet :as oikeudet]
            [taoensso.timbre :as log]))

(defn hae-jarjestelmatunnukset [db user]
  (oikeudet/lue oikeudet/hallinta-api-jarjestelmatunnukset user)
  (into []
        (comp (map konv/alaviiva->rakenne)
              (map #(konv/array->vec % :urakat)))
        (q/hae-jarjestelmatunnukset db)))

(defn tallenna-jarjestelmatunnukset [db user tunnukset]
  (oikeudet/kirjoita oikeudet/hallinta-api-jarjestelmatunnukset user)
  (log/info "TALLENNA: " tunnukset)
  (hae-jarjestelmatunnukset db user))

(defrecord APIJarjestelmatunnukset []
  component/Lifecycle
  (start [{http :http-palvelin db :db :as this}]
    (julkaise-palvelu http :hae-jarjestelmatunnukset
                      (fn [user payload]
                        (hae-jarjestelmatunnukset db user)))
    (julkaise-palvelu http :tallenna-jarjestelmatunnukset
                      (fn [user payload]
                        (tallenna-jarjestelmatunnukset db user payload)))
    this)

  (stop [{http :http-palvelin :as this}]
    (poista-palvelut http :hae-jarjestelmatunnukset)
    this))
