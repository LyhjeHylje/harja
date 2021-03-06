(ns harja.palvelin.integraatiot.api.tyokalut.json-skeemat
  (:require [harja.tyokalut.json-validointi :refer [tee-validaattori]]))

(def +onnistunut-kirjaus+ "api/schemas/kirjaus-response.schema.json")
(def onnistunut-kirjaus (tee-validaattori "api/schemas/kirjaus-response.schema.json"))
(def +virhevastaus+ "api/schemas/virhe-response.schema.json")
(def virhevastaus (tee-validaattori "api/schemas/virhe-response.schema.json"))
(def +kirjausvastaus+ "api/schemas/kirjaus-response.schema.json")
(def kirjausvastaus (tee-validaattori "api/schemas/kirjaus-response.schema.json"))

(def +urakan-haku-vastaus+ "api/schemas/urakan-haku-response.schema.json")
(def urakan-haku-vastaus (tee-validaattori "api/schemas/urakan-haku-response.schema.json"))
(def +urakoiden-haku-vastaus+ "api/schemas/urakoiden-haku-response.schema.json")
(def urakoiden-haku-vastaus (tee-validaattori "api/schemas/urakoiden-haku-response.schema.json"))

(def +laatupoikkeaman-kirjaus+ "api/schemas/laatupoikkeaman-kirjaus-request.schema.json")
(def laatupoikkeaman-kirjaus (tee-validaattori "api/schemas/laatupoikkeaman-kirjaus-request.schema.json"))

(def +ilmoitustoimenpiteen-kirjaaminen+ "api/schemas/ilmoitustoimenpiteen-kirjaaminen-request.schema.json")
(def ilmoitustoimenpiteen-kirjaaminen (tee-validaattori "api/schemas/ilmoitustoimenpiteen-kirjaaminen-request.schema.json"))
(def +ilmoitusten-haku+ "api/schemas/ilmoitusten-haku-response.schema.json")
(def ilmoitusten-haku (tee-validaattori "api/schemas/ilmoitusten-haku-response.schema.json"))
(def +tietyoilmoituksen-kirjaus+ "api/schemas/tietyoilmoituksen-kirjaus-request.schema.json")
(def tietyoilmoituksen-kirjaus (tee-validaattori "api/schemas/tietyoilmoituksen-kirjaus-request.schema.json"))

(def +pistetoteuman-kirjaus+ "api/schemas/pistetoteuman-kirjaus-request.schema.json")
(def pistetoteuman-kirjaus (tee-validaattori "api/schemas/pistetoteuman-kirjaus-request.schema.json"))
(def +reittitoteuman-kirjaus+ "api/schemas/reittitoteuman-kirjaus-request.schema.json")
(def reittitoteuman-kirjaus (tee-validaattori "api/schemas/reittitoteuman-kirjaus-request.schema.json"))

(def +turvallisuuspoikkeamien-kirjaus+ "api/schemas/turvallisuuspoikkeamien-kirjaus-request.schema.json")
(def turvallisuuspoikkeamien-kirjaus (tee-validaattori "api/schemas/turvallisuuspoikkeamien-kirjaus-request.schema.json"))

(def +tielupien-haku+ "api/schemas/tielupien-haku-request.schema.json")
(def tielupien-haku (tee-validaattori "api/schemas/tielupien-haku-request.schema.json"))
(def +tielupien-haku-vastaus+ "api/schemas/tielupien-haku-response.schema.json")
(def tielupien-haku-vastaus (tee-validaattori "api/schemas/tielupien-haku-response.schema.json"))

(def +tietolajien-haku+ "api/schemas/tietolajien-haku-response.schema.json")
(def tietolajien-haku (tee-validaattori "api/schemas/tietolajien-haku-response.schema.json"))

(def +varusteiden-haku-vastaus+ "api/schemas/varusteiden-haku-response.schema.json")
(def varusteiden-haku-vastaus (tee-validaattori "api/schemas/varusteiden-haku-response.schema.json"))
(def +varusteen-lisays+ "api/schemas/varusteen-lisays-request.schema.json")
(def varusteen-lisays (tee-validaattori "api/schemas/varusteen-lisays-request.schema.json"))
(def +varusteen-paivitys+ "api/schemas/varusteen-paivitys-request.schema.json")
(def varusteen-paivitys (tee-validaattori "api/schemas/varusteen-paivitys-request.schema.json"))
(def +varusteen-poisto+ "api/schemas/varusteen-poisto-request.schema.json")
(def varusteen-poisto (tee-validaattori "api/schemas/varusteen-poisto-request.schema.json"))
(def +varustetoteuman-kirjaus+ "api/schemas/varustetoteuman-kirjaus-request.schema.json")
(def varustetoteuman-kirjaus (tee-validaattori "api/schemas/varustetoteuman-kirjaus-request.schema.json"))

(def +siltatarkastuksen-kirjaus+ "api/schemas/siltatarkastuksen-kirjaus-request.schema.json")
(def siltatarkastuksen-kirjaus (tee-validaattori "api/schemas/siltatarkastuksen-kirjaus-request.schema.json"))
(def +tiestotarkastuksen-kirjaus+ "api/schemas/tiestotarkastuksen-kirjaus-request.schema.json")
(def tiestotarkastuksen-kirjaus (tee-validaattori "api/schemas/tiestotarkastuksen-kirjaus-request.schema.json"))
(def +soratietarkastuksen-kirjaus+ "api/schemas/soratietarkastuksen-kirjaus-request.schema.json")
(def soratietarkastuksen-kirjaus (tee-validaattori "api/schemas/soratietarkastuksen-kirjaus-request.schema.json"))
(def +talvihoitotarkastuksen-kirjaus+ "api/schemas/talvihoitotarkastuksen-kirjaus-request.schema.json")
(def talvihoitotarkastuksen-kirjaus (tee-validaattori "api/schemas/talvihoitotarkastuksen-kirjaus-request.schema.json"))

(def +paivystajatietojen-kirjaus+ "api/schemas/paivystajatietojen-kirjaus-request.schema.json")
(def paivystajatietojen-kirjaus (tee-validaattori "api/schemas/paivystajatietojen-kirjaus-request.schema.json"))
(def +paivystajatietojen-haku-vastaus+ "api/schemas/paivystajatietojen-haku-response.schema.json")
(def paivystajatietojen-haku-vastaus (tee-validaattori "api/schemas/paivystajatietojen-haku-response.schema.json"))
(def +paivystajatietojen-poisto+ "api/schemas/paivystajatietojen-poisto-request.schema.json")
(def paivystyksen-poisto (tee-validaattori "api/schemas/paivystyksen-poisto-request.schema.json"))

(def +tyokoneenseuranta-kirjaus+ "api/schemas/tyokoneenseurannan-kirjaus-request.schema.json")
(def tyokoneenseuranta-kirjaus (tee-validaattori "api/schemas/tyokoneenseurannan-kirjaus-request.schema.json"))

(def +urakan-yllapitokohteiden-haku-vastaus+ "api/schemas/urakan-yllapitokohteet-response.schema.json")
(def urakan-yllapitokohteiden-haku-vastaus (tee-validaattori +urakan-yllapitokohteiden-haku-vastaus+))

(def +paallystysilmoituksen-kirjaus+ "api/schemas/paallystysilmoituksen-kirjaus-request.schema.json")
(def paallystysilmoituksen-kirjaus (tee-validaattori +paallystysilmoituksen-kirjaus+))

(def +yllapidon-aikataulun-kirjaus+ "api/schemas/aikataulun-kirjaus-request.schema.json")
(def yllapidon-aikataulun-kirjaus (tee-validaattori +yllapidon-aikataulun-kirjaus+))

(def +suljetun-tieosuuden-kirjaus+ "api/schemas/suljetun-tieosuuden-kirjaus-request.schema.json")
(def suljetun-tieosuuden-kirjaus (tee-validaattori +suljetun-tieosuuden-kirjaus+))
(def +suljetun-tieosuuden-poisto+ "api/schemas/suljetun-tieosuuden-poisto-request.schema.json")
(def suljetun-tieosuuden-poisto (tee-validaattori +suljetun-tieosuuden-poisto+))

