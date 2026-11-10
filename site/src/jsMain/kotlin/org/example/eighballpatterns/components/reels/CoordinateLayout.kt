package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.varabyte.kobweb.navigation.BasePath
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.graphics.Image
import com.varabyte.kobweb.silk.components.text.SpanText
import org.example.eighballpatterns.model.BallTarget
import org.example.eighballpatterns.model.ImagePoint
import org.example.eighballpatterns.model.LayoutTapGesture
import org.example.eighballpatterns.model.BallSelection
import org.example.eighballpatterns.model.LayoutBall
import kotlinx.browser.window
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.percent
import org.w3c.dom.Element
import org.w3c.dom.HTMLImageElement
import kotlin.math.min

@Composable
internal fun CoordinateLayout(imagePath: String, target: BallTarget, selection: ImagePoint?, enabled: Boolean,
    onSelected: (ImagePoint) -> Unit, onReady: () -> Unit, ballSelection: BallSelection? = null) {
    val gesture = remember(imagePath) { LayoutTapGesture() }
    val aspect=target.imageWidth.toDouble()/target.imageHeight
    var pickHint by remember(imagePath) { mutableStateOf<String?>(null) }
    var pulse by remember(imagePath) { mutableStateOf(0) }
    fun selectBall(ball: LayoutBall) {
        if (!enabled) return
        onSelected(ImagePoint(ball.x,ball.y))
        pulse++
        pickHint=null
        // Optional progressive enhancement; unsupported browsers still get the selection outline.
        try {
            val navigator=window.navigator.asDynamic()
            if (js("typeof navigator.vibrate === 'function'") as Boolean) navigator.vibrate(12)
        } catch (_: Throwable) { }
    }
    // The inner wrapper is precisely the rendered photograph. Its percentage hit areas and
    // selection outlines resize with CSS, without an asynchronous measurement/render race.
    Box(Modifier.fillMaxSize().minHeight(0.px).minWidth(0.px).styleModifier {
        property("container-type","size")
    },contentAlignment=Alignment.Center) {
        Box(Modifier.position(Position.Relative).styleModifier {
            property("width","min(100cqw, ${aspect*100}cqh)")
            property("height","min(100cqh, ${100/aspect}cqw)")
            property("touch-action", "auto")
            property("cursor", if (enabled && ballSelection!=null) "pointer" else if (enabled) "crosshair" else "default")
        }.attrsModifier {
            attr("data-coordinate-layout", "true")
            attr("aria-label", "Tap the ball you predict will be potted")
            addEventListener("pointerdown") { e ->
                val p = e.nativeEvent.asDynamic()
                if ((p.button as Int) == 0) gesture.down(p.pointerId as Int, p.clientX as Double, p.clientY as Double)
            }
            addEventListener("pointermove") { e -> val p=e.nativeEvent.asDynamic(); gesture.move(p.clientX as Double, p.clientY as Double) }
            addEventListener("pointercancel") { gesture.cancel() }
            addEventListener("pointerleave") { gesture.cancel() }
            addEventListener("pointerup") { e ->
                val p=e.nativeEvent.asDynamic()
                if (gesture.up(p.pointerId as Int, p.clientX as Double, p.clientY as Double) && enabled) {
                    val rect = (p.currentTarget as Element).getBoundingClientRect()
                    val s=min(rect.width/target.imageWidth,rect.height/target.imageHeight)
                    val w=target.imageWidth*s; val h=target.imageHeight*s
                    val point = ImagePoint(((p.clientX as Double)-rect.left-(rect.width-w)/2)/w,
                        ((p.clientY as Double)-rect.top-(rect.height-h)/2)/h)
                    if (point.x in 0.0..1.0 && point.y in 0.0..1.0) {
                        if (ballSelection!=null) {
                            val ball=ballSelection.ballAt(point,14/w)
                            if (ball!=null) selectBall(ball)
                            else pickHint="Tap nearer a ball’s centre, or zoom in."
                        } else onSelected(point)
                    }
                }
            }
        }) {
            Image(src=BasePath.prependTo(imagePath), alt="Pool layout: tap to choose a ball",
                modifier=Modifier.fillMaxSize().position(Position.Absolute).styleModifier {
                    property("inset","0"); property("object-fit","contain")
                }.attrsModifier {
                    attr("draggable", "false")
                    addEventListener("load") { onReady() }
                    ref { image ->
                        val img=image as? HTMLImageElement
                        if (img != null && img.complete && img.naturalWidth > 0) onReady()
                        onDispose {}
                    }
                })
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
    Column(Modifier.fillMaxWidth().padding(12.px).gap(8.px), horizontalAlignment=Alignment.CenterHorizontally) {
        SpanText(if (selection == null) "Tap a ball. Pinch to zoom for a closer look." else if (ballMode) "Ball selected. Tap another ball to change your choice." else "Tap again to adjust. The inner circle is your scoring area.")
        Button(onClick={onConfirm()}, enabled=enabled && selection != null,
            modifier=Modifier.fillMaxWidth().minHeight(46.px).borderRadius(16.px)) {
            SpanText("I am happy with my choice")
        }
    }
}
