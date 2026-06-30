package com.testingbot.gradle

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

/**
 * DSL configuration for the TestingBot plugin, exposed as the `testingbot { }` block.
 *
 * All members are lazy Gradle properties so values can be wired from other providers
 * (environment variables, Gradle properties, build outputs) and remain compatible with
 * the configuration cache.
 */
abstract class TestingBotExtension {
    /**
     * TestingBot API key. Defaults to the `testingbot.key` Gradle property or the
     * `TESTINGBOT_KEY` environment variable. Find it at
     * https://testingbot.com/members/user/api
     */
    abstract val key: Property<String>

    /**
     * TestingBot API secret. Defaults to the `testingbot.secret` Gradle property or the
     * `TESTINGBOT_SECRET` environment variable.
     */
    abstract val secret: Property<String>

    /** Base URL of the TestingBot REST API. Defaults to `https://api.testingbot.com/v1`. */
    abstract val apiBaseUrl: Property<String>

    /** The application APK to upload / test. */
    abstract val appApk: RegularFileProperty

    /** The instrumented Espresso test APK (the `androidTest` artifact). */
    abstract val testApk: RegularFileProperty

    /**
     * Device capabilities to run the Espresso suite against — one run is started per entry.
     * The maps are passed through to the API verbatim, e.g.
     * `mapOf("deviceName" to "Pixel.*", "version" to "14", "platformName" to "Android", "realDevice" to true)`.
     */
    abstract val capabilities: ListProperty<Map<String, Any>>

    /** Optional Espresso run options passed through to the API (e.g. `clearPackageData`). */
    abstract val espressoOptions: MapProperty<String, Any>

    /** Whether [com.testingbot.gradle.task.EspressoTestTask] polls for results. Default `true`. */
    abstract val waitForResults: Property<Boolean>

    /** Seconds between status polls while waiting for results. Default `15`. */
    abstract val pollIntervalSeconds: Property<Long>

    /** Maximum minutes to wait for results before timing out. Default `30`. */
    abstract val timeoutMinutes: Property<Long>

    /** Whether the build fails when the Espresso run reports failures. Default `true`. */
    abstract val failBuildOnTestFailure: Property<Boolean>

    /** Where the downloaded JUnit XML report is written. Default `build/testingbot/espresso-junit.xml`. */
    abstract val reportXml: RegularFileProperty

    /**
     * Optional: when the Android Gradle Plugin is applied, naming a variant (e.g. `"debug"`)
     * makes the plugin depend on the matching `assemble<Variant>` /
     * `assemble<Variant>AndroidTest` tasks and default [appApk] / [testApk] to that variant's
     * conventional output paths. Explicitly set paths always win.
     */
    abstract val autoWireFromVariant: Property<String>
}
