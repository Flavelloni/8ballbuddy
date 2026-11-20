package org.example.eighballpatterns.model

import kotlin.test.*
import org.example.eighballpatterns.data.parseEightBallMeta

class TableSelectionTest {
    private val table=TableSelection(listOf(ImagePoint(.25,.1),ImagePoint(.75,.1),ImagePoint(.95,.9),ImagePoint(.05,.9)),1000,750,"a".repeat(64))

    @Test fun quarterNumbersFollowReadingOrderAndExcludeOutsideTheCloth() {
        for (q in 1..4) assertEquals(q,table.quarterAt(table.center(q)))
        assertEquals(1,table.quarterAt(ImagePoint(.3,.2)))
        assertEquals(2,table.quarterAt(ImagePoint(.7,.2)))
        assertEquals(3,table.quarterAt(ImagePoint(.2,.8)))
        assertEquals(4,table.quarterAt(ImagePoint(.8,.8)))
        assertNull(table.quarterAt(ImagePoint(.05,.2)))
        assertNull(table.quarterAt(ImagePoint(Double.NaN,.5)))
        assertEquals(1,table.quarterAt(table.point(.5,.5)),"Boundary ties use a stable reading order")
        assertTrue(table.point(.5,.5).y < .5,"The far half is foreshortened in perspective")
    }

    @Test fun nineBallAnswersAndDescriptionsDoNotUseGroupsOrBallNumbers() {
        val meta=parseEightBallMeta("""{"discipline":9,"player":"Emil","quarter":3,"group":1,"ball":6}""")
        assertEquals(3,meta.correctAnswer)
        assertEquals("9-ball",eightBallPlayerStatus(meta))
        assertEquals("Correct: cue ball finishes bottom-left.",eightBallReelResultText(3,meta))
        assertEquals("You picked top-right. Cue ball finishes bottom-left.",eightBallReelResultText(2,meta))
        assertEquals(6,parseEightBallMeta("""{"group":1,"ball":6}""").correctAnswer)
    }

    @Test fun tableParserRejectsCrossedAndStaleSchemaShapes() {
        val corners="""[{"x":0.25,"y":0.1},{"x":0.75,"y":0.1},{"x":0.95,"y":0.9},{"x":0.05,"y":0.9}]"""
        val json="""{"discipline":9,"quarter":3,"tableSelection":{"schemaVersion":1,"corners":$corners,"imageWidth":1000,"imageHeight":750,"imageSha256":"${"a".repeat(64)}"}}"""
        assertNotNull(parseEightBallMeta(json).tableSelection)
        assertNull(parseEightBallMeta(json.replace("schemaVersion\":1","schemaVersion\":2")).tableSelection)
        assertNull(parseEightBallMeta(json.replace("0.25","1.25")).tableSelection)
        assertNull(parseEightBallMeta(json.replace("imageWidth\":1000","imageWidth\":0")).tableSelection)
        assertNull(parseEightBallMeta(json.replace(corners,"[]")).tableSelection)
    }
}
