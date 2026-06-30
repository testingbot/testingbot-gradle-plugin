package com.testingbot.gradle

import com.testingbot.gradle.internal.AndroidWiring
import com.testingbot.gradle.task.EspressoTestTask
import com.testingbot.gradle.task.ListDevicesTask
import com.testingbot.gradle.task.UploadAppTask
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Registers the `testingbot { }` extension and the plugin's tasks:
 *  - `testingbotUpload`   — upload the app APK to TestingBot Storage
 *  - `testingbotEspresso` — upload app + test APK, run Espresso tests, fetch the JUnit report
 *  - `testingbotDevices`  — list available Android devices
 */
class TestingBotPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val ext = project.extensions.create("testingbot", TestingBotExtension::class.java)
        val providers = project.providers
        val layout = project.layout

        ext.apiBaseUrl.convention("https://api.testingbot.com/v1")
        ext.key.convention(
            providers.gradleProperty("testingbot.key").orElse(providers.environmentVariable("TESTINGBOT_KEY")),
        )
        ext.secret.convention(
            providers.gradleProperty("testingbot.secret").orElse(providers.environmentVariable("TESTINGBOT_SECRET")),
        )
        ext.capabilities.convention(emptyList())
        ext.espressoOptions.convention(emptyMap())
        ext.waitForResults.convention(true)
        ext.pollIntervalSeconds.convention(15L)
        ext.timeoutMinutes.convention(30L)
        ext.failBuildOnTestFailure.convention(true)
        ext.reportXml.convention(layout.buildDirectory.file("testingbot/espresso-junit.xml"))

        val group = "testingbot"

        val upload = project.tasks.register("testingbotUpload", UploadAppTask::class.java) {
            it.group = group
            it.description = "Uploads the app APK to TestingBot Storage and prints the tb:// app URL."
            it.key.set(ext.key)
            it.secret.set(ext.secret)
            it.apiBaseUrl.set(ext.apiBaseUrl)
            it.appApk.set(ext.appApk)
            it.appUrlFile.set(layout.buildDirectory.file("testingbot/app-url.txt"))
        }

        val espresso = project.tasks.register("testingbotEspresso", EspressoTestTask::class.java) {
            it.group = group
            it.description = "Uploads app + test APK, runs Espresso tests on TestingBot, and writes a JUnit report."
            it.key.set(ext.key)
            it.secret.set(ext.secret)
            it.apiBaseUrl.set(ext.apiBaseUrl)
            it.appApk.set(ext.appApk)
            it.testApk.set(ext.testApk)
            it.capabilities.set(ext.capabilities)
            it.espressoOptions.set(ext.espressoOptions)
            it.waitForResults.set(ext.waitForResults)
            it.pollIntervalSeconds.set(ext.pollIntervalSeconds)
            it.timeoutMinutes.set(ext.timeoutMinutes)
            it.failBuildOnTestFailure.set(ext.failBuildOnTestFailure)
            it.reportXml.set(ext.reportXml)
        }

        project.tasks.register("testingbotDevices", ListDevicesTask::class.java) {
            it.group = group
            it.description = "Lists Android devices available on TestingBot (helper for choosing capabilities)."
            it.key.set(ext.key)
            it.secret.set(ext.secret)
            it.apiBaseUrl.set(ext.apiBaseUrl)
        }

        AndroidWiring.apply(project, ext, listOf(upload, espresso))
    }
}
