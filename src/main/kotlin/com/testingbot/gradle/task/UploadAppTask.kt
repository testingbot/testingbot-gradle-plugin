package com.testingbot.gradle.task

import com.testingbot.gradle.internal.Json
import com.testingbot.gradle.internal.StorageUploadResponse
import com.testingbot.gradle.internal.TestingBotClient
import com.testingbot.gradle.internal.requireCredentials
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/** Uploads the app APK to TestingBot Storage and records the returned `tb://` app URL. */
@DisableCachingByDefault(because = "Performs a network upload with a remote side effect")
abstract class UploadAppTask : DefaultTask() {
    @get:Internal
    abstract val key: Property<String>

    @get:Internal
    abstract val secret: Property<String>

    @get:Input
    abstract val apiBaseUrl: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val appApk: RegularFileProperty

    /** Text file the resulting `tb://<appkey>` URL is written to for downstream consumption. */
    @get:OutputFile
    abstract val appUrlFile: RegularFileProperty

    @TaskAction
    fun upload() {
        val (k, s) = requireCredentials(key.orNull, secret.orNull)
        val apk = appApk.get().asFile
        if (!apk.isFile) throw GradleException("App APK not found: ${apk.absolutePath}")

        val client = TestingBotClient(apiBaseUrl.get(), k, s)
        logger.lifecycle("Uploading ${apk.name} to TestingBot Storage…")
        val response = client.uploadMultipart("/storage", apk)
        val appUrl = Json.fromJson(response, StorageUploadResponse::class.java).app_url
            ?: throw GradleException("TestingBot Storage upload did not return an app_url. Response: $response")

        val out = appUrlFile.get().asFile
        out.parentFile.mkdirs()
        out.writeText(appUrl)
        logger.lifecycle("Uploaded to TestingBot Storage: $appUrl")
        logger.lifecycle("Saved app URL to ${out.absolutePath}")
    }
}
