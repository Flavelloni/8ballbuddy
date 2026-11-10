package org.example.eighballpatterns.model

import kotlin.math.hypot

internal data class ImagePoint(val x: Double, val y: Double)

/** x/y use the final image's width/height; radius uses image width for a true circle. */
internal data class BallTarget(
    val x: Double, val y: Double, val radius: Double,
    val imageWidth: Int, val imageHeight: Int, val imageSha256: String,
) {
    fun contains(point: ImagePoint): Boolean = point.x.isFinite() && point.y.isFinite() &&
        point.x in 0.0..1.0 && point.y in 0.0..1.0 &&
        hypot(point.x - x, (point.y - y) * imageHeight / imageWidth) <= radius + 1e-12
}

/** Movement and multi-pointer gestures must never place a marker. */
internal class LayoutTapGesture {
    private val pointers = mutableSetOf<Int>()
    private var start: ImagePoint? = null
    private var cancelled = false
    fun down(id: Int, x: Double, y: Double) {
        if (pointers.isEmpty()) { start = ImagePoint(x, y); cancelled = false }
        pointers.add(id)
        if (pointers.size > 1) cancelled = true
    }
    fun move(x: Double, y: Double) {
        start?.let { if (hypot(x - it.x, y - it.y) > 8) cancelled = true }
    }
    fun up(id: Int, x: Double, y: Double): Boolean {
        move(x, y)
        val tap = pointers.contains(id) && pointers.size == 1 && !cancelled
        pointers.remove(id)
        return tap
    }
    fun cancel() { cancelled = true; pointers.clear() }
}
