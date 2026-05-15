package no.kartverket.matrikkel.token

import no.kartverket.matrikkel.token.TokenClientTestHelper.metadataResponse
import no.kartverket.matrikkel.token.TokenClientTestHelper.signedJwt
import no.kartverket.matrikkel.token.TokenClientTestHelper.tokenResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MatrikkelTokenClientTest {

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

    private fun buildClient() = MatrikkelTokenClient(
        username = "testbruker",
        password = "testpassord",
        wellKnownUrl = server.url("/.well-known/openid-configuration").toString(),
    )

    @Test
    fun `getToken henter access token via password grant`() {
        val expectedToken = signedJwt()
        server.enqueue(metadataResponse(server))
        server.enqueue(tokenResponse(expectedToken))

        val token = buildClient().getToken()

        assertEquals(expectedToken, token)
        val tokenRequest = server.takeRequest() // metadata
        server.takeRequest().let { req ->        // token-kall
            val body = req.body.readUtf8()
            assertTrue(body.contains("grant_type=password"))
            assertTrue(body.contains("username=testbruker"))
        }
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

    @Test
    fun `getToken bruker refresh token naar access token er utloept`() {
        val utloeptToken = signedJwt(expiresInSeconds = -60)
        val nyttToken = signedJwt()
        val refreshToken = "test-refresh-token"

        server.enqueue(metadataResponse(server))
        server.enqueue(tokenResponse(utloeptToken, refreshToken))
        server.enqueue(tokenResponse(nyttToken))

        val client = buildClient()
        client.getToken()        // henter utløpt token + lagrer refresh token
        val token = client.getToken() // oppdager utløpt, bruker refresh token

        assertEquals(nyttToken, token)
        assertEquals(3, server.requestCount) // 1 well-known + 2 token-kall
        val refreshRequest = server.takeRequest() // metadata
        server.takeRequest()                      // første token-kall
        server.takeRequest().let { req ->         // refresh-kall
            assertTrue(req.body.readUtf8().contains("grant_type=refresh_token"))
        }
    }
}
