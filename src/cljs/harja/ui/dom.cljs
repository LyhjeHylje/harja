(ns harja.ui.dom
  "Yleisiä apureita DOMin ja selaimen hallintaan"
  (:require [reagent.core :as r]
            [harja.asiakas.tapahtumat :as t]
            [harja.loki :refer [log]])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction run!]]))

(defn sisalla?
  "Tarkistaa onko annettu tapahtuma tämän React komponentin sisällä."
  [komponentti tapahtuma]
  (let [dom (r/dom-node komponentti)
        elt (.-target tapahtuma)]
    (loop [ylempi (.-parentNode elt)]
      (if (or (nil? ylempi)
              (= ylempi js/document.body))
        false
        (if (= dom ylempi)
          true
          (recur (.-parentNode ylempi)))))))


(def ie? (let [ua (-> js/window .-navigator .-userAgent)]
           (or (not= -1 (.indexOf ua "MSIE "))
               (not= -1 (.indexOf ua "Trident/"))
               (not= -1 (.indexOf ua "Edge/")))))

(defn karttakuva
  "Palauttaa kuvatiedoston nimen, jos käytössä IE palauttaa .png kuvan, muuten .svg"
  [perusnimi]
  (str perusnimi (if ie? ".png" ".svg")))

(defn assertoi-ikonin-vari [vari]
  (assert #{"keltainen" "lime" "magenta" "musta" "oranssi" "pinkki"
            "punainen" "sininen" "syaani" "tummansininen" "turkoosi"
            "vihrea" "violetti"} vari))

(def ikonikansio "images/tuplarajat/")

(defn sijainti-ikoni
  "Palauttaa sijaintia kuvaavan ikonin, jonka ulko- ja sisäreunan väri voidaan itse asettaa.
   Jos annetaan vain sisäreuna, palautetaan tällä sisäreunalla varustettu ikoni, jolla on musta ulkoreuna."
  ([vari-sisareuna] (sijainti-ikoni "musta" vari-sisareuna))
  ([vari-sisareuna vari-ulkoreuna]
   (assert (#{"vihrea" "punainen" "oranssi" "musta" "harmaa"} vari-ulkoreuna))
   (assertoi-ikonin-vari vari-sisareuna)
   (karttakuva (str ikonikansio"sijainnit/sijainti-"vari-ulkoreuna"-"vari-sisareuna))))

(defn nuoli-ikoni [vari-str]
  (assertoi-ikonin-vari vari-str)
  (karttakuva (str ikonikansio"nuolet/nuoli-"vari-str)))

(defn pinni-ikoni [vari-str]
  (assertoi-ikonin-vari vari-str)
  (karttakuva (str ikonikansio"pinnit/pinni-"vari-str)))

(defonce korkeus (atom (-> js/window .-innerHeight)))
(defonce leveys (atom (-> js/window .-innerWidth)))

(defonce ikkunan-koko
         (reaction [@leveys @korkeus]))

(defn- ikkunan-koko-muuttunut [& _]
  (t/julkaise! {:aihe :ikkunan-koko-muuttunut :leveys @leveys :korkeus @korkeus}))

(defonce ikkunan-koko-tapahtuman-julkaisu
  (do (add-watch korkeus ::ikkunan-koko-muuttunut ikkunan-koko-muuttunut)
      (add-watch leveys ::ikkunan-koko-muuttunut ikkunan-koko-muuttunut)
      true))

(defonce koon-kuuntelija (do (set! (.-onresize js/window)
                                   (fn [_]
                                     (reset! korkeus (-> js/window .-innerHeight))
                                     (reset! leveys (-> js/window .-innerWidth))
                                     ))
                             true))

(defn elementti-idlla [id]
  (.getElementById js/document (name id)))

(defn sijainti
  "Laskee DOM-elementin sijainnin, palauttaa [x y w h]."
  [elt]
  (assert elt (str "Ei voida laskea sijaintia elementille null"))
  (let [r (.getBoundingClientRect elt)
        sijainti [(.-left r) (.-top r) (- (.-right r) (.-left r)) (- (.-bottom r) (.-top r))]]
    sijainti))

(defn offset-korkeus [elt]
  (loop [offset (.-offsetTop elt)
         parent (.-offsetParent elt)]
    (if (or (nil? parent)
            (= js/document.body parent))
      offset
      (recur (+ offset (.-offsetTop parent))
             (.-offsetParent parent)))))


(defn sijainti-sailiossa
  "Palauttaa elementin sijainnin suhteessa omaan säiliöön."
  [elt]
  (let [[x1 y1 w1 h1] (sijainti elt)
        [x2 y2 w2 h2] (sijainti (.-parentNode elt))]
    [(- x1 x2) (- y1 y2) w1 h1]))

(defn elementin-etaisyys-alareunaan [solmu]
  (let [r (.getBoundingClientRect solmu)
        etaisyys (- @korkeus (.-bottom r))]
    etaisyys))

(defn elementin-etaisyys-ylareunaan [solmu]
  (let [r (.getBoundingClientRect solmu)
        etaisyys (.-top r)]
    etaisyys))

(defn elementin-etaisyys-oikeaan-reunaan [solmu]
  (let [r (.getBoundingClientRect solmu)
        etaisyys (- @leveys (.-right r))]
    etaisyys))