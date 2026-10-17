package org.example.eighballpatterns.data

internal fun currentChallengeDate(): String = js("new Date().toISOString().slice(0, 10)") as String
