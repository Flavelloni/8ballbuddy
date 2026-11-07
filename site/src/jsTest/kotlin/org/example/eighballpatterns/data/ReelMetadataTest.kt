package org.example.eighballpatterns.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.example.eighballpatterns.model.EightBallEntry
import org.example.eighballpatterns.model.EightBallShot
import org.example.eighballpatterns.model.browseEightBallShots

class ReelMetadataTest {
    @Test fun explicitNullMeansNoPotButMissingBallStaysUnknown() {
        val meta = parseEightBallMeta("""{"group":1,"ball":null}""")
        assertEquals(0, meta.correctAnswer)
        assertEquals("Correct: No ball potted", org.example.eighballpatterns.model.eightBallReelResultText(0, meta))
        assertEquals("You picked 2. Correct: No ball potted", org.example.eighballpatterns.model.eightBallReelResultText(2, meta))
        assertEquals(-1, parseEightBallMeta("""{"group":1}""").correctAnswer)
    }
    @Test fun ballInHandDefaultsFalseAndLoadsExplicitTrue() {
        assertEquals(true, parseEightBallMeta("""{"ballInHand":true}""").ballInHand)
        assertEquals(false, parseEightBallMeta("""{}""").ballInHand)
        assertEquals(false, parseEightBallMeta("""{"ballInHand":false}""").ballInHand)
    }
    @Test
    fun browseOrderIsIndependentOfFolderNamesAndDoesNotChangeStableIds() {
        val ordinary = parseEightBallMeta("""{"source":"MagicCitySlate"}""")
        val featured = parseEightBallMeta("""{"source":"OBK","browseOrder":-100}""")
        fun shot(id: String, meta: org.example.eighballpatterns.model.EightBallMeta) =
            EightBallShot(EightBallEntry(id, "meta.json", "layout.jpg", "clip.mp4"), meta)
        val magic = shot("AMDgls4nJhQ/051958", ordinary)
        val second = shot("QymvyRiXJ9A/003105", featured)
        val first = shot("QymvyRiXJ9A/001412", featured)
        val original = listOf(magic, second, first)
        assertEquals(listOf(first, second, magic), browseEightBallShots(original))
        assertEquals(listOf(magic, second, first), original)
    }

    @Test
    fun matchBallColorsOverrideOnlyExplicitlyConfiguredBalls() {
        val obk = parseEightBallMeta("""{"source":"OBK","ballColors":{"12":"#F4A6C1"}}""")
        assertEquals(mapOf(12 to 0xF4A6C1), obk.ballColors)
        assertNull(obk.ballColors[4])
        val magicCity = parseEightBallMeta("""{"source":"MagicCitySlate"}""")
        assertEquals(emptyMap(), magicCity.ballColors)
        assertEquals(emptyMap(), parseEightBallMeta("""{"source":"OBK"}""").ballColors)
    }

    @Test
    fun invalidBallColorMetadataCannotBecomeCssOrInvalidBallNumbers() {
        val meta = parseEightBallMeta("""{"ballColors":{"12":"red; display:none","16":"#FFFFFF","0":"#FFFFFF","04":"#FFFFFF","5":123,"6":"#abcdEF"}}""")
        assertEquals(mapOf(6 to 0xABCDEF), meta.ballColors)
        assertEquals(emptyMap(), parseEightBallMeta("""{"ballColors":["#FFFFFF"]}""").ballColors)
        assertEquals(emptyMap(), parseEightBallMeta("""{"ballColors":null}""").ballColors)
    }

    @Test
    fun archivedMetadataRemainsLoadableButDoesNotAppearInBrowse() {
        val meta = parseEightBallMeta("""{"player":"Archived player","group":1,"ball":5,"archived":true}""")
        val archived = EightBallShot(EightBallEntry("old", "meta.json", "layout.jpg", "clip.mp4"), meta)
        val active = archived.copy(entry = archived.entry.copy(id = "new"), meta = meta.copy(archived = false))
        val all = listOf(archived, active)
        assertEquals(listOf(active), browseEightBallShots(all))
        assertEquals(5, all.find { it.entry.id == "old" }?.meta?.correctAnswer)
    }

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
