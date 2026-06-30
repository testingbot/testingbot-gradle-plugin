# TestingBot Gradle Plugin

[![Build](https://github.com/testingbot/testingbot-gradle-plugin/actions/workflows/build.yml/badge.svg)](https://github.com/testingbot/testingbot-gradle-plugin/actions/workflows/build.yml)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/com.testingbot.gradle)](https://plugins.gradle.org/plugin/com.testingbot.gradle)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A [Gradle](https://gradle.org/) plugin that uploads Android apps to
[TestingBot](https://testingbot.com) Storage and runs **Espresso** tests on TestingBot's
real device cloud — straight from your build, no `curl` scripts required.

## Features

- **`testingbotUpload`** — upload an APK to TestingBot Storage and get a reusable
  `tb://<appkey>` identifier.
- **`testingbotEspresso`** — upload your app + instrumented test APK, run Espresso across
  one or more devices, wait for results, download a JUnit XML report, and fail the build on
  test failures.
- **`testingbotDevices`** — list the Android devices available on your account.
- Credentials from the DSL, Gradle properties, or environment variables.
- Optional Android Gradle Plugin auto-wiring (build + test in one command).
- Zero extra HTTP dependencies on your build classpath; configuration-cache compatible.

## Requirements

- Gradle 8.0+ running on JDK 17 or newer.
- A TestingBot account. Grab your **key** and **secret** at
  <https://testingbot.com/members/user/api>.

## Installation

```kotlin
// build.gradle.kts (your Android app module)
plugins {
    id("com.testingbot.gradle") version "0.1.0"
}
```

<details>
<summary>Groovy DSL</summary>

```groovy
// build.gradle
plugins {
    id 'com.testingbot.gradle' version '0.1.0'
}
```
</details>

## Credentials

Resolved in this order (first match wins):

1. The DSL — `testingbot { key.set("…"); secret.set("…") }`
2. Gradle properties — `-Ptestingbot.key=… -Ptestingbot.secret=…` (or in `gradle.properties`)
3. Environment variables — `TESTINGBOT_KEY` / `TESTINGBOT_SECRET`

In CI, prefer environment variables backed by secrets. **Never commit credentials.**

## Usage

```kotlin
testingbot {
    // Credentials (optional here if provided via env vars / gradle properties)
    key.set(providers.environmentVariable("TESTINGBOT_KEY"))
    secret.set(providers.environmentVariable("TESTINGBOT_SECRET"))

    // The app + instrumented test APKs
    appApk.set(layout.projectDirectory.file("app/build/outputs/apk/debug/app-debug.apk"))
    testApk.set(
        layout.projectDirectory.file("app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk")
    )

    // One run is started per capability. Keys are passed through to TestingBot verbatim.
    capabilities.set(
        listOf(
            mapOf("deviceName" to "Pixel.*", "version" to "14", "platformName" to "Android", "realDevice" to true),
            mapOf("deviceName" to "Galaxy S.*", "version" to "13", "platformName" to "Android"),
        )
    )

    // Optional behavior
    waitForResults.set(true)            // poll until the run completes (default true)
    failBuildOnTestFailure.set(true)    // default true
    reportXml.set(layout.buildDirectory.file("testingbot/espresso-junit.xml"))
}
```

Then:

```bash
# Upload only
./gradlew testingbotUpload

# Build the APKs first, then run Espresso on TestingBot
./gradlew assembleDebug assembleDebugAndroidTest testingbotEspresso

# Discover device names/versions for your capabilities
./gradlew testingbotDevices
```

### Device capabilities

`deviceName` accepts a regular expression, so `"Pixel.*"` matches any Pixel and `"*"` any
device. Commonly used keys: `deviceName`, `version`, `platformName` (`"Android"`),
`realDevice`, `phoneOnly`, `tabletOnly`. Run `./gradlew testingbotDevices` to see what's
available on your plan.

### Optional: Android Gradle Plugin auto-wiring

If the `com.android.application` plugin is applied, naming a variant lets the plugin fill in
the conventional APK paths and depend on the matching `assemble` tasks automatically:

```kotlin
testingbot {
    autoWireFromVariant.set("debug")   // defaults appApk/testApk + dependsOn assembleDebug(AndroidTest)
    capabilities.set(listOf(mapOf("deviceName" to "Pixel.*", "version" to "14", "platformName" to "Android")))
}
```

Now `./gradlew testingbotEspresso` builds the APKs and runs the tests in one go. Explicitly
set `appApk`/`testApk` paths always take precedence.

## Full DSL reference

| Property | Type | Default | Description |
|---|---|---|---|
| `key` | `String` | `TESTINGBOT_KEY` / `testingbot.key` | API key |
| `secret` | `String` | `TESTINGBOT_SECRET` / `testingbot.secret` | API secret |
| `apiBaseUrl` | `String` | `https://api.testingbot.com/v1` | API base URL |
| `appApk` | `RegularFile` | — | App APK to upload / test |
| `testApk` | `RegularFile` | — | Instrumented Espresso test APK |
| `capabilities` | `List<Map<String, Any>>` | `[]` | One run per entry; passed through verbatim |
| `espressoOptions` | `Map<String, Any>` | `{}` | Optional run options passed through verbatim |
| `waitForResults` | `Boolean` | `true` | Poll until the run completes |
| `pollIntervalSeconds` | `Long` | `15` | Seconds between status polls |
| `timeoutMinutes` | `Long` | `30` | Max minutes to wait for results |
| `failBuildOnTestFailure` | `Boolean` | `true` | Fail the build when a run reports failures |
| `reportXml` | `RegularFile` | `build/testingbot/espresso-junit.xml` | Where to write the JUnit report |
| `autoWireFromVariant` | `String` | — | Optional AGP variant to auto-wire |

## CI example (GitHub Actions)

```yaml
- uses: actions/setup-java@v4
  with: { distribution: temurin, java-version: 21 }
- run: ./gradlew assembleDebug assembleDebugAndroidTest testingbotEspresso
  env:
    TESTINGBOT_KEY: ${{ secrets.TESTINGBOT_KEY }}
    TESTINGBOT_SECRET: ${{ secrets.TESTINGBOT_SECRET }}
```

The JUnit report written to `reportXml` can be published with your CI's test-report tooling.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Security issues: see [SECURITY.md](SECURITY.md).

## License

[MIT](LICENSE) © TestingBot
