package org.example.eighballpatterns.data

import kotlin.test.Test
import kotlin.test.assertEquals
import org.example.eighballpatterns.model.orderPlayerChoices

class PlayerRepositoryTest {
    @Test
    fun localPortraitsAndNationalFlagsAreLoadedFromTheRegistry() {
        val profiles = parsePlayerProfiles("""{
            "Erik": {"imageUrl": "/images/players/erik-gardelof.png",
                     "country": "Sweden", "countryFlagUrl": "/images/flags/se.png"},
            "Emil": {"country": "Norway", "countryFlagUrl": "/images/flags/no.png"},
            "Other": {}
        }""")
        assertEquals("/images/players/erik-gardelof.png", profiles.getValue("erik").imageUrl)
        assertEquals("Sweden", profiles.getValue("erik").country)
        assertEquals("/images/flags/se.png", profiles.getValue("erik").countryFlagUrl)
        assertEquals("Norway", profiles.getValue("emil").country)
        assertEquals("", profiles.getValue("other").countryFlagUrl)
        assertEquals("", profiles.getValue("other").country)
    }

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
