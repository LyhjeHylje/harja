CREATE TYPE ilmoituksenselite AS ENUM (
  'tyomaajarjestelyihinLiittyvaIlmoitus',
  'kuoppiaTiessa',
  'kelikysely',
  'soratienKuntoHuono',
  'saveaTiella',
  'liikennettaVaarantavaEsteTiella',
  'irtokiviaTiella',
  'kevyenLiikenteenVaylaanLiittyvaIlmoitus',
  'raivausJaKorjaustoita',
  'auraustarve',
  'kaivonKansiRikki',
  'kevyenLiikenteenVaylatOvatLiukkaita',
  'routaheitto',
  'avattavatPuomit',
  'tievalaistusVioittunutOnnettomuudessa',
  'muuKyselyTaiNeuvonta',
  'soratienTasaustarve',
  'tieTaiTienReunaOnPainunut',
  'siltaanLiittyvaIlmoitus',
  'polynsidontatarve',
  'liikennevalotEivatToimi',
  'kunnossapitoJaHoitotyo',
  'vettaTiella',
  'aurausvallitNakemaesteena',
  'ennakoivaVaroitus',
  'levahdysalueeseenLiittyvaIlmoitus',
  'sohjonPoisto',
  'liikennekeskusKuitannutLoppuneeksi',
  'muuToimenpidetarve',
  'hiekoitustarve',
  'tietOvatJaatymassa',
  'jaatavaaSadetta',
  'tienvarsilaitteisiinLiittyvaIlmoitus',
  'oljyaTiella',
  'sahkojohtoOnPudonnutTielle',
  'tieOnSortunut',
  'tievalaistusVioittunut',
  'testilahetys',
  'tievalaistuksenLamppujaPimeana',
  'virkaApupyynto',
  'tiemerkintoihinLiittyvaIlmoitus',
  'tulvavesiOnNoussutTielle',
  'niittotarve',
  'kuormaOnLevinnytTielle',
  'tieOnLiukas',
  'tiellaOnEste',
  'harjaustarve',
  'hoylaystarve',
  'tietyokysely',
  'paallystevaurio',
  'rikkoutunutAjoneuvoTiella',
  'mustaaJaataTiella',
  'kevyenLiikenteenVaylillaOnLunta',
  'hirviaitaVaurioitunut',
  'korvauskysely',
  'puitaOnKaatunutTielle',
  'rumpuunLiittyvaIlmoitus',
  'lasiaTiella',
  'liukkaudentorjuntatarve',
  'alikulkukaytavassaVetta',
  'tievalaistuksenLamppuPimeana',
  'kevyenLiikenteenVaylatOvatJaisiaJaLiukkaita',
  'kuoppa',
  'toimenpidekysely',
  'pysakkiinLiittyvaIlmoitus',
  'nakemaalueenRaivaustarve',
  'vesakonraivaustarve',
  'muuttuvatOpasteetEivatToimi',
  'tievalaistus',
  'vesiSyovyttanytTienReunaa',
  'raskasAjoneuvoJumissa',
  'myrskyvaurioita',
  'kaidevaurio',
  'liikennemerkkeihinLiittyvaIlmoitus'
);

CREATE TYPE kuittaustyyppi AS ENUM (
  'vastaanotto',
  'lopetus',
  'vastaus',
  'muutos',
  'aloitus'
);

CREATE TYPE ilmoitustyyppi AS ENUM (
  'kysely',
  'tiedoitus',
  'toimenpidepyynto'
);

CREATE TYPE ilmoittajatyyppi AS ENUM (
  'viranomainen',
  'muu',
  'asukas',
  'tienkayttaja'
);

CREATE TABLE ilmoitus (
  id SERIAL PRIMARY KEY,
  urakka INTEGER REFERENCES urakka (id),
  ilmoitusid INTEGER NOT NULL,

  ilmoitettu TIMESTAMP NOT NULL,
  valitetty TIMESTAMP,

  yhteydenottopyynto BOOLEAN,
  vapaateksti TEXT,
  sijainti POINT,
  tr_numero INTEGER,
  tr_alkuosa INTEGER,
  tr_loppuosa INTEGER,
  tr_loppuetaisyys INTEGER,

  ilmoitustyyppi ilmoitustyyppi,
  selitteet ilmoituksenselite[],
  urakkatyyppi urakkatyyppi,

  ilmoittaja_etunimi TEXT,
  ilmoittaja_sukunimi TEXT,
  ilmoittaja_tyopuhelin TEXT,
  ilmoittaja_matkapuhelin TEXT,
  ilmoittaja_sahkoposti TEXT,
  ilmoittaja_tyyppi ilmoittajatyyppi,

  lahettaja_etunimi TEXT,
  lahettaja_sukunimi TEXT,
  lahettaja_puhelinnumero TEXT,
  lahettaja_sahkoposti TEXT
);

CREATE TABLE kuittaus (
  id SERIAL PRIMARY KEY,
  ilmoitus INTEGER REFERENCES ilmoitus (id),
  ilmoitusid INTEGER NOT NULL,
  kuitattu TIMESTAMP NOT NULL,
  vapaateksti TEXT,
  kuittaustyyppi kuittaustyyppi NOT NULL,

  kuittaaja_henkilo_etunimi TEXT,
  kuittaaja_henkilo_sukunimi TEXT,
  kuittaaja_henkilo_matkapuhelin TEXT,
  kuittaaja_henkilo_tyopuhelin TEXT,
  kuittaaja_henkilo_sahkoposti TEXT,
  kuittaaja_organisaatio_nimi TEXT,
  kuittaaja_organisaatio_ytunnus TEXT,

  kasittelija_henkilo_etunimi TEXT,
  kasittelija_henkilo_sukunimi TEXT,
  kasittelija_henkilo_matkapuhelin TEXT,
  kasittelija_henkilo_tyopuhelin TEXT,
  kasittelija_henkilo_sahkoposti TEXT,
  kasittelija_organisaatio_nimi TEXT,
  kasittelija_organisaatio_ytunnus TEXT
);