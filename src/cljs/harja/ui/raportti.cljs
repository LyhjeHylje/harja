(ns harja.ui.raportti
  "Harjan raporttielementtien HTML näyttäminen."
  (:require [harja.ui.grid :as grid]
            [harja.ui.yleiset :as yleiset]
            [harja.visualisointi :as vis]
            [harja.loki :refer [log]]))

(defmulti muodosta-html
  "Muodostaa Reagent komponentin annetulle raporttielementille."
  (fn [elementti]
    (assert (and (vector? elementti)
                 (> (count elementti) 1)
                 (keyword? (first elementti)))
            (str "Raporttielementin on oltava vektori, jonka 1. elementti on tyyppi ja muut sen sisältöä. Raporttielementti oli: " (pr-str elementti)))
    (first elementti)))

(defmethod muodosta-html :taulukko [[_ {:keys [otsikko viimeinen-rivi-yhteenveto?]} sarakkeet data]]
  (log "GRID DATALLA: " (pr-str sarakkeet) " => " (pr-str data))
  [grid/grid {:otsikko (or otsikko "") :tunniste hash :piilota-toiminnot? true}
   (into []
         (map-indexed (fn [i sarake]
                        {:hae     #(get % i)
                         :leveys  (:leveys sarake)
                         :otsikko (:otsikko sarake)
                         :pakota-rivitys? (:pakota-rivitys? sarake)
                         :otsikkorivi-luokka (:otsikkorivi-luokka sarake)
                         :nimi    (str "sarake" i)})
                      sarakkeet))
   (if (empty? data)
     [(grid/otsikko "Ei tietoja")]
     (let [viimeinen-rivi (last data)]
      (into []
            (map (fn [rivi]
                   (if-let [otsikko (:otsikko rivi)]
                     (grid/otsikko otsikko)
                     (let [mappina (zipmap (range (count sarakkeet))
                                           rivi)]
                       (if (and viimeinen-rivi-yhteenveto?
                                (= viimeinen-rivi rivi))
                         (assoc mappina :yhteenveto true)
                         mappina)))))
            data)))])


(defmethod muodosta-html :otsikko [[_ teksti]]
  [:h3 teksti])

(defmethod muodosta-html :otsikko-kuin-pylvaissa [[_ teksti]]
  [:h3 teksti])

(defmethod muodosta-html :teksti [[_ teksti {:keys [vari]}]]
  [:p {:style {:color (when vari vari)}} teksti])

(defmethod muodosta-html :varoitusteksti [[_ teksti]]
  (muodosta-html [:teksti teksti {:vari "#dd0000"}]))

(defmethod muodosta-html :pylvaat [[_ {:keys [otsikko vari fmt piilota-arvo? legend]} pylvaat]]
  (let [w (int (* 0.85 @yleiset/leveys))
        h (int (/ w 2.9))]
    [:div.pylvaat
     [:h3 otsikko]
     [vis/bars {:width         w
                :height        h
                :format-amount (or fmt str)
                :hide-value?   piilota-arvo?
                :legend legend
                }
      pylvaat]]))

(defmethod muodosta-html :yhteenveto [[_ otsikot-ja-arvot]]
  (apply yleiset/taulukkotietonakyma {}
         (mapcat identity otsikot-ja-arvot)))

  
(defmethod muodosta-html :raportti [[_ raportin-tunnistetiedot & sisalto]]
  (log "muodosta html raportin-tunnistetiedot " (pr-str raportin-tunnistetiedot))
  [:div.raportti {:class (:tunniste raportin-tunnistetiedot)}
   (when (:nimi raportin-tunnistetiedot)
     [:h3 (:nimi raportin-tunnistetiedot)])
   (keep-indexed (fn [i elementti]
                   (when elementti
                     ^{:key i}
                     [muodosta-html elementti]))
                 (mapcat (fn [sisalto]
                           (if (list? sisalto)
                             sisalto
                             [sisalto]))
                         sisalto))])
