package org.example.eighballpatterns.model

import kotlin.test.*
import org.example.eighballpatterns.data.parseEightBallMeta

class LayoutBallsTest {
    private val balls=BallSelection(listOf(LayoutBall("a",.3,.4,.015),LayoutBall("b",.36,.4,.015)),"a",6,1000,500,"a".repeat(64))
    @Test fun scoringUsesBallIdentityAndEmptySpaceIsNotAChoice() {
        assertEquals("a",balls.ballAt(ImagePoint(.3,.4))?.id)
        assertEquals("b",balls.ballAt(ImagePoint(.36,.4))?.id)
        assertNull(balls.ballAt(ImagePoint(.8,.8)))
        assertNull(balls.ballAt(ImagePoint(Double.NaN,.4)))
        assertEquals(6,balls.correctNumber)
    }
    @Test fun enlargedTargetsPreferNearestCentreAndRejectAmbiguousTies() {
        assertEquals("a",balls.ballAt(ImagePoint(.31,.4),.06)?.id)
        assertNull(balls.ballAt(ImagePoint(.33,.4),.06))
        assertEquals("b",balls.ballAt(ImagePoint(.35,.4),.06)?.id)
    }
    @Test fun validAnnotationsLoadAndMalformedRegionsAreIgnored() {
        val json="""{"ballSelection":{"schemaVersion":1,"correctNumber":6,"correctBallId":"a","imageWidth":1000,"imageHeight":500,"imageSha256":"${"a".repeat(64)}","balls":[{"id":"a","x":0.3,"y":0.4,"radius":0.015},{"id":"b","x":0.36,"y":0.4,"radius":0.015}]}}"""
        assertEquals(balls,parseEightBallMeta(json).ballSelection)
        assertNull(parseEightBallMeta(json.replace("\"correctBallId\":\"a\"","\"correctBallId\":\"none\"")).ballSelection)
        assertNull(parseEightBallMeta(json.replace("\"correctNumber\":6","\"correctNumber\":0")).ballSelection)
        assertNull(parseEightBallMeta(json.replace("\"id\":\"b\"","\"id\":\"a\"")).ballSelection)
        assertNull(parseEightBallMeta("{}").ballSelection)
    }
}
