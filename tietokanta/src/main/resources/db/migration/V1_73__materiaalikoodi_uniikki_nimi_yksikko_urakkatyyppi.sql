ALTER TABLE materiaalikoodi ADD CONSTRAINT uniikki_nimi_yksikko_urakkatyyppi UNIQUE (nimi, yksikko, urakkatyyppi);