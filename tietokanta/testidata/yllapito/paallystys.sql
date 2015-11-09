-- Päällystyskohteet

INSERT INTO paallystyskohde (urakka, sopimus, kohdenumero, nimi, sopimuksen_mukaiset_tyot, muu_tyo, arvonvahennykset, bitumi_indeksi, kaasuindeksi) VALUES ((SELECT id FROM urakka WHERE  nimi = 'Muhoksen päällystysurakka'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Muhoksen päällystysurakka') AND paasopimus IS null), 'L03', 'Leppäjärven ramppi', 400, true, 100, 4543.95, 0);

INSERT INTO paallystyskohde (urakka, sopimus, kohdenumero, nimi, sopimuksen_mukaiset_tyot, muu_tyo, arvonvahennykset, bitumi_indeksi, kaasuindeksi) VALUES ((SELECT id FROM urakka WHERE  nimi = 'Muhoksen päällystysurakka'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Muhoksen päällystysurakka') AND paasopimus IS null), 308, 'Mt 2855 Viisari - Renko', 9000, false, 200, 565, 100);

INSERT INTO paallystyskohde (urakka, sopimus, kohdenumero, nimi, sopimuksen_mukaiset_tyot, muu_tyo, arvonvahennykset, bitumi_indeksi, kaasuindeksi) VALUES ((SELECT id FROM urakka WHERE  nimi = 'Muhoksen päällystysurakka'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Muhoksen päällystysurakka') AND paasopimus IS null), 'L010', 'Tie 357', 500, true, 3457, 5, 6);

INSERT INTO paallystyskohde (urakka, sopimus, kohdenumero, nimi, sopimuksen_mukaiset_tyot, muu_tyo, arvonvahennykset, bitumi_indeksi, kaasuindeksi) VALUES ((SELECT id FROM urakka WHERE  nimi = 'Muhoksen päällystysurakka'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Muhoksen päällystysurakka') AND paasopimus IS null), 310, 'Oulaisten ohitusramppi', 500, false, 3457, 5, 6);
INSERT INTO paallystyskohdeosa (paallystyskohde, nimi, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, kvl, nykyinen_paallyste, toimenpide, sijainti) VALUES ((SELECT id FROM paallystyskohde WHERE nimi ='Oulaisten ohitusramppi'), 'Laivaniemi 1', 19521, 10, 5, 10, 15, 2, 2, 'PAB-B 16/80 MPKJ', ST_GeomFromText('MULTILINESTRING((426888 7212758,427081 7212739),(434777 7215499,436899 7217174,438212 7219910,438676 7220554,440102 7221432,441584 7222729,442255 7223162,443128 7223398,443750 7223713,448682 7225293,451886 7226708,456379 7228018,459945 7229222,461039 7229509))'));
INSERT INTO paallystyskohdeosa (paallystyskohde, nimi, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, kvl, nykyinen_paallyste, toimenpide, sijainti) VALUES ((SELECT id FROM paallystyskohde WHERE nimi ='Oulaisten ohitusramppi'), 'Laivaniemi 2', 19521, 10, 5, 10, 15, 2, 2, 'PAB-B 16/80 MPKJ', ST_GeomFromText('MULTILINESTRING((384276 6674532,384269 6674528,383563 6674582,383518 6674607,383350 6674736,383244 6674822,383201 6674859,383028 6675028,382959 6675071,382825 6675131,382737 6675175,382737 6675213,382730 6675251,382615 6675745,382569 6675961,382555 6675978,382529 6675975,382519 6675967))'));


INSERT INTO paallystyskohde (urakka, sopimus, kohdenumero, nimi) VALUES ((SELECT id FROM urakka WHERE  nimi = 'Tienpäällystysurakka KAS ELY 1 2015'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Tienpäällystysurakka KAS ELY 1 2015') AND paasopimus IS null), '1501', 'Vt13 Hartikkala - Pelkola');

INSERT INTO paallystyskohde (urakka, sopimus, kohdenumero, nimi) VALUES ((SELECT id FROM urakka WHERE  nimi = 'Tienpäällystysurakka KAS ELY 1 2015'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Tienpäällystysurakka KAS ELY 1 2015') AND paasopimus IS null), '1502', 'Vt 13 Kähärilä - Liikka');

INSERT INTO paallystyskohde (urakka, sopimus, kohdenumero, nimi) VALUES ((SELECT id FROM urakka WHERE  nimi = 'Tienpäällystysurakka KAS ELY 1 2015'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Tienpäällystysurakka KAS ELY 1 2015') AND paasopimus IS null), '1503', 'Mt 387 Mattila - Hanhi-Kemppi');

INSERT INTO paallystyskohde (urakka, sopimus, kohdenumero, nimi) VALUES ((SELECT id FROM urakka WHERE  nimi = 'Tienpäällystysurakka KAS ELY 1 2015'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Tienpäällystysurakka KAS ELY 1 2015') AND paasopimus IS null), '1504', 'Mt 408 Pallo - Kivisalmi');

INSERT INTO paallystyskohde (urakka, sopimus, kohdenumero, nimi) VALUES ((SELECT id FROM urakka WHERE  nimi = 'Tienpäällystysurakka KAS ELY 1 2015'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Tienpäällystysurakka KAS ELY 1 2015') AND paasopimus IS null), '1505', 'Kt 62 Sotkulampi - Rajapatsas');

INSERT INTO paallystyskohde (urakka, sopimus, kohdenumero, nimi) VALUES ((SELECT id FROM urakka WHERE  nimi = 'Tienpäällystysurakka KAS ELY 1 2015'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Tienpäällystysurakka KAS ELY 1 2015') AND paasopimus IS null), '1506', 'Kt 62 Haloniemi - Syyspohja');

INSERT INTO paallystyskohde (urakka, sopimus, kohdenumero, nimi) VALUES ((SELECT id FROM urakka WHERE  nimi = 'Tienpäällystysurakka KAS ELY 1 2015'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Tienpäällystysurakka KAS ELY 1 2015') AND paasopimus IS null), '1507', 'Mt 387 Raippo - Koskenkylä');

-- Päällystysilmoitukset

INSERT INTO paallystysilmoitus (paallystyskohde, tila, aloituspvm, takuupvm, muutoshinta, ilmoitustiedot) VALUES ((SELECT id FROM paallystyskohde WHERE nimi ='Leppäjärven ramppi'), 'aloitettu'::paallystystila, '2005-11-14 00:00:00+02', '2005-12-20 00:00:00+02', 2000, '{"osoitteet":[{"tie":2846,"aosa":5,"aet":22,"losa":5,"let":9377,"ajorata":0,"suunta":0,"kaista":1,"paallystetyyppi":21,"raekoko":16,"massa":100,"rc%":0,"tyomenetelma":12,"leveys":6.5,"massamaara":1781,"edellinen-paallystetyyppi":12,"pinta-ala":15},{"tie":2846,"aosa":5,"aet":22,"losa":5,"let":9377,"ajorata":1,"suunta":0,"kaista":1,"paallystetyyppi":21,"raekoko":10,"massa":512,"rc%":0,"tyomenetelma":12,"leveys":4,"massamaara":1345,"edellinen-paallystetyyppi":11,"pinta-ala":9}],"kiviaines":[{"esiintyma":"KAMLeppäsenoja","km-arvo":"An14","muotoarvo":"Fi20","sideainetyyppi":"B650/900","pitoisuus":4.3,"lisaaineet":"Tartuke"}],"alustatoimet":[{"aosa":22,"aet":3,"losa":5,"let":4785,"kasittelymenetelma":13,"paksuus":30,"verkkotyyppi":1,"tekninen-toimenpide":2}],"tyot":[{"tyyppi":"ajoradan-paallyste","tyo":"AB 16/100 LTA","tilattu-maara":10000,"toteutunut-maara":10100,"yksikkohinta":20, "yksikko": "km"}]}');
INSERT INTO paallystysilmoitus (paallystyskohde, tila, aloituspvm, valmispvm_kohde, valmispvm_paallystys, takuupvm, muutoshinta, ilmoitustiedot) VALUES ((SELECT id FROM paallystyskohde WHERE nimi ='Tie 357'), 'valmis'::paallystystila, '2005-11-14 00:00:00+02', '2005-12-19 00:00:00+02', '2005-12-19 00:00:00+02', '2005-12-20 00:00:00+02', 2000, '{"osoitteet":[{"tie":2846,"aosa":5,"aet":22,"losa":5,"let":9377,"ajorata":0,"suunta":0,"kaista":1,"paallystetyyppi":21,"raekoko":16,"massa":100,"rc%":0,"tyomenetelma":12,"leveys":6.5,"massamaara":1781,"edellinen-paallystetyyppi":12,"pinta-ala":15},{"tie":2846,"aosa":5,"aet":22,"losa":5,"let":9377,"ajorata":1,"suunta":0,"kaista":1,"paallystetyyppi":21,"raekoko":10,"massa":512,"rc%":0,"tyomenetelma":12,"leveys":4,"massamaara":1345,"edellinen-paallystetyyppi":11,"pinta-ala":9}],"kiviaines":[{"esiintyma":"KAMLeppäsenoja","km-arvo":"An14","muotoarvo":"Fi20","sideainetyyppi":"B650/900","pitoisuus":4.3,"lisaaineet":"Tartuke"}],"alustatoimet":[{"aosa":22,"aet":3,"losa":5,"let":4785,"kasittelymenetelma":13,"paksuus":30,"verkkotyyppi":1,"tekninen-toimenpide":2}],"tyot":[{"tyyppi":"ajoradan-paallyste","tyo":"AB 16/100 LTA","tilattu-maara":10000,"toteutunut-maara":10100,"yksikkohinta":20, "yksikko": "km"}]}');
INSERT INTO paallystysilmoitus (paallystyskohde, tila, aloituspvm, valmispvm_kohde, valmispvm_paallystys, takuupvm, muutoshinta, ilmoitustiedot, paatos_tekninen_osa, paatos_taloudellinen_osa, perustelu_tekninen_osa, perustelu_taloudellinen_osa, kasittelyaika_tekninen_osa, kasittelyaika_taloudellinen_osa) VALUES ((SELECT id FROM paallystyskohde WHERE nimi ='Oulaisten ohitusramppi'), 'valmis'::paallystystila, '2005-11-14 00:00:00+02', '2005-12-19 00:00:00+02', '2005-12-19 00:00:00+02', '2005-12-20 00:00:00+02', 2000, '{"osoitteet":[{"tie":2846,"aosa":5,"aet":22,"losa":5,"let":9377,"ajorata":0,"suunta":0,"kaista":1,"paallystetyyppi":21,"raekoko":16,"massa":100,"rc%":0,"tyomenetelma":12,"leveys":6.5,"massamaara":1781,"edellinen-paallystetyyppi":12,"pinta-ala":15},{"tie":2846,"aosa":5,"aet":22,"losa":5,"let":9377,"ajorata":1,"suunta":0,"kaista":1,"paallystetyyppi":21,"raekoko":10,"massa":512,"rc%":0,"tyomenetelma":12,"leveys":4,"massamaara":1345,"edellinen-paallystetyyppi":11,"pinta-ala":9}],"kiviaines":[{"esiintyma":"KAMLeppäsenoja","km-arvo":"An14","muotoarvo":"Fi20","sideainetyyppi":"B650/900","pitoisuus":4.3,"lisaaineet":"Tartuke"}],"alustatoimet":[{"aosa":22,"aet":3,"losa":5,"let":4785,"kasittelymenetelma":13,"paksuus":30,"verkkotyyppi":1,"tekninen-toimenpide":2}],"tyot":[{"tyyppi":"ajoradan-paallyste","tyo":"AB 16/100 LTA","tilattu-maara":10000,"toteutunut-maara":10100,"yksikkohinta":20, "yksikko": "km"}]}', 'hylatty'::paallystysilmoituksen_paatostyyppi, 'hylatty'::paallystysilmoituksen_paatostyyppi, 'Ei tässä ole mitään järkeä', 'Ei tässä ole mitään järkeä', '2005-12-20 00:00:00+02', '2005-12-20 00:00:00+02');