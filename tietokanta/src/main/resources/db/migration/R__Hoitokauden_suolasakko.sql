-- Uusi suolasakon laskenta


CREATE OR REPLACE FUNCTION hoitokauden_suolasakkorivi(
  urakka_id INTEGER,
  hk_alkupvm DATE,
  hk_loppupvm DATE)
  RETURNS hk_suolasakko AS $$
DECLARE
  ss suolasakko%rowtype; -- hoitokauden suolasakkomäärittely
  lampotilat lampotilat%rowtype; -- talvikauden lämpötilat
  lampotilapoikkeama NUMERIC;
  suolankaytto NUMERIC; -- yhteenlaskettu suolan käyttö hoitokaudella
  sallittu_suolankaytto NUMERIC; -- kuinka paljon suolaa sallitaan
  suolasakko NUMERIC; -- suolasakon määrä
  vertailu NUMERIC; -- pitkä keskilämpö vertailukaudelle
  urakan_alkuvuosi INTEGER;
BEGIN
  -- Haetaan urakan alkuvuosi vertailukauden päättelemiseksi
  SELECT EXTRACT(YEAR FROM alkupvm) INTO urakan_alkuvuosi
    FROM urakka
   WHERE id = urakka_id;

  -- Haetaan relevantti suolasakkomäärittely ja lämpötilat
  SELECT * INTO ss FROM suolasakko
  WHERE urakka = urakka_id
        AND hoitokauden_alkuvuosi = EXTRACT(YEAR from hk_alkupvm);

  IF ss IS NULL OR ss.kaytossa = FALSE
  THEN
    RAISE NOTICE 'Urakalle % ei ole suolasakkomäärittelyä tai suolasakot eivät ole käyttössä % - %', urakka_id, hk_alkupvm, hk_loppupvm;
    RETURN NULL;
  END IF;

  SELECT * INTO lampotilat FROM lampotilat
  WHERE urakka = urakka_id
        AND alkupvm = hk_alkupvm AND loppupvm = hk_loppupvm;

  IF urakan_alkuvuosi <= 2014 THEN
    vertailu := lampotilat.pitka_keskilampotila_vanha;
  ELSE
    vertailu := lampotilat.pitka_keskilampotila;
  END IF;

  IF (lampotilat IS NULL OR lampotilat.keskilampotila IS NULL OR vertailu IS NULL)
  THEN
    RAISE NOTICE 'Urakalle % ei ole lämpötiloja hoitokaudelle % - %', urakka_id, hk_alkupvm, hk_loppupvm;
    RAISE NOTICE 'Keskilämpötila hoitokaudella %, pitkän ajan keskilämpötila %', lampotilat.keskilampotila, vertailu;
  END IF;

  RAISE NOTICE 'maksukuukausi: %', ss.maksukuukausi;

  -- HUOM: sallittu suolankäyttö ja raportoitu suolankäyttö
  -- oletetaan olevan kirjattu tonneina, joka on nykytilanne
  -- tietokannan materiaalikoodin yksiköissä.
  -- Jos raportointia tehdään muissa yksiköissä,
  -- näiden hakujen tulee muuntaa ne tonneiksi laskentaa varten.

  -- Haetaan suolan käytön rajat hoitokaudella
  sallittu_suolankaytto := ss.talvisuolaraja;

  RAISE NOTICE 'sallittu suolankäyttö: %', sallittu_suolankaytto;

  -- Haetaan suolankäytön toteuma
  SELECT SUM(maara) INTO suolankaytto
    FROM sopimuksen_kaytetty_materiaali skm
         JOIN materiaalikoodi mk ON skm.materiaalikoodi=mk.id
   WHERE mk.materiaalityyppi = 'talvisuola'::materiaalityyppi AND
         skm.sopimus IN (SELECT id FROM sopimus WHERE urakka = urakka_id) AND
	 skm.alkupvm BETWEEN hk_alkupvm AND hk_loppupvm;

  RAISE NOTICE 'Suolaa käytetty: %', suolankaytto;

  -- Tarkistetaan lämpötilakorjaus sallittuun suolamäärään
  lampotilapoikkeama := lampotilat.keskilampotila - vertailu;
  IF lampotilapoikkeama IS NULL THEN
    sallittu_suolankaytto := NULL;
  ELSIF lampotilapoikkeama >= 4.0 THEN
    RAISE NOTICE 'Lämpötilapoikkeama % >= 4 astetta, 30%% korotus sallittuun suolankäyttöön', lampotilapoikkeama;
    sallittu_suolankaytto := 1.30 * sallittu_suolankaytto;
  ELSIF lampotilapoikkeama >= 3.0 THEN
    RAISE NOTICE 'Lämpötilapoikkeama % >= 3 astetta, 20%% korotus sallittuun suolankäyttöön', lampotilapoikkeama;
    sallittu_suolankaytto := 1.20 * sallittu_suolankaytto;
  ELSIF lampotilapoikkeama > 2.0 THEN
    RAISE NOTICE 'Lämpötilapoikkeama % > 2 astetta, 10%% korotus sallittuun suolankäyttöön', lampotilapoikkeama;
    sallittu_suolankaytto := 1.10 * sallittu_suolankaytto;
  ELSE
    RAISE NOTICE 'Lämpötilapoikkeama % alle 2 astetta, ei korotusta sallittuun suolankäyttöön', lampotilapoikkeama;
  END IF;

  -- Tarkistetaan ylittyykö sallittu suolankäyttö yli 5%
  IF suolankaytto > 1.05 * sallittu_suolankaytto THEN
    RAISE NOTICE 'sakotellaan, %', ss.maara;
    suolasakko := ss.maara * (suolankaytto - (1.05 * sallittu_suolankaytto));
  ELSE
    suolasakko := 0.0;
  END IF;

  RETURN (urakka_id,
  	  -- talvikauden keskilämpötila, vertailukauden keskilämpötila ja poikkeama
          lampotilat.keskilampotila, vertailu, lampotilapoikkeama,

          -- toteutunut suolankäyttö
          suolankaytto,

	  -- käyttöraja ja sakkoraja ennen kohtuullistamista
	  ss.talvisuolaraja, 1.05 * ss.talvisuolaraja,

	  -- käyttöraja ja sakkoraja kohtuullistamisen jälkeen
	  sallittu_suolankaytto, 1.05 * sallittu_suolankaytto,

          -- sakon määrä per ylitystonni ja sakon loppusumma
	  ss.maara, -suolasakko);
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION hoitokauden_suolasakko(
  urakka_id INTEGER,
  hk_alkupvm DATE,
  hk_loppupvm DATE)
  RETURNS NUMERIC AS $$
DECLARE
  ss hk_suolasakko;
BEGIN
  ss := hoitokauden_suolasakkorivi(urakka_id, hk_alkupvm, hk_loppupvm);
  RETURN ss.suolasakko;
END;
$$ LANGUAGE plpgsql;
