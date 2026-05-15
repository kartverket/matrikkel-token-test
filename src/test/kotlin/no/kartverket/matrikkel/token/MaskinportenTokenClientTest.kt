package no.kartverket.matrikkel.token

import no.kartverket.matrikkel.token.TokenClientTestHelper.metadataResponse
import no.kartverket.matrikkel.token.TokenClientTestHelper.privateKeyPem
import no.kartverket.matrikkel.token.TokenClientTestHelper.signedJwt
import no.kartverket.matrikkel.token.TokenClientTestHelper.tokenResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MaskinportenTokenClientTest {

    private lateinit var server: MockWebServer

    @BeforeTest
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @AfterTest
    fun tearDown() {
        server.shutdown()
    }

    private fun buildClient() = MaskinportenTokenClient(
        clientId = "test-client-id",
        scope = "kartverket:test/read",
        privateKeyPem = privateKeyPem(),
        keyId = "test-kid",
        wellKnownUrl = server.url("/.well-known/oauth-authorization-server").toString(),
    )

    @Test
    fun `getToken henter access token via JWT bearer grant`() {
        val expectedToken = signedJwt()
        server.enqueue(metadataResponse(server))
        server.enqueue(tokenResponse(expectedToken))

        val token = buildClient().getToken()

        assertEquals(expectedToken, token)
    }

    @Test
    fun `getToken sender JWT bearer grant type i token-request`() {
        server.enqueue(metadataResponse(server))
        server.enqueue(tokenResponse(signedJwt()))

        buildClient().getToken()

        server.takeRequest() // metadata
        val tokenRequest = server.takeRequest()
        val body = tokenRequest.body.readUtf8()
        assertTrue(
            body.contains("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer"),
            "Forventet JWT bearer grant type i request-body, men fikk: $body"
        )
        assertTrue(body.contains("assertion="), "Forventet assertion-parameter i request-body")
    }

    @Test
    fun `getToken returnerer cachet token ved andre kall`() {
        val expectedToken = signedJwt()
        server.enqueue(metadataResponse(server))
        server.enqueue(tokenResponse(expectedToken))

        val client = buildClient()
        val token1 = client.getToken()
        val token2 = client.getToken()

        assertEquals(token1, token2)
        // Kun 2 HTTP-kall totalt: 1 well-known + 1 token — ingen ekstra ved andre getToken()
        assertEquals(2, server.requestCount)
    }
}
