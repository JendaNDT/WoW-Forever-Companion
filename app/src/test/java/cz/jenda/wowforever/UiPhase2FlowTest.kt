package cz.jenda.wowforever

import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import cz.jenda.wowforever.data.*
import cz.jenda.wowforever.data.db.QuestState
import cz.jenda.wowforever.ui.GuideScreen
import cz.jenda.wowforever.ui.GuideTheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UiPhase2FlowTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private val model get()=ViewModelProvider(rule.activity)[GuideViewModel::class.java]
    private val store get()=(rule.activity.application as GuideApplication).store
    @Before fun ready() {
        rule.waitUntil(30_000) { rule.onAllNodesWithTag("nav_SEARCH").fetchSemanticsNodes().isNotEmpty() }
        rule.runOnIdle { model.navigate(Destination.MAP);model.chooseSpoilers(Spoilers.FULL) }
    }
    private fun shot(name:String,sheet:Boolean=false) { rule.runOnIdle {
        val view=if(sheet)ShadowDialog.getShownDialogs().last { it.isShowing }.window!!.decorView else rule.activity.window.decorView
        val bitmap=captureView(view)
        File("build/reports/ui2-$name.png").apply { parentFile.mkdirs();outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) } };bitmap.recycle()
    } }
    private fun search(query:String,key:String) {
        rule.openSearch();rule.onNodeWithTag("catalog_query").performTextReplacement(query)
        rule.onNodeWithTag("catalog_list").performScrollToNode(hasTestTag("catalog_$key"));rule.onNodeWithTag("catalog_$key").performClick()
    }
    @Test fun threeTabsAndOneSettingsKeepSearchDraftAndProfileData() {
        listOf("nav_MAP","nav_QUESTS","nav_SEARCH").forEach { rule.onNodeWithTag(it).assertExists() }
        listOf("nav_NOW","nav_JOURNEY","nav_CHARACTER").forEach { rule.onNodeWithTag(it).assertDoesNotExist() }
        val before=runBlocking { store.journal.profileBackup(model.profile.id) }
        rule.openSearch();rule.onNodeWithTag("catalog_query").performTextReplacement("Ruzan")
        rule.openJournal();rule.onNodeWithTag("quests_journal").assertIsSelected()
        rule.openSearch();rule.onNodeWithTag("catalog_query").assertTextContains("Ruzan")
        rule.openSettings();listOf("settings_character","backup","packages","open_help","content_coverage").forEach { rule.onNodeWithTag(it).performScrollTo().assertExists() }
        assertEquals(before,runBlocking { store.journal.profileBackup(model.profile.id) })
        shot("settings")
    }
    @Test @Config(sdk=[28],qualifiers="w400dp-h800dp")
    fun oneSheetNavigatesRelatedEntriesAndRestoresTheSearchAfterRotation() {
        search("Ruzan","wow-forever:npc:5765")
        rule.onNodeWithTag("entry_sheet").assertExists();rule.onNodeWithTag("detail_expand").performClick()
        rule.onAllNodesWithTag("related_wow-forever:quest:1485").onFirst().performScrollTo().performClick()
        rule.onNodeWithTag("detail_title").assertIsDisplayed()
        rule.runOnIdle { assertEquals(1,ShadowDialog.getShownDialogs().count { it.isShowing }) }
        rule.activityRule.scenario.recreate()
        rule.onNodeWithTag("detail_back").assertTextContains("Zpět na předchozí").performClick()
        rule.onNodeWithTag("detail_title").assertTextEquals("Ruzan")
        shot("shared-detail",true)
        rule.onNodeWithTag("detail_back").performClick()
        rule.onNodeWithTag("catalog_query").assertTextContains("Ruzan")
        rule.onNodeWithTag("catalog_list").performScrollToNode(hasTestTag("result_map_wow-forever:npc:5765"))
        rule.onNodeWithTag("result_map_wow-forever:npc:5765").performScrollTo().performClick()
        rule.onNodeWithTag("map_1411").assertIsSelected()
        rule.onNodeWithTag("selected_point_detail").performClick()
        rule.onNodeWithTag("detail_title").assertTextEquals("Ruzan")
    }
    @Test fun questDetailFromBothSearchAndSuggestionsWritesTheSameJournal() {
        search("4641",E6_A)
        rule.onNodeWithTag("quest_state_IN_PROGRESS").performClick()
        rule.waitUntil(15_000) { rule.onAllNodesWithTag("entry_sheet").fetchSemanticsNodes();!model.isBusy && model.questStates[E6_A]==QuestState.IN_PROGRESS }
        rule.onNodeWithTag("detail_back").performClick();rule.openSuggestions()
        rule.onNodeWithTag("advice_list").performScrollToNode(hasTestTag("advice_open_$E6_A"));rule.onNodeWithTag("advice_open_$E6_A").performClick()
        rule.onNodeWithTag("quest_state_IN_PROGRESS").assertIsSelected()
        rule.onNodeWithTag("quest_state_COMPLETED").performClick()
        rule.waitUntil(15_000) { rule.onAllNodesWithTag("entry_sheet").fetchSemanticsNodes();!model.isBusy && model.questStates[E6_A]==QuestState.COMPLETED }
        rule.onNodeWithTag("detail_back").performClick();rule.openJournal()
        rule.onNodeWithTag("journey_list").performScrollToNode(hasTestTag("journal_filter_COMPLETED"));rule.onNodeWithTag("journal_filter_COMPLETED").performClick()
        rule.onNodeWithTag("journey_list").performScrollToNode(hasTestTag("journal_$E6_A"));rule.onNodeWithTag("journal_$E6_A").assertExists()
        shot("journal")
    }
    @Test fun recentSearchesStayAtFiveAndLegacyHistoryKeepsTheJournalAndPersonalRecords() {
        rule.runOnIdle { model.requestQuest(E6_A,QuestState.IN_PROGRESS) }
        rule.waitUntil(15_000) { !model.isBusy && model.questStates[E6_A]==QuestState.IN_PROGRESS }
        val before=runBlocking { store.journal.profileBackup(model.profile.id) }
        rule.runOnIdle { (1..7).forEach { model.rememberSearch("Dotaz $it") };model.rememberSearch("dotaz 7") }
        rule.waitUntil(15_000) { runBlocking { store.session.read().recentSearches.firstOrNull()=="dotaz 7" } }
        val extras=ViewModelStore()
        lateinit var restored:GuideViewModel
        rule.runOnIdle {
            restored=GuideViewModel(rule.activity.application,SavedStateHandle(mapOf("history" to arrayListOf("MAP","NOW","CHARACTER","JOURNEY"))))
            extras.put("legacy",restored)
            rule.activity.setContent { GuideTheme { GuideScreen(restored) } }
        }
        try {
            rule.waitUntil(30_000) { rule.onAllNodesWithTag("nav_QUESTS").fetchSemanticsNodes();restored.ready || restored.dataError!=null }
            rule.runOnIdle { assertNull(restored.dataError);assertEquals(Destination.QUESTS,restored.destination);assertEquals("JOURNAL",restored.questTab);assertEquals(5,restored.recentSearches.size);assertEquals("dotaz 7",restored.recentSearches.first()) }
            assertEquals(before,runBlocking { store.journal.profileBackup(restored.profile.id) })
        } finally { rule.runOnIdle { extras.clear() } }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UiPhase2LargeTextTest:E11LargeTextChecks() {
    @Test fun searchAndSharedDetailCanCloseWithTwoHundredPercentText() {
        rule.openSearch();rule.onNodeWithTag("catalog_query").performTextReplacement("4641")
        rule.onNodeWithTag("catalog_list").performScrollToNode(hasTestTag("catalog_$E6_A"));rule.onNodeWithTag("catalog_$E6_A").performClick()
        rule.onNodeWithTag("quest_state_COMPLETED").performScrollTo().assertIsDisplayed()
        shot("ui2-large-detail",true)
        rule.onNodeWithTag("detail_back").assertIsDisplayed().performClick()
        rule.onNodeWithTag("catalog_query").assertTextContains("4641")
    }
}
