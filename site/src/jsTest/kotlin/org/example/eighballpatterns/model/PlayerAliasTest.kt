package org.example.eighballpatterns.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerAliasTest {
    @Test
    fun normalizesBeforeCheckingLength() {
        assertEquals("Pool Fan_42", normalizePlayerAlias("  Pool   Fan_42  "))
        assertEquals("Pool", normalizePlayerAlias("\uff30\uff4f\uff4f\uff4c"))
        assertFalse(hasValidPlayerAliasLength(normalizePlayerAlias("  ab  ")))
        assertTrue(hasValidPlayerAliasLength(normalizePlayerAlias("  abc  ")))
    }

    @Test
    fun usesCharacterCountsAtBothBoundaries() {
        assertFalse(hasValidPlayerAliasLength("ab"))
        assertTrue(hasValidPlayerAliasLength("abc"))
        assertTrue(hasValidPlayerAliasLength("a".repeat(20)))
        assertFalse(hasValidPlayerAliasLength("a".repeat(21)))
        assertFalse(hasValidPlayerAliasLength("\ud801\udc00\ud801\udc01"))
        assertTrue(hasValidPlayerAliasLength("\ud801\udc00\ud801\udc01\ud801\udc02"))
    }
}
