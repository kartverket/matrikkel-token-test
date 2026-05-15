package no.kartverket.matrikkel.token

import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.AuthorizationGrant
import com.nimbusds.oauth2.sdk.RefreshTokenGrant
import com.nimbusds.oauth2.sdk.ResourceOwnerPasswordCredentialsGrant
import com.nimbusds.oauth2.sdk.Scope
import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.oauth2.sdk.TokenResponse
import com.nimbusds.oauth2.sdk.auth.Secret
import com.nimbusds.oauth2.sdk.id.ClientID
import com.nimbusds.oauth2.sdk.token.RefreshToken
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata
import java.net.URI
import java.time.Instant
import java.util.Date

const val MATRIKKEL_CLIENT_ID = "matrikkel-token-exchange"

class MatrikkelTokenClient(
    private val username: String,
    private val password: String,
    private val wellKnownUrl: String,
) {
    private val clientID = ClientID(MATRIKKEL_CLIENT_ID)

    private val providerMetadata: OIDCProviderMetadata by lazy {
        OIDCProviderMetadata.parse(URI(wellKnownUrl).toURL().readText())
    }

    private var accessToken: String? = null
    private var refreshToken: RefreshToken? = null

    fun getToken(): String {
        val current = accessToken
        return when {
            current != null && !isExpired(current) -> current
            refreshToken != null -> fetchWithRefreshToken()
            else -> fetchWithPassword()
        }
    }

    private fun fetchWithPassword(): String =
        sendTokenRequest(ResourceOwnerPasswordCredentialsGrant(username, Secret(password)))

    private fun fetchWithRefreshToken(): String {
        val token = refreshToken ?: return fetchWithPassword()
        return runCatching { sendTokenRequest(RefreshTokenGrant(token)) }
            .getOrElse { fetchWithPassword() }
    }

    private fun sendTokenRequest(grant: AuthorizationGrant): String {
        val request = TokenRequest(providerMetadata.tokenEndpointURI, clientID, grant, null as Scope?)
        val tokens = TokenResponse.parse(request.toHTTPRequest().send())
            .toSuccessResponse().tokens
        accessToken = tokens.accessToken.value
        refreshToken = tokens.refreshToken
        return tokens.accessToken.value
    }

    private fun isExpired(jwt: String): Boolean =
        SignedJWT.parse(jwt).jwtClaimsSet.expirationTime
            ?.before(Date.from(Instant.now().plusSeconds(30)))
            ?: true
}
