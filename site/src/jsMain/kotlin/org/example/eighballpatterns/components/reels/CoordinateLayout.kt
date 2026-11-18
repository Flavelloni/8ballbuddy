package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.attrsModifier
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.text.SpanText
import com.varabyte.kobweb.silk.components.icons.fa.FaPlay
import org.example.eighballpatterns.model.BallTarget
import org.example.eighballpatterns.model.ImagePoint
import org.example.eighballpatterns.model.BallSelection
import org.example.eighballpatterns.model.LayoutBall
import kotlinx.browser.window
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.percent

@Composable
internal fun CoordinateLayout(imagePath: String, target: BallTarget, selection: ImagePoint?, enabled: Boolean,
    onSelected: (ImagePoint) -> Unit, onReady: () -> Unit, ballSelection: BallSelection? = null) {
    val currentEnabled by rememberUpdatedState(enabled)
    val currentSelected by rememberUpdatedState(onSelected)
    val aspect=target.imageWidth.toDouble()/target.imageHeight
    var pickHint by remember(imagePath) { mutableStateOf<String?>(null) }
    var pulse by remember(imagePath) { mutableStateOf(0) }
    fun selectBall(ball: LayoutBall) {
        if (!currentEnabled) return
        currentSelected(ImagePoint(ball.x,ball.y))
        pulse++
        pickHint=null
        // Optional progressive enhancement; unsupported browsers still get the selection outline.
        try {
            val navigator=window.navigator.asDynamic()
            if (js("typeof navigator.vibrate === 'function'") as Boolean) navigator.vibrate(12)
        } catch (_: Throwable) { }
    }
    ZoomableLayout(imagePath, aspect, onReady, onTap = { point, width ->
        if (currentEnabled) {
            if (ballSelection != null) {
                val ball = ballSelection.ballAt(point, 14 / width)
                if (ball != null) selectBall(ball) else pickHint = "Tap nearer a ball’s centre."
            } else currentSelected(point)
        }
    }) {
        Box(Modifier.fillMaxSize().attrsModifier { attr("data-coordinate-layout", "true") }) {
            // Real keyboard-accessible buttons; pointer picks use shared hit testing so overlapping
            // enlarged targets never depend on DOM stacking order.
            ballSelection?.balls?.forEachIndexed { index, ball ->
                val selected=selection?.let { it.x==ball.x && it.y==ball.y } == true
                Button(onClick={ selectBall(ball) }, enabled=enabled,
                    modifier=Modifier.position(Position.Absolute).padding(0.px).borderRadius(999.px)
                        .styleModifier {
                            property("left","${ball.x*100}%");property("top","${ball.y*100}%")
                            property("transform","translate(-50%,-50%)")
                            property("width","max(28px,${ball.radius*270}%)")
                            property("height","max(28px,${ball.radius*270*aspect}%)")
                            property("background","transparent"); property("border","none")
                            property("pointer-events","none")
                        }.attrsModifier {
                            classes("layout-ball-button")
                            attr("aria-label","Select ball ${index+1}")
                            attr("aria-pressed",selected.toString())
                            attr("data-layout-ball",ball.id)
                        }) { }
            }
            selection?.let { point ->
                val selectedRadius=ballSelection?.balls?.firstOrNull { it.x==point.x && it.y==point.y }?.radius ?: target.radius
                if (pulse > 0) key(pulse) {
                    Box(Modifier.position(Position.Absolute).styleModifier {
                        property("left","${point.x*100}%"); property("top","${point.y*100}%")
                        property("width","${selectedRadius*330}%");property("height","${selectedRadius*330*aspect}%")
                        property("border","3px solid #b7ffe0");property("border-radius","50%")
                        property("box-sizing","border-box");property("pointer-events","none")
                    }.attrsModifier { classes("ball-selection-pulse");attr("aria-hidden","true") })
                }
                for (outer in listOf(true, false)) {
                    val radius = selectedRadius * if (outer) 1.65 else 1.15
                    Box(Modifier.position(Position.Absolute).styleModifier {
                        property("left", "${point.x*100}%"); property("top", "${point.y*100}%")
                        property("width", "${radius*200}%"); property("height", "${radius*200*aspect}%")
                        property("transform", "translate(-50%, -50%)")
                        property("border-radius", "50%")
                        property("border", if (outer) "2px solid #101915" else "2px solid #b7ffe0")
                        property("box-shadow", if (outer) "0 0 0 1px #b7ffe0" else "0 0 0 1px #101915")
                        property("background", if (outer) "transparent" else "rgba(148,201,181,.15)")
                        property("box-sizing", "border-box"); property("pointer-events", "none")
                    }.attrsModifier { attr("aria-hidden", "true") })
                }
            }
            pickHint?.let { hint ->
                SpanText(hint,Modifier.position(Position.Absolute).padding(8.px).styleModifier {
                    property("bottom","6px");property("left","6px");property("right","6px")
                    property("background","rgba(0,0,0,.8)");property("color","#d6f7e8");property("pointer-events","none")
                }.attrsModifier { attr("role","status") })
            }
        }
    }
}

@Composable
internal fun CoordinateConfirm(selection: ImagePoint?, enabled: Boolean, ballMode: Boolean = false, onConfirm: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(leftRight=14.px,topBottom=10.px).gap(8.px).attrsModifier {classes("coordinate-confirm")}, horizontalAlignment=Alignment.CenterHorizontally) {
        SpanText(if (selection == null) "Tap the ball you think goes in." else if (ballMode) "Ball selected. Tap another to change your pick." else "Tap again to adjust your scoring circle.")
        Button(onClick={onConfirm()}, enabled=enabled && selection != null,
            modifier=Modifier.fillMaxWidth().attrsModifier {classes("reel-primary-button");attr("aria-label","Reveal shot")}) {
            FaPlay()
            SpanText("Reveal")
        }
    }
}
