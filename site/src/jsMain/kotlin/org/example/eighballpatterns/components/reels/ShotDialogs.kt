package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.attrsModifier
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxWidth
import com.varabyte.kobweb.compose.ui.modifiers.flexWrap
import com.varabyte.kobweb.compose.ui.modifiers.gap
import com.varabyte.kobweb.compose.ui.modifiers.margin
import com.varabyte.kobweb.compose.ui.modifiers.position
import com.varabyte.kobweb.compose.ui.modifiers.width
import com.varabyte.kobweb.compose.ui.modifiers.zIndex
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.navigation.BasePath
import com.varabyte.kobweb.silk.components.icons.fa.FaCompress
import org.example.eighballpatterns.browser.eightBallReelsShareUrl
import org.example.eighballpatterns.browser.openEightBallEmailShare
import org.example.eighballpatterns.browser.openEightBallWhatsAppShare
import org.example.eighballpatterns.model.EightBallShot
import org.example.eighballpatterns.model.eightBallReelTitle
import org.jetbrains.compose.web.css.FlexWrap
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

@Composable
internal fun EightBallShareSheet(
    shot: EightBallShot,
    copied: Boolean,
    onCopy: () -> Unit,
    onClose: () -> Unit,
) {
    val shareUrl = eightBallReelsShareUrl(shot)
    Row(
        modifier = Modifier
            .position(Position.Fixed)
            .zIndex(10001)
            .styleModifier {
                property("inset", "0")
                property("display", "flex")
                property("align-items", "end")
                property("justify-content", "center")
                property("background", "rgba(0,0,0,.58)")
                property("padding", "20px")
                property("box-sizing", "border-box")
            }
            .attrsModifier {
                onClick { onClose() }
            }
    ) {
        Column(
            modifier = Modifier
                .width(100.percent)
                .styleModifier {
                    property("max-width", "420px")
                    property("align-items", "stretch")
                    property("border-radius", "22px")
                    property("background", "#101414")
                    property("border", "1px solid rgba(255,255,255,.12)")
                    property("box-shadow", "0 28px 90px rgba(0,0,0,.56)")
                    property("padding", "18px")
                    property("box-sizing", "border-box")
                }
                .attrsModifier {
                    onClick { it.stopPropagation() }
                }
        ) {
            P(
                attrs = Modifier
                    .margin(0.px)
                    .styleModifier {
                        property("color", "rgba(255,255,255,.92)")
                        property("font-size", ".98rem")
                        property("font-weight", "750")
                        property("line-height", "1.3")
                    }
                    .toAttrs()
            ) {
                Text(eightBallReelTitle(shot.meta))
            }
            P(
                attrs = Modifier
                    .margin(top = 0.38.cssRem, bottom = 1.cssRem)
                    .styleModifier {
                        property("color", "rgba(255,255,255,.58)")
                        property("font-size", ".78rem")
                        property("line-height", "1.35")
                        property("overflow-wrap", "anywhere")
                    }
                    .toAttrs()
            ) {
                Text(shareUrl)
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .gap(0.6.cssRem)
                    .flexWrap(FlexWrap.Wrap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EightBallShareButton("WhatsApp") {
                    openEightBallWhatsAppShare(shot)
                }
                EightBallShareButton("Email") {
                    openEightBallEmailShare(shot)
                }
                EightBallShareButton(if (copied) "Copied" else "Copy") {
                    onCopy()
                }
            }
        }
    }
}

@Composable
private fun EightBallShareButton(label: String, onClick: () -> Unit) {
    Button(
        attrs = Modifier
            .styleModifier {
                property("flex", "1 1 104px")
                property("min-height", "42px")
                property("border", "1px solid rgba(255,255,255,.14)")
                property("border-radius", "999px")
                property("background", "rgba(255,255,255,.08)")
                property("color", "white")
                property("font-weight", "750")
                property("cursor", "pointer")
            }
            .toAttrs {
                onClick { onClick() }
            }
    ) {
        Text(label)
    }
}

@Composable
internal fun EightBallFullscreenImage(
    shot: EightBallShot?,
    onClose: () -> Unit,
) {
    if (shot == null) return

    Row(
        modifier = Modifier
            .position(Position.Fixed)
            .styleModifier {
                property("inset", "0")
                property("width", "100vw")
                property("height", "100vh")
                property("background", "#000")
                property("z-index", "10000")
                property("display", "flex")
                property("align-items", "center")
                property("justify-content", "center")
                property("overflow", "hidden")
            }
    ) {
        Img(
            src = BasePath.prependTo(shot.entry.imagePath),
            attrs = Modifier
                .styleModifier {
                    property("width", "min(100dvh, 177.777vw)")
                    property("height", "auto")
                    property("max-width", "none")
                    property("max-height", "none")
                    property("transform", "rotate(90deg)")
                    property("user-select", "none")
                    property("-webkit-user-select", "none")
                    property("-webkit-user-drag", "none")
                }
                .toAttrs {
                    attr("alt", "8-ball layout")
                    attr("draggable", "false")
                }
        )

        Button(
            attrs = Modifier
                .position(Position.Absolute)
                .styleModifier {
                    property("right", "18px")
                    property("top", "18px")
                    property("width", "48px")
                    property("height", "48px")
                    property("padding", "0")
                    property("border", "none")
                    property("border-radius", "50%")
                    property("background", "rgba(0,0,0,0.65)")
                    property("color", "white")
                    property("font-size", "28px")
                    property("cursor", "pointer")
                    property("display", "flex")
                    property("align-items", "center")
                    property("justify-content", "center")
                }
                .toAttrs {
                    attr("aria-label", "Close fullscreen image")
                    title("Close fullscreen image")
                    onClick { onClose() }
                }
        ) {
            FaCompress()
        }
    }
}
