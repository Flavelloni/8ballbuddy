package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.attrsModifier
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.icons.fa.FaThumbsUp
import com.varabyte.kobweb.silk.components.icons.fa.FaThumbsDown
import com.varabyte.kobweb.silk.components.icons.fa.FaVolumeHigh
import com.varabyte.kobweb.silk.components.icons.fa.FaVolumeXmark
import com.varabyte.kobweb.silk.components.text.SpanText
import org.example.eighballpatterns.browser.*
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.px

@Composable
internal fun PredictionFeedback(correct: Boolean) {
    Box(Modifier.position(Position.Absolute).fillMaxSize().zIndex(10002).styleModifier {
        property("inset","0");property("background","rgba(3,12,8,.78)")
        property("backdrop-filter","blur(3px)")
    }.attrsModifier { attr("role","status");attr("aria-live","polite");attr("data-prediction-feedback",correct.toString()) },
        contentAlignment=Alignment.Center) {
        Column(Modifier.gap(18.px).styleModifier {
            property("color",if(correct) "#a8ffd0" else "#ffaaaa")
        }.attrsModifier { classes("prediction-pop") }, horizontalAlignment=Alignment.CenterHorizontally) {
            Box(Modifier.width(150.px).height(150.px).borderRadius(999.px).styleModifier {
                property("background",if(correct) "radial-gradient(circle at 30% 20%,#347956,#102c1d)" else "radial-gradient(circle at 30% 20%,#8c4040,#361515)")
                property("border","2px solid currentColor");property("font-size","70px")
                property("box-shadow","0 0 70px ${if(correct) "#76ffc044" else "#ff777744"}")
            },contentAlignment=Alignment.Center) { if(correct) FaThumbsUp() else FaThumbsDown() }
            SpanText(if(correct) "Great pick!" else "Not this time!",Modifier.styleModifier { property("font-size","1.8rem");property("font-weight","800") })
        }
    }
}

@Composable
internal fun PredictionSoundToggle() {
    var enabled by remember { mutableStateOf(predictionSoundEnabled()) }
    Button(onClick={ enabled=!enabled;setPredictionSound(enabled);if(enabled) primePredictionAudio() },
        modifier=Modifier.width(34.px).height(34.px).padding(6.px).borderRadius(999.px)
            .attrsModifier { classes("reel-utility-button");attr("aria-label",if(enabled) "Mute feedback sounds" else "Enable feedback sounds");attr("aria-pressed",enabled.toString()) }) {
        if(enabled) FaVolumeHigh() else FaVolumeXmark()
    }
}
