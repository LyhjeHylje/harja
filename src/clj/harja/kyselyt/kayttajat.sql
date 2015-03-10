-- name: hae-kirjautumistiedot
-- Hakee annetulle KOKA käyttäjätunnukselle kirjautumistiedot
SELECT k.id, k.kayttajanimi, k.etunimi, k.sukunimi, k.sahkoposti, k.puhelin,
       o.id as org_id, o.nimi as org_nimi, o.tyyppi as org_tyyppi
  FROM kayttaja k LEFT JOIN organisaatio o ON k.organisaatio = o.id
 WHERE k.kayttajanimi = :koka

-- name: hae-kayttajat
-- Hakee käyttäjiä käyttäjähallinnan listausta varten.
-- Haun suorittava käyttäjä annetaan parametrina ja vain käyttäjät, jotka hän saa nähdä palautetaan.
SELECT k.id, k.kayttajanimi, k.etunimi, k.sukunimi, k.sahkoposti, k.puhelin,
       o.id as org_id, o.nimi as org_nimi, o.tyyppi as org_tyyppi,
       array_cat((SELECT array_agg(rooli) FROM kayttaja_rooli WHERE kayttaja = k.id),
                 (SELECT array_agg(rooli) FROM kayttaja_urakka_rooli WHERE kayttaja = k.id)) as roolit
  FROM kayttaja k
       LEFT JOIN organisaatio o ON k.organisaatio = o.id
 WHERE
       -- tarkistetaan käyttöoikeus: pääkäyttäjä näkee kaikki, muuten oman organisaation
       ((SELECT COUNT(*) FROM kayttaja_rooli WHERE kayttaja=:hakija AND rooli='jarjestelmavastuuhenkilo') > 0
        OR
	k.organisaatio IN (SELECT kor.organisaatio FROM kayttaja_organisaatio_rooli kor
	                    WHERE kor.kayttaja = :hakija
			      AND kor.rooli = 'urakoitsijan paakayttaja'))
       -- tarkistetaan hakuehto
       AND
       (:haku = '' OR (k.kayttajanimi LIKE :haku OR k.etunimi LIKE :haku OR k.sukunimi LIKE :haku))
OFFSET :alku
 LIMIT :maara

-- name: hae-kayttajat-lkm
-- Hakee lukumäärän käyttäjälukumäärälle, jonka hae-kayttajat palauttaisi ilman LIMIT/OFFSET määritystä.
SELECT COUNT(k.id) as lkm
  FROM kayttaja k
 WHERE
       -- tarkistetaan käyttöoikeus: pääkäyttäjä näkee kaikki, muuten oman organisaation
       ((SELECT COUNT(*) FROM kayttaja_rooli WHERE kayttaja=:hakija AND rooli='jarjestelmavastuuhenkilo') > 0
        OR
	k.organisaatio IN (SELECT kor.organisaatio FROM kayttaja_organisaatio_rooli kor
	                    WHERE kor.kayttaja = :hakija
			      AND kor.rooli = 'urakoitsijan paakayttaja'))
       -- tarkistetaan hakuehto
       AND
       (:haku = '' OR (k.kayttajanimi LIKE :haku OR k.etunimi LIKE :haku OR k.sukunimi LIKE :haku))
         
-- name: hae-kayttajan-urakka-roolit
-- Hakee käyttäjän urakka roolit.
SELECT rooli, urakka as urakka_id, luotu,
       ur.nimi as urakka_nimi,
       urk.nimi as urakka_urakoitsija_nimi, urk.id as urakka_urakoitsija_id,
       hal.nimi as urakka_hallintayksikko_nimi, hal.id as urakka_hallintayksikko_id
  FROM kayttaja_urakka_rooli
       LEFT JOIN urakka ur ON urakka=ur.id
       LEFT JOIN organisaatio urk ON ur.urakoitsija=urk.id
       LEFT JOIN organisaatio hal ON ur.hallintayksikko=hal.id
 WHERE kayttaja = :kayttaja AND poistettu = false
