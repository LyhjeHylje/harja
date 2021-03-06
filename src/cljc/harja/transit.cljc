(ns harja.transit
  "Harjan transit laajennokset"
  (:require [cognitect.transit :as t]
            [harja.domain.roolit :as roolit]
            #?(:clj
               [harja.geo :as geo]

               :cljs
               [harja.pvm :as pvm]))
  (:import #?(:clj
              (java.text SimpleDateFormat)

              :cljs
              (goog.date DateTime UtcDateTime))))



#?(:clj (def +fi-date-time-format+ "dd.MM.yyyy HH:mm:ss")
   :cljs (deftype DateTimeHandler []
           Object
           (tag [_ v] "dt")
           (rep [_ v] (pvm/pvm-aika-sek v))))

#?(:clj
   (def write-optiot {:handlers

                      {java.util.Date
                       (t/write-handler (constantly "dt")
                                        #(.format (SimpleDateFormat. +fi-date-time-format+) %))

                       java.math.BigDecimal
                       (t/write-handler (constantly "bd") double)

                       org.postgresql.geometric.PGpoint
                       (t/write-handler (constantly "pp") geo/pg->clj)

                       org.postgis.PGgeometry
                       (t/write-handler "pg" geo/pg->clj)

                       harja.domain.roolit.EiOikeutta
                       (t/write-handler (constantly "eo") #(:syy %))}})

   :cljs
   (def write-optiot {:handlers
                      {DateTime (DateTimeHandler.)
                       UtcDateTime (DateTimeHandler.)}}))

#?(:clj
   (def read-optiot {:handlers
                     {"dt" (t/read-handler #(.parse (SimpleDateFormat. +fi-date-time-format+) %))}})

   :cljs
   (def read-optiot {:handlers
                     {"dt" #(pvm/->pvm-aika-sek %)

                      ;; Serveri lähettää java.math.BigDecimal typen doubleksi
                      ;; muunnettuna, joten tässä kelpaa identity
                      "bd" identity

                      ;; Serveri lähettää PGpoint ja PGgeometry muunnettuina
                      ;; kelpaa meille sellaisenaan
                      "pp" js->clj
                      "pg" js->clj

                      ;; EiOikeutta tulee serveriltä "eo" tägillä ja pelkkänä syy stringiä
                      "eo" #(roolit/->EiOikeutta %)}}))


(defn clj->transit
  "Muuntaa Clojure tietorakenteen Transit+JSON merkkijonoksi."
  [data]
  #?(:clj
     (with-open [out (java.io.ByteArrayOutputStream.)]
       (t/write (t/writer out :json write-optiot) data)
       (str out))

     :cljs
     (t/write (t/writer :json write-optiot) data)))


(defn lue-transit
  "Lukee Transit+JSON muotoisen tiedon annetusta inputista."
  [in]
  #?(:clj
     (t/read (t/reader in :json read-optiot))

     :cljs
     (t/read (t/reader :json read-optiot) in)))

#?(:clj
   (defn lue-transit-string [in]
     (lue-transit (java.io.ByteArrayInputStream. (.getBytes in)))))
