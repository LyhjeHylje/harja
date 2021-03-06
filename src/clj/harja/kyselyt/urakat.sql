-- name: hae-kaikki-urakat-aikavalilla
SELECT
  u.id     AS urakka_id,
  u.nimi   AS urakka_nimi,
  u.tyyppi AS tyyppi,
  o.id     AS hallintayksikko_id,
  o.nimi   AS hallintayksikko_nimi
FROM urakka u
  JOIN organisaatio o ON u.hallintayksikko = o.id
WHERE ((u.loppupvm >= :alku AND u.alkupvm <= :loppu) OR (u.loppupvm IS NULL AND u.alkupvm <= :loppu)) AND
      (:urakoitsija :: INTEGER IS NULL OR :urakoitsija = u.urakoitsija) AND
      (:urakkatyyppi :: urakkatyyppi IS NULL OR u.tyyppi :: TEXT = :urakkatyyppi) AND
      (:hallintayksikko :: INTEGER IS NULL OR u.hallintayksikko IN (:hallintayksikko));

-- name: hae-kaynnissa-olevat-urakat
SELECT
  u.id,
  u.nimi,
  u.tyyppi
FROM urakka u
WHERE (u.alkupvm IS NULL OR u.alkupvm <= current_date) AND
      (u.loppupvm IS NULL OR u.loppupvm >= current_date);

-- name: hae-kaynnissa-olevat-ja-tulevat-urakat
SELECT
  u.id,
  u.nimi,
  u.tyyppi,
  u.alkupvm,
  u.loppupvm
FROM urakka u
WHERE u.alkupvm >= current_date
      OR
      (u.alkupvm <= current_date AND
       u.loppupvm >= current_date);

-- name: hae-hallintayksikon-urakat
SELECT
  u.id,
  u.nimi,
  u.tyyppi
FROM urakka u
  JOIN organisaatio o ON o.id = u.hallintayksikko
WHERE o.id = :hy;

-- name: listaa-urakat-hallintayksikolle
-- Palauttaa listan annetun hallintayksikön (id) urakoista. Sisältää perustiedot ja geometriat.
SELECT
  u.id,
  u.nimi,
  u.sampoid,
  u.alue,
  u.alkupvm,
  u.loppupvm,
  u.tyyppi,
  u.sopimustyyppi,
  u.indeksi,
  hal.id                      AS hallintayksikko_id,
  hal.nimi                    AS hallintayksikko_nimi,
  hal.lyhenne                 AS hallintayksikko_lyhenne,
  urk.id                      AS urakoitsija_id,
  urk.nimi                    AS urakoitsija_nimi,
  urk.ytunnus                 AS urakoitsija_ytunnus,
  yt.yhatunnus                AS yha_yhatunnus,
  yt.yhaid                    AS yha_yhaid,
  yt.yhanimi                  AS yha_yhanimi,
  yt.elyt :: TEXT []          AS yha_elyt,
  yt.vuodet :: INTEGER []     AS yha_vuodet,
  yt.kohdeluettelo_paivitetty AS yha_kohdeluettelo_paivitetty,
  yt.sidonta_lukittu          AS yha_sidonta_lukittu,
  u.takuu_loppupvm,
  (SELECT array_agg(concat((CASE WHEN paasopimus IS NULL
    THEN '*'
                            ELSE '' END),
                           id, '=', sampoid))
   FROM sopimus s
   WHERE urakka = u.id)       AS sopimukset,
  ST_Simplify(au.alue, 50)    AS alueurakan_alue
FROM urakka u
  LEFT JOIN organisaatio hal ON u.hallintayksikko = hal.id
  LEFT JOIN organisaatio urk ON u.urakoitsija = urk.id
  LEFT JOIN alueurakka au ON u.urakkanro = au.alueurakkanro
  LEFT JOIN yhatiedot yt ON u.id = yt.urakka
WHERE hallintayksikko = :hallintayksikko
      AND (u.id IN (:sallitut_urakat)
           OR (('hallintayksikko' :: organisaatiotyyppi = :kayttajan_org_tyyppi :: organisaatiotyyppi OR
                'liikennevirasto' :: organisaatiotyyppi = :kayttajan_org_tyyppi :: organisaatiotyyppi)
               OR ('urakoitsija' :: organisaatiotyyppi = :kayttajan_org_tyyppi :: organisaatiotyyppi AND
                   :kayttajan_org_id = urk.id)));

-- name: hae-urakan-organisaatio
-- Hakee urakan organisaation urakka-id:llä.
SELECT
  o.nimi,
  o.ytunnus
FROM organisaatio o
  JOIN urakka u ON o.id = u.urakoitsija
WHERE u.id = :urakka;

-- name: hae-urakoita
-- Hakee urakoita tekstihaulla.
SELECT
  u.id,
  u.nimi,
  u.sampoid,
  u.alue,
  u.alkupvm,
  u.loppupvm,
  u.tyyppi,
  u.sopimustyyppi,
  u.takuu_loppupvm,
  hal.id                   AS hallintayksikko_id,
  hal.nimi                 AS hallintayksikko_nimi,
  hal.lyhenne              AS hallintayksikko_lyhenne,
  urk.id                   AS urakoitsija_id,
  urk.nimi                 AS urakoitsija_nimi,
  urk.ytunnus              AS urakoitsija_ytunnus,
  (SELECT array_agg(concat(id, '=', sampoid))
   FROM sopimus s
   WHERE urakka = u.id)    AS sopimukset,
  ST_Simplify(au.alue, 50) AS alueurakan_alue
FROM urakka u
  LEFT JOIN organisaatio hal ON u.hallintayksikko = hal.id
  LEFT JOIN organisaatio urk ON u.urakoitsija = urk.id
  LEFT JOIN alueurakka au ON u.urakkanro = au.alueurakkanro
WHERE u.nimi ILIKE :teksti
      OR hal.nimi ILIKE :teksti
      OR urk.nimi ILIKE :teksti;

-- name: hae-organisaation-urakat
-- Hakee organisaation "omat" urakat, joko urakat joissa annettu hallintayksikko on tilaaja
-- tai urakat joissa annettu urakoitsija on urakoitsijana.
SELECT
  u.id,
  u.nimi,
  u.sampoid,
  u.alue,
  u.alkupvm,
  u.loppupvm,
  u.tyyppi,
  u.sopimustyyppi,
  u.takuu_loppupvm,
  hal.id                   AS hallintayksikko_id,
  hal.nimi                 AS hallintayksikko_nimi,
  hal.lyhenne              AS hallintayksikko_lyhenne,
  urk.id                   AS urakoitsija_id,
  urk.nimi                 AS urakoitsija_nimi,
  urk.ytunnus              AS urakoitsija_ytunnus,
  (SELECT array_agg(concat(id, '=', sampoid))
   FROM sopimus s
   WHERE urakka = u.id)    AS sopimukset,
  ST_Simplify(au.alue, 50) AS alueurakan_alue
FROM urakka u
  LEFT JOIN organisaatio hal ON u.hallintayksikko = hal.id
  LEFT JOIN organisaatio urk ON u.urakoitsija = urk.id
  LEFT JOIN alueurakka au ON u.urakkanro = au.alueurakkanro
WHERE urk.id = :organisaatio
      OR hal.id = :organisaatio;

-- name: tallenna-urakan-sopimustyyppi!
-- Tallentaa urakalle sopimustyypin
UPDATE urakka
SET sopimustyyppi = :sopimustyyppi :: sopimustyyppi
WHERE id = :urakka;

-- name: tallenna-urakan-tyyppi!
-- Vaihtaa urakan tyypin
UPDATE urakka
SET tyyppi = :urakkatyyppi :: urakkatyyppi
WHERE id = :urakka;

-- name: hae-urakan-sopimustyyppi
-- Hakee urakan sopimustyypin
SELECT sopimustyyppi
FROM urakka
WHERE id = :urakka;

-- name: hae-urakan-tyyppi
-- Hakee urakan tyypin
SELECT tyyppi
FROM urakka
WHERE id = :urakka;

-- name: hae-urakoiden-tunnistetiedot
-- Hakee urakoista ydintiedot tekstihaulla.
SELECT
  u.id,
  u.nimi,
  u.hallintayksikko,
  u.sampoid
FROM urakka u
  LEFT JOIN organisaatio urk ON u.urakoitsija = urk.id
WHERE (u.nimi ILIKE :teksti
       OR u.sampoid ILIKE :teksti)
      AND (('hallintayksikko' :: organisaatiotyyppi = :kayttajan_org_tyyppi :: organisaatiotyyppi OR
            'liikennevirasto' :: organisaatiotyyppi = :kayttajan_org_tyyppi :: organisaatiotyyppi)
           OR ('urakoitsija' :: organisaatiotyyppi = :kayttajan_org_tyyppi :: organisaatiotyyppi AND
               :kayttajan_org_id = urk.id))
LIMIT 11;

-- name: hae-urakka
-- Hakee urakan perustiedot id:llä APIa varten.
SELECT
  u.id,
  u.nimi,
  u.tyyppi,
  u.alkupvm,
  u.loppupvm,
  u.indeksi,
  u.takuu_loppupvm,
  u.urakkanro AS alueurakkanumero,
  urk.nimi    AS urakoitsija_nimi,
  urk.ytunnus AS urakoitsija_ytunnus
FROM urakka u
  LEFT JOIN organisaatio urk ON u.urakoitsija = urk.id
WHERE u.id = :id;

-- name: hae-urakoiden-organisaatiotiedot
-- Hakee joukolle urakoita urakan ja hallintayksikön nimet ja id:t
SELECT
  u.id     AS urakka_id,
  u.nimi   AS urakka_nimi,
  u.tyyppi AS tyyppi,
  hy.id    AS hallintayksikko_id,
  hy.nimi  AS hallintayksikko_nimi
FROM urakka u
  JOIN organisaatio hy ON u.hallintayksikko = hy.id
WHERE u.id IN (:id);

-- name: hae-urakat-ytunnuksella
SELECT
  u.id,
  u.nimi,
  u.tyyppi,
  u.alkupvm,
  u.loppupvm,
  u.takuu_loppupvm,
  u.urakkanro AS alueurakkanumero,
  urk.nimi    AS urakoitsija_nimi,
  urk.ytunnus AS urakoitsija_ytunnus
FROM urakka u
  JOIN hanke h ON h.id = u.hanke
  JOIN organisaatio urk ON u.urakoitsija = urk.id
                           AND urk.ytunnus = :ytunnus;

-- name: hae-urakan-sopimukset
-- Hakee urakan sopimukset urakan id:llä.
SELECT
  s.id,
  s.nimi,
  s.alkupvm,
  s.loppupvm
FROM sopimus s
WHERE s.urakka = :urakka;

-- name: onko-olemassa
-- Tarkistaa onko id:n mukaista urakkaa olemassa tietokannassa
SELECT EXISTS(SELECT id
              FROM urakka
              WHERE id = :id);

-- name: paivita-hankkeen-tiedot-urakalle!
-- Päivittää hankkeen sampo id:n avulla urakalle
UPDATE urakka
SET hanke = (SELECT id
             FROM hanke
             WHERE sampoid = :hanke_sampo_id)
WHERE hanke_sampoid = :hanke_sampo_id;

-- name: luo-urakka<!
-- Luo uuden urakan.
INSERT INTO urakka (nimi, alkupvm, loppupvm, hanke_sampoid, sampoid, tyyppi, hallintayksikko,
                    sopimustyyppi, urakkanro)
VALUES (:nimi, :alkupvm, :loppupvm, :hanke_sampoid, :sampoid, :urakkatyyppi :: urakkatyyppi, :hallintayksikko,
        :sopimustyyppi :: sopimustyyppi, :urakkanumero);

-- name: paivita-urakka!
-- Paivittaa urakan
UPDATE urakka
SET nimi        = :nimi, alkupvm = :alkupvm, loppupvm = :loppupvm, hanke_sampoid = :hanke_sampoid,
  tyyppi        = :urakkatyyppi :: urakkatyyppi, hallintayksikko = :hallintayksikko,
  sopimustyyppi = :sopimustyyppi :: sopimustyyppi,
  urakkanro     = :urakkanro
WHERE id = :id;

-- name: paivita-tyyppi-hankkeen-urakoille!
-- Paivittaa annetun tyypin kaikille hankkeen urakoille
UPDATE urakka
SET tyyppi = :urakkatyyppi :: urakkatyyppi
WHERE hanke = (SELECT id
               FROM hanke
               WHERE sampoid = :hanke_sampoid);

-- name: hae-id-sampoidlla
-- Hakee urakan id:n sampo id:llä
SELECT urakka.id
FROM urakka
WHERE sampoid = :sampoid;

-- name: aseta-urakoitsija-sopimuksen-kautta!
-- Asettaa urakalle urakoitsijan sopimuksen Sampo id:n avulla
UPDATE urakka
SET urakoitsija = (
  SELECT id
  FROM organisaatio
  WHERE sampoid = (
    SELECT urakoitsija_sampoid
    FROM sopimus
    WHERE sampoid = :sopimus_sampoid))
WHERE sampoid = (
  SELECT urakka_sampoid
  FROM sopimus
  WHERE sampoid = :sopimus_sampoid AND
        paasopimus IS NULL);

-- name: aseta-urakoitsija-urakoille-yhteyshenkilon-kautta!
-- Asettaa urakoille urakoitsijan yhteyshenkilön Sampo id:n avulla
UPDATE urakka
SET urakoitsija = (
  SELECT id
  FROM organisaatio
  WHERE sampoid = :urakoitsija_sampoid)
WHERE sampoid IN (
  SELECT urakka_sampoid
  FROM sopimus
  WHERE urakoitsija_sampoid = :urakoitsija_sampoid AND
        paasopimus IS NULL);

-- name: hae-yksittainen-urakka
-- Hakee yhden urakan id:n avulla
SELECT
  u.id,
  u.nimi,
  u.sampoid,
  u.alue,
  u.alkupvm,
  u.loppupvm,
  u.tyyppi,
  u.sopimustyyppi,
  u.takuu_loppupvm,
  hal.id                                                        AS hallintayksikko_id,
  hal.nimi                                                      AS hallintayksikko_nimi,
  hal.lyhenne                                                   AS hallintayksikko_lyhenne,
  urk.id                                                        AS urakoitsija_id,
  urk.nimi                                                      AS urakoitsija_nimi,
  urk.ytunnus                                                   AS urakoitsija_ytunnus,
  yt.yhatunnus                                                  AS yha_yhatunnus,
  yt.yhaid                                                      AS yha_yhaid,
  yt.yhanimi                                                    AS yha_yhanimi,
  yt.elyt :: TEXT []                                            AS yha_elyt,
  yt.vuodet :: INTEGER []                                       AS yha_vuodet,
  yt.kohdeluettelo_paivitetty                                   AS yha_kohdeluettelo_paivitetty,
  yt.sidonta_lukittu                                            AS yha_sidonta_lukittu,
  (SELECT EXISTS(SELECT id
                 FROM paallystysilmoitus
                 WHERE paallystyskohde IN (SELECT id
                                           FROM yllapitokohde
                                           WHERE urakka = u.id)))
  OR
  (SELECT EXISTS(SELECT id
                 FROM paikkausilmoitus
                 WHERE paikkauskohde IN (SELECT id
                                         FROM yllapitokohde
                                         WHERE urakka = u.id))) AS sisaltaa_ilmoituksia,
  (SELECT array_agg(concat(id, '=', sampoid))
   FROM sopimus s
   WHERE urakka = u.id)                                         AS sopimukset,
  ST_Simplify(au.alue, 50)                                      AS alueurakan_alue
FROM urakka u
  LEFT JOIN organisaatio hal ON u.hallintayksikko = hal.id
  LEFT JOIN organisaatio urk ON u.urakoitsija = urk.id
  LEFT JOIN alueurakka au ON u.urakkanro = au.alueurakkanro
  LEFT JOIN yhatiedot yt ON u.id = yt.urakka
WHERE u.id = :urakka_id;

-- name: hae-urakan-urakoitsija
-- Hakee valitun urakan urakoitsijan id:n
SELECT urakoitsija
FROM urakka
WHERE id = :urakka_id;

-- name: paivita-urakka-alueiden-nakyma
-- Päivittää urakka-alueiden materialisoidun näkymän
SELECT paivita_urakoiden_alueet();

-- name: hae-urakan-alueurakkanumero
-- Hakee urakan alueurakkanumeron
SELECT urakkanro AS alueurakkanro
FROM urakka
WHERE id = :id;

-- name: hae-aktiivisten-hoitourakoiden-alueurakkanumerot
-- Hakee käynnissäolevien hoitourakoiden alueurakkanumerot
SELECT
  u.id,
  u.hanke,
  u.nimi,
  u.urakkanro AS alueurakkanro
FROM urakka u
WHERE u.id IN (SELECT id
               FROM urakka
               WHERE (tyyppi = 'hoito' AND
                      u.hanke IS NOT NULL AND
                      (SELECT EXTRACT(YEAR FROM u.alkupvm)) <= :vuosi AND
                      :vuosi <= (SELECT EXTRACT(YEAR FROM u.loppupvm))));

-- name: hae-hallintayksikon-kaynnissa-olevat-urakat
-- Palauttaa nimen ja id:n hallintayksikön käynnissä olevista urakoista
SELECT
  id,
  nimi
FROM urakka
WHERE hallintayksikko = :hal
      AND (alkupvm IS NULL OR alkupvm <= current_date)
      AND (loppupvm IS NULL OR loppupvm >= current_date);

-- name: onko-urakalla-tehtavaa
SELECT EXISTS(
    SELECT tpk.id
    FROM toimenpidekoodi tpk
      INNER JOIN toimenpideinstanssi tpi
        ON tpi.toimenpide = tpk.emo
    WHERE
      tpi.urakka = :urakkaid AND
      tpk.id = :tehtavaid);

-- name: hae-urakka-sijainnilla
-- Hakee sijainnin ja urakan tyypin perusteella urakan. Urakan täytyy myös olla käynnissä.
SELECT u.id
FROM urakka u
  LEFT JOIN urakoiden_alueet ua ON u.id = ua.id
WHERE u.tyyppi = :urakkatyyppi :: urakkatyyppi
      AND (u.alkupvm IS NULL OR u.alkupvm <= current_timestamp)
      AND (u.loppupvm IS NULL OR u.loppupvm > current_timestamp)
      AND
      ((:urakkatyyppi = 'hoito' AND (st_contains(ua.alue, ST_MakePoint(:x, :y))))
       OR
       (:urakkatyyppi = 'valaistus' AND
        exists(SELECT id
               FROM valaistusurakka vu
               WHERE vu.valaistusurakkanro = u.urakkanro AND
                     st_dwithin(vu.alue, st_makepoint(:x, :y), :threshold)))
       OR
       ((:urakkatyyppi = 'paallystys' OR :urakkatyyppi = 'paikkaus') AND
        exists(SELECT id
               FROM paallystyspalvelusopimus pps
               WHERE pps.paallystyspalvelusopimusnro = u.urakkanro AND
                     st_dwithin(pps.alue, st_makepoint(:x, :y), :threshold))))

ORDER BY id ASC;

-- name: luo-alueurakka<!
INSERT INTO alueurakka (alueurakkanro, alue, elynumero)
VALUES (:alueurakkanro, ST_GeomFromText(:alue) :: GEOMETRY, :elynumero);

-- name: paivita-alueurakka!
UPDATE alueurakka
SET alueurakkanro = :alueurakkanro,
  alue            = ST_GeomFromText(:alue) :: GEOMETRY,
  elynumero       = :elynumero;

-- name: hae-alueurakka-numerolla
SELECT *
FROM alueurakka
WHERE alueurakkanro = :alueurakkanro;

-- name: tuhoa-alueurakkadata!
DELETE FROM alueurakka;

-- name: hae-urakan-geometria
SELECT
  u.alue          AS urakka_alue,
  alueurakka.alue AS alueurakka_alue
FROM urakka u
  LEFT JOIN alueurakka ON u.urakkanro = alueurakka.alueurakkanro
WHERE u.id = :id;

-- name: hae-urakoiden-geometriat
SELECT
  ST_Simplify(u.alue, :toleranssi)          AS urakka_alue,
  u.id                                      AS urakka_id,
  ST_Simplify(alueurakka.alue, :toleranssi) AS alueurakka_alue
FROM urakka u
  LEFT JOIN alueurakka ON u.urakkanro = alueurakka.alueurakkanro
WHERE u.id IN (:idt);

-- name: hae-urakan-sampo-id
-- single?: true
SELECT sampoid
FROM urakka
WHERE id = :urakka;

-- name: hae-urakan-perustiedot-sampo-idlla
SELECT
  id,
  nimi,
  alkupvm,
  loppupvm,
  tyyppi
FROM urakka
WHERE sampoid = :sampoid;

-- name: aseta-takuun-loppupvm!
UPDATE urakka
SET takuu_loppupvm = :loppupvm
WHERE id = :urakka

-- name: aseta-urakan-indeksi!
UPDATE urakka
SET indeksi = :indeksi
WHERE id = :urakka

-- name: tuhoa-valaistusurakkadata!
DELETE FROM valaistusurakka;

-- name: hae-valaistusurakan-alueurakkanumero-sijainnilla
SELECT alueurakka
FROM valaistusurakka
WHERE st_dwithin(alue, st_makepoint(:x, :y), :treshold);

-- name: luo-valaistusurakka<!
INSERT INTO valaistusurakka (alueurakkanro, alue, valaistusurakkanro)
VALUES (:alueurakkanro, ST_GeomFromText(:alue) :: GEOMETRY, :valaistusurakka);

-- name: tuhoa-paallystyspalvelusopimusdata!
DELETE FROM paallystyspalvelusopimus;

-- name: hae-paallystyspalvelusopimus-alueurakkanumero-sijainnilla
SELECT alueurakka
FROM paallystyspalvelusopimus
WHERE st_dwithin(alue, st_makepoint(:x, :y), :treshold);

-- name: luo-paallystyspalvelusopimus<!
INSERT INTO paallystyspalvelusopimus (alueurakkanro, alue, paallystyspalvelusopimusnro)
VALUES (:alueurakkanro, ST_GeomFromText(:alue) :: GEOMETRY, :paallystyssopimus);
