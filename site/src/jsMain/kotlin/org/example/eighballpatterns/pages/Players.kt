package org.example.eighballpatterns.pages

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.core.Page

/** Shareable practice entry, independent of today's challenge completion. */
@Page("/players")
@Composable
fun PlayersPage() {
    EightBallReelsPage(startWithPlayerChoice = true)
}
