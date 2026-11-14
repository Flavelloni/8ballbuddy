package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.attrsModifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.navigation.BasePath
import com.varabyte.kobweb.silk.components.graphics.Image
import com.varabyte.kobweb.silk.components.text.SpanText
import kotlinx.browser.window
import kotlinx.browser.document
import kotlin.js.Promise
import kotlin.js.jsTypeOf
import org.example.eighballpatterns.model.ImagePoint
import org.example.eighballpatterns.model.LayoutTapGesture
import org.example.eighballpatterns.model.layoutImageGeometry
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.px
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLImageElement
import kotlin.math.hypot

private data class PendingLayoutTap(val time: Double, val client: ImagePoint)
private val loadedLayoutAspects = mutableMapOf<String, Double>()
internal fun isLayoutImageLoaded(path: String): Boolean = path in loadedLayoutAspects
private val pendingLayoutImages = mutableMapOf<String, Promise<Boolean>>()

/** Only callers' current/next layouts are warmed. Decode before an automatic transition. */
internal fun preloadLayoutImage(path: String): Promise<Boolean> = pendingLayoutImages.getOrPut(path) {
    Promise { resolve, _ ->
        val image = document.createElement("img") as HTMLImageElement
        fun complete() {
            val loaded = image.naturalWidth > 0 && image.naturalHeight > 0
            if (loaded) loadedLayoutAspects[path] = image.naturalWidth.toDouble() / image.naturalHeight
            // Re-decode on later visits: a browser may evict pixels from its image cache.
            pendingLayoutImages.remove(path)
            resolve(loaded)
        }
        image.onload = {
            if (jsTypeOf(image.asDynamic().decode) == "function") {
                (image.asDynamic().decode() as Promise<dynamic>).then<Unit>({ complete() }, { complete() })
            } else complete()
        }
        image.onerror = { _, _, _, _, _ ->
            pendingLayoutImages.remove(path)
            resolve(false)
        }
        image.src = BasePath.prependTo(path)
    }
}

@Composable
internal fun ZoomableLayout(
    imagePath: String,
    imageAspect: Double? = null,
    onReady: () -> Unit,
    onTap: ((ImagePoint, Double) -> Unit)? = null,
    pulseOnReady: Boolean = false,
    content: @Composable () -> Unit = {},
) {
    var zoomed by remember(imagePath) { mutableStateOf(false) }
    var imageReady by remember(imagePath) { mutableStateOf(isLayoutImageLoaded(imagePath)) }
    var pulsePaused by remember(imagePath) { mutableStateOf(false) }
    var pulseDismissed by remember(imagePath) { mutableStateOf(false) }
    var dragging by remember(imagePath) { mutableStateOf(false) }
    var viewport by remember(imagePath) { mutableStateOf(ImagePoint(0.0, 0.0)) }
    var aspect by remember(imagePath) { mutableStateOf(imageAspect ?: loadedLayoutAspects[imagePath] ?: 1.0) }
    var pan by remember(imagePath) { mutableStateOf(ImagePoint(0.0, 0.0)) }
    val geometry = layoutImageGeometry(viewport.x, viewport.y, aspect, zoomed)
    val visiblePan = geometry.clampPan(pan)
    val pulseScale = if (geometry.height > 0) layoutImageGeometry(viewport.x, viewport.y, aspect, true).height / geometry.height else 1.0
    val pulse = pulseOnReady && imageReady && viewport.x > 0 && viewport.y > 0 && !zoomed && !pulseDismissed
    val gesture = remember(imagePath) { LayoutTapGesture() }
    val pointers = remember(imagePath) { mutableSetOf<Int>() }
    var dragStart by remember(imagePath) { mutableStateOf(ImagePoint(0.0, 0.0)) }
    var panStart by remember(imagePath) { mutableStateOf(ImagePoint(0.0, 0.0)) }
    var pendingTap by remember(imagePath) { mutableStateOf<PendingLayoutTap?>(null) }
    var tapTimer by remember(imagePath) { mutableStateOf<Int?>(null) }
    val currentTap by rememberUpdatedState(onTap)
    val currentReady by rememberUpdatedState(onReady)

    fun cancelTap() {
        tapTimer?.let { window.clearTimeout(it) }
        tapTimer = null
        pendingTap = null
    }
    fun toggleZoom() {
        cancelTap()
        pulseDismissed = true
        zoomed = !zoomed
        pan = ImagePoint(0.0, 0.0)
    }
    DisposableEffect(imagePath) {
        onDispose { tapTimer?.let { window.clearTimeout(it) } }
    }
    Column(Modifier.fillMaxSize().minHeight(0.px).attrsModifier {
        classes("layout-image-viewer")
        attr("tabindex", "0")
        attr("role", "group")
        attr("aria-label", "Pool layout. Double-tap or double-click to zoom. Enter toggles zoom; arrow keys pan; Escape zooms out.")
        addEventListener("focusin") { pulsePaused = true }
        onKeyDown { e ->
            if (e.target == e.currentTarget) {
                when (e.key) {
                    "Enter", " " -> { e.preventDefault(); toggleZoom() }
                    "Escape" -> if (zoomed) { e.preventDefault(); toggleZoom() }
                    "ArrowLeft", "ArrowRight", "ArrowUp", "ArrowDown" -> if (zoomed) {
                        e.preventDefault()
                        pan = geometry.clampPan(ImagePoint(
                            visiblePan.x + when (e.key) { "ArrowLeft" -> 40; "ArrowRight" -> -40; else -> 0 },
                            visiblePan.y + when (e.key) { "ArrowUp" -> 40; "ArrowDown" -> -40; else -> 0 },
                        ))
                    }
                }
            }
        }
    }) {
        Box(Modifier.fillMaxWidth().weight(1).minHeight(0.px).position(Position.Relative).styleModifier {
            property("overflow", "hidden")
            property("container-type", "size")
            property("touch-action", if (zoomed) "none" else "pan-y")
            property("user-select", "none")
            property("-webkit-user-select", "none")
            property("cursor", if (zoomed) { if (dragging) "grabbing" else "zoom-out" } else "zoom-in")
        }.attrsModifier {
            classes("layout-image-viewport")
            attr("data-layout-zoomed", zoomed.toString())
            ref { element ->
                val root = element as HTMLElement
                val onResize = {
                    val size = ImagePoint(root.clientWidth.toDouble(), root.clientHeight.toDouble())
                    if (viewport != size) {
                        viewport = size
                        pan = ImagePoint(0.0, 0.0)
                    }
                }
                onResize()
                // Container size changes with orientation, browser chrome and the controls below the image.
                val observer = js("new ResizeObserver(onResize)")
                observer.observe(root)
                onDispose { observer.disconnect() }
            }
            addEventListener("pointerdown") { e ->
                pulsePaused = true
                val p = e.nativeEvent.asDynamic()
                if ((p.button as Int) == 0) {
                    val id = p.pointerId as Int
                    gesture.down(id, p.clientX as Double, p.clientY as Double)
                    pointers.add(id)
                    if (pointers.size == 1) {
                        dragStart = ImagePoint(p.clientX as Double, p.clientY as Double)
                        panStart = visiblePan
                        dragging = zoomed
                    } else {
                        cancelTap()
                        dragging = false
                    }
                    if (zoomed) (p.currentTarget as Element).setPointerCapture(id)
                }
            }
            addEventListener("pointermove") { e ->
                val p = e.nativeEvent.asDynamic()
                gesture.move(p.clientX as Double, p.clientY as Double)
                if (dragging && pointers.size == 1) {
                    pan = geometry.clampPan(ImagePoint(
                        panStart.x + (p.clientX as Double) - dragStart.x,
                        panStart.y + (p.clientY as Double) - dragStart.y,
                    ))
                }
            }
            addEventListener("pointerup") { e ->
                val p = e.nativeEvent.asDynamic()
                val id = p.pointerId as Int
                val tap = gesture.up(id, p.clientX as Double, p.clientY as Double)
                pointers.remove(id)
                dragging = false
                val root = p.currentTarget as Element
                if (root.hasPointerCapture(id)) root.releasePointerCapture(id)
                if (tap) {
                    val client = ImagePoint(p.clientX as Double, p.clientY as Double)
                    val previous = pendingTap
                    if (previous != null && e.nativeEvent.timeStamp.toDouble() - previous.time <= 320 &&
                        hypot(client.x - previous.client.x, client.y - previous.client.y) <= 24) {
                        toggleZoom()
                    } else {
                        cancelTap()
                        // Read the rendered image bounds: CSS pulse, zoom and pan all transform
                        // the photograph and its ball overlays together, including during a tap.
                        val rect = root.querySelector("[data-layout-image]")?.getBoundingClientRect()
                        val point = rect?.takeIf { it.width > 0 && it.height > 0 }?.let {
                            ImagePoint((client.x - it.left) / it.width, (client.y - it.top) / it.height)
                                .takeIf { p -> p.x in 0.0..1.0 && p.y in 0.0..1.0 }
                        }
                        if (point != null) {
                            pendingTap = PendingLayoutTap(e.nativeEvent.timeStamp.toDouble(), client)
                            // Wait for a possible second tap so zooming never also selects a ball.
                            tapTimer = window.setTimeout({
                                pendingTap = null
                                tapTimer = null
                                currentTap?.invoke(point, rect.width)
                            }, 320)
                        }
                    }
                } else cancelTap()
            }
            addEventListener("pointercancel") {
                gesture.cancel()
                pointers.clear()
                dragging = false
                cancelTap()
            }
            addEventListener("lostpointercapture") {
                if (pointers.isNotEmpty()) {
                    gesture.cancel()
                    pointers.clear()
                    dragging = false
                    cancelTap()
                }
            }
            addEventListener("pointerleave") {
                // Touch pointers leave after every release; preserve the pending tap in that case.
                if (!dragging && pointers.isNotEmpty()) { gesture.cancel(); pointers.clear(); cancelTap() }
            }
            onWheel { if (zoomed) { it.preventDefault(); it.stopPropagation() } }
        }) {
            Box(Modifier.position(Position.Absolute).styleModifier {
                property("left", "50%")
                property("top", "50%")
                // The container has a size before ResizeObserver's first callback. Render the
                // cached photograph immediately instead of flashing a zero-size image per reel.
                property("width", if (viewport.x > 0) "${geometry.width}px" else "min(100cqw, ${aspect * 100}cqh)")
                property("height", if (viewport.y > 0) "${geometry.height}px" else "min(100cqh, ${100 / aspect}cqw)")
                property("transform", "translate(calc(-50% + ${visiblePan.x}px), calc(-50% + ${visiblePan.y}px))")
                if (pulse) {
                    // Transform-only animation keeps layout, decoded pixels and hit targets intact.
                    property("--layout-pulse-scale", pulseScale.toString())
                    property("animation-play-state", if (pulsePaused) "paused" else "running")
                }
            }.attrsModifier {
                attr("data-layout-image", "true")
                if (pulse) classes("layout-arrival-pulse")
            }) {
                Image(src = BasePath.prependTo(imagePath), alt = "8-ball layout",
                    modifier = Modifier.fillMaxSize().position(Position.Absolute).styleModifier {
                        property("inset", "0")
                        property("pointer-events", "none")
                        property("object-fit", "contain")
                    }.attrsModifier {
                        attr("draggable", "false")
                        fun loaded(image: HTMLImageElement) {
                            if (image.naturalWidth > 0 && image.naturalHeight > 0) {
                                aspect = image.naturalWidth.toDouble() / image.naturalHeight
                                loadedLayoutAspects[imagePath] = aspect
                                imageReady = true
                                currentReady()
                            }
                        }
                        addEventListener("load") { loaded(it.nativeEvent.target as HTMLImageElement) }
                        ref { image ->
                            val img = image as HTMLImageElement
                            if (img.complete) loaded(img)
                            onDispose {}
                        }
                    })
                content()
            }
        }
        SpanText(if (zoomed) "Drag to pan · Double-tap / double-click to zoom out"
            else "Double-tap / double-click the image to zoom in",
            Modifier.fillMaxWidth().padding(leftRight = 8.px, topBottom = 5.px)
                .attrsModifier { classes("layout-zoom-hint"); attr("aria-live", "polite") })
    }
}
