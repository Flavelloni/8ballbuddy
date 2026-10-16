package org.example.eighballpatterns.model

internal fun normalizePlayerAlias(value: String): String =
    (value.asDynamic().normalize("NFKC") as String).trim().replace(Regex("\\s+"), " ")

internal fun hasValidPlayerAliasLength(value: String): Boolean =
    (js("Array.from(value).length") as Int) in 3..20
