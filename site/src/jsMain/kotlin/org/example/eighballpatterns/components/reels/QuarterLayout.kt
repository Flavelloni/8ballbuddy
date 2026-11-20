package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.attrsModifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.silk.components.forms.Button
import org.example.eighballpatterns.model.*
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.px
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.PI

@Composable
internal fun QuarterLayout(imagePath: String, table: TableSelection, selection: ImagePoint?, enabled: Boolean,
    onSelected: (ImagePoint) -> Unit, onReady: () -> Unit) {
    val currentEnabled by rememberUpdatedState(enabled)
    val currentSelected by rememberUpdatedState(onSelected)
    val chosen=selection?.let(table::quarterAt)
    val aspect=table.imageWidth.toDouble()/table.imageHeight
    ZoomableLayout(imagePath, aspect, onReady, onTap={ point, _ ->
        if (currentEnabled) table.quarterAt(point)?.let { currentSelected(table.center(it)) }
    }) {
        Box(Modifier.fillMaxSize().attrsModifier { attr("data-quarter-layout","true") }) {
            table.quarters.forEachIndexed { index, polygon ->
                Button(onClick={ if (currentEnabled) currentSelected(table.center(index+1)) }, enabled=enabled,
                    modifier=Modifier.fillMaxSize().position(Position.Absolute).padding(0.px).borderRadius(0.px)
                        .styleModifier {
                            property("inset","0")
                            property("clip-path","polygon(${polygon.joinToString(",") { "${it.x*100}% ${it.y*100}%" }})")
                            property("background",if (chosen==index+1) "rgba(148,201,181,.24)" else "transparent")
                            property("border","0")
                            property("pointer-events","none")
                        }.attrsModifier {
                            classes("table-quarter-button")
                            attr("aria-label","Select ${quarterLabel(index+1)} quarter")
                            attr("aria-pressed",(chosen==index+1).toString())
                            attr("data-quarter",(index+1).toString())
                        }) { }
            }
            val quarters=table.quarters
            val segments=listOf(quarters[0][1] to quarters[2][2], quarters[0][3] to quarters[1][2])
            segments.forEach { (a,b) ->
                val dx=b.x-a.x; val dy=(b.y-a.y)/aspect
                // A rotated CSS border keeps dotted dividers aligned inside the zoomed image.
                Box(Modifier.position(Position.Absolute).styleModifier {
                    property("left","${a.x*100}%"); property("top","${a.y*100}%")
                    property("width","${hypot(dx,dy)*100}%");property("height","0")
                    property("transform-origin","0 0"); property("transform","rotate(${atan2(dy,dx)*180/PI}deg)")
                    property("border-top","2px dotted rgba(255,255,255,.8)")
                    property("filter","drop-shadow(0 1px 1px #102219)");property("pointer-events","none")
                }.attrsModifier { attr("aria-hidden","true") })
            }
        }
    }
}
