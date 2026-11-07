package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.css.Overflow
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.attrsModifier
import com.varabyte.kobweb.compose.ui.graphics.Color
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.backgroundColor
import com.varabyte.kobweb.compose.ui.modifiers.border
import com.varabyte.kobweb.compose.ui.modifiers.borderRadius
import com.varabyte.kobweb.compose.ui.modifiers.flexGrow
import com.varabyte.kobweb.compose.ui.modifiers.height
import com.varabyte.kobweb.compose.ui.modifiers.width
import com.varabyte.kobweb.compose.ui.modifiers.maxWidth
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxSize
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxWidth
import com.varabyte.kobweb.compose.ui.modifiers.fontSize
import com.varabyte.kobweb.compose.ui.modifiers.fontWeight
import com.varabyte.kobweb.compose.ui.modifiers.gap
import com.varabyte.kobweb.compose.ui.modifiers.minHeight
import com.varabyte.kobweb.compose.ui.modifiers.overflow
import com.varabyte.kobweb.compose.ui.modifiers.padding
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.icons.fa.FaUser
import com.varabyte.kobweb.silk.components.icons.fa.FaShuffle
import com.varabyte.kobweb.silk.components.icons.fa.FaChevronRight
import com.varabyte.kobweb.silk.components.text.SpanText
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.px

@Composable
internal fun EightBallPlayerChoice(players: List<String>, onSelected: (String?) -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().minHeight(0.px).overflow(Overflow.Auto)
            .padding(topBottom = 64.px, leftRight = 24.px).gap(16.px)
            .attrsModifier { attr("aria-label", "Choose a player") },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SpanText("Choose a player", Modifier.fontSize(1.8.cssRem).fontWeight(FontWeight.Bold))
        SpanText("Watch everyone, or follow one player's shots.",
            Modifier.fontSize(0.9.cssRem).color(Color.rgb(0x94C9B5)))
        Column(Modifier.fillMaxWidth().maxWidth(480.px).gap(12.px)) {
            PlayerChoiceCard("Any player", "Mix it up · All available players", anyPlayer = true) { onSelected(null) }
            for (name in players) {
                PlayerChoiceCard(name, "Follow this player's shots") { onSelected(name) }
            }
        }
        Button(onClick = { onBack() }, modifier = Modifier.padding(12.px)
            .backgroundColor(Color.rgb(0x101C17)).color(Color.rgb(0x94C9B5)).borderRadius(12.px)) { SpanText("Back") }
    }
}

@Composable
private fun PlayerChoiceCard(name: String, detail: String, anyPlayer: Boolean = false, onClick: () -> Unit) {
    val mint = Color.rgb(0x94C9B5)
    Button(onClick = { onClick() }, modifier = Modifier.fillMaxWidth().minHeight(82.px)
        .padding(16.px).borderRadius(18.px)
        .backgroundColor(Color.rgb(if (anyPlayer) 0x142D23 else 0x101C17))
        .border(1.px, LineStyle.Solid, if (anyPlayer) mint else Color.rgb(0x2F493E))
        .color(Color.rgb(0xF5FAF7))
        .attrsModifier { classes("player-choice-card"); attr("aria-label", if (anyPlayer) "Any player" else "Watch $name") }) {
        Row(Modifier.fillMaxWidth().gap(14.px), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(44.px).height(44.px).borderRadius(50.percent)
                .backgroundColor(Color.rgb(0x203F32)).color(mint), contentAlignment = Alignment.Center) {
                if (anyPlayer) FaShuffle() else FaUser()
            }
            Column(Modifier.flexGrow(1).gap(5.px), horizontalAlignment = Alignment.Start) {
                SpanText(name, Modifier.fontSize(1.cssRem).fontWeight(FontWeight.Bold))
                SpanText(detail, Modifier.fontSize(0.75.cssRem).fontWeight(FontWeight.Normal).color(mint))
            }
            FaChevronRight(Modifier.color(mint).fontSize(0.8.cssRem))
        }
    }
}
