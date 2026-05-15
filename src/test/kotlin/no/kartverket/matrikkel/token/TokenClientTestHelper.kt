package no.kartverket.matrikkel.token

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.time.Instant
import java.util.Base64
import java.util.Date

/**
 * Delt testverktøy for begge token-klientene.
 */
object TokenClientTestHelper {

    /** RSA-nøkkelpar generert én gang for hele testkjøringen (unngår treg keygen per test). */
    val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    /** Privatnøkkel i PKCS#8 PEM-format — kan brukes direkte av MaskinportenTokenClient. */
    fun privateKeyPem(): String {
        val encoded = Base64.getEncoder().encodeToString(keyPair.private.encoded)
        return "-----BEGIN PRIVATE KEY-----\n${encoded.chunked(64).joinToString("\n")}\n-----END PRIVATE KEY-----"
    }

    /**
     * Lager et signert JWT med gitt levetid.
     * Positivt tall = utløper om N sekunder (gyldig).
     * Negativt tall = utløpte N sekunder siden (utløpt).
     */
    fun signedJwt(expiresInSeconds: Long = 3600): String {
        val signer = RSASSASigner(keyPair.private as RSAPrivateKey)
        val claims = JWTClaimsSet.Builder()
            .subject("test")
            .expirationTime(Date.from(Instant.now().plusSeconds(expiresInSeconds)))
            .build()
        return SignedJWT(JWSHeader(JWSAlgorithm.RS256), claims)
            .apply { sign(signer) }
            .serialize()
    }

    /** Minimalt gyldig OIDC-metadata-svar som OIDCProviderMetadata.parse() godtar. */
    fun metadataResponse(server: MockWebServer): MockResponse {
        val base = server.url("/")
        val json = """
            {
              "issuer": "$base",
              "authorization_endpoint": "${server.url("/auth")}",
              "token_endpoint": "${server.url("/token")}",
              "jwks_uri": "${server.url("/jwks")}",
              "response_types_supported": ["code"],
              "subject_types_supported": ["public"],
              "id_token_signing_alg_values_supported": ["RS256"]
            }
        """.trimIndent()
        return MockResponse()
            .setBody(json)
            .addHeader("Content-Type", "application/json")
    }

    /** Token-respons i OAuth2-format. */
    fun tokenResponse(accessToken: String, refreshToken: String? = null): MockResponse {
        val body = buildString {
            append("""{"access_token":"$accessToken","token_type":"Bearer","expires_in":3600""")
            if (refreshToken != null) append(""","refresh_token":"$refreshToken"""")
            append("}")
        }
        return MockResponse()
            .setBody(body)
            .addHeader("Content-Type", "application/json")
    }
}
