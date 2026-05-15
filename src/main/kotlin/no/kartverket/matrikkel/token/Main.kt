package no.kartverket.matrikkel.token

fun main() {
    val username = requireEnv("MATRIKKEL_USERNAME")
    val password = requireEnv("MATRIKKEL_PASSWORD")
    val wellKnownUrl = requireEnv("MATRIKKEL_WELL_KNOWN_URL")

    println("=== Matrikkel token (password grant) ===")
    println("Well-known:  $wellKnownUrl")
    println("Bruker:      $username")
    println()

    val matrikkelClient = MatrikkelTokenClient(username, password, wellKnownUrl)

    println("--- Henter token (password grant) ---")
    val token1 = matrikkelClient.getToken()
    println("OK: ${token1.take(40)}...")
    println()

    println("--- Henter token igjen (skal bruke cachet access token) ---")
    val token2 = matrikkelClient.getToken()
    check(token1 == token2) { "Forventet samme token, men fikk ulikt" }
    println("OK: samme token returnert fra cache")
    println()

    // Maskinporten-test er valgfri — kjøres bare hvis config er satt i local.properties
    val maskinportenClientId = optionalEnv("MASKINPORTEN_CLIENT_ID")
    if (maskinportenClientId != null) {
        testMaskinporten(maskinportenClientId)
    } else {
        println("(Maskinporten-test hoppet over — MASKINPORTEN_CLIENT_ID er ikke satt)")
    }

    println()
    println("Alle tester passerte.")
}

private fun testMaskinporten(clientId: String) {
    val scope = requireEnv("MASKINPORTEN_SCOPE")
    val privateKeyPem = requireEnv("MASKINPORTEN_PRIVATE_KEY").replace("\\n", "\n")
    val keyId = requireEnv("MASKINPORTEN_KEY_ID")
    val wellKnownUrl = requireEnv("MASKINPORTEN_WELL_KNOWN_URL")

    println("=== Maskinporten token (JWT bearer grant) ===")
    println("Well-known:  $wellKnownUrl")
    println("Client ID:   $clientId")
    println("Scope:       $scope")
    println("Key ID:      $keyId")
    println()

    val maskinportenClient = MaskinportenTokenClient(
        clientId = clientId,
        scope = scope,
        privateKeyPem = privateKeyPem,
        keyId = keyId,
        wellKnownUrl = wellKnownUrl,
    )

    println("--- Henter Maskinporten-token ---")
    val mpToken1 = maskinportenClient.getToken()
    println("OK: ${mpToken1.take(40)}...")
    println()

    println("--- Henter Maskinporten-token igjen (skal bruke cache) ---")
    val mpToken2 = maskinportenClient.getToken()
    check(mpToken1 == mpToken2) { "Forventet cachet token, men fikk ulikt" }
    println("OK: samme token returnert fra cache")
    println()
}

private fun requireEnv(name: String): String =
    System.getProperty(name) ?: System.getenv(name)
    ?: error("$name er ikke satt (local.properties eller miljøvariabel)")

private fun optionalEnv(name: String): String? =
    System.getProperty(name) ?: System.getenv(name)
