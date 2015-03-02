(defproject harja "0.0.1-SNAPSHOT"
  :description "Liikenneviraston Harja"
  
  :dependencies [[org.clojure/clojure "1.7.0-alpha5"] ; siirrytään 1.7.0 heti kun valmis
                 [org.clojure/clojurescript "0.0-2913"]

                 ;;;;;;; Yleiset ;;;;;;;
                 [prismatic/schema "0.3.7"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]

                 ;; Transit tietomuoto asiakkaan ja palvelimen väliseen kommunikointiin
                 [com.cognitect/transit-cljs "0.8.205"]
                 [com.cognitect/transit-clj "0.8.269"]
                 
                 ;;;;;;; Palvelin ;;;;;;;

                 ;; Komponenttituki
                 [com.stuartsierra/component "0.2.2"]
                 
                 ;; Lokitus
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.2"]

                 ;; HTTP palvelin ja reititys
                 [http-kit "2.1.19"]
                 [compojure "1.3.2"]
                 

                 [org.clojure/core.cache "0.6.4"]
                 
                 ;; Tietokanta: ajuri, kirjastot ja -migraatiot
                 [org.postgresql/postgresql "9.3-1102-jdbc41"]
                 [clojunauts/postgis-jdbc "2.1.0SVN"]
                 ;;[org.postgis/postgis-jdbc "2.1.4dev"] ;; mvnrepossa vain 1.3.3 versio, piti buildata itse!
                 [com.mchange/c3p0 "0.9.5"]
                 [yesql "0.4.0"]

                 ;; GeoTools
                 [org.geotools/gt-shapefile "12.2"]
                 [org.geotools/gt-process-raster "12.2"]
                 [org.geotools/gt-epsg-wkt "12.2"] ;; EPSG koordinaatistot
                 [org.geotools/gt-swing "12.2"] ;; just for experimentation, remove when no longer needed

                                 
                 ;; Asiakas
                 [spyscope "0.1.5"]
                 [spellhouse/clairvoyant "0.0-48-gf5e59d3"]
                 
                 [cljs-ajax "0.3.10"]
                 [lively "0.2.0"] 

                 [reagent "0.5.0-alpha3" :exclusions [[cljsjs/react :classifier "*"]]]
                 [cljsjs/react-with-addons "0.12.2-7"]
                 
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]

                 [com.andrewmcveigh/cljs-time "0.3.2"]

                 ;; Microsoft dokumenttimuotojen tuki
                 [org.apache.poi/poi "3.11"] ;; siirrä oikeisiin depseihin, kun tarvitaan XLS export feature
                 [org.apache.poi/poi-scratchpad "3.11"] ;; .ppt varten
                 [org.apache.poi/poi-ooxml "3.11"] ;; .xlsx tiedoston lukua varten
                 [org.clojure/data.json "0.2.5"]
                 
                 ]
  
  :dev-dependencies [;; Selain REPL
                     ;[com.cemerick/piggieback "0.1.3"]
                     ;; Testaus


                     ]
  
  :profiles {:test {:dependencies [[clj-webdriver "0.6.0"]
                                   [org.seleniumhq.selenium/selenium-java "2.44.0"]
                                   [org.seleniumhq.selenium/selenium-firefox-driver "2.44.0"]]}}
  
  :repositories [["osgeo" "http://download.osgeo.org/webdav/geotools/"]] ;; FIXME: move artifacts to mvn.solita.fi

  
  :plugins [[lein-cljsbuild "1.0.5"]
            [cider/cider-nrepl "0.8.2"]
            [lein-less "1.7.2"]
            [lein-ancient "0.5.5"]
            ;;[mvxcvi/whidbey "0.5.1"]
            ]  ;; Asiakaspuolen cljs buildin tietoja
  :cljsbuild {
              :builds [{:id "dev"
                        :source-paths ["src/cljs" "src/cljs-dev"]
                        :compiler {:optimizations :none
                                   :source-map true
                                   ;;:preamble ["reagent/react.js"]
                                   :output-to "dev-resources/js/harja.js"
                                   :output-dir "dev-resources/js/out"}
                        :notify-command ["terminal-notifier"
                                         "-title"
                                         "Harja"
                                         "-subtitle"
                                         "cljsbuild"
                                         "-group"
                                         "some-group"
                                         "-sound"
                                         "default"
                                         "-activate"
                                         "com.googlecode.iTerm2"
                                         "-message"]
                        
                        }
                       
                       {:id "prod"
                        :source-paths ["src/cljs" "src/cljs-prod"]
                        :compiler {:optimizations :advanced
                                   
                                   ;;:preamble ["reagent/react.min.js"]
                                   :output-to "resources/public/js/harja.js"
                                   :externs ["externs/leaflet.js"]

                                   ;; Nämä voi ottaa käyttöön, jos advanced compilation buildia pitää debugata
                                   ;;:source-map "resources/public/js/harja.js.map"
                                   ;;:output-dir "resources/public/js/"
                                   }}
                                   
                       ]}

  :clean-targets #^{:protect false} ["dev-resources/js/out" "target"
                                     "resources/public/js/harja.js"
                                     "resource/public/js/harja"]

  ;; Less CSS käännös tuotanto varten (dev modessa selain tekee less->css muunnoksen)
  :less {:source-paths ["dev-resources/less"]
         :target-path "resources/public/css"}


  ;; Palvelimen buildin tietoja
  :source-paths ["src/clj"]
  :aot :all
  :main harja.palvelin.main
  :auto-clean false ;; for uberjar

  ;; REPL kehitys
  :repl-options {:init-ns harja.palvelin.main
                 :init (harja.palvelin.main/-main)}

  ;; Tehdään komentoaliakset ettei build-komento jää vain johonkin Jenkins jobin konfiguraatioon
  :aliases {"tuotanto" ["do" "clean," "deps," "compile," "cljsbuild" "once" "prod," "less" "once," "uberjar"]

            ;; työkaluja, joita devaamisessa ja asiakkaalta saadun datan hieromisessa oikeaan muotoon, tarvitaan
            "elyt" ["run" "-m" "harja.tyokalut.elyt"] ;; ELY rajojen SHP file => hallintayksikkö SQL inserteiksi
            "sampo" ["run" "-m" "harja.tyokalut.sampo"] ;; SAMPO tuotelista XLS file => toimenpidekoodi SQL inserteiksi
            "gitlog" ["run" "-m" "harja.tyokalut.gitlog"] ;; tekee gitlogin resources alle
            "testit" ["with-profiles" "test" "do" "clean," "compile," "test"]
            }
  
  )
