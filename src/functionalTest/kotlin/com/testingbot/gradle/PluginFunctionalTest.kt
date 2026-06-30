package com.testingbot.gradle

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

/** End-to-end tests that apply the plugin in a throwaway project and drive a mock API. */
class PluginFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    private lateinit var server: HttpServer

    /** Toggled per test to simulate a passing vs failing Espresso run. */
    @Volatile
    private var espressoSuccess = true

    private val port get() = server.address.port

    @BeforeEach
    fun setUp() {
        espressoSuccess = true
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { route(it) }
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.stop(0)
    }

    private fun route(ex: HttpExchange) {
        ex.requestBody.readBytes() // drain request
        val (code, body) = when (ex.requestURI.path) {
            "/storage" -> 200 to """{"app_url":"tb://abc123"}"""
            "/app-automate/espresso/app" -> 200 to """{"id":42}"""
            "/app-automate/espresso/42/tests" -> 200 to """{"id":42}"""
            "/app-automate/espresso/42/run" ->
                200 to """{"success":true,"id":42,"runs":[{"id":1,"capabilities":{}}]}"""
            "/app-automate/espresso/42/report" -> 200 to """<testsuite name="espresso"/>"""
            "/app-automate/espresso/42" -> 200 to """{"runs":[],"success":$espressoSuccess,"completed":true}"""
            "/devices" ->
                200 to """[{"id":1,"name":"Pixel 8","platform_name":"Android","version":"14","model_number":"GKWS6","available":true}]"""
            else -> 404 to """{"error":"not found"}"""
        }
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        ex.sendResponseHeaders(code, bytes.size.toLong())
        ex.responseBody.use { it.write(bytes) }
    }

    private fun writeProject(extra: String = "") {
        File(projectDir, "settings.gradle.kts").writeText("""rootProject.name = "consumer"""")
        File(projectDir, "app.apk").writeText("APP")
        File(projectDir, "app-androidTest.apk").writeText("TEST")
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins { id("com.testingbot.gradle") }

            testingbot {
                key.set("k")
                secret.set("s")
                apiBaseUrl.set("http://127.0.0.1:$port")
                appApk.set(layout.projectDirectory.file("app.apk"))
                testApk.set(layout.projectDirectory.file("app-androidTest.apk"))
                capabilities.set(
                    listOf(mapOf("deviceName" to "Pixel.*", "version" to "14", "platformName" to "Android"))
                )
                pollIntervalSeconds.set(1L)
                $extra
            }
            """.trimIndent(),
        )
    }

    private fun runner(vararg args: String) = GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments(*args)
        .forwardOutput()

    @Test
    fun `registers the testingbot tasks`() {
        writeProject()
        val result = runner("tasks", "--group", "testingbot").build()
        assertTrue(result.output.contains("testingbotUpload"))
        assertTrue(result.output.contains("testingbotEspresso"))
        assertTrue(result.output.contains("testingbotDevices"))
    }

    @Test
    fun `testingbotUpload uploads and records the app url`() {
        writeProject()
        val result = runner("testingbotUpload").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":testingbotUpload")?.outcome)
        assertEquals("tb://abc123", File(projectDir, "build/testingbot/app-url.txt").readText())
    }

    @Test
    fun `testingbotEspresso passes and writes the junit report`() {
        espressoSuccess = true
        writeProject()
        val result = runner("testingbotEspresso").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":testingbotEspresso")?.outcome)
        assertTrue(
            File(projectDir, "build/testingbot/espresso-junit.xml").readText().contains("testsuite"),
        )
    }

    @Test
    fun `testingbotEspresso fails the build when a run fails`() {
        espressoSuccess = false
        writeProject()
        val result = runner("testingbotEspresso").buildAndFail()
        assertEquals(TaskOutcome.FAILED, result.task(":testingbotEspresso")?.outcome)
    }

    @Test
    fun `testingbotDevices lists android devices`() {
        writeProject()
        val result = runner("testingbotDevices").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":testingbotDevices")?.outcome)
        assertTrue(result.output.contains("Pixel 8"), result.output)
        assertTrue(result.output.contains("14"), result.output)
    }

    @Test
    fun `testingbotUpload is configuration-cache compatible`() {
        writeProject()
        runner("testingbotUpload", "--configuration-cache").build()
        val second = runner("testingbotUpload", "--configuration-cache").build()
        assertTrue(
            second.output.contains("Reusing configuration cache"),
            "expected configuration cache to be reused:\n${second.output}",
        )
    }
}
