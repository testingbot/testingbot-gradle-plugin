package com.testingbot.gradle.task

import com.testingbot.gradle.internal.EspressoIdResponse
import com.testingbot.gradle.internal.EspressoRunResult
import com.testingbot.gradle.internal.EspressoStatusResponse
import com.testingbot.gradle.internal.Json
import com.testingbot.gradle.internal.TestingBotClient
import com.testingbot.gradle.internal.requireCredentials
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Uploads the app + Espresso test APK to TestingBot, starts a run for each configured
 * capability, optionally polls for completion, downloads the JUnit report, and (by default)
 * fails the build when any run reports failures.
 */
@DisableCachingByDefault(because = "Drives a remote test run with side effects")
abstract class EspressoTestTask : DefaultTask() {
    @get:Internal
    abstract val key: Property<String>

    @get:Internal
    abstract val secret: Property<String>

    @get:Input
    abstract val apiBaseUrl: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val appApk: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val testApk: RegularFileProperty

    @get:Input
    abstract val capabilities: ListProperty<Map<String, Any>>

    @get:Input
    abstract val espressoOptions: MapProperty<String, Any>

    @get:Input
    abstract val waitForResults: Property<Boolean>

    @get:Input
    abstract val pollIntervalSeconds: Property<Long>

    @get:Input
    abstract val timeoutMinutes: Property<Long>

    @get:Input
    abstract val failBuildOnTestFailure: Property<Boolean>

    @get:Optional
    @get:OutputFile
    abstract val reportXml: RegularFileProperty

    @TaskAction
    fun run() {
        val (k, s) = requireCredentials(key.orNull, secret.orNull)
        val caps = capabilities.get()
        if (caps.isEmpty()) {
            throw GradleException(
                "No capabilities configured. Set at least one device, e.g.\n" +
                    "  testingbot { capabilities.set(listOf(mapOf(\"deviceName\" to \"Pixel.*\", " +
                    "\"version\" to \"14\", \"platformName\" to \"Android\"))) }",
            )
        }
        val app = appApk.get().asFile
        val tests = testApk.get().asFile
        if (!app.isFile) throw GradleException("App APK not found: ${app.absolutePath}")
        if (!tests.isFile) throw GradleException("Test APK not found: ${tests.absolutePath}")

        val client = TestingBotClient(apiBaseUrl.get(), k, s)

        logger.lifecycle("Uploading app APK ${app.name}…")
        val projectId = Json.fromJson(
            client.uploadMultipart("/app-automate/espresso/app", app),
            EspressoIdResponse::class.java,
        ).id ?: throw GradleException("Espresso app upload did not return a project id.")

        logger.lifecycle("Uploading test APK ${tests.name}…")
        client.uploadMultipart("/app-automate/espresso/$projectId/tests", tests)

        val payload = linkedMapOf<String, Any>("capabilities" to caps)
        espressoOptions.get().takeIf { it.isNotEmpty() }?.let { payload["espressoOptions"] = it }

        logger.lifecycle("Starting Espresso run across ${caps.size} capability/capabilities…")
        val runResult = Json.fromJson(
            client.postJson("/app-automate/espresso/$projectId/run", Json.toJson(payload)),
            EspressoRunResult::class.java,
        )
        runResult.runs.orEmpty().forEach { logger.lifecycle("  • run ${it.id}: ${Json.toJson(it.capabilities)}") }
        logger.lifecycle("Track progress in your TestingBot dashboard: https://testingbot.com/members")

        if (!waitForResults.get()) {
            logger.lifecycle("waitForResults is false — not polling for results (project $projectId).")
            return
        }

        val status = poll(client, projectId)
        writeReport(client, projectId)

        val passed = status.success == true
        if (passed) {
            logger.lifecycle("Espresso run passed (project $projectId).")
        } else if (failBuildOnTestFailure.get()) {
            throw GradleException("Espresso run reported failures (project $projectId). See the report for details.")
        } else {
            logger.warn("Espresso run reported failures (project $projectId), but failBuildOnTestFailure is false.")
        }
    }

    private fun poll(client: TestingBotClient, projectId: Long): EspressoStatusResponse {
        val intervalMs = pollIntervalSeconds.get().coerceAtLeast(1) * 1000
        val deadline = System.currentTimeMillis() + timeoutMinutes.get().coerceAtLeast(1) * 60_000
        while (true) {
            val status = Json.fromJson(
                client.get("/app-automate/espresso/$projectId"),
                EspressoStatusResponse::class.java,
            )
            if (status.completed == true) return status
            if (System.currentTimeMillis() >= deadline) {
                throw GradleException(
                    "Timed out after ${timeoutMinutes.get()} min waiting for Espresso project $projectId to complete.",
                )
            }
            logger.lifecycle("Waiting for results… (project $projectId)")
            Thread.sleep(intervalMs)
        }
    }

    private fun writeReport(client: TestingBotClient, projectId: Long) {
        val target = reportXml.orNull?.asFile ?: return
        try {
            val xml = client.get("/app-automate/espresso/$projectId/report")
            target.parentFile?.mkdirs()
            target.writeText(xml)
            logger.lifecycle("JUnit report written to ${target.absolutePath}")
        } catch (e: Exception) {
            logger.warn("Could not download JUnit report for project $projectId: ${e.message}")
        }
    }
}
