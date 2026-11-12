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
import com.varabyte.kobweb.silk.components.icons.fa.FaWhatsapp
import com.varabyte.kobweb.silk.components.icons.fa.FaEnvelope
import com.varabyte.kobweb.silk.components.icons.fa.FaLink
import com.varabyte.kobweb.silk.components.icons.fa.FaCheck
import com.varabyte.kobweb.silk.components.icons.fa.FaXmark
import com.varabyte.kobweb.silk.components.forms.Button as SilkButton
import com.varabyte.kobweb.silk.components.text.SpanText
import com.varabyte.kobweb.compose.ui.modifiers.padding
import com.varabyte.kobweb.compose.ui.modifiers.height
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
                    property("background", "#102017")
                    property("border", "1px solid #46624d")
                    property("box-shadow", "0 28px 90px rgba(0,0,0,.56)")
                    property("padding", "22px")
                    property("box-sizing", "border-box")
                }
                .attrsModifier {
                    onClick { it.stopPropagation() }
                }
        ) {
            Row(Modifier.fillMaxWidth().styleModifier {property("justify-content","space-between");property("align-items","center")}) {
                SpanText("Share this shot",Modifier.styleModifier {property("font-size","1.1rem");property("font-weight","750")})
                SilkButton(onClick={onClose()},modifier=Modifier.width(32.px).height(32.px).padding(6.px).attrsModifier {
                    classes("reel-utility-button");attr("aria-label","Close share sheet")
                }) { FaXmark() }
            }
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
    SilkButton(onClick={onClick()},modifier=Modifier.attrsModifier {classes("reel-subtle-button","reel-share-button")}) {
        when(label) {
            "WhatsApp" -> FaWhatsapp()
            "Email" -> FaEnvelope()
            "Copied" -> FaCheck()
            else -> FaLink()
        }
        Text(label)
    }
}
