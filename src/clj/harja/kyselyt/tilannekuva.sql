-- name: hae-ilmoitukset
SELECT
  i.id,
  i.urakka,
  i.ilmoitusid,
  i.ilmoitettu,
  i.valitetty,
  i.yhteydenottopyynto,
  i.otsikko,
  i.paikankuvaus,
  i.lisatieto,
  i.tila,
  i.ilmoitustyyppi,
  i.selitteet,
  i.urakkatyyppi,

  ST_Simplify(i.sijainti, :toleranssi) AS sijainti,
  i.tr_numero,
  i.tr_alkuosa,
  i.tr_loppuosa,
  i.tr_alkuetaisyys,
  i.tr_loppuetaisyys,

  i.ilmoittaja_etunimi,
  i.ilmoittaja_sukunimi,
  i.ilmoittaja_tyopuhelin,
  i.ilmoittaja_matkapuhelin,
  i.ilmoittaja_sahkoposti,
  i.ilmoittaja_tyyppi,

  i.lahettaja_etunimi,
  i.lahettaja_sukunimi,
  i.lahettaja_puhelinnumero,
  i.lahettaja_sahkoposti,

  it.id                               AS kuittaus_id,
  it.kuitattu                         AS kuittaus_kuitattu,
  it.vapaateksti                      AS kuittaus_vapaateksti,
  it.kuittaustyyppi                   AS kuittaus_kuittaustyyppi,

  it.kuittaaja_henkilo_etunimi        AS kuittaus_kuittaaja_etunimi,
  it.kuittaaja_henkilo_sukunimi       AS kuittaus_kuittaaja_sukunimi,
  it.kuittaaja_henkilo_matkapuhelin   AS kuittaus_kuittaaja_matkapuhelin,
  it.kuittaaja_henkilo_tyopuhelin     AS kuittaus_kuittaaja_tyopuhelin,
  it.kuittaaja_henkilo_sahkoposti     AS kuittaus_kuittaaja_sahkoposti,
  it.kuittaaja_organisaatio_nimi      AS kuittaus_kuittaaja_organisaatio,
  it.kuittaaja_organisaatio_ytunnus   AS kuittaus_kuittaaja_ytunnus,

  it.kasittelija_henkilo_etunimi      AS kuittaus_kasittelija_etunimi,
  it.kasittelija_henkilo_sukunimi     AS kuittaus_kasittelija_sukunimi,
  it.kasittelija_henkilo_matkapuhelin AS kuittaus_kasittelija_matkapuhelin,
  it.kasittelija_henkilo_tyopuhelin   AS kuittaus_kasittelija_tyopuhelin,
  it.kasittelija_henkilo_sahkoposti   AS kuittaus_kasittelija_sahkoposti,
  it.kasittelija_organisaatio_nimi    AS kuittaus_kasittelija_organisaatio,
  it.kasittelija_organisaatio_ytunnus AS kuittaus_kasittelija_ytunnus,

  EXISTS(SELECT * FROM ilmoitustoimenpide WHERE ilmoitus = i.id
                                                AND kuittaustyyppi = 'vastaanotto'::kuittaustyyppi) as vastaanotettu,
  EXISTS(SELECT * FROM ilmoitustoimenpide WHERE ilmoitus = i.id
                                                AND kuittaustyyppi = 'aloitus'::kuittaustyyppi) as aloitettu,
  EXISTS(SELECT * FROM ilmoitustoimenpide WHERE ilmoitus = i.id
                                                AND kuittaustyyppi = 'lopetus'::kuittaustyyppi) as lopetettu
FROM ilmoitus i
  LEFT JOIN ilmoitustoimenpide it ON it.ilmoitus = i.id
WHERE
  ((:alku :: DATE IS NULL AND :loppu :: DATE IS NULL)
   OR i.ilmoitettu BETWEEN :alku AND :loppu) AND
  (i.urakka IS NULL OR i.urakka IN (:urakat)) AND
  i.ilmoitustyyppi :: TEXT IN (:tyypit);

-- name: hae-laatupoikkeamat
SELECT
  l.id,
  l.aika,
  l.kohde,
  l.tekija,
  l.kuvaus,
  ST_Simplify(l.sijainti, :toleranssi) AS sijainti,
  l.tarkastuspiste,
  CONCAT(k.etunimi, ' ', k.sukunimi) AS tekijanimi,
  l.kasittelyaika                    AS paatos_kasittelyaika,
  l.paatos                           AS paatos_paatos,
  l.kasittelytapa                    AS paatos_kasittelytapa,
  l.perustelu                        AS paatos_perustelu,
  l.muu_kasittelytapa                AS paatos_muukasittelytapa,
  l.selvitys_pyydetty                AS selvityspyydetty,

  l.tr_numero,
  l.tr_alkuosa,
  l.tr_alkuetaisyys,
  l.tr_loppuosa,
  l.tr_loppuetaisyys
FROM laatupoikkeama l
  JOIN kayttaja k ON l.luoja = k.id
WHERE (l.urakka IN (:urakat) OR l.urakka IS NULL)
      AND (l.aika BETWEEN :alku AND :loppu OR
           l.kasittelyaika BETWEEN :alku AND :loppu) AND
      l.tekija :: TEXT IN (:tekijat)
      AND l.poistettu IS NOT TRUE;

-- name: hae-tarkastukset
-- fetch-size: 64
-- row-fn: geo/muunna-reitti
SELECT
  ST_Simplify(t.sijainti, :toleranssi) AS reitti,
  t.laadunalitus,
  t.tyyppi,
  CASE WHEN o.tyyppi = 'urakoitsija' :: organisaatiotyyppi
       THEN 'urakoitsija' :: osapuoli
       ELSE 'tilaaja' :: osapuoli
       END AS tekija
FROM tarkastus t
     JOIN kayttaja k ON t.luoja = k.id
     JOIN organisaatio o ON o.id = k.organisaatio
WHERE sijainti IS NOT NULL AND
      (t.urakka IN (:urakat) OR t.urakka IS NULL) AND
      (t.aika BETWEEN :alku AND :loppu) AND
      ST_Intersects(t.envelope, ST_MakeEnvelope(:xmin, :ymin, :xmax, :ymax)) AND
      t.tyyppi :: TEXT IN (:tyypit);

-- name: hae-turvallisuuspoikkeamat
SELECT
  t.id,
  t.urakka,
  t.tapahtunut,
  t.kasitelty,
  t.tyontekijanammatti,
  t.kuvaus,
  t.vammat,
  t.sairauspoissaolopaivat,
  t.sairaalavuorokaudet,
  t.vakavuusaste,
  ST_Simplify(t.sijainti, :toleranssi) AS sijainti,
  t.tr_numero,
  t.tr_alkuetaisyys,
  t.tr_loppuetaisyys,
  t.tr_alkuosa,
  t.tr_loppuosa,
  t.tyyppi,

  k.id              AS korjaavatoimenpide_id,
  k.kuvaus          AS korjaavatoimenpide_kuvaus,
  k.suoritettu      AS korjaavatoimenpide_suoritettu
FROM turvallisuuspoikkeama t
  LEFT JOIN korjaavatoimenpide k ON t.id = k.turvallisuuspoikkeama
                                    AND k.poistettu IS NOT TRUE
WHERE
  (t.urakka IS NULL OR t.urakka IN (:urakat)) AND
  (t.tapahtunut :: DATE BETWEEN :alku AND :loppu OR
   t.kasitelty BETWEEN :alku AND :loppu);

-- name: hae-paallystykset-nykytilanteeseen
-- Hakee nykytilanteeseen kaikki päällystyskohteet, jotka eivät ole valmiita tai ovat
-- valmistuneet viikon sisällä.
SELECT
  ypk.id,
  ypk.kohdenumero,
  ypk.nimi AS kohde_nimi,
  ypko.nimi AS kohdeosa_nimi,
  ST_Simplify(ypko.sijainti, :toleranssi) AS sijainti,
  ypko.tr_numero,
  ypko.tr_alkuosa,
  ypko.tr_alkuetaisyys,
  ypko.tr_loppuosa,
  ypko.tr_loppuetaisyys,
  pi.id   AS paallystysilmoitus_id,
  pi.tila AS paallystysilmoitus_tila,
  pi.aloituspvm,
  pi.valmispvm_paallystys AS paallystysvalmispvm,
  pi.valmispvm_kohde AS kohdevalmispvm,
  pi.tila
FROM yllapitokohdeosa ypko
  LEFT JOIN yllapitokohde ypk ON ypko.yllapitokohde = ypk.id
  LEFT JOIN paallystysilmoitus pi ON pi.paallystyskohde = ypk.id
WHERE ypk.poistettu IS NOT TRUE
      AND (pi.tila :: TEXT != 'valmis' OR
           (now() - pi.valmispvm_kohde) < INTERVAL '7 days');

-- name: hae-paallystykset-historiakuvaan
-- Hakee historiakuvaan kaikki päällystyskohteet, jotka ovat olleet aktiivisia
-- annetulla aikavälillä
SELECT
  ypk.id,
  ypk.kohdenumero,
  ypk.nimi                                AS kohde_nimi,
  ypko.nimi                               AS kohdeosa_nimi,
  ST_Simplify(ypko.sijainti, :toleranssi) AS sijainti,
  ypko.tr_numero,
  ypko.tr_alkuosa,
  ypko.tr_alkuetaisyys,
  ypko.tr_loppuosa,
  ypko.tr_loppuetaisyys,
  pi.id                                   AS paallystysilmoitus_id,
  pi.tila                                 AS paallystysilmoitus_tila,
  pi.aloituspvm,
  pi.valmispvm_paallystys                 AS paallystysvalmispvm,
  pi.valmispvm_kohde                      AS kohdevalmispvm,
  pi.tila
FROM yllapitokohdeosa ypko
  LEFT JOIN yllapitokohde ypk ON ypko.yllapitokohde = ypk.id
  LEFT JOIN paallystysilmoitus pi ON pi.paallystyskohde = ypk.id
WHERE ypk.poistettu IS NOT TRUE AND
      (pi.aloituspvm < :loppu AND (pi.valmispvm_kohde IS NULL OR pi.valmispvm_kohde > :alku));

-- name: hae-paikkaukset-nykytilanteeseen
-- Hakee nykytilanteeseen kaikki paikkauskohteet, jotka eivät ole valmiita tai ovat
-- valmistuneet viikon sisällä.
SELECT
  ypk.id,
  ypk.kohdenumero,
  ypk.nimi AS kohde_nimi,
  ypk.nimi AS kohdeosa_nimi,
  ST_Simplify(ypko.sijainti, :toleranssi) AS sijainti,
  ypko.tr_numero,
  ypko.tr_alkuosa,
  ypko.tr_alkuetaisyys,
  ypko.tr_loppuosa,
  ypko.tr_loppuetaisyys,
  pi.id   AS paikkausilmoitus_id,
  pi.tila AS paikkausilmoitus_tila,
  pi.aloituspvm,
  pi.valmispvm_paikkaus AS paikkausvalmispvm,
  pi.valmispvm_kohde AS kohdevalmispvm,
  pi.tila
FROM yllapitokohdeosa ypko
  LEFT JOIN yllapitokohde ypk ON ypko.yllapitokohde = ypk.id
  LEFT JOIN paikkausilmoitus pi ON pi.paikkauskohde = ypk.id
WHERE ypk.poistettu IS NOT TRUE
      AND (pi.tila :: TEXT != 'valmis' OR
           (now() - pi.valmispvm_kohde) < INTERVAL '7 days');

-- name: hae-paikkaukset-historiakuvaan
-- Hakee historiakuvaan kaikki paikkauskohteet, jotka ovat olleet aktiivisia
-- annetulla aikavälillä
SELECT
  pk.id,
  pk.kohdenumero,
  pk.nimi AS kohde_nimi,
  ypko.nimi AS kohdeosa_nimi,
  ST_Simplify(ypko.sijainti, :toleranssi) AS sijainti,
  ypko.tr_numero,
  ypko.tr_alkuosa,
  ypko.tr_alkuetaisyys,
  ypko.tr_loppuosa,
  ypko.tr_loppuetaisyys,
  pi.id   AS paikkausilmoitus_id,
  pi.tila AS paikkausilmoitus_tila,
  pi.aloituspvm,
  pi.valmispvm_paikkaus AS paikkausvalmispvm,
  pi.valmispvm_kohde AS kohdevalmispvm,
  pi.tila
FROM yllapitokohdeosa ypko
  LEFT JOIN yllapitokohde pk ON ypko.yllapitokohde = pk.id
  LEFT JOIN paikkausilmoitus pi ON pi.paikkauskohde = pk.id
WHERE pk.poistettu IS NOT TRUE AND
      (pi.aloituspvm < :loppu AND (pi.valmispvm_kohde IS NULL OR pi.valmispvm_kohde > :alku));

-- name: hae-toteumat
-- fetch-size: 64
-- row-fn: muunna-reitti
SELECT
  t.tyyppi,
  ST_Simplify(t.reitti, :toleranssi) as reitti,
  tt.toimenpidekoodi          AS tehtava_toimenpidekoodi,
  tpk.nimi                    AS tehtava_toimenpide
FROM toteuma_tehtava tt
  JOIN toteuma t ON tt.toteuma = t.id
                    AND t.alkanut >= :alku
                    AND t.paattynyt <= :loppu
                    AND t.poistettu IS NOT TRUE
  JOIN toimenpidekoodi tpk ON tpk.id = tt.toimenpidekoodi
WHERE tt.poistettu IS NOT TRUE AND
      tt.toimenpidekoodi IN (:toimenpidekoodit) AND
      (t.urakka IN (:urakat) OR t.urakka IS NULL) AND
      (t.alkanut BETWEEN :alku AND :loppu) AND
      (t.paattynyt BETWEEN :alku AND :loppu) AND
      ST_Intersects(t.envelope, ST_MakeEnvelope(:xmin, :ymin, :xmax, :ymax));

-- name: hae-toteumien-selitteet
SELECT
  count(*) AS lukumaara,
  tt.toimenpidekoodi AS toimenpidekoodi,
          (SELECT nimi
           FROM toimenpidekoodi tpk
           WHERE id = tt.toimenpidekoodi) AS toimenpide
FROM toteuma_tehtava tt
  JOIN toteuma t ON tt.toteuma = t.id
                    AND t.alkanut >= :alku
                    AND t.paattynyt <= :loppu
                    AND tt.toimenpidekoodi IN (:toimenpidekoodit)
                    AND tt.poistettu IS NOT TRUE
                    AND t.poistettu IS NOT TRUE
WHERE (t.urakka IN (:urakat) OR t.urakka IS NULL) AND
      (t.alkanut BETWEEN :alku AND :loppu) AND
      (t.paattynyt BETWEEN :alku AND :loppu) AND
      ST_Intersects(t.envelope, ST_MakeEnvelope(:xmin, :ymin, :xmax, :ymax))
GROUP BY tt.toimenpidekoodi;


-- name: hae-tyokoneet
SELECT
  t.tyokoneid,
  t.jarjestelma,
  t.organisaatio,
  (SELECT nimi
   FROM organisaatio
   WHERE id = t.organisaatio) AS organisaationimi,
  t.viestitunniste,
  t.lahetysaika,
  t.vastaanotettu,
  t.tyokonetyyppi,
  t.sijainti,
  t.suunta,
  t.edellinensijainti,
  t.urakkaid,
  (SELECT nimi
   FROM urakka
   WHERE id = t.urakkaid)     AS urakkanimi,
  t.tehtavat
FROM tyokonehavainto t
WHERE ST_Contains(ST_MakeEnvelope(:xmin, :ymin, :xmax, :ymax),
                  CAST(sijainti AS GEOMETRY)) AND
      (t.urakkaid IN (:urakat) OR
      -- Jos urakkatietoa ei ole, näytetään vain oman organisaation (tai tilaajalle kaikki)
       (t.urakkaid IS NULL AND
       (:nayta-kaikki OR t.organisaatio = :organisaatio))) AND
      -- Rajaa toimenpiteellä
      (t.tehtavat && :toimenpiteet :: suoritettavatehtava []);

-- name: hae-toimenpidekoodit
SELECT
  id
FROM toimenpidekoodi
WHERE suoritettavatehtava :: TEXT IN (:toimenpiteet);

-- name: hae-suljetut-tieosuudet
-- hakee liikenneohjausaidoilla suljettujen tieosuuksien geometriat
SELECT st.geometria AS "geometria",
  ypk.nimi                                                       AS "yllapitokohteen-nimi",
  ypk.kohdenumero                                                AS "yllapitokohteen-numero",
  st.kaistat                                                     AS "kaistat",
  st.ajoradat                                                    AS "ajoradat",
  st.asetettu                                                    AS "aika",
  st.tr_tie                                                      AS "tie",
  st.tr_aosa                                                     AS "aosa",
  st.tr_aet                                                      AS "aet",
  st.tr_losa                                                     AS "losa",
  st.tr_let                                                      AS "let"
FROM suljettu_tieosuus st
  LEFT JOIN yllapitokohde ypk ON ypk.id = st.yllapitokohde
WHERE st.poistettu IS NULL
      AND ST_Intersects(ST_MakeEnvelope(:x1, :y1, :x2, :y2), st.envelope);
