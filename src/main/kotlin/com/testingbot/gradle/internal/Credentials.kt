package com.testingbot.gradle.internal

import org.gradle.api.GradleException

/**
 * Validates that both credentials are present, throwing a helpful [GradleException] otherwise.
 * Resolution order is handled upstream via property conventions (DSL → Gradle property → env var).
 */
internal fun requireCredentials(key: String?, secret: String?): Pair<String, String> {
    if (key.isNullOrBlank() || secret.isNullOrBlank()) {
        throw GradleException(
            """
            |TestingBot credentials are missing. Provide them in one of these ways:
            |  • DSL:               testingbot { key.set("..."); secret.set("...") }
            |  • Gradle properties: -Ptestingbot.key=... -Ptestingbot.secret=...
            |  • Environment:       TESTINGBOT_KEY / TESTINGBOT_SECRET
            |Find your key and secret at https://testingbot.com/members/user/api
            """.trimMargin(),
        )
    }
    return key to secret
}
