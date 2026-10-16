package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.graphics.Color
import com.varabyte.kobweb.compose.ui.modifiers.backgroundColor
import com.varabyte.kobweb.compose.ui.modifiers.border
import com.varabyte.kobweb.compose.ui.modifiers.borderRadius
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.fontSize
import com.varabyte.kobweb.compose.ui.modifiers.fontWeight
import com.varabyte.kobweb.compose.ui.modifiers.lineHeight
import com.varabyte.kobweb.compose.ui.modifiers.padding
import com.varabyte.kobweb.compose.ui.modifiers.position
import com.varabyte.kobweb.compose.ui.modifiers.zIndex
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.silk.components.icons.fa.FaHeart
import com.varabyte.kobweb.silk.components.icons.fa.FaShare
import com.varabyte.kobweb.silk.components.icons.fa.IconStyle
import org.example.eighballpatterns.model.EightBallReelStats
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
internal fun EightBallReelActions(
    stats: EightBallReelStats,
    liked: Boolean,
    likesLabel: String,
    onLike: () -> Unit,
    onShare: () -> Unit,
) {
    Column(
        Modifier
            .position(Position.Absolute)
            .zIndex(5)
            .styleModifier {
                property("right", "12px")
                property("top", "50%")
                property("transform", "translateY(-50%)")
                property("gap", "18px")
                property("align-items", "center")
                property("pointer-events", "auto")
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EightBallReelStatsBadge(stats)
        EightBallReelActionButton(
            label = likesLabel,
            color = if (liked) "#ff405f" else "rgba(255,255,255,.94)",
            ariaLabel = "Like shot",
            onClick = onLike,
        ) {
            FaHeart(style = if (liked) IconStyle.FILLED else IconStyle.OUTLINE)
        }
        EightBallReelActionButton(
            label = "Share",
            color = "rgba(255,255,255,.94)",
            ariaLabel = "Share shot",
            onClick = onShare,
        ) {
            FaShare()
        }
    }
}

@Composable
private fun EightBallReelStatsBadge(stats: EightBallReelStats) {
    Column(
        Modifier
            .padding(leftRight = 0.55.cssRem, topBottom = 0.42.cssRem)
            .borderRadius(10.px)
            .backgroundColor(Color.rgba(0, 0, 0, 0.42f))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.14f))
            .styleModifier {
                property("min-width", "58px")
                property("box-shadow", "0 12px 28px rgba(0,0,0,.32)")
                property("backdrop-filter", "blur(8px)")
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Span(
            attrs = Modifier
                .fontSize(0.72.cssRem)
                .fontWeight(FontWeight.Bold)
                .lineHeight(1.15)
                .color(Color.rgba(255, 255, 255, 0.9f))
                .toAttrs()
        ) {
            Text("${stats.correct}/${stats.answered}")
        }
        Span(
            attrs = Modifier
                .fontSize(0.62.cssRem)
                .lineHeight(1.15)
                .color(Color.rgba(255, 255, 255, 0.62f))
                .toAttrs()
        ) {
            Text("Streak ${stats.streak}")
        }
        if (stats.bestStreak > 1) {
            Span(
                attrs = Modifier
                    .fontSize(0.62.cssRem)
                    .lineHeight(1.15)
                    .color(Color.rgba(126, 218, 116, 0.82f))
                    .toAttrs()
            ) {
                Text("Best ${stats.bestStreak}")
            }
        }
    }
}

@Composable
private fun EightBallReelActionButton(
    label: String,
    color: String,
    ariaLabel: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            attrs = Modifier
                .styleModifier {
                    property("width", "48px")
                    property("height", "48px")
                    property("padding", "0")
                    property("border", "1px solid rgba(255,255,255,.18)")
                    property("border-radius", "999px")
                    property("background", "rgba(0,0,0,.38)")
                    property("color", color)
                    property("font-size", "1.72rem")
                    property("line-height", "1")
                    property("cursor", "pointer")
                    property("display", "flex")
                    property("align-items", "center")
                    property("justify-content", "center")
                    property("text-shadow", "0 8px 18px rgba(0,0,0,.74)")
                    property("backdrop-filter", "blur(8px)")
                }
                .toAttrs {
                    attr("aria-label", ariaLabel)
                    title(ariaLabel)
                    onClick { onClick() }
                }
        ) {
            icon()
        }
        Span(
            attrs = Modifier
                .styleModifier {
                    property("margin-top", "5px")
                    property("color", "rgba(255,255,255,.9)")
                    property("font-size", ".72rem")
                    property("font-weight", "750")
                    property("line-height", "1")
                    property("text-shadow", "0 6px 14px rgba(0,0,0,.72)")
                }
                .toAttrs()
        ) {
            Text(label)
        }
    }
}

internal fun eightBallReelLikeLabel(shotId: String): String {
    var value = 0
    shotId.forEach { char ->
        value = (value * 31 + char.code) % 76000
    }
    val count = 3600 + value
    return if (count < 10000) {
        count.toString()
    } else {
        "${count / 1000}.${(count % 1000) / 100}k"
    }
}
