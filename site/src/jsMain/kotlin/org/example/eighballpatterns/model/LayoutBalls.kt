package org.example.eighballpatterns.model

import kotlin.math.hypot
import kotlin.math.max

internal data class LayoutBall(val id: String, val x: Double, val y: Double, val radius: Double)

internal data class BallSelection(
    val balls: List<LayoutBall>, val correctBallId: String, val correctNumber: Int,
    val imageWidth: Int, val imageHeight: Int, val imageSha256: String,
) {
    val imageTarget: BallTarget get() = balls.first { it.id == correctBallId }.let {
        BallTarget(it.x,it.y,it.radius,imageWidth,imageHeight,imageSha256)
    }

    /** Enlarged targets remain unambiguous: nearest centre wins; close ties require a clearer tap. */
    fun ballAt(point: ImagePoint, minimumRadius: Double = 0.0): LayoutBall? {
        if (!point.x.isFinite() || !point.y.isFinite() || point.x !in 0.0..1.0 || point.y !in 0.0..1.0) return null
        val hits=balls.map { it to hypot(point.x-it.x,(point.y-it.y)*imageHeight/imageWidth) }
            .filter { (ball,distance) -> distance <= max(ball.radius*1.35,minimumRadius) }
            .sortedBy { it.second }
        val first=hits.firstOrNull() ?: return null
        if (hits.size>1 && hits[1].second-first.second < minOf(first.first.radius,hits[1].first.radius)*.2) return null
        return first.first
    }
}
