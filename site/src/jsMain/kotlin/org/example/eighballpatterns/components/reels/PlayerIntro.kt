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
import com.varabyte.kobweb.silk.components.layout.SimpleGrid
import com.varabyte.kobweb.silk.components.layout.numColumns
import com.varabyte.kobweb.silk.components.text.SpanText
import org.example.eighballpatterns.components.widgets.PoolBall
import org.example.eighballpatterns.model.EightBallShot
import org.example.eighballpatterns.model.PlayerProfile
import org.example.eighballpatterns.model.eightBallReelTitle
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Text

@Composable
internal fun EightBallPlayerIntro(shot: EightBallShot, player: PlayerProfile?) {
    val imageUrl = player?.imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
        if (url.startsWith("https://") || url.startsWith("http://")) url else BasePath.prependTo(url)
    }
    var imageReady by remember(imageUrl) { mutableStateOf(false) }
    var imageFailed by remember(imageUrl) { mutableStateOf(false) }
    val initials = shot.meta.player.trim().split(Regex("\\s+"))
        .filter { it.isNotEmpty() }.take(2).joinToString("") { it.take(1) }.uppercase()
    val balls = when (shot.meta.group) {
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
                property("padding-top", "max(16px, env(safe-area-inset-top))")
                property("padding-bottom", "max(18px, env(safe-area-inset-bottom))")
            }
            .attrsModifier {
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
                        property("aspect-ratio", "1")
                        property("flex-shrink", "1")
                        property("position", "relative")
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    if (!imageReady || imageFailed) {
                        SpanText(
                            initials,
                            Modifier.fontSize(5.cssRem).fontWeight(FontWeight.Bold).color(Color.rgb(0x94C9B5)),
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
                                    addEventListener("load") { imageReady = true }
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
                    H2(
                        Modifier.margin(0.px).fontSize(1.65.cssRem).fontWeight(FontWeight.Bold)
                            .lineHeight(1.2).color(Colors.White)
                            .styleModifier { property("text-align", "center") }
                            .toAttrs()
                    ) {
                        Text(eightBallReelTitle(shot.meta))
                    }
                    Column(Modifier.gap(6.px), horizontalAlignment = Alignment.CenterHorizontally) {
                        balls.chunked(7).forEach { row ->
                            Row(Modifier.gap(6.px)) {
                                row.forEach { ball ->
                                    Box(
                                        Modifier.width(28.px).height(28.px).attrsModifier {
                                            attr("role", "img")
                                            attr("aria-label", "$ball ball")
                                        }
                                    ) {
                                        PoolBall(ball, fillParent = true)
                                    }
                                }
                            }
                        }
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

        Box(Modifier.fillMaxWidth().height(76.px), contentAlignment = Alignment.Center) {
            EightBallReelSwipeHint("Swipe to the shot")
        }
    }
}
