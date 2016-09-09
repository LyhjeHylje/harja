----------------------------
-- Muhoksen päällystysurakka
----------------------------

-- Päällystyskohteet

INSERT INTO yllapitokohde
(urakka, sopimus, kohdenumero, nimi, sopimuksen_mukaiset_tyot, arvonvahennykset, bitumi_indeksi, kaasuindeksi,
 aikataulu_paallystys_alku, aikataulu_paallystys_loppu, aikataulu_tiemerkinta_alku, aikataulu_tiemerkinta_loppu,
 aikataulu_kohde_valmis, aikataulu_muokkaaja, aikataulu_muokattu, valmis_tiemerkintaan, yllapitokohdetyyppi, yllapitokohdetyotyyppi, yhaid,
 tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, tr_ajorata, tr_kaista)
VALUES
  ((SELECT id FROM urakka WHERE  nimi = 'Muhoksen päällystysurakka'),
    (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Muhoksen päällystysurakka') AND paasopimus IS null),
    'L03', 'Leppäjärven ramppi', 400, 100, 4543.95, 0,
    '2016-05-19 06:00:00+02', '2016-05-21 16:00:00+02', null, null,
                                                              null, (SELECT id FROM kayttaja where kayttajanimi = 'jvh'), NOW(), '2016-05-21 16:00:00+02', 'paallyste' :: yllapitokohdetyyppi, 'paallystys'::yllapitokohdetyotyyppi, 1233534,
                                                              18652, 1, 5190, 1, 3312, 1, 1),
  ((SELECT id FROM urakka WHERE  nimi = 'Muhoksen päällystysurakka'),
    (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Muhoksen päällystysurakka') AND paasopimus IS null),
    308, 'Tie 833', 9000, 200, 565, 100,
    '2016-05-21 06:00:00+02', null, null, null,
                                          null, (SELECT id FROM kayttaja where kayttajanimi = 'jvh'), NOW(), null, 'paallyste' :: yllapitokohdetyyppi,'paallystys'::yllapitokohdetyotyyppi, 54523243,
                                          833, 2, 334, 4, 3042, 1 ,1),
  ((SELECT id FROM urakka WHERE  nimi = 'Muhoksen päällystysurakka'),
    (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Muhoksen päällystysurakka') AND paasopimus IS null),
    'L010', 'Nakkilan ramppi', 500, 3457, 5, 6,
    '2016-05-26 06:00:00+02', null, null, null,
                                          null, (SELECT id FROM kayttaja where kayttajanimi = 'jvh'), NOW(), null, 'paallyste' :: yllapitokohdetyyppi,'paallystys'::yllapitokohdetyotyyppi, 265257,
                                          8484, 1, 2728, 1, 5254, 1 ,1 ),
  ((SELECT id FROM urakka WHERE  nimi = 'Muhoksen päällystysurakka'),
    (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Muhoksen päällystysurakka') AND paasopimus IS null),
    310, 'Oulaisten ohitusramppi', 500, 3457, 5, 6,
    '2016-06-02 06:00:00+02', null, null, null,
                                          null, (SELECT id FROM kayttaja where kayttajanimi = 'jvh'), NOW(), null, 'paallyste' :: yllapitokohdetyyppi,'paallystys'::yllapitokohdetyotyyppi, 456896958,
                                          19521, 10, 5, 10, 15, 1, 1),
  ((SELECT id FROM urakka WHERE  nimi = 'Muhoksen päällystysurakka'),
    (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Muhoksen päällystysurakka') AND paasopimus IS null),
    666, 'Kuusamontien testi', 500, 3457, 5, 6,
    '2016-06-02 06:00:00+02', null, null, null,
                                          null, (SELECT id FROM kayttaja where kayttajanimi = 'jvh'), NOW(), null, 'paallyste' :: yllapitokohdetyyppi,'paallystys'::yllapitokohdetyotyyppi, 456896959,
                                          20, 1, 1, 5, 15, 1 ,1);

INSERT INTO yllapitokohdeosa (yllapitokohde, nimi, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, tr_ajorata, tr_kaista,  sijainti) VALUES ((SELECT id FROM yllapitokohde WHERE nimi ='Oulaisten ohitusramppi'), 'Laivaniemi 1', 19521, 10, 5, 10, 15, 1, 1, ST_GeomFromText('MULTILINESTRING((426888 7212758,427081 7212739),(434777 7215499,436899 7217174,438212 7219910,438676 7220554,440102 7221432,441584 7222729,442255 7223162,443128 7223398,443750 7223713,448682 7225293,451886 7226708,456379 7228018,459945 7229222,461039 7229509))'));
INSERT INTO yllapitokohdeosa (yllapitokohde, nimi, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, tr_ajorata, tr_kaista, sijainti) VALUES ((SELECT id FROM yllapitokohde WHERE nimi ='Nakkilan ramppi'), 'Laivaniemi 3', 8484, 1, 2728, 1, 5254, 1, 1, ST_GeomFromText('LINESTRING(420852.939900323 7223514.49386093,420804.9806 7223518.3268,420747.5075 7223522.9287,420698.0565 7223526.6629,420610.8429 7223534.1198,420565.0569 7223537.7396,420471.1844 7223545.1734,420401.3421 7223550.7871,420348.0056 7223555.8095,420343.7581 7223556.2037,420339.2283 7223556.6623,420290.4384 7223562.4226,420248.5176 7223568.9529,420209.5639 7223575.7583,420178.7836 7223582.3631,420119.6745 7223597.171,420080.1538 7223608.031,420042.4889 7223619.6825,420001.3513 7223634.176,419981.7348 7223641.1357,419964.0443 7223647.4266,419923.946 7223662.1923,419913.9275 7223666.0331,419879.3142 7223679.3057,419837.1784 7223694.9873,419777.7328 7223717.269,419733.8377 7223733.6355,419691.4888 7223749.8115,419648.1416 7223766.3138,419630.5679 7223772.7714,419600.5249 7223783.8113,419523.7039 7223812.5137,419490.4187 7223824.7077,419457.258 7223837.1418,419427.4026 7223848.3835,419398.1679 7223860.1506,419395.6671 7223861.0975,419363.4962 7223872.571,419331.8643 7223883.0022,419308.1498 7223889.947,419283.4419 7223897.3867,419264.3161 7223902.599,419246.7864 7223906.2921,419227.5803 7223910.2734,419196.3605 7223916.4523,419169.8045 7223920.2043,419138.5455 7223924.6466,419125.9899 7223926.0176,419117.2918 7223926.8525,419109.7079 7223927.5642,419071.828 7223931.0852,419017.9526 7223935.6787,419015.8562 7223935.8485,418945.8311 7223942.1935,418847.1024 7223950.5784,418757.0754 7223958.5226,418631.6055 7223969.5737,418630.655 7223969.6559,418556.1714 7223975.8337,418489.9334 7223982.1787,418432.0987 7223986.9855,418391.036526719 7223990.45389087)'));
INSERT INTO yllapitokohdeosa (yllapitokohde, nimi, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, tr_ajorata, tr_kaista, sijainti) VALUES ((SELECT id FROM yllapitokohde WHERE nimi ='Tie 833'), 'Laivaniemi 4',833, 2, 334, 4, 3042, 1, 1, ST_GeomFromText('LINESTRING(446772.5999 7213579.3073,446783.6618 7213578.9601,446823.0461 7213576.2378,446867.5099 7213571.5192,446896.6571 7213567.579,446936.5947 7213561.3351,446969.4189 7213555.5362,447051.7453 7213541.0456,447136.0508 7213525.9238,447143.1177 7213524.6582,447224.465 7213510.0587,447330.0492 7213491.4445,447348.7003 7213488.1058,447458.1758 7213468.9223,447521.8446 7213458.5304,447564.6772 7213449.3475,447612.1718 7213440.812,447617.3686 7213439.5803,447617.7694 7213439.4803,447618.5091 7213439.3546,447628.5276 7213437.5715,447658.3967 7213432.8041,447732.3847 7213419.675,447799.1396 7213407.8138,447847.0207 7213399.2009,447852.3754 7213399.2938,447939.3817 7213383.3626,448016.3112 7213370.8243,448016.8937 7213370.7207,448101.1884 7213355.4029,448133.862 7213349.7874,448196.2431 7213339.0959,448258.9656 7213328.0257,448302.2085 7213320.4162,448362.94 7213308.3365,448397.9517 7213300.8712,448432.3672 7213291.3988,448464.7876 7213281.5495,448508.7893 7213266.1936,448513.8902 7213265.3205,448530.8666 7213259.0284,448552.2799 7213249.091,448557.6328 7213246.7933,448617.1922 7213222.0323,448738.4944 7213170.6827,448782.9117 7213151.7922,448834.5544 7213130.6105,448839.064 7213128.8322,448884.4574 7213112.0005,448937.9451 7213093.8158,448949.4305 7213089.975,448973.3118 7213081.9784,449019.4342 7213066.108,449062.1536 7213052.0534,449084.5567 7213043.9526,449116.9117 7213030.468,449150.6905 7213014.8292,449173.6868 7213003.5053,449193.7095 7212992.3624,449214.0943 7212981.606,449235.3426 7212970.9848,449246.6386 7212965.7284,449289.8886 7212947.4817,449333.8033 7212929.2433,449342.9672 7212925.3501,449374.6229 7212911.9364,449429.3815 7212887.7781,449505.541 7212853.9522,449581.651 7212818.9703,449655.745 7212786.08,449661.9894 7212783.1719,449684.462135075 7212772.97370103)'));
INSERT INTO yllapitokohdeosa (yllapitokohde, nimi, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, tr_ajorata, tr_kaista, sijainti) VALUES ((SELECT id FROM yllapitokohde WHERE nimi ='Leppäjärven ramppi'), 'Laivaniemi 5', 18652, 1, 5190, 1, 3312, 1, 1, ST_GeomFromText('LINESTRING(459799.865516997 7192178.79677912,459791.1678 7192177.6569,459772.7799 7192174.7274,459754.1461 7192169.465,459753.915 7192169.3935,459742.9931 7192165.9524,459706.5586 7192154.1466,459700.7257 7192151.9455,459612.3698 7192120.1176,459606.6215 7192117.7473,459538.411 7192092.5075,459533.3541 7192090.0908,459495.1705 7192074.8921,459454.3402 7192059.7673,459449.0367 7192059.1473,459422.015 7192051.5718,459410.0235 7192048.4642,459399.8466 7192046.098,459374.4817 7192041.7141,459368.9901 7192041.2841,459364.1482 7192040.7142,459329.2312 7192036.684,459302.4114 7192036.45,459252.9443 7192038.2045,459247.4682 7192038.354,459214.9203 7192040.1597,459161.0288 7192044.5365,459092.6521 7192048.9615,459086.7853 7192049.0228,459001.4519 7192054.6902,458875.0446 7192060.7172,458867.8794 7192061.1704,458778.0406 7192066.9992,458776.8257 7192067.0724,458697.7361 7192071.747,458695.4164 7192071.8726,458617.3256 7192076.3113,458616.6341 7192076.3399,458575.4155 7192078.9264,458566.5018 7192079.3856,458517.1753 7192082.0847,458515.9049 7192082.196,458459.0899 7192087.2047,458404.0424 7192089.5553,458401.2296 7192089.6691,458397.1696 7192089.8561,458379.4399 7192090.9615,458360.1944 7192092.2747,458338.2594 7192091.7011,458320.4831 7192089.444,458285.587 7192083.5575,458257.0347 7192077.0969,458229.5259 7192066.213,458189.016 7192048.335,458134.0454 7192020.8684,458133.1973 7192020.4694,458102.9191 7192006.0747,458067.4411 7191988.8196,457977.63022087 7191945.16875886)'));

-- Päällystysilmoitukset

INSERT INTO paallystysilmoitus (paallystyskohde, tila, aloituspvm, takuupvm, muutoshinta, ilmoitustiedot) VALUES ((SELECT id FROM yllapitokohde WHERE nimi ='Leppäjärven ramppi'), 'aloitettu'::paallystystila, '2005-11-14 00:00:00+02', '2005-12-20 00:00:00+02', 2000, '{"osoitteet":[{"rc%": 12, "leveys": 12, "km-arvo": "12", "raekoko": 1, "esiintyma": "12", "muotoarvo": "12", "pinta-ala": 12, "pitoisuus": 12, "kuulamylly": 2, "lisaaineet": "12", "kohdeosa-id": 4, "massamenekki": 1, "tyomenetelma": 21, "sideainetyyppi": 2, "paallystetyyppi": 2, "tr-alkuetaisyys": 5190, "tr-loppuetaisyys": 3312, "kokonaismassamaara": 12, "edellinen-paallystetyyppi": 2}],"alustatoimet":[{"tr-alkuosa":22,"tr-alkuetaisyys":3,"tr-loppuosa":5,"tr-loppuetaisyys":4785,"kasittelymenetelma":13,"paksuus":30,"verkkotyyppi":1, "verkon-tarkoitus": 1, "verkon-sijainti": 1, "tekninen-toimenpide":2}],"tyot":[{"tyyppi":"ajoradan-paallyste","tyo":"AB 16/100 LTA","tilattu-maara":10000,"toteutunut-maara":10100,"yksikkohinta":20, "yksikko": "km"}]}');
INSERT INTO paallystysilmoitus (paallystyskohde, tila, aloituspvm, valmispvm_kohde, valmispvm_paallystys, takuupvm, muutoshinta, ilmoitustiedot) VALUES ((SELECT id FROM yllapitokohde WHERE nimi ='Tie 833'), 'valmis'::paallystystila, '2005-11-14 00:00:00+02', '2005-12-19 00:00:00+02', '2005-12-19 00:00:00+02', '2005-12-20 00:00:00+02', 2000, '{"osoitteet":[{"rc%": 12, "leveys": 12, "km-arvo": "12", "raekoko": 1, "esiintyma": "12", "muotoarvo": "12", "pinta-ala": 12, "pitoisuus": 12, "kuulamylly": 2, "lisaaineet": "12", "kohdeosa-id": 3, "massamenekki": 1, "tyomenetelma": 21, "sideainetyyppi": 2, "paallystetyyppi": 2, "tr-alkuetaisyys": 5190, "tr-loppuetaisyys": 3312, "kokonaismassamaara": 12, "edellinen-paallystetyyppi": 2}],"alustatoimet":[{"tr-alkuosa":22,"tr-alkuetaisyys":3,"tr-loppuosa":5,"tr-loppuetaisyys":4785,"kasittelymenetelma":13,"paksuus":30,"verkkotyyppi":1, "verkon-tarkoitus": 1, "verkon-sijainti": 1, "tekninen-toimenpide":2}],"tyot":[{"tyyppi":"ajoradan-paallyste","tyo":"AB 16/100 LTA","tilattu-maara":10000,"toteutunut-maara":10100,"yksikkohinta":20, "yksikko": "km"}]}');
INSERT INTO paallystysilmoitus (paallystyskohde, tila, aloituspvm, valmispvm_kohde, valmispvm_paallystys, takuupvm, muutoshinta, ilmoitustiedot, paatos_tekninen_osa, paatos_taloudellinen_osa, perustelu_tekninen_osa, perustelu_taloudellinen_osa, kasittelyaika_tekninen_osa, kasittelyaika_taloudellinen_osa) VALUES ((SELECT id FROM yllapitokohde WHERE nimi ='Oulaisten ohitusramppi'), 'valmis'::paallystystila, '2005-11-14 00:00:00+02', '2005-12-19 00:00:00+02', '2005-12-19 00:00:00+02', '2005-12-20 00:00:00+02', 2000, '{"osoitteet":[{"rc%": 12, "leveys": 12, "km-arvo": "12", "raekoko": 1, "esiintyma": "12", "muotoarvo": "12", "pinta-ala": 12, "pitoisuus": 12, "kuulamylly": 2, "lisaaineet": "12", "kohdeosa-id": 1, "massamenekki": 1, "tyomenetelma": 21, "sideainetyyppi": 2, "paallystetyyppi": 2, "tr-alkuetaisyys": 5190, "tr-loppuetaisyys": 3312, "kokonaismassamaara": 12, "edellinen-paallystetyyppi": 2}],"alustatoimet":[{"tr-alkuosa":22,"tr-alkuetaisyys":3,"tr-loppuosa":5,"tr-loppuetaisyys":4785,"kasittelymenetelma":13,"paksuus":30,"verkkotyyppi":1, "verkon-tarkoitus": 1, "verkon-sijainti": 1, "tekninen-toimenpide":2}],"tyot":[{"tyyppi":"ajoradan-paallyste","tyo":"AB 16/100 LTA","tilattu-maara":10000,"toteutunut-maara":10100,"yksikkohinta":20, "yksikko": "km"}]}', 'hylatty'::paallystysilmoituksen_paatostyyppi, 'hylatty'::paallystysilmoituksen_paatostyyppi, 'Ei tässä ole mitään järkeä', 'Ei tässä ole mitään järkeä', '2005-12-20 00:00:00+02', '2005-12-20 00:00:00+02');

----------------------------
-- Tienpäällystysurakka
----------------------------

INSERT INTO yllapitokohde (urakka, sopimus, kohdenumero, nimi, yllapitokohdetyyppi, yllapitokohdetyotyyppi,
                           tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys)
VALUES
  ((SELECT id FROM urakka WHERE  nimi = 'Tienpäällystysurakka KAS ELY 1 2015'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Tienpäällystysurakka KAS ELY 1 2015') AND paasopimus IS null), '1501', 'Vt13 Hartikkala - Pelkola', 'paallyste' :: yllapitokohdetyyppi,'paallystys'::yllapitokohdetyotyyppi,
                                                                                13, 239, 0, 239, 4894);
INSERT INTO yllapitokohdeosa (yllapitokohde, nimi, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, tr_ajorata, tr_kaista, sijainti) VALUES ((SELECT id FROM yllapitokohde WHERE nimi ='Vt13 Hartikkala - Pelkola'), 'Vt13 Hartikkala - Pelkola', 13, 239, 0, 239, 222, 1, 1,  'MULTILINESTRING((569679.576280243 6770940.38019019,569685.927911525 6770936.54598464,569694.917866912 6770931.11985125,569702.825096966 6770926.21423598,569717.059540404 6770917.1194623,569768.068529403 6770886.26298144,569772.905655448 6770883.31437137,569800.582449535 6770866.39038579,569825.147450518 6770851.51214363,569826.659573521 6770850.53304547,569830.600979599 6770848.01502418,569849.985812847 6770836.16460137,569852.934422925 6770834.4297395,569853.208975388 6770834.26715203,569869.210775049 6770824.97723093),(570700.430827977 6770091.60690774,570705.758098659 6770086.85732879,570844.195073798 6769973.54994409,570872.894391503 6769951.86928975))');
INSERT INTO yllapitokohdeosa (yllapitokohde, nimi, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, tr_ajorata, tr_kaista, sijainti) VALUES ((SELECT id FROM yllapitokohde WHERE nimi ='Vt13 Hartikkala - Pelkola'), 'Vt13 Hartikkala - Pelkola', 13, 239, 222, 239, 820, 1, 1,  'MULTILINESTRING((569869.210775049 6770824.97723093,569872.124508548 6770823.28564906,569918.85619648 6770796.53077951,569962.043715846 6770770.06058692,569985.755877074 6770755.92441063,570007.509246432 6770743.31881966,570092.85754233 6770692.64036563,570152.581934252 6770655.3316041,570259.021934027 6770569.17811313,570337.970358406 6770487.07441987,570348.766508366 6770474.12501744),(570872.894391503 6769951.86928975,570913.557984301 6769921.15032602,571038.051744102 6769821.61642248,571042.671491299 6769817.89894639,571157.696466689 6769725.89230659,571340.874536167 6769579.60903354))');
INSERT INTO yllapitokohdeosa (yllapitokohde, nimi, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, tr_ajorata, tr_kaista, sijainti) VALUES ((SELECT id FROM yllapitokohde WHERE nimi ='Vt13 Hartikkala - Pelkola'), 'Vt13 Hartikkala - Pelkola', 13, 239, 820, 239, 870, 1, 1, 'MULTILINESTRING((570348.766508366 6770474.12501744,570380.784445928 6770435.72121979),(571340.874536167 6769579.60903354,571371.85691699 6769554.86696651,571373.690046018 6769553.31672776,571379.770800303 6769548.19579434))');
INSERT INTO yllapitokohdeosa (yllapitokohde, nimi, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, tr_ajorata, tr_kaista, sijainti) VALUES ((SELECT id FROM yllapitokohde WHERE nimi ='Vt13 Hartikkala - Pelkola'), 'Vt13 Hartikkala - Pelkola', 13, 239, 870, 239, 1275,1, 1,  'MULTILINESTRING((570380.784445928 6770435.72121979,570380.996481422 6770435.46689459,570381.23768261 6770435.1726887,570498.097573703 6770293.76209003,570611.87068515 6770173.67844084,570652.178907897 6770135.64614894),(571379.770800303 6769548.19579434,571411.39783173 6769521.56095359,571569.835447595 6769389.46608079,571570.855639286 6769388.70257481,571697.443978293 6769304.28692351,571698.415334188 6769303.70923177,571700.883529032 6769302.3581588))');
INSERT INTO yllapitokohdeosa (yllapitokohde, nimi, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, tr_ajorata, tr_kaista, sijainti) VALUES ((SELECT id FROM yllapitokohde WHERE nimi ='Vt13 Hartikkala - Pelkola'), 'Vt13 Hartikkala - Pelkola', 13, 239, 870, 239, 1275, 1, 1,  'MULTILINESTRING((570380.784445928 6770435.72121979,570380.996481422 6770435.46689459,570381.23768261 6770435.1726887,570498.097573703 6770293.76209003,570611.87068515 6770173.67844084,570652.178907897 6770135.64614894),(571379.770800303 6769548.19579434,571411.39783173 6769521.56095359,571569.835447595 6769389.46608079,571570.855639286 6769388.70257481,571697.443978293 6769304.28692351,571698.415334188 6769303.70923177,571700.883529032 6769302.3581588))');

INSERT INTO yllapitokohde (urakka, sopimus, kohdenumero, nimi, yllapitokohdetyyppi, yllapitokohdetyotyyppi,
                           tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys)
VALUES
  ((SELECT id FROM urakka WHERE  nimi = 'Tienpäällystysurakka KAS ELY 1 2015'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Tienpäällystysurakka KAS ELY 1 2015') AND paasopimus IS null), '1502', 'Vt 13 Kähärilä - Liikka', 'paallyste' :: yllapitokohdetyyppi,'paallystys'::yllapitokohdetyotyyppi,
                                                                                13, 241, 0, 241, 4723);
INSERT INTO yllapitokohdeosa (yllapitokohde, nimi, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, tr_ajorata, tr_kaista, sijainti) VALUES ((SELECT id FROM yllapitokohde WHERE nimi ='Vt 13 Kähärilä - Liikka'), 'Vt 13 Kähärilä - Liikka', 13, 241, 0, 241, 30, 1, 1, 'MULTILINESTRING((578249.322868685 6763497.87157121,578262.945673555 6763491.3180456,578275.84176723 6763483.88913412),(581383.200687944 6760054.36044461,581382.427437082 6760078.6053179,581386.126450856 6760082.99815731,581391.183933542 6760078.6196113,581393.197140761 6760055.39623812),(581397.589195136 6759849.00348945,581396.515373133 6759862.00903338,581395.753058267 6759871.11750491,581399.465769886 6759877.98846319,581404.941334631 6759871.56834268,581407.346106159 6759850.14085703))');

INSERT INTO yllapitokohde (urakka, sopimus, kohdenumero, nimi, yllapitokohdetyyppi, yllapitokohdetyotyyppi,
                           tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys) VALUES
  ((SELECT id FROM urakka WHERE  nimi = 'Tienpäällystysurakka KAS ELY 1 2015'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Tienpäällystysurakka KAS ELY 1 2015') AND paasopimus IS null), '1503', 'Mt 387 Mattila - Hanhi-Kemppi', 'paallyste' :: yllapitokohdetyyppi,'paallystys'::yllapitokohdetyotyyppi,
                                                                                387, 1, 2413, 2, 1988);
INSERT INTO yllapitokohde (urakka, sopimus, kohdenumero, nimi, yllapitokohdetyyppi, yllapitokohdetyotyyppi,
                           tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys)
VALUES
  ((SELECT id FROM urakka WHERE  nimi = 'Tienpäällystysurakka KAS ELY 1 2015'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Tienpäällystysurakka KAS ELY 1 2015') AND paasopimus IS null), '1504', 'Mt 408 Pallo - Kivisalmi', 'paallyste' :: yllapitokohdetyyppi,'paallystys'::yllapitokohdetyotyyppi,
                                                                                408, 1, 1989, 2, 127);
INSERT INTO yllapitokohde (urakka, sopimus, kohdenumero, nimi, yllapitokohdetyyppi, yllapitokohdetyotyyppi,
                           tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys)
VALUES
  ((SELECT id FROM urakka WHERE  nimi = 'Tienpäällystysurakka KAS ELY 1 2015'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Tienpäällystysurakka KAS ELY 1 2015') AND paasopimus IS null), '1505', 'Kt 62 Sotkulampi - Rajapatsas', 'paallyste' :: yllapitokohdetyyppi,'paallystys'::yllapitokohdetyotyyppi,
                                                                                62, 24, 0, 24, 4240);
INSERT INTO yllapitokohde (urakka, sopimus, kohdenumero, nimi, yllapitokohdetyyppi, yllapitokohdetyotyyppi,
                           tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys)
VALUES
  ((SELECT id FROM urakka WHERE  nimi = 'Tienpäällystysurakka KAS ELY 1 2015'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Tienpäällystysurakka KAS ELY 1 2015') AND paasopimus IS null), '1506', 'Kt 62 Haloniemi - Syyspohja', 'paallyste' :: yllapitokohdetyyppi,'paallystys'::yllapitokohdetyotyyppi,
                                                                                62, 19, 7800, 22, 2800);
INSERT INTO yllapitokohde (urakka, sopimus, kohdenumero, nimi, yllapitokohdetyyppi, yllapitokohdetyotyyppi,
                           tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys)
VALUES
  ((SELECT id FROM urakka WHERE  nimi = 'Tienpäällystysurakka KAS ELY 1 2015'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Tienpäällystysurakka KAS ELY 1 2015') AND paasopimus IS null), '1507', 'Mt 387 Raippo - Koskenkylä', 'paallyste' :: yllapitokohdetyyppi,'paallystys'::yllapitokohdetyotyyppi,
                                                                                387, 3, 5955, 7, 55);
