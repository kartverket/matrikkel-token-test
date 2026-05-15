package no.kartverket.matrikkel.token

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.AuthorizationGrant
import com.nimbusds.oauth2.sdk.JWTBearerGrant
import com.nimbusds.oauth2.sdk.Scope
import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.oauth2.sdk.TokenResponse
import com.nimbusds.oauth2.sdk.id.ClientID
import com.nimbusds.oauth2.sdk.`as`.AuthorizationServerMetadata
import java.net.URI
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import java.time.Instant
import java.util.Date
import java.util.UUID

class MaskinportenTokenClient(
    private val clientId: String,
    private val scope: String,
    private val privateKeyPem: String,
    private val keyId: String,
    private val wellKnownUrl: String,
) {
    private val clientID = ClientID(clientId)

    private val providerMetadata: AuthorizationServerMetadata by lazy {
        AuthorizationServerMetadata.parse(URI(wellKnownUrl).toURL().readText())
    }

    private val privateKey: RSAPrivateKey by lazy {
        val der = privateKeyPem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
            .let { Base64.getDecoder().decode(it) }
        KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(der)) as RSAPrivateKey
    }

    private var accessToken: String? = null

    fun getToken(): String {
        val current = accessToken
        return when {
            current != null && !isExpired(current) -> current
            else -> fetchWithJwtAssertion()
        }
    }

    private fun fetchWithJwtAssertion(): String =
        sendTokenRequest(JWTBearerGrant(buildAssertion()))

    private fun sendTokenRequest(grant: AuthorizationGrant): String {
        val request = TokenRequest(providerMetadata.tokenEndpointURI, clientID, grant, null as Scope?)
        val tokens = TokenResponse.parse(request.toHTTPRequest().send())
            .toSuccessResponse().tokens
        accessToken = tokens.accessToken.value
        return tokens.accessToken.value
    }

    private fun buildAssertion(): SignedJWT {
        val now = Instant.now()
        val claims = JWTClaimsSet.Builder()
            .audience(providerMetadata.issuer.value)
            .issuer(clientId)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(119)))
            .jwtID(UUID.randomUUID().toString())
            .claim("scope", scope)
            .build()

        val header = JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(keyId)
            .build()

        return SignedJWT(header, claims).apply { sign(RSASSASigner(privateKey)) }
    }

    private fun isExpired(jwt: String): Boolean =
        SignedJWT.parse(jwt).jwtClaimsSet.expirationTime
            ?.before(Date.from(Instant.now().plusSeconds(30)))
            ?: true
}
