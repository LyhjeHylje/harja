(ns harja.asiakas.ymparisto
  "Dev ympäristön spesifisiä asioita."
  (:require lively))

(defn alusta
  "Alusta tämän ympäristön vaatimat asiat, Lively reload."
  []
  (.log js/console "Alustetaan koodin uudelleenlataus")
  (lively/start "/js/harja.js"
                {:polling-rate 1000
                 :on-reload (fn [] (.log js/console "Koodia ladattu uudelleen."))}))



