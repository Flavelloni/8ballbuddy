package org.example.eighballpatterns.browser

import kotlinx.browser.window

/** Only identities whose name we saw being generated receive the first-result explanation. */
internal class ChallengeNameHintStore(private val read: (String) -> String?, private val write: (String, String) -> Unit) {
    private fun key(uid: String) = "challenge-name-hint-v1:$uid"
    fun assigned(uid: String) { if (read(key(uid)) == null) write(key(uid), "pending") }
    fun pending(uid: String?): Boolean = uid != null && read(key(uid)) == "pending"
    fun seen(uid: String) = write(key(uid), "seen")
}

private val nameHints = ChallengeNameHintStore(
    { key -> try { window.localStorage.getItem(key) } catch (_: Throwable) { null } },
    { key, value -> try { window.localStorage.setItem(key, value) } catch (_: Throwable) { } },
)

internal fun markAutomaticChallengeName(uid: String) = nameHints.assigned(uid)
internal fun hasAutomaticChallengeNameHint(uid: String?) = nameHints.pending(uid)
internal fun markChallengeNameHintSeen(uid: String) = nameHints.seen(uid)
