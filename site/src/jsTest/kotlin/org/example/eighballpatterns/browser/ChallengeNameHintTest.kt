package org.example.eighballpatterns.browser

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChallengeNameHintTest {
    @Test fun assignmentHintSurvivesReloadButIsShownOnlyOncePerIdentity() {
        val saved = mutableMapOf<String, String>()
        fun store() = ChallengeNameHintStore({ saved[it] }, { key, value -> saved[key] = value })
        assertFalse(store().pending(null))
        assertFalse(store().pending("existing"))
        store().assigned("new")
        assertTrue(store().pending("new"))
        store().seen("new")
        store().assigned("new")
        assertFalse(store().pending("new"))
        assertFalse(store().pending("other"))
    }
}
