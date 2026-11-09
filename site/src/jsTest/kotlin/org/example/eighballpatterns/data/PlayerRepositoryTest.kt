package org.example.eighballpatterns.data

import kotlin.test.Test
import kotlin.test.assertEquals
import org.example.eighballpatterns.model.orderPlayerChoices

class PlayerRepositoryTest {
    @Test
    fun obkPlayersComeFirstWithoutAddingUnavailablePlayers() {
        val profiles = parsePlayerProfiles("""{
            "Kim": {"Club": "OBK"},
            "Emil": {"Club": " obk "},
            "Unavailable": {"Club": "OBK"},
            "Rob": {"Club": "Other"}
        }""")
        assertEquals("OBK", profiles.getValue("kim").club)
        assertEquals("", profiles.getValue("rob").fargoDate)
        assertEquals(listOf(" KIM ", "Emil", "Rob", "Unknown", "Lance"),
            orderPlayerChoices(listOf("Rob", " KIM ", "Unknown", "Emil", "Lance"), profiles))
    }

    @Test
    fun missingClubsPreserveTheExistingOrder() {
        val profiles = parsePlayerProfiles("""{"Kim": {"FargoRating": 677}}""")
        assertEquals("", profiles.getValue("kim").club)
        val players = listOf("Rob", "Kim")
        assertEquals(players, orderPlayerChoices(players, profiles))
        assertEquals(players, orderPlayerChoices(players, emptyMap()))
    }
}
