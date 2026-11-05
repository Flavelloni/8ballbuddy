package org.example.eighballpatterns.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReelMetadataTest {
    @Test
    fun breakCountZeroIsAnAnswerNotAnUnknownBall() {
        val meta = parseEightBallMeta("""{
            "player":"Lance Ireland","opponent":"Rob Hall","group":null,"ball":null,
            "isBreak":true,"ballsPotted":0
        }""")
        assertEquals(true, meta.isBreak)
        assertEquals(0, meta.ballsPotted)
        assertEquals(0, meta.correctAnswer)
        assertEquals(-1, meta.ball)
        assertEquals(-1, meta.copy(ballsPotted = null).correctAnswer)
    }

    @Test
    fun playerProfilesLoadFargoWithoutRequiringAPhoto() {
        val profiles = parsePlayerProfiles("""{
            "Rob Hall": {"FargoRating":696,"FargoSource":"Fargo App","FargoDate":"Nov 2026"},
            "Lance Ireland": {"FargoRating":708,"FargoSource":"Fargo App","FargoDate":"Nov 2026"},
            "Legacy Player": {"imageUrl":"/photo.jpg"}
        }""")
        val rob = profiles.getValue("rob hall")
        assertEquals(696, rob.fargoRating)
        assertEquals("Fargo App", rob.fargoSource)
        assertEquals("Nov 2026", rob.fargoDate)
        assertEquals("", rob.imageUrl)
        assertEquals(708, profiles.getValue("lance ireland").fargoRating)
        assertNull(profiles.getValue("legacy player").fargoRating)
    }

    @Test
    fun shotSourceLoadsAlongsideAnswerAndLegacyShotsRemainCompatible() {
        val shot = parseEightBallMeta("""{
            "player":"Rob Hall","opponent":"Lance Ireland","group":0,"ball":14,
            "source":"MagicCitySlate","videoLink":"https://www.youtube.com/watch?v=AMDgls4nJhQ"
        }""")
        assertEquals(14, shot.ball)
        assertEquals("MagicCitySlate", shot.source)
        assertEquals("https://www.youtube.com/watch?v=AMDgls4nJhQ", shot.videoLink)
        val legacy = parseEightBallMeta("""{"player":"Sindre","group":1,"ball":5}""")
        assertEquals("", legacy.source)
        assertEquals("", legacy.videoLink)
    }
}
