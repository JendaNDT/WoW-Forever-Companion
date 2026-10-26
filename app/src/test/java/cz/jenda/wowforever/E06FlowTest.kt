package cz.jenda.wowforever

import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import cz.jenda.wowforever.content.ContentJson
import cz.jenda.wowforever.data.*
import cz.jenda.wowforever.data.db.QuestState
import cz.jenda.wowforever.domain.*
import cz.jenda.wowforever.ui.GuideScreen
import cz.jenda.wowforever.ui.GuideTheme
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class E06FlowTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private var custom:GuideViewModel?=null
    private val extras=ViewModelStore()
    private val model get()=custom ?: ViewModelProvider(rule.activity)[GuideViewModel::class.java]
    private val store get()=(rule.activity.application as GuideApplication).store
    @Before fun ready(){
        rule.waitUntil(20_000){rule.onAllNodesWithTag("nav_QUESTS").fetchSemanticsNodes().isNotEmpty()}
        rule.runOnIdle{model.showSuggestions();model.chooseSpoilers(Spoilers.FULL);model.chooseGuideFocus(GuideFocus.BALANCED)}
    }
    @After fun close(){rule.runOnIdle{extras.clear();custom=null;model.navigate(Destination.MAP);model.chooseMap(1411)}}
    private fun settled(predicate:()->Boolean) {rule.waitUntil(15_000){rule.onAllNodesWithTag("nav_QUESTS").fetchSemanticsNodes();!model.isBusy && predicate()}}
    private fun scroll(tag:String){rule.onNodeWithTag("advice_list").performScrollToNode(hasTestTag(tag));rule.onNodeWithTag(tag).performScrollTo()}
    private fun shot(name:String,dialog:Boolean=false) {
        lateinit var bitmap:android.graphics.Bitmap
        rule.runOnIdle{bitmap=captureView(if(dialog)org.robolectric.shadows.ShadowDialog.getShownDialogs().last { it.isShowing }.window!!.decorView else rule.activity.window.decorView)}
        val file=File("build/reports/e06-$name.png");file.parentFile!!.mkdirs();file.outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
    }
    private fun fixture(level:Int=5){
        runBlocking{CatalogInstaller(store.content).install(ContentJson.encodeToString(e06Fixture().copy(generation=currentPackage().generation+1)).encodeToByteArray())}
        rule.runOnIdle{
            custom=GuideViewModel(rule.activity.application,SavedStateHandle());extras.put("fixture",model)
            rule.activity.setContent { GuideTheme { GuideScreen(model) } }
        }
        rule.waitUntil(20_000){rule.onAllNodesWithTag("nav_QUESTS").fetchSemanticsNodes();model.ready || model.dataError!=null}
        rule.runOnIdle{assertNull(model.dataError);model.saveProfile(model.profile.copy(level=level));model.showSuggestions()}
        settled{model.profile.level==level && model.guideRules.size==5}
    }
    @Test fun realCatalogShowsUncertaintyAndCoverageWithoutInventedAvailableQuests() {
        rule.runOnIdle{assertTrue(model.recommendations.now.isEmpty());assertTrue(model.recommendations.uncertain.isNotEmpty())}
        scroll("reference_notice");rule.onNodeWithTag("reference_notice").assertExists()
        scroll("advice_$E6_A");rule.onNodeWithTag("availability_$E6_A",useUnmergedTree=true).assertDoesNotExist();shot("now")
        rule.onNodeWithTag("advice_open_$E6_A").performScrollTo().performClick()
        rule.onNodeWithTag("advice_explanation").assertExists();shot("why-reference",true)
        rule.onNodeWithTag("detail_back").performClick()
        scroll("advice_coverage");rule.onNodeWithTag("advice_coverage").performClick()
        rule.onNodeWithTag("coverage_counts").assertExists();shot("coverage",true)
        rule.onNodeWithText("Zavřít pokrytí").performClick()
    }
    @Test fun actualProfileEditorChangesLevelAndRaceAndRecommendationsFollowSavedValues() {
        fixture(1)
        rule.runOnIdle{assertEquals(Availability.LATER,model.recommendations.all.first { it.entry.key==E6_A }.availability)}
        rule.openCharacter()
        rule.onNodeWithTag("profile_level").performScrollTo().performTextReplacement("5")
        rule.onNodeWithTag("save_profile").performScrollTo().performClick();settled{model.profile.level==5}
        rule.openSuggestions();scroll("advice_$E6_A")
        rule.onNodeWithTag("availability_$E6_A",useUnmergedTree=true).assertTextContains("Dostupné",substring=true)
        shot("fixture-available")
        rule.openCharacter();rule.onNodeWithTag("race_UNDEAD").performScrollTo().performClick()
        rule.onNodeWithTag("save_profile").performScrollTo().performClick();settled{model.profile.race=="UNDEAD"}
        rule.runOnIdle { assertEquals(Availability.EXCLUDED,model.recommendations.all.first { it.entry.key==E6_A }.availability) }
        rule.openSearch();rule.onNodeWithTag("catalog_query").performTextReplacement("4641")
        rule.onNodeWithTag("catalog_$E6_A").performClick()
        rule.onNodeWithText("Vyloučené podmínkou").performScrollTo().assertExists()
    }
    @Test fun completionAndDeferralImmediatelyUpdateFeedAndLeavingChecklist() {
        fixture();scroll("advice_open_$E6_A");rule.onNodeWithTag("advice_open_$E6_A").performClick()
        rule.onNodeWithTag("quest_state_COMPLETED").performScrollTo().performClick();settled{model.questStates[E6_A]==QuestState.COMPLETED}
        rule.onNodeWithText("Zpět").performClick()
        rule.runOnIdle{assertTrue(model.recommendations.all.none{it.entry.key==E6_A})}
        scroll("advice_open_$E6_C");rule.onNodeWithTag("advice_open_$E6_C").performClick()
        rule.onNodeWithTag("quest_state_DEFERRED").performScrollTo().performClick();settled{model.questStates[E6_C]==QuestState.DEFERRED}
        rule.onNodeWithTag("detail_back").performClick();rule.openJournal()
        rule.onNodeWithTag("journey_list").performScrollToNode(hasTestTag("journal_filter_DEFERRED"));rule.onNodeWithTag("journal_filter_DEFERRED").performClick()
        rule.onNodeWithTag("journey_list").performScrollToNode(hasTestTag("journal_$E6_C"));rule.onNodeWithTag("journal_$E6_C").assertExists()
        rule.runOnIdle{assertTrue(model.recommendations.now.none{it.entry.key==E6_C});assertTrue(model.recommendations.leave.none{it.entry.key==E6_C})}
        rule.runOnIdle{model.undoPersonal()};settled{E6_C !in model.questStates}
        rule.runOnIdle{assertTrue(model.recommendations.now.any{it.entry.key==E6_C})}
    }
    @Test fun spoilerChoiceHidesReasonsUntilSingleDetailIsRevealed() {
        fixture();rule.runOnIdle{model.chooseSpoilers(Spoilers.HINTS)}
        scroll("advice_$E6_A");rule.onNodeWithText("Testovací důvod pro 4641").assertDoesNotExist()
        rule.onNodeWithTag("advice_open_$E6_A").performScrollTo().performClick()
        rule.onNodeWithText("Testovací důvod pro 4641").assertDoesNotExist()
        rule.onNodeWithTag("reveal_advice").performScrollTo().performClick()
        rule.onNodeWithText("Testovací důvod pro 4641").performScrollTo().assertIsDisplayed();shot("fixture-reason",true)
        rule.onNodeWithTag("detail_back").performClick()
        rule.onNodeWithText("Testovací důvod pro 4641").assertDoesNotExist()
    }
    @Test fun acquiredWishDisappearsAndRelatedNpcStillOpensRealMap() {
        fixture();rule.runOnIdle{model.toggleBookmark(model.historyEntry(E6_ITEM)!!)};settled{E6_ITEM in model.bookmarks}
        rule.openJournal()
        rule.onNodeWithTag("journey_list").performScrollToNode(hasTestTag("journey_$E6_ITEM"))
        rule.onNodeWithTag("journey_$E6_ITEM").performClick()
        rule.onNodeWithTag("item_acquired").performScrollTo().performClick();settled{E6_ITEM in model.acquiredItems}
        rule.onNodeWithText("Zpět").performClick();rule.runOnIdle{assertTrue(model.recommendations.leave.none{it.entry.key==E6_ITEM})}
        rule.runOnIdle{model.toggleBookmark(model.historyEntry(E6_NPC)!!)};settled{E6_NPC in model.bookmarks}
        rule.onNodeWithTag("journey_list").performScrollToNode(hasTestTag("journey_$E6_NPC"))
        rule.onNodeWithTag("journey_$E6_NPC").performClick()
        rule.onNodeWithTag("entry_location_0").performScrollTo().performClick()
        rule.onNodeWithTag("map_1411").assertIsSelected()
        rule.waitUntil(10_000){rule.onAllNodesWithContentDescription("Ruzan",useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
    }
    @Test @Config(sdk=[28],qualifiers="w560dp-h360dp-land")
    fun focusAndSpoilersPersistAcrossRecreationAndFreshViewModelInShortLandscape() {
        rule.openDisplay();rule.onNodeWithTag("focus_EQUIPMENT").performScrollTo().performClick()
        rule.runOnIdle{model.chooseSpoilers(Spoilers.HINTS)}
        rule.waitUntil(10_000){runBlocking{store.session.read().let { it.guideFocus=="EQUIPMENT" && it.spoilers=="HINTS" }}}
        rule.activityRule.scenario.recreate();rule.onNodeWithTag("focus_EQUIPMENT").performScrollTo().assertIsSelected()
        rule.runOnIdle{
            custom=GuideViewModel(rule.activity.application,SavedStateHandle());extras.put("cold",model)
            rule.activity.setContent { GuideTheme { GuideScreen(model) } }
        }
        rule.waitUntil(20_000){rule.onAllNodesWithTag("nav_QUESTS").fetchSemanticsNodes();model.ready || model.dataError!=null}
        rule.runOnIdle{assertNull(model.dataError);assertEquals(GuideFocus.EQUIPMENT,model.guideFocus);assertEquals(Spoilers.HINTS,model.spoilers)}
        rule.openSuggestions();rule.onNodeWithTag("nav_QUESTS").assertIsSelected();scroll("reference_notice");shot("small-landscape")
    }
}
