# matrikkel-token-test

Kommandolinjeverktøy for å teste token-henting mot Matrikkelens autentiseringstjenester. Brukes til å verifisere at token-flytene.

## Klienter

### `MatrikkelTokenClient`
Henter access token fra Keycloak via **Resource Owner Password Credentials Grant** (brukernavn/passord). Støtter automatisk fornyelse via refresh token.

### `MaskinportenTokenClient`
Henter access token fra Maskinporten via **JWT Bearer Grant**. Bygger og signerer et JWT-assertion med en RSA-privatnøkkel og sender det til Maskinportens token-endepunkt. Token-endepunkt og issuer hentes automatisk fra well-known-URL.

Begge klientene cacher access token og gjenbruker det frem til utløp.

## Forutsetninger

- Java 21
- Gradle (eller bruk den medfølgende `./gradlew`)

## Oppsett

Kopier eksempelfilen og fyll inn verdiene:

```bash
cp local.properties.example local.properties
```

For Maskinporten-klienten må du opprette en klient i [Samarbeidsportalen](https://samarbeidsportalen.digdir.no) og generere et nøkkelpar der. Derfra henter du:

- `MASKINPORTEN_CLIENT_ID` — klient-ID-en som tildeles klienten
- `MASKINPORTEN_KEY_ID` — nøkkel-ID-en (`kid`) for nøkkelparet du genererte
- `MASKINPORTEN_PRIVATE_KEY` — privatnøkkelen (PKCS#8, `-----BEGIN PRIVATE KEY-----`)

Privatnøkkelen må stå på én linje i `local.properties`. Hvis du har den i en fil, konverter slik:

```bash
awk 'NF {printf "%s\\n", $0}' private_key.pem
```

## Kjøring

```bash
./gradlew run
```

Maskinporten-testen kjøres automatisk hvis `MASKINPORTEN_CLIENT_ID` er satt i `local.properties`. Ellers hoppes den over.

## Testing

```bash
./gradlew test
```

Testene bruker MockWebServer og integrerer ikke mot noe eksternt. Hver test starter en lokal HTTP-server og verifiserer at klientene sender riktige requests og håndterer svar korrekt.

| Test | Beskrivelse |
|------|-------------|
| `getToken henter access token via password grant` | Verifiserer at Matrikkel-klienten sender korrekt grant type og brukernavn |
| `getToken returnerer cachet token ved andre kall` | Verifiserer at ingen ekstra HTTP-kall gjøres når token er gyldig |
| `getToken bruker refresh token når access token er utløpt` | Verifiserer at refresh token brukes fremfor nytt password-kall |
| `getToken henter access token via JWT bearer grant` | Verifiserer at Maskinporten-klienten returnerer riktig token |
| `getToken sender JWT bearer grant type i token-request` | Verifiserer at riktig grant type og assertion sendes i request |
| `getToken returnerer cachet token ved andre kall` | Verifiserer caching for Maskinporten-klienten |
