package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.attrsModifier
import com.varabyte.kobweb.compose.ui.modifiers.gap
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.icons.fa.FaHeart
import com.varabyte.kobweb.silk.components.icons.fa.FaShare
import com.varabyte.kobweb.silk.components.icons.fa.IconStyle
import com.varabyte.kobweb.silk.components.text.SpanText
import org.example.eighballpatterns.model.EightBallReelStats
import org.jetbrains.compose.web.css.px

/** Controls live below the game, never on top of a ball or the video. */
@Composable
internal fun EightBallReelActions(stats: EightBallReelStats, liked: Boolean, onLike: () -> Unit, onShare: () -> Unit) {
    Row(Modifier.attrsModifier { classes("reel-utility-bar");attr("aria-label","Shot actions") },
        verticalAlignment=Alignment.CenterVertically) {
        Row(Modifier.gap(8.px).attrsModifier {classes("reel-score")},verticalAlignment=Alignment.CenterVertically) {
            SpanText("${stats.correct}/${stats.answered} correct")
        }
        Row(Modifier.gap(2.px)) {
            Button(onClick={onLike()},modifier=Modifier.attrsModifier {
                classes("reel-utility-button");attr("aria-label","Like shot");attr("aria-pressed",liked.toString())
            }) {
                FaHeart(style=if(liked) IconStyle.FILLED else IconStyle.OUTLINE)
                SpanText(if(liked) "Liked" else "Like")
            }
            Button(onClick={onShare()},modifier=Modifier.attrsModifier {
                classes("reel-utility-button");attr("aria-label","Share shot")
            }) { FaShare();SpanText("Share") }
        }
    }
}
