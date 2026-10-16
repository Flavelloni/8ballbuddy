package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.attrsModifier
import com.varabyte.kobweb.silk.components.icons.fa.FaChevronDown
import com.varabyte.kobweb.silk.components.icons.fa.FaRotateRight
import com.varabyte.kobweb.silk.components.text.SpanText
import org.example.eighballpatterns.model.DailyLeaderboard
import org.example.eighballpatterns.model.formatChallengeTime
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Table
import org.jetbrains.compose.web.dom.Tbody
import org.jetbrains.compose.web.dom.Td
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Th
import org.jetbrains.compose.web.dom.Thead
import org.jetbrains.compose.web.dom.Tr

@Composable
internal fun EightBallLeaderboard(
    leaderboard: DailyLeaderboard?,
    playerUid: String?,
    busy: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
) {
    Column(Modifier.attrsModifier { classes("daily-standings"); attr("aria-busy", busy.toString()) }) {
        Row(Modifier.attrsModifier { classes("daily-standings-heading") }, verticalAlignment = Alignment.CenterVertically) {
            H3 { Text("Today's standings") }
            Button(attrs = {
                classes("daily-back")
                attr("aria-label", "Refresh standings")
                title("Refresh standings")
                if (busy) attr("disabled", "")
                onClick { onRefresh() }
            }) { FaRotateRight() }
        }
        if (leaderboard?.ownRank != null) {
            P { Text("You placed #${leaderboard.ownRank} of ${leaderboard.total}") }
        } else if (leaderboard != null) {
            P(attrs = { classes("daily-muted") }) { Text("${leaderboard.total} finishers") }
        }
        if (error != null) {
            P(attrs = { classes("daily-incorrect"); attr("role", "alert") }) { Text(error) }
        }
        if (leaderboard == null && busy) {
            P(attrs = { classes("daily-muted"); attr("role", "status") }) { Text("Loading standings...") }
        }
        if (leaderboard != null && leaderboard.entries.isEmpty()) {
            P(attrs = { classes("daily-muted") }) { Text("No finishers yet.") }
        }
        if (leaderboard?.entries?.isNotEmpty() == true) {
            Table(attrs = { classes("daily-breakdown", "daily-leaderboard"); attr("aria-label", "Daily leaderboard") }) {
                Thead {
                    Tr {
                        listOf("#", "Player", "Score", "Time").forEach { heading ->
                            Th(attrs = { attr("scope", "col") }) { Text(heading) }
                        }
                    }
                }
                Tbody {
                    leaderboard.entries.forEach { entry ->
                        val own = entry.uid == playerUid
                        Tr(attrs = { if (own) classes("daily-own-result") }) {
                            Td { Text(entry.rank.toString()) }
                            Td {
                                Text(entry.displayName)
                                if (own) SpanText(" (you)", Modifier.attrsModifier { classes("daily-own-label") })
                            }
                            Td { Text("${entry.correctCount}/${entry.shotCount}") }
                            Td { Text(formatChallengeTime(entry.totalElapsedMs)) }
                        }
                    }
                }
            }
        }
        if (leaderboard?.nextAfterUid != null) {
            Button(attrs = {
                classes("daily-command")
                attr("aria-label", "Load more finishers")
                if (busy) attr("disabled", "")
                onClick { onLoadMore() }
            }) {
                Text(if (busy) "Loading..." else "More finishers")
                FaChevronDown()
            }
        }
    }
}
