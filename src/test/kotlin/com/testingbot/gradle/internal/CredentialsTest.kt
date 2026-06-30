package com.testingbot.gradle.internal

import org.gradle.api.GradleException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class CredentialsTest {
    @Test
    fun `returns the pair when both are present`() {
        assertEquals("k" to "s", requireCredentials("k", "s"))
    }

    @Test
    fun `throws when key is missing`() {
        assertThrows(GradleException::class.java) { requireCredentials(null, "s") }
    }

    @Test
    fun `throws when secret is blank`() {
        assertThrows(GradleException::class.java) { requireCredentials("k", "  ") }
    }
}
