package org.example.eighballpatterns.model

import kotlin.math.max
import kotlin.math.min

/** Image pixels and overlays share this rectangle, including while cropped and panned. */
internal data class LayoutImageGeometry(
    val viewportWidth: Double, val viewportHeight: Double,
    val width: Double, val height: Double,
) {
    fun clampPan(point: ImagePoint): ImagePoint {
        val xLimit = max(0.0, (width - viewportWidth) / 2)
        val yLimit = max(0.0, (height - viewportHeight) / 2)
        return ImagePoint(
            if (xLimit == 0.0) 0.0 else point.x.coerceIn(-xLimit, xLimit),
            if (yLimit == 0.0) 0.0 else point.y.coerceIn(-yLimit, yLimit),
        )
    }

    fun imagePoint(x: Double, y: Double, pan: ImagePoint): ImagePoint? {
        if (width <= 0 || height <= 0) return null
        val point = ImagePoint(
            (x - (viewportWidth - width) / 2 - pan.x) / width,
            (y - (viewportHeight - height) / 2 - pan.y) / height,
        )
        return point.takeIf { it.x in 0.0..1.0 && it.y in 0.0..1.0 }
    }
}

internal fun layoutImageGeometry(width: Double, height: Double, aspect: Double, zoomed: Boolean): LayoutImageGeometry {
    if (width <= 0 || height <= 0 || aspect <= 0) return LayoutImageGeometry(width, height, 0.0, 0.0)
    val fittedHeight = min(height, width / aspect)
    // Fill the available vertical space on a phone. Still enlarge layouts that already fit by height.
    val imageHeight = if (zoomed) max(height, fittedHeight * 1.5) else fittedHeight
    return LayoutImageGeometry(width, height, imageHeight * aspect, imageHeight)
}
