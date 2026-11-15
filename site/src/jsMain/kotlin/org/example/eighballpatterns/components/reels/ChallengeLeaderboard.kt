package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.attrsModifier
import com.varabyte.kobweb.silk.components.icons.fa.FaRotateRight
import com.varabyte.kobweb.silk.components.text.SpanText
import org.example.eighballpatterns.model.DailyLeaderboard
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Table
import org.jetbrains.compose.web.dom.Tbody
import org.jetbrains.compose.web.dom.Td
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Tr

@Composable
internal fun CompactChallengeLeaderboard(
    leaderboard: DailyLeaderboard?, playerUid: String?, busy: Boolean, error: String?,
    allTime: Boolean, onRefresh: () -> Unit,
) {
    Column(Modifier.attrsModifier { classes("daily-compact-standings"); attr("aria-busy",busy.toString()) }) {
        Row(Modifier.attrsModifier {classes("daily-compact-standings-heading")}, verticalAlignment=Alignment.CenterVertically) {
            SpanText(if(allTime) "Top challengers · total correct" else "Today's top challengers")
            Button(attrs={classes("daily-back");attr("aria-label","Refresh standings");if(busy)attr("disabled","");onClick {onRefresh()}}) { FaRotateRight() }
        }
        if(error!=null) P(attrs={classes("daily-incorrect");attr("role","alert")}) { Text(error) }
        if(leaderboard==null && busy) PoolLoading("Loading standings…")
        if(leaderboard!=null) {
            val top = leaderboard.entries.take(3)
            val rows = top + listOfNotNull(leaderboard.ownEntry?.takeIf { own -> top.none { it.uid==own.uid } })
            if(rows.isEmpty()) P {Text("Be the first challenger.")}
            Table(attrs={classes("daily-breakdown","daily-leaderboard");attr("aria-label",if(allTime) "All-time leaderboard" else "Daily leaderboard")}) {
                Tbody {
                    for(entry in rows) Tr(attrs={if(entry.uid==playerUid)classes("daily-own-result")}) {
                        Td {Text("#${entry.rank}")}
                        Td {Text(entry.displayName);if(entry.uid==playerUid)SpanText(" (you)")}
                        Td {Text(if(allTime) entry.correctCount.toString() else "${entry.correctCount}/${entry.shotCount}")}
                    }
                }
            }
        }
    }
}
