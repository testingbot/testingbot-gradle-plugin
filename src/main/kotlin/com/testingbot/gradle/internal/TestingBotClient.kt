package com.testingbot.gradle.internal

import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Raised when the TestingBot API returns a non-2xx response or cannot be reached.
 * [statusCode] is the HTTP status, or `null` for connection-level failures.
 */
internal class TestingBotApiException(val statusCode: Int?, message: String) : RuntimeException(message)

/**
 * Minimal HTTP client for the TestingBot REST API built on [HttpURLConnection] so the
 * plugin adds zero HTTP dependencies to the build classpath. Stateless and safe to create
 * inside a task action (no Gradle [org.gradle.api.Project] references).
 */
internal class TestingBotClient(
    baseUrl: String,
    private val key: String,
    private val secret: String,
    private val maxRetries: Int = 3,
) {
    private val base: String = baseUrl.trimEnd('/')
    private val authHeader: String =
        "Basic " + Base64.getEncoder().encodeToString("$key:$secret".toByteArray(StandardCharsets.UTF_8))

    /** Streams [file] to [path] as `multipart/form-data` (field name `file`) plus optional text [fields]. */
    fun uploadMultipart(path: String, file: File, fields: Map<String, String> = emptyMap()): String {
        val boundary = "----testingbotBoundary" + java.lang.Long.toHexString(System.nanoTime())
        val crlf = "\r\n"
        val conn = open(path, "POST")
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        // Stream the body so large APKs are never fully buffered in memory.
        conn.setChunkedStreamingMode(0)

        try {
            DataOutputStream(BufferedOutputStream(conn.outputStream)).use { out ->
                for ((name, value) in fields) {
                    out.writeBytes("--$boundary$crlf")
                    out.writeBytes("Content-Disposition: form-data; name=\"$name\"$crlf$crlf")
                    out.write(value.toByteArray(StandardCharsets.UTF_8))
                    out.writeBytes(crlf)
                }
                out.writeBytes("--$boundary$crlf")
                out.writeBytes(
                    "Content-Disposition: form-data; name=\"file\"; filename=\"${file.name}\"$crlf",
                )
                out.writeBytes("Content-Type: application/vnd.android.package-archive$crlf$crlf")
                file.inputStream().use { it.copyTo(out) }
                out.writeBytes(crlf)
                out.writeBytes("--$boundary--$crlf")
            }
            return readResponse(conn, "POST $path")
        } finally {
            conn.disconnect()
        }
    }

    /** Sends a JSON body to [path] via POST and returns the response body. Not retried. */
    fun postJson(path: String, json: String): String {
        val conn = open(path, "POST")
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        try {
            conn.outputStream.use { it.write(json.toByteArray(StandardCharsets.UTF_8)) }
            return readResponse(conn, "POST $path")
        } finally {
            conn.disconnect()
        }
    }

    /** Performs a GET against [path], retrying transient failures, and returns the response body. */
    fun get(path: String): String = withRetry("GET $path") {
        val conn = open(path, "GET")
        try {
            readResponse(conn, "GET $path")
        } finally {
            conn.disconnect()
        }
    }

    private fun open(path: String, method: String): HttpURLConnection {
        val url = URI.create(base + ensureLeadingSlash(path)).toURL()
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.setRequestProperty("Authorization", authHeader)
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "testingbot-gradle-plugin")
        conn.connectTimeout = 30_000
        conn.readTimeout = 120_000
        return conn
    }

    private fun readResponse(conn: HttpURLConnection, context: String): String {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val body = stream?.readBytes()?.toString(StandardCharsets.UTF_8).orEmpty()
        if (code !in 200..299) {
            throw TestingBotApiException(code, "TestingBot API request failed ($context): HTTP $code ${body.take(2000)}")
        }
        return body
    }

    private fun <T> withRetry(context: String, block: () -> T): T {
        var attempt = 0
        while (true) {
            attempt++
            try {
                return block()
            } catch (e: IOException) {
                // Connection-level failures are transient.
                if (attempt >= maxRetries) {
                    throw TestingBotApiException(null, "TestingBot API request failed ($context): ${e.message}")
                }
            } catch (e: TestingBotApiException) {
                // Retry only server-side (5xx) or connection-level failures, based on the status code.
                val retryable = e.statusCode == null || e.statusCode in 500..599
                if (!retryable || attempt >= maxRetries) throw e
            }
            Thread.sleep(1000L * attempt)
        }
    }

    private fun ensureLeadingSlash(path: String) = if (path.startsWith("/")) path else "/$path"
}
