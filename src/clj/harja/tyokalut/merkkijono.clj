(ns harja.tyokalut.merkkijono
  (:import (java.text SimpleDateFormat)
           (java.text ParseException SimpleDateFormat)))

(defn leikkaa [merkkia merkkijonosta]
  (apply str (take merkkia merkkijonosta)))

(defn tayta-oikealle [pituus merkkijono]
  (format (str "%1$-" pituus "s") merkkijono))

(defn parsittavissa-intiksi? [arvo]
  (try
    (Integer/parseInt arvo)
    true
    (catch NumberFormatException e false)))

(defn kokonaisluku? [arvo]
  (some? (re-matches #"^[0-9]*$" arvo)))

(defn iso-8601-paivamaara? [arvo]
  (try
    (.parse (SimpleDateFormat. "yyyy-MM-dd") arvo)
    true
    (catch ParseException e
      false)))