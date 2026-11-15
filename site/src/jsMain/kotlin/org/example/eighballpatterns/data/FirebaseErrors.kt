package org.example.eighballpatterns.data

import kotlinx.browser.window

internal fun firebaseErrorMessage(error: Throwable, subject: String = "daily challenge"): String =
    firebaseErrorMessageForCode(error.asDynamic().code as? String, error.message, subject, window.navigator.onLine)

/** A missing deployment or a server failure is not the same as being offline. */
internal fun firebaseErrorMessageForCode(code: String?, message: String?, subject: String = "daily challenge", online: Boolean = true): String = when (
    // The callable SDK can report a failed fetch as internal rather than unavailable.
    if (!online && code in setOf("functions/internal", "functions/unknown", "functions/unavailable", "auth/network-request-failed")) "functions/unavailable" else code
) {
    "auth/operation-not-allowed", "auth/configuration-not-found" -> "The daily challenge isn't available yet. Please try browsing."
    "auth/too-many-requests", "functions/resource-exhausted" -> "Too many requests. Please try again shortly."
    "functions/not-found" -> "The $subject isn't available yet. Please try again later."
    "functions/internal", "functions/unknown" -> "The $subject couldn't be loaded. Please try again."
    "functions/unavailable", "auth/network-request-failed" -> "Could not connect to the $subject. Please check your connection and try again."
    "functions/deadline-exceeded", "auth/timeout" -> "The $subject took too long to respond. Please try again."
    "functions/unauthenticated" -> "Your player session has expired. Please reload the page and try again."
    else -> message?.takeIf { it.isNotBlank() } ?: "Could not load the $subject. Please try again."
}
