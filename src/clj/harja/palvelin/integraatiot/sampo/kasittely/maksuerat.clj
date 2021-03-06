(ns harja.palvelin.integraatiot.sampo.kasittely.maksuerat
  (:require [taoensso.timbre :as log]
            [clojure.java.jdbc :as jdbc]
            [hiccup.core :refer [html]]
            [harja.tyokalut.xml :as xml]
            [harja.kyselyt.maksuerat :as qm]
            [harja.kyselyt.konversio :as konversio]
            [harja.palvelin.integraatiot.sampo.sanomat.maksuera_sanoma :as maksuera-sanoma]
            [harja.kyselyt.toimenpideinstanssit :as toimenpideinstanssit]
            [harja.kyselyt.maksuerat :as maksuerat]
            [harja.kyselyt.kustannussuunnitelmat :as kustannussuunnitelmat]
            [harja.palvelin.integraatiot.integraatioloki :as integraatioloki]
            [harja.palvelin.komponentit.sonja :as sonja])
  (:import (java.util UUID)))

(def +xsd-polku+ "xsd/sampo/outbound/")

(def maksueratyypit ["kokonaishintainen" "yksikkohintainen" "lisatyo" "indeksi" "bonus" "sakko" "akillinen-hoitotyo" "muu"])

(defn tee-xml-sanoma [sisalto]
  (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" (html sisalto)))

(defn hae-maksuera [db numero]
  (let [{urakka-id :urakka-id :as maksuera} (konversio/alaviiva->rakenne (first (qm/hae-lahetettava-maksuera db numero)))
        tpi (get-in maksuera [:toimenpideinstanssi :id])
        tyyppi (keyword (get-in maksuera [:maksuera :tyyppi]))

        ;; Haetaan maksuerätiedot ja valitaan niistä tämän toimenpideinstanssin rivi
        summat (first (filter #(= (:tpi_id %) tpi)
                              (qm/hae-urakan-maksueratiedot db urakka-id)))]
    (assoc-in maksuera
              [:maksuera :summa]
              (get summat tyyppi))))

(defn hae-maksueranumero [db lahetys-id]
  (:numero (first (qm/hae-maksueranumero-lahetys-idlla db lahetys-id))))

(defn lukitse-maksuera [db numero]
  (let [lukko (str (UUID/randomUUID))]
    (log/debug "Lukitaan maksuera:" numero ", lukolla:" lukko)
    (let [onnistuiko? (= 1 (qm/lukitse-maksuera! db lukko numero))]
      onnistuiko?)))

(defn merkitse-maksuera-odottamaan-vastausta [db numero lahetys-id]
  (log/debug "Merkitään maksuerä: " numero " odottamaan vastausta ja avataan lukko. ")
  (= 1 (qm/merkitse-maksuera-odottamaan-vastausta! db lahetys-id numero)))

(defn merkitse-maksueralle-lahetysvirhe [db numero]
  (log/debug "Merkitään lähetysvirhe maksuerälle (numero:" numero ").")
  (= 1 (qm/merkitse-maksueralle-lahetysvirhe! db numero)))

(defn merkitse-maksuera-lahetetyksi [db numero]
  (log/debug "Merkitään maksuerä (numero:" numero ") lähetetyksi.")
  (= 1 (qm/merkitse-maksuera-lahetetyksi! db numero)))

(defn muodosta-sampoon-lahetettava-maksuerasanoma [db numero]
  (let [maksueran-tiedot (hae-maksuera db numero)
        ;; Sakot lähetetään Sampoon negatiivisena
        maksueran-tiedot (if (= (:tyyppi (:maksuera maksueran-tiedot)) "sakko")
                           (update-in maksueran-tiedot [:maksuera :summa] -)
                           maksueran-tiedot)
        maksuera-xml (tee-xml-sanoma (maksuera-sanoma/muodosta maksueran-tiedot))]
    (if (xml/validi-xml? +xsd-polku+ "nikuxog_product.xsd" maksuera-xml)
      maksuera-xml
      (do
        (log/error "Maksuerää ei voida lähettää. Maksuerä XML ei ole validi.")
        nil))))

(defn tee-makseuran-nimi [toimenpiteen-nimi maksueratyyppi]
  (let [tyyppi (case maksueratyyppi
                 "kokonaishintainen" "Kokonaishintaiset"
                 "yksikkohintainen" "Yksikköhintaiset"
                 "lisatyo" "Lisätyöt"
                 "indeksi" "Indeksit"
                 "bonus" "Bonukset"
                 "sakko" "Sakot"
                 "akillinen-hoitotyo" "Äkilliset hoitotyöt"
                 "Muut")]
    (str toimenpiteen-nimi ": " tyyppi)))

(defn perusta-maksuerat-hoidon-urakoille [db]
  (log/debug "Perustetaan maksuerät hoidon maksuerättömille toimenpideinstansseille")
  (let [maksuerattomat-tpit (toimenpideinstanssit/hae-hoidon-maksuerattomat-toimenpideistanssit db)]
    (if (empty? maksuerattomat-tpit)
      (log/debug "Kaikki maksuerät on jo perustettu hoidon urakoiden toimenpiteille"))
    (doseq [tpi maksuerattomat-tpit]
      (doseq [maksueratyyppi maksueratyypit]
        (let [maksueran-nimi (tee-makseuran-nimi (:toimenpide_nimi tpi) maksueratyyppi)
              maksueranumero (:numero (maksuerat/luo-maksuera<! db (:toimenpide_id tpi) maksueratyyppi maksueran-nimi))]
          (kustannussuunnitelmat/luo-kustannussuunnitelma<! db maksueranumero))))))

(defn laheta-maksuera [sonja integraatioloki db lahetysjono-ulos numero]
  (log/debug "Lähetetään maksuera (numero: " numero ") Sampoon.")
  (let [tapahtuma-id (integraatioloki/kirjaa-alkanut-integraatio integraatioloki "sampo" "maksuera-lahetys" nil nil)]
    (try
      (if (lukitse-maksuera db numero)
        (if-let [maksuera-xml (muodosta-sampoon-lahetettava-maksuerasanoma db numero)]
          (if-let [viesti-id (sonja/laheta sonja lahetysjono-ulos maksuera-xml)]
            (do
              (merkitse-maksuera-odottamaan-vastausta db numero viesti-id)
              (integraatioloki/kirjaa-jms-viesti integraatioloki tapahtuma-id viesti-id "ulos" maksuera-xml lahetysjono-ulos))
            (do
              (log/error "Maksuerän (numero: " numero ") lähetys Sonjaan epäonnistui.")
              (integraatioloki/kirjaa-epaonnistunut-integraatio
                integraatioloki (str "Maksuerän (numero: " numero ") lähetys Sonjaan epäonnistui.") nil tapahtuma-id nil)
              (merkitse-maksueralle-lahetysvirhe db numero)
              {:virhe :sonja-lahetys-epaonnistui}))
          (do
            (log/warn "Maksuerän (numero: " numero ") sanoman muodostus epäonnistui.")
            (merkitse-maksueralle-lahetysvirhe db numero)
            {:virhe :maksueran-lukitseminen-epaonnistui}))
        (do
          (log/warn "Maksuerän (numero: " numero ") lukitus epäonnistui.")
          {:virhe :maksueran-lukitseminen-epaonnistui}))
      (catch Throwable e
        (log/error e "Sampo maksuerälähetyksessä tapahtui poikkeus.")
        (merkitse-maksueralle-lahetysvirhe db numero)
        (integraatioloki/kirjaa-epaonnistunut-integraatio
          integraatioloki
          "Sampo maksuerälähetyksessä tapahtui poikkeus"
          (str "Poikkeus: " (.getMessage e))
          tapahtuma-id
          nil)
        {:virhe :poikkeus}))))

(defn kasittele-maksuera-kuittaus [db kuittaus viesti-id]
  (jdbc/with-db-transaction [db db]
    (if-let [maksueranumero (hae-maksueranumero db viesti-id)]
      (if (contains? kuittaus :virhe)
        (do
          (log/error "Vastaanotettiin virhe Sampon maksuerälähetyksestä: " kuittaus)
          (merkitse-maksueralle-lahetysvirhe db maksueranumero))
        (merkitse-maksuera-lahetetyksi db maksueranumero))
      (log/error "Viesti-id:llä " viesti-id " ei löydy maksuerää."))))
