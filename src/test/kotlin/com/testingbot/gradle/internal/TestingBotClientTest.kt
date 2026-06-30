package com.testingbot.gradle.internal

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class TestingBotClientTest {
    private lateinit var server: HttpServer
    private val baseUrl get() = "http://127.0.0.1:${server.address.port}"

    @BeforeEach
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun `uploadMultipart sends basic auth and a file part`() {
        val auth = AtomicReference<String>()
        val body = AtomicReference<String>()
        server.createContext("/storage") { ex ->
            auth.set(ex.requestHeaders.getFirst("Authorization"))
            body.set(ex.requestBody.readBytes().toString(StandardCharsets.UTF_8))
            respond(ex, 200, """{"app_url":"tb://abc"}""")
        }
        val apk = File.createTempFile("app", ".apk").apply { writeText("DUMMYAPK") }

        val response = TestingBotClient(baseUrl, "key", "secret").uploadMultipart("/storage", apk)

        assertEquals("""{"app_url":"tb://abc"}""", response)
        val expectedAuth = "Basic " + Base64.getEncoder().encodeToString("key:secret".toByteArray())
        assertEquals(expectedAuth, auth.get())
        assertTrue(body.get().contains("""name="file"; filename="${apk.name}""""), body.get())
        assertTrue(body.get().contains("DUMMYAPK"), body.get())
    }

    @Test
    fun `get retries on 5xx then succeeds`() {
        val calls = AtomicInteger(0)
        server.createContext("/devices") { ex ->
            if (calls.incrementAndGet() < 2) {
                ex.sendResponseHeaders(503, -1)
                ex.close()
            } else {
                respond(ex, 200, "[]")
            }
        }

        val result = TestingBotClient(baseUrl, "k", "s", maxRetries = 3).get("/devices")

        assertEquals("[]", result)
        assertEquals(2, calls.get())
    }

    @Test
    fun `non-2xx response throws with status and body`() {
        server.createContext("/storage") { ex -> respond(ex, 400, "bad request") }
        val apk = File.createTempFile("app", ".apk").apply { writeText("x") }

        val ex = assertThrows(TestingBotApiException::class.java) {
            TestingBotClient(baseUrl, "k", "s").uploadMultipart("/storage", apk)
        }
        assertTrue(ex.message!!.contains("HTTP 400"), ex.message)
        assertTrue(ex.message!!.contains("bad request"), ex.message)
    }

    private fun respond(ex: com.sun.net.httpserver.HttpExchange, code: Int, body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        ex.sendResponseHeaders(code, bytes.size.toLong())
        ex.responseBody.use { it.write(bytes) }
    }
}
