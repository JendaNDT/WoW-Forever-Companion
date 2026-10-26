package cz.jenda.wowforever

import cz.jenda.wowforever.data.*
import org.junit.Assert.*
import org.junit.Test

class UiPhase2NavigationTest {
    @Test fun legacyTabsKeepTheirMostRecentDestinationWhenTwoOldTabsMerge() {
        assertEquals(listOf("MAP","SETTINGS","QUESTS"),migrateDestinationHistory(listOf("MAP","NOW","CHARACTER","JOURNEY")))
        assertEquals(listOf("MAP","QUESTS","SETTINGS"),migrateDestinationHistory(listOf("MAP","JOURNEY","NOW","CHARACTER")))
        assertEquals(Destination.QUESTS,Destination.fromSaved("NOW"))
        assertEquals(Destination.QUESTS,Destination.fromSaved("JOURNEY"))
        assertEquals(Destination.SETTINGS,Destination.fromSaved("CHARACTER"))
    }
    @Test fun unknownHistoryDoesNotHideMapAndNewTabsKeepBackOrder() {
        assertEquals(listOf("MAP"),migrateDestinationHistory(listOf("REMOVED","", "MAP")))
        assertEquals(listOf("MAP","SEARCH","SETTINGS","QUESTS"),migrateDestinationHistory(listOf("MAP","QUESTS","SEARCH","SETTINGS","QUESTS")))
        assertEquals(listOf(Destination.MAP,Destination.QUESTS,Destination.SEARCH),Destination.tabs)
        assertFalse(Destination.SETTINGS in Destination.tabs)
    }
}
