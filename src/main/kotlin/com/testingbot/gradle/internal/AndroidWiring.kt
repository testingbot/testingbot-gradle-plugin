package com.testingbot.gradle.internal

import com.testingbot.gradle.TestingBotExtension
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

/**
 * Optional, best-effort integration with the Android Gradle Plugin. Active only when
 * `com.android.application` is applied **and** [TestingBotExtension.autoWireFromVariant] is set.
 *
 * It defaults [TestingBotExtension.appApk] / [TestingBotExtension.testApk] to the variant's
 * conventional output paths (explicit DSL values always win) and makes the upload/Espresso
 * tasks depend on the matching `assemble<Variant>` / `assemble<Variant>AndroidTest` tasks so a
 * single command both builds and tests. The plugin works without AGP; this only adds convenience.
 */
internal object AndroidWiring {
    fun apply(project: Project, ext: TestingBotExtension, appTasks: List<TaskProvider<*>>) {
        project.plugins.withId("com.android.application") {
            project.afterEvaluate {
                val variant = ext.autoWireFromVariant.orNull?.takeIf { it.isNotBlank() } ?: return@afterEvaluate
                val cap = variant.replaceFirstChar { it.uppercase() }

                ext.appApk.convention(
                    project.layout.buildDirectory.file("outputs/apk/$variant/${project.name}-$variant.apk"),
                )
                ext.testApk.convention(
                    project.layout.buildDirectory.file(
                        "outputs/apk/androidTest/$variant/${project.name}-$variant-androidTest.apk",
                    ),
                )

                val assembleApp = project.tasks.findByName("assemble$cap")
                if (assembleApp == null) {
                    project.logger.warn(
                        "[testingbot] autoWireFromVariant=\"$variant\" but task assemble$cap was not found; " +
                            "skipping build wiring.",
                    )
                    return@afterEvaluate
                }
                appTasks.forEach { tp -> tp.configure { it.dependsOn(assembleApp) } }
                project.tasks.findByName("assemble${cap}AndroidTest")?.let { assembleTest ->
                    project.tasks.named("testingbotEspresso").configure { it.dependsOn(assembleTest) }
                }
            }
        }
    }
}
