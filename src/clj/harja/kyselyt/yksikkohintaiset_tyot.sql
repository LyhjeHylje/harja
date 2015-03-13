-- name: listaa-urakan-yksikkohintaiset-tyot
-- Hakee kaikki yksikkohintaiset-tyot
SELECT yt.alkupvm, yt.loppupvm, yt.maara, yt.yksikko, yt.yksikkohinta, yt.tehtava, yt.urakka, yt.sopimus,
	   tk.nimi as tehtavan_nimi
  FROM yksikkohintainen_tyo yt
	   LEFT JOIN toimenpidekoodi tk ON yt.tehtava = tk.id
 WHERE urakka = :urakka

 -- name: paivita-urakan-yksikkohintainen-tyo!
-- Päivittää urakan hoitokauden yksikkohintaiset tyot
UPDATE yksikkohintainen_tyo
   SET maara =:maara, yksikko =:yksikko, yksikkohinta =:yksikkohinta
 WHERE urakka = :urakka AND sopimus = :sopimus AND tehtava = :tehtava 
 	   AND alkupvm = :alkupvm AND loppupvm = :loppupvm

 	   --SET alkupvm =:alkupvm, loppupvm =:loppupvm, maara =:maara, yksikko =:yksikko,
   --yksikkohinta =:yksikkohinta, tehtava =:tehtava, urakka =:urakka, sopimus =:sopimus,