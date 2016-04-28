(ns harja.palvelin.raportointi.excel
  "Harja raporttielementtien vienti Excel muotoon"
  (:require [taoensso.timbre :as log]
            [dk.ative.docjure.spreadsheet :as excel]
            [clojure.string :as str])
  (:import (org.apache.poi.ss.util CellReference WorkbookUtil)))

(defmulti muodosta-excel
  "Muodostaa Excel data annetulle raporttielementille.
  Dispatch tyypin mukaan (vektorin 1. elementti)."
  (fn [elementti workbook]
    (assert (and (vector? elementti)
                 (> (count elementti) 1)
                 (keyword? (first elementti)))
            (str "Raporttielementin on oltava vektori, jonka 1. elementti on tyyppi "
                 "ja muut sen sisältöä, sain: " (pr-str elementti)))
    (first elementti)))


(defn solu [rivi-nro sarake-nro]
  (.formatAsString (CellReference. rivi-nro sarake-nro)))

(defmulti aseta-kaava!
  (fn [[tyyppi &_] _ _ _]
    tyyppi))

(defmethod aseta-kaava! :summa-vasen [[_ alkusarake-nro] cell rivi-nro sarake-nro]
  (.setCellFormula cell
                   (str "SUM("
                        (solu rivi-nro alkusarake-nro)
                        ":"
                        (solu rivi-nro (dec sarake-nro))
                        ")")))


(defn- ilman-soft-hyphenia [data]
  (if (string? data)
    (.replace data "\u00AD" "")
    data))

(defmethod muodosta-excel :taulukko [[_ optiot sarakkeet data] workbook]
  (try
    (let [nimi (:otsikko optiot)
          aiempi-sheet (last (excel/sheet-seq workbook))
          [sheet nolla] (if (and (nil? nimi) aiempi-sheet)
                          [aiempi-sheet (+ 2 (.getLastRowNum aiempi-sheet))]
                          [(excel/add-sheet! workbook
                                             (WorkbookUtil/createSafeSheetName
                                              (or (:sheet-nimi optiot) nimi))) 0])
          sarake-tyyli (excel/create-cell-style! workbook {:background :blue
                                                           :font {:color :white}})
          otsikko-rivi (.createRow sheet nolla)]
      ;; Luodaan otsikot saraketyylillä
      (dorun
       (map-indexed
        (fn [sarake-nro {:keys [otsikko] :as sarake}]
          (let [cell (.createCell otsikko-rivi sarake-nro)]
            (excel/set-cell! cell (ilman-soft-hyphenia otsikko))
            (excel/set-cell-style! cell sarake-tyyli)))
        sarakkeet))

      (dorun
       (map-indexed
        (fn [rivi-nro rivi]
          (let [rivi-nro (+ nolla 1 rivi-nro)
                [data optiot] (if (map? rivi)
                                [(:rivi rivi) rivi]
                                [rivi {}])
                row (.createRow sheet rivi-nro)]
            (dorun
             (map-indexed
              (fn [sarake-nro sarake]
                (let [cell (.createCell row sarake-nro)
                      lihavoi? (:lihavoi? optiot)
                      tyyli (excel/create-cell-style! workbook
                                                      {:font {:bold lihavoi?}})]

                  (if-let [kaava (:excel sarake)]
                    (aseta-kaava! kaava cell rivi-nro sarake-nro)
                    (excel/set-cell! cell (ilman-soft-hyphenia (nth data sarake-nro))))
                  (excel/set-cell-style! cell tyyli)))
              sarakkeet))))
        data))

      ;; Laitetaan automaattiset leveydet
      (dotimes [i (count sarakkeet)]
        (.autoSizeColumn sheet i)))
    (catch Throwable t
      (log/error t "Virhe Excel muodostamisessa"))))

(defmethod muodosta-excel :raportti [[_ raportin-tunnistetiedot & sisalto] workbook]
  (let [sisalto (mapcat #(if (seq? %) % [%]) sisalto)]
    (doseq [elementti sisalto]
      (muodosta-excel elementti workbook)))
  (:nimi raportin-tunnistetiedot))

(defmethod muodosta-excel :default [elementti workbook]
  (log/debug "Excel ei tue elementtiä: " elementti)
  nil)