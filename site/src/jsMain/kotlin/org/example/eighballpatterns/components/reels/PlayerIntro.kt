package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.attrsModifier
import com.varabyte.kobweb.compose.ui.graphics.Color
import com.varabyte.kobweb.compose.ui.graphics.Colors
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxSize
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxWidth
import com.varabyte.kobweb.compose.ui.modifiers.flexShrink
import com.varabyte.kobweb.compose.ui.modifiers.fontSize
import com.varabyte.kobweb.compose.ui.modifiers.fontWeight
import com.varabyte.kobweb.compose.ui.modifiers.gap
import com.varabyte.kobweb.compose.ui.modifiers.height
import com.varabyte.kobweb.compose.ui.modifiers.lineHeight
import com.varabyte.kobweb.compose.ui.modifiers.margin
import com.varabyte.kobweb.compose.ui.modifiers.minHeight
import com.varabyte.kobweb.compose.ui.modifiers.padding
import com.varabyte.kobweb.compose.ui.modifiers.width
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.navigation.BasePath
import com.varabyte.kobweb.silk.components.icons.fa.FaUser
import com.varabyte.kobweb.silk.components.icons.fa.FaChevronDown
import com.varabyte.kobweb.silk.components.icons.fa.FaChevronUp
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.graphics.Image
import com.varabyte.kobweb.silk.components.layout.SimpleGrid
import com.varabyte.kobweb.silk.components.layout.numColumns
import com.varabyte.kobweb.silk.components.text.SpanText
import org.example.eighballpatterns.components.widgets.PoolBall
import org.example.eighballpatterns.model.EightBallShot
import org.example.eighballpatterns.model.PlayerProfile
import org.example.eighballpatterns.model.eightBallPlayerStatus
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

private val loadedPlayerPortraits = mutableSetOf<String>()

@Composable
internal fun EightBallPlayerIntro(shot: EightBallShot, player: PlayerProfile?, challenge: Boolean = false) {
    val imageUrl = player?.imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
        if (url.startsWith("https://") || url.startsWith("http://")) url else BasePath.prependTo(url)
    }
    var imageReady by remember(imageUrl) { mutableStateOf(imageUrl != null && imageUrl in loadedPlayerPortraits) }
    var imageFailed by remember(imageUrl) { mutableStateOf(false) }
    var detailsOpen by remember(shot.entry.id) { mutableStateOf(false) }
    val balls = if (shot.meta.isBreak) emptyList() else when (shot.meta.group) {
        0 -> (1..15).filter { it != 8 }
        1 -> (1..7).toList()
        2 -> (9..15).toList()
        else -> listOf(8)
    }

    SimpleGrid(
        numColumns(1),
        Modifier
            .fillMaxSize()
            .gap(12.px)
            .padding(leftRight = 24.px, top = 16.px, bottom = 18.px)
            .styleModifier {
                property("grid-template-rows", "minmax(0, 1fr) auto")
                property("box-sizing", "border-box")
                property("padding-top", if (challenge) "calc(104px + env(safe-area-inset-top))" else "calc(78px + env(safe-area-inset-top))")
                property("padding-bottom", "max(18px, env(safe-area-inset-bottom))")
            }
            .attrsModifier {
                classes("reel-player-intro")
                attr("role", "article")
                attr("aria-label", "Player introduction")
            },
    ) {
        Box(Modifier.fillMaxSize().minHeight(0.px), contentAlignment = Alignment.Center) {
            Column(
                Modifier.fillMaxWidth().gap(12.px).styleModifier { property("max-height", "100%") },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.fillMaxWidth().minHeight(0.px).styleModifier {
                        property("height", if(detailsOpen) "min(24dvh,200px)" else "min(38dvh,310px)")
                        property("flex-shrink", "1")
                        property("position", "relative")
                    }.attrsModifier {classes("player-portrait")},
                    contentAlignment = Alignment.Center,
                ) {
                    if (!imageReady || imageFailed) {
                        FaUser(
                            Modifier.fontSize(5.cssRem).color(Color.rgb(0x94C9B5))
                                .attrsModifier { attr("aria-hidden", "true") },
                        )
                    }
                    if (imageUrl != null && !imageFailed) {
                        Img(
                            src = imageUrl,
                            alt = shot.meta.player,
                            attrs = Modifier.fillMaxSize()
                                .styleModifier {
                                    property("min-height", "0")
                                    property("position", "absolute")
                                    property("inset", "0")
                                    property("object-fit", "contain")
                                    property("object-position", player.imagePosition)
                                    property("opacity", if (imageReady) "1" else "0")
                                    property("pointer-events", "none")
                                }
                                .toAttrs {
                                    attr("draggable", "false")
                                    attr("referrerpolicy", "no-referrer")
                                    addEventListener("load") {
                                        loadedPlayerPortraits.add(imageUrl)
                                        imageReady = true
                                    }
                                    ref { image ->
                                        if (image.complete && image.naturalWidth > 0) {
                                            loadedPlayerPortraits.add(imageUrl)
                                            imageReady = true
                                        }
                                        onDispose {}
                                    }
                                    addEventListener("error") { imageFailed = true }
                                },
                        )
                    }
                }

                Column(
                    Modifier.fillMaxWidth().gap(12.px)
                        .styleModifier { property("flex-shrink", "0") },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(Modifier.fillMaxWidth().gap(10.px).styleModifier {property("justify-content","center");property("flex-wrap","wrap")}, verticalAlignment = Alignment.CenterVertically) {
                        H2(
                            Modifier.margin(0.px).fontSize(1.85.cssRem).fontWeight(FontWeight.Bold)
                                .lineHeight(1.2).color(Colors.White)
                                .styleModifier { property("text-align", "center") }
                                .toAttrs()
                        ) {
                            Text(shot.meta.player)
                        }
                        if (player != null && player.countryFlagUrl.isNotBlank() && player.country.isNotBlank()) {
                            Image(
                                src = BasePath.prependTo(player.countryFlagUrl),
                                alt = "${player.country} flag",
                                modifier = Modifier.width(32.px).height(24.px).flexShrink(0)
                                    .attrsModifier { attr("title", player.country) },
                            )
                        }
                    }
                    Column(Modifier.gap(6.px), horizontalAlignment = Alignment.CenterHorizontally) {
                        SpanText(
                            eightBallPlayerStatus(shot.meta),
                            Modifier.fontSize(.85.cssRem).color(Color.rgb(0xB8DCC6))
                                .padding(leftRight=14.px,topBottom=7.px)
                                .styleModifier {property("background","#173323");property("border-radius","999px")},
                        )
                        if (shot.meta.isBreak) {
                            P(Modifier.margin(0.px).color(Colors.White).toAttrs()) {
                                Text("How many balls will drop?")
                            }
                        }
                        if (shot.meta.ballInHand) {
                            SpanText("Ball in hand", Modifier.fontSize(1.cssRem).fontWeight(FontWeight.Bold)
                                .color(Color.rgb(0x94C9B5)))
                        }
                        balls.chunked(7).forEach { row ->
                            Row(Modifier.gap(6.px)) {
                                row.forEach { ball ->
                                    Box(
                                        Modifier.width(28.px).height(28.px).attrsModifier {
                                            attr("role", "img")
                                            attr("aria-label", "$ball ball")
                                        }
                                    ) {
                                        PoolBall(ball, fillParent = true, colorOverride = shot.meta.ballColors[ball]?.let { Color.rgb(it) })
                                    }
                                }
                            }
                        }
                    }
                    Button(onClick={detailsOpen=!detailsOpen},modifier=Modifier.attrsModifier {
                        classes("reel-subtle-button");attr("aria-expanded",detailsOpen.toString());attr("aria-controls","player-more-details")}) {
                        Text(if(detailsOpen) "Less detail" else "More details")
                        if(detailsOpen) FaChevronUp() else FaChevronDown()
                    }
                    if(detailsOpen) Column(Modifier.fillMaxWidth().gap(7.px).attrsModifier {
                        id("player-more-details");classes("player-more-details")
                    },horizontalAlignment=Alignment.CenterHorizontally) {
                        player?.fargoRating?.let { rating ->
                            Row(Modifier.gap(7.px),verticalAlignment=Alignment.CenterVertically) {
                                SpanText("Fargo Rating",Modifier.fontSize(.85.cssRem))
                                SpanText(rating.toString(),Modifier.fontSize(1.35.cssRem).fontWeight(FontWeight.Bold).color(Color.rgb(0x94C9B5)))
                            }
                            SpanText(listOf(player.fargoSource,player.fargoDate).filter {it.isNotBlank()}.joinToString(" · "),
                                Modifier.fontSize(.65.cssRem).color(Color.rgba(255,255,255,.45f)))
                        }
                        shot.meta.matchYear?.let {year ->
                            SpanText("Match · $year",Modifier.fontSize(.75.cssRem).color(Color.rgba(255,255,255,.55f)))
                        }
                        if (imageReady && !imageFailed && !player?.imageSourceUrl.isNullOrBlank()) {
                        A(
                            href = player.imageSourceUrl,
                            attrs = Modifier.fontSize(0.65.cssRem).color(Color.rgba(255, 255, 255, 0.5f))
                                .toAttrs {
                                    attr("target", "_blank")
                                    attr("rel", "noopener noreferrer")
                                },
                        ) {
                            Text("Photo: ${player.imageCredit}")
                        }
                        }
                    }
                }
            }
        }

        Box(Modifier.fillMaxWidth().height(62.px), contentAlignment = Alignment.Center) {
            EightBallReelSwipeHint("Swipe up to the shot")
        }
    }
}
