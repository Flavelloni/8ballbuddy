package org.example.eighballpatterns.model

import kotlin.test.*

class LayoutImageGeometryTest {
    @Test fun zoomFillsPhoneHeightAndRevealsBothCroppedEdgesByPanning() {
        val fitted = layoutImageGeometry(390.0, 520.0, 2.0, false)
        assertEquals(390.0, fitted.width)
        assertEquals(195.0, fitted.height)
        val zoomed = layoutImageGeometry(390.0, 520.0, 2.0, true)
        assertEquals(520.0, zoomed.height)
        assertEquals(1040.0, zoomed.width)
        val leftEdge = zoomed.clampPan(ImagePoint(9999.0, 9999.0))
        val rightEdge = zoomed.clampPan(ImagePoint(-9999.0, -9999.0))
        assertEquals(0.0, zoomed.imagePoint(0.0, 0.0, leftEdge)!!.x, 1e-9)
        assertEquals(1.0, zoomed.imagePoint(390.0, 520.0, rightEdge)!!.x, 1e-9)
        assertEquals(0.0, leftEdge.y)
        assertEquals(0.0, rightEdge.y)
    }

    @Test fun ballCoordinatesStayCorrectAfterZoomAndPan() {
        val geometry = layoutImageGeometry(390.0, 520.0, 2.0, true)
        val pan = geometry.clampPan(ImagePoint(200.0, 0.0))
        val ball = ImagePoint(.4, .6)
        val x = (geometry.viewportWidth - geometry.width) / 2 + pan.x + ball.x * geometry.width
        val y = (geometry.viewportHeight - geometry.height) / 2 + pan.y + ball.y * geometry.height
        val selected = geometry.imagePoint(x, y, pan)!!
        assertEquals(ball.x, selected.x, 1e-9)
        assertEquals(ball.y, selected.y, 1e-9)
    }

    @Test fun shortViewportsStillEnlargeAndEveryEdgeIsReachable() {
        val geometry = layoutImageGeometry(680.0, 200.0, 2.0, true)
        assertEquals(300.0, geometry.height)
        val top = geometry.clampPan(ImagePoint(0.0, 9999.0))
        val bottom = geometry.clampPan(ImagePoint(0.0, -9999.0))
        assertEquals(0.0, geometry.imagePoint(340.0, 0.0, top)!!.y, 1e-9)
        assertEquals(1.0, geometry.imagePoint(340.0, 200.0, bottom)!!.y, 1e-9)
    }

    @Test fun letterboxTapsDoNotSelectAndNormalViewCannotPan() {
        val geometry = layoutImageGeometry(390.0, 520.0, 2.0, false)
        assertNull(geometry.imagePoint(195.0, 0.0, ImagePoint(0.0, 0.0)))
        assertEquals(ImagePoint(0.0, 0.0), geometry.clampPan(ImagePoint(100.0, -100.0)))
    }
}
