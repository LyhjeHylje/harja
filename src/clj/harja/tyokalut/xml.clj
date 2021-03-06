(ns harja.tyokalut.xml
  "Tämä namespace sisältää apufunktioita XML-tiedostojen käsittelyyn"
  (:require [clojure.xml :refer [parse]]
            [clojure.java.io :as io]
            [clojure.zip :refer [xml-zip]]
            [taoensso.timbre :as log]
            [hiccup.core :refer [html]]
            [clj-time.format :as f]
            [clj-time.coerce :as tc]
            [clojure.data.zip.xml :as z])
  (:import (javax.xml.validation SchemaFactory)
           (javax.xml XMLConstants)
           (javax.xml.transform.stream StreamSource)
           (java.io ByteArrayInputStream)
           (org.w3c.dom.ls LSResourceResolver LSInput)
           (org.xml.sax SAXParseException ErrorHandler)
           (java.text SimpleDateFormat ParseException)
           (java.util Date)))

(defn validoi-xml
  "Validoi annetun XML sisällön vasten annettua XSD-skeemaa."
  [xsd-skeema-polku xsd-skeema-tiedosto xml-sisalto]
  (log/debug "Validoidaan XML käyttäen XSD-skeemaa:" xsd-skeema-tiedosto ". XML:n sisältö on:" xml-sisalto)
  (let [virheet (atom [])
        schema-factory (SchemaFactory/newInstance XMLConstants/W3C_XML_SCHEMA_NS_URI)]
    (.setResourceResolver schema-factory
                          (reify LSResourceResolver
                            (resolveResource [this type namespaceURI publicId systemId baseURI]
                              (let [systemId (if (.startsWith systemId "./")
                                               (.substring systemId 2)
                                               systemId)
                                    xsd-file (io/resource (str xsd-skeema-polku systemId))]
                                (reify LSInput
                                  (getByteStream [_] (io/input-stream xsd-file))
                                  (getPublicId [_] publicId)
                                  (getSystemId [_] systemId)
                                  (getBaseURI [_] baseURI)
                                  (getCharacterStream [_] (io/reader xsd-file))
                                  (getEncoding [_] "UTF-8")
                                  (getStringData [_] (slurp xsd-file)))))))
    (try
      (let [v (-> schema-factory
                  (.newSchema (StreamSource. (io/input-stream (io/resource (str xsd-skeema-polku xsd-skeema-tiedosto)))))
                  .newValidator)]
        (.setErrorHandler v
                          (reify ErrorHandler
                            (error [this e]
                              (log/error e (format "XSD-validointivirhe skeemalle: %s" xsd-skeema-tiedosto) )
                              (swap! virheet conj (.getMessage e)))
                            (fatalError [this e]
                              (log/error e (format "Fataali XSD-validointi virhe skeemalle: %s" xsd-skeema-tiedosto) )
                              (swap! virheet conj (.getMessage e)))
                            (warning [this e]
                              (log/warn e (format "XSD-validointivaroitus skeemalle: %s" xsd-skeema-tiedosto) ))))
        (.validate v (StreamSource. (ByteArrayInputStream. (.getBytes xml-sisalto)))))
      (if (empty? @virheet)
        nil
        @virheet)
      (catch SAXParseException e
        (log/error e (format "XSD-validointivirhe skeemalle: %s" xsd-skeema-tiedosto))
        @virheet))))

(defn validi-xml?
  "Validoi annetun XML sisällön vasten annettua XSD-skeemaa."
  [xsd-skeema-polku xsd-skeema-tiedosto xml-sisalto]
  (nil? (validoi-xml xsd-skeema-polku xsd-skeema-tiedosto xml-sisalto)))

(defn lue
  ([xml] (lue xml "UTF-8"))
  ([xml charset]
   (let [in (ByteArrayInputStream. (.getBytes xml charset))]
     (try (xml-zip (parse in))
          (catch SAXParseException e
            (log/error e "Tapahtui poikkeus luettuessa XML-sisältöä: " xml)
            nil)))))

(defn tee-xml-sanoma [sisalto]
  (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" (html sisalto)))

(defn formatoi-aikaleima [paivamaara]
  (.format (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.S") paivamaara))

(defn formatoi-paivamaara [paivamaara]
  (.format (SimpleDateFormat. "yyyy-MM-dd") paivamaara))

(defn formatoi-kellonaika [paivamaara]
  (.format (SimpleDateFormat. "HH:mm:ss") paivamaara))

(defn parsi-kokonaisluku [data]
  (when (and data (not (empty? data))) (Integer/parseInt data)))

(defn parsi-desimaaliluku [data]
  (when (and data (not (empty? data))) (Double/parseDouble data)))

(defn parsi-totuusarvo [data]
  (when (and data (not (empty? data))) (Boolean/parseBoolean data)))

(defn parsi-avain [data]
  (when (and data (not (empty? data))) (keyword (clojure.string/lower-case data))))

(defn parsi-aika [formaatti data]
  (when (and data (not (empty? data)))
    (try (new Date (.getTime (.parse (SimpleDateFormat. formaatti) data)))
         (catch ParseException e
           (log/error e "Virhe parsiessa päivämäärää: " data ", formaatilla: " formaatti)
           nil))))

(defn parsi-aikaleima
  ([teksti] (parsi-aika teksti "yyyy-MM-dd'T'HH:mm:ss.SSS"))
  ([teksti formaatti] (parsi-aika formaatti teksti)))

(defn parsi-paivamaara [teksti] (f/formatters :date-time-no-ms)
  (parsi-aika "yyyy-MM-dd" teksti))

(def xsd-datetime-fmt
  (f/with-zone (f/formatters :date-hour-minute-second) (org.joda.time.DateTimeZone/forID "EET")))

(defn parsi-xsd-datetime [teksti]
  (tc/to-date (f/parse xsd-datetime-fmt teksti)))

(defn formatoi-xsd-datetime [date]
  (f/unparse xsd-datetime-fmt
             (if (instance? java.util.Date date)
               (tc/from-date date)
               date)))

(defn json-date-time->joda-time
  "Muuntaa JSONin date-time -formaatissa olevan stringin (esim. 2016-01-30T12:00:00.000)
  org.joda.time.DateTime -muotoon."
  [aika-teksti]
  (let [formatter (f/formatters :date-time-no-ms)]
    (f/parse formatter aika-teksti)))

(defn joda-time->xml-xs-date
  "Muuntaa joda-timen XML:n xs:date-muotoon (esim. 2015-03-03)."
  [joda-time]
  (let [formatter (f/formatter "yyyy-MM-dd")]
    (f/unparse formatter joda-time)))

(defn json-date-time->xml-xs-date
  "Muuntaa JSON aikaleiman XML:n xs-date-muotoon"
  [aika]
  (when aika
    (joda-time->xml-xs-date
      (json-date-time->joda-time aika))))

(defn raakateksti
  "Palauttaa elementin arvon raakatekstinä, jolloin mm. mukana on kaikki välimerkit"
  [data avain]
  (:content (first (z/xml1-> data avain))))
