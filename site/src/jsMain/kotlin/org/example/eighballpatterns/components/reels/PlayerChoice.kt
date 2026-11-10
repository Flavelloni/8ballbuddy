package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.varabyte.kobweb.compose.ui.modifiers.flexShrink
import com.varabyte.kobweb.compose.ui.modifiers.opacity
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
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.navigation.BasePath
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.graphics.Image
import com.varabyte.kobweb.silk.components.icons.fa.FaUser
import com.varabyte.kobweb.silk.components.icons.fa.FaShuffle
import com.varabyte.kobweb.silk.components.icons.fa.FaChevronRight
import com.varabyte.kobweb.silk.components.text.SpanText
import org.example.eighballpatterns.model.PlayerProfile
import org.example.eighballpatterns.model.orderPlayerChoices
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Text

@Composable
internal fun EightBallPlayerChoice(players: List<String>, profiles: Map<String, PlayerProfile>,
    onSelected: (String?) -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().minHeight(0.px).overflow(Overflow.Auto)
            .gap(16.px)
            .attrsModifier { classes("reel-player-choice");attr("aria-label", "Choose a player") },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        H2 { Text("Choose a player") }
        Column(Modifier.fillMaxWidth().maxWidth(480.px).gap(12.px)) {
            PlayerChoiceCard("Any player", null, anyPlayer = true) { onSelected(null) }
            for (name in orderPlayerChoices(players, profiles)) {
                val profile = profiles[name.trim().lowercase()]
                val detail = profile?.fargoRating?.let { "Fargo $it" }
                PlayerChoiceCard(name, detail, profile = profile) { onSelected(name) }
            }
        }
        Button(onClick = { onBack() }, modifier = Modifier.attrsModifier {classes("reel-subtle-button")}) { SpanText("Back to choices") }
    }
}

@Composable
private fun PlayerChoiceCard(name: String, detail: String?, anyPlayer: Boolean = false,
    profile: PlayerProfile? = null, onClick: () -> Unit) {
    val mint = Color.rgb(0x94C9B5)
    val imageUrl = profile?.imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
        if (url.startsWith("https://") || url.startsWith("http://")) url else BasePath.prependTo(url)
    }
    var imageReady by remember(imageUrl) { mutableStateOf(false) }
    var imageFailed by remember(imageUrl) { mutableStateOf(false) }
    Button(onClick = { onClick() }, modifier = Modifier.fillMaxWidth().minHeight(78.px)
        .padding(15.px).borderRadius(18.px)
        .backgroundColor(Color.rgb(if (anyPlayer) 0x142D23 else 0x101C17))
        .border(1.px, LineStyle.Solid, Color.rgb(if(anyPlayer) 0x4E7960 else 0x233D2D))
        .color(Color.rgb(0xF5FAF7))
        .attrsModifier { classes("player-choice-card"); attr("aria-label", if (anyPlayer) "Any player" else "Watch $name") }) {
        Row(Modifier.fillMaxWidth().gap(14.px), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(44.px).height(44.px).borderRadius(50.percent)
                .flexShrink(0).overflow(Overflow.Hidden)
                .backgroundColor(Color.rgb(0x203F32)).color(mint), contentAlignment = Alignment.Center) {
                if (anyPlayer) {
                    FaShuffle()
                } else {
                    if (!imageReady || imageFailed) FaUser()
                    if (imageUrl != null && !imageFailed) {
                        Image(src = imageUrl, alt = name,
                            modifier = Modifier.fillMaxSize().opacity(if (imageReady) 1 else 0)
                                .styleModifier {
                                    property("object-fit", "cover")
                                    property("object-position", profile.imagePosition)
                                }
                                .attrsModifier {
                                    attr("draggable", "false")
                                    attr("referrerpolicy", "no-referrer")
                                    addEventListener("load") { imageReady = true }
                                    addEventListener("error") { imageFailed = true }
                                })
                    }
                }
            }
            Column(Modifier.flexGrow(1).gap(5.px).styleModifier {property("min-width","0")}, horizontalAlignment = Alignment.Start) {
                SpanText(name, Modifier.fontSize(1.cssRem).fontWeight(FontWeight.Bold))
                if (detail != null) SpanText(detail, Modifier.fontSize(0.75.cssRem).fontWeight(FontWeight.Normal).color(mint))
            }
            FaChevronRight(Modifier.color(mint).fontSize(0.8.cssRem))
        }
    }
}
