package org.example.eighballpatterns.model

import kotlin.test.*
import org.example.eighballpatterns.data.parseEightBallMeta

class BallTargetTest {
    private val target=BallTarget(.5,.5,.02,1000,500,"a".repeat(64))
    @Test fun innerCircleScoresInImagePixelsNotStretchedNormalizedCoordinates() {
        assertTrue(target.contains(ImagePoint(.52,.5)))
        assertTrue(target.contains(ImagePoint(.5,.54)))
        assertFalse(target.contains(ImagePoint(.525,.5))) // Inside outer ring, outside scoring circle.
        assertFalse(target.contains(ImagePoint(.5,.55)))
        assertFalse(target.contains(ImagePoint(Double.NaN,.5)))
    }
    @Test fun targetMetadataIsOptionalAndValidated() {
        assertNull(parseEightBallMeta("{}").target)
        val meta=parseEightBallMeta("""{"target":{"x":0.5,"y":0.5,"radius":0.02,"imageWidth":1000,"imageHeight":500,"imageSha256":"${"a".repeat(64)}"}}""")
        assertEquals(target,meta.target)
        assertNull(parseEightBallMeta("""{"target":{"x":2}}""").target)
    }
    @Test fun onlyATapPlacesAMarkerNotAPanPinchOrCancellation() {
        val g=LayoutTapGesture()
        g.down(1,10.0,10.0); assertTrue(g.up(1,11.0,11.0))
        g.down(1,10.0,10.0); g.move(40.0,10.0); assertFalse(g.up(1,40.0,10.0))
        g.down(1,10.0,10.0); g.down(2,20.0,20.0); assertFalse(g.up(1,10.0,10.0)); assertFalse(g.up(2,20.0,20.0))
        g.down(1,10.0,10.0); g.cancel(); assertFalse(g.up(1,10.0,10.0))
    }
}
