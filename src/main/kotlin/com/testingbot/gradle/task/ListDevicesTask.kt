package com.testingbot.gradle.task

import com.testingbot.gradle.internal.Device
import com.testingbot.gradle.internal.Json
import com.testingbot.gradle.internal.TestingBotClient
import com.testingbot.gradle.internal.requireCredentials
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/** Prints the Android devices available on TestingBot to help author [capabilities]. */
@DisableCachingByDefault(because = "Queries live device availability")
abstract class ListDevicesTask : DefaultTask() {
    @get:Internal
    abstract val key: Property<String>

    @get:Internal
    abstract val secret: Property<String>

    @get:Input
    abstract val apiBaseUrl: Property<String>

    @TaskAction
    fun list() {
        val (k, s) = requireCredentials(key.orNull, secret.orNull)
        val client = TestingBotClient(apiBaseUrl.get(), k, s)
        val devices = Json.fromJson(client.get("/devices?platform=android"), Array<Device>::class.java)

        logger.lifecycle(String.format("%-28s %-10s %-10s", "DEVICE", "VERSION", "AVAILABLE"))
        devices.sortedWith(compareBy({ it.name }, { it.version })).forEach { d ->
            logger.lifecycle(
                String.format(
                    "%-28s %-10s %-10s",
                    d.name ?: "?",
                    d.version ?: "?",
                    if (d.available == true) "yes" else "no",
                ),
            )
        }
        logger.lifecycle("\n${devices.size} Android device model(s). Use the name (regex allowed) in capabilities.")
    }
}
