import com.vanniktech.maven.publish.GradlePublishPlugin
import org.gradle.plugins.signing.Sign
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
    alias(libs.plugins.plugin.publish)
    alias(libs.plugins.maven.publish)
}

group = providers.gradleProperty("GROUP").get()
version = providers.gradleProperty("VERSION_NAME").get()
description = providers.gradleProperty("POM_DESCRIPTION").get()

dependencies {
    implementation(libs.gson)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Compile to JVM 17 bytecode so the plugin runs on any Gradle build using JDK 17+,
// even though this project may be built with a newer JDK.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

gradlePlugin {
    website.set(providers.gradleProperty("POM_URL"))
    vcsUrl.set(providers.gradleProperty("POM_URL"))

    plugins {
        create("testingbot") {
            id = "com.testingbot.gradle"
            displayName = "TestingBot Gradle Plugin"
            description = providers.gradleProperty("POM_DESCRIPTION").get()
            implementationClass = "com.testingbot.gradle.TestingBotPlugin"
            tags.set(listOf("testingbot", "espresso", "android", "mobile-testing", "app-automate"))
        }
    }
}

// --- Functional tests (Gradle TestKit) --------------------------------------
val functionalTest: SourceSet by sourceSets.creating
gradlePlugin.testSourceSets(functionalTest)

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

dependencies {
    "functionalTestImplementation"(gradleTestKit())
}

val functionalTestTask = tasks.register<Test>("functionalTest") {
    description = "Runs the functional tests using Gradle TestKit."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = functionalTest.output.classesDirs
    classpath = functionalTest.runtimeClasspath
    useJUnitPlatform()
    shouldRunAfter(tasks.named("test"))
}

tasks.named("check") {
    dependsOn(functionalTestTask)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// --- Publishing: Gradle Plugin Portal (plugin-publish) + Maven Central -------
mavenPublishing {
    // Use the GradlePublishPlugin platform because com.gradle.plugin-publish already
    // creates the publication, the sources/javadoc jars, and the plugin marker.
    configure(GradlePublishPlugin())
    // vanniktech 0.32+ targets the Central Portal only, so no SonatypeHost argument.
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
}

// Only sign when a key is configured (it is in the publish workflow). This keeps
// `build`, `publishPlugins --validate-only` and `publishToMavenLocal` working locally
// and in no-secrets PR builds, while Maven Central releases are still signed in CI.
val hasSigningKey =
    providers.gradleProperty("signingInMemoryKey").isPresent ||
        providers.gradleProperty("signing.keyId").isPresent
tasks.withType<Sign>().configureEach {
    onlyIf { hasSigningKey }
}
