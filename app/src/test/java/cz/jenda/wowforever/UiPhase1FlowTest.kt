package cz.jenda.wowforever

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import cz.jenda.wowforever.data.Destination
import cz.jenda.wowforever.data.Spoilers
import cz.jenda.wowforever.data.db.QuestState
import kotlinx.coroutines.runBlocking
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
class UiPhase1FlowTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private val model get()=ViewModelProvider(rule.activity)[GuideViewModel::class.java]
    private val store get()=(rule.activity.application as GuideApplication).store
    @Before fun ready() {
        rule.waitUntil(30_000) { rule.onAllNodesWithTag("quick_level_up").fetchSemanticsNodes().isNotEmpty() }
        rule.runOnIdle { model.navigate(Destination.MAP);model.chooseSpoilers(Spoilers.FULL) }
    }
    private fun settled(check:()->Boolean) {
        rule.waitUntil(15_000) { rule.onAllNodesWithTag("quick_level").fetchSemanticsNodes();!model.isBusy && check() }
    }
    private fun shot(name:String,dialog:Boolean=false) {
        rule.runOnIdle {
            val view=if(dialog)org.robolectric.shadows.ShadowDialog.getShownDialogs().last { it.isShowing }.window!!.decorView else rule.activity.window.decorView
            val bitmap=captureView(view)
            File("build/reports/ui1-$name.png").apply { parentFile.mkdirs();outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) } }
            bitmap.recycle()
        }
    }
    @Test fun burstSavesEveryTapAndSurvivesRecreationWithoutChangingOtherProfileFields() {
        val original=model.profile
        rule.onNodeWithTag("quick_level_up").performClick()
        // The remaining taps arrive together while the first change is still pending.
        rule.runOnIdle { repeat(11) { model.changeLevel(1) };model.changeLevel(-1) }
        settled { model.profile.level==original.level+11 }
        val persisted=runBlocking { store.personal.personal().profile(original.id)!! }
        assertEquals(original.copy(level=original.level+11),persisted)
        rule.activityRule.scenario.recreate()
        settled { model.profile.level==persisted.level }
        rule.onNodeWithTag("quick_level").assertTextEquals(persisted.level.toString())
        rule.runOnIdle { repeat(100) { model.changeLevel(1) } }
        settled { model.profile.level==60 }
        rule.onNodeWithTag("quick_level_up").assertIsNotEnabled()
        rule.runOnIdle { repeat(100) { model.changeLevel(-1) } }
        settled { model.profile.level==1 }
        rule.onNodeWithTag("quick_level_down").assertIsNotEnabled()
        shot("map")
    }
    @Test fun areaPickerSavesWithoutFormAndBrowsingMapDoesNotChangeIt() {
        rule.onNodeWithTag("quick_area").performClick()
        rule.onNodeWithTag("map_selector_list").performScrollToNode(hasTestTag("select_map_1454"))
        rule.onNodeWithTag("select_map_1454").performClick()
        settled { model.profile.currentUiMapId==1454 && model.mapId==1454 }
        rule.runOnIdle { model.chooseMap(1411) }
        assertEquals(1454,model.profile.currentUiMapId)
        rule.activityRule.scenario.recreate()
        settled { model.profile.currentUiMapId==1454 && model.mapId==1411 }
        rule.onNodeWithTag("quick_area").assertTextContains("Orgrimmar",substring=true)
    }
    @Test fun detailStartsWithQuestActionsAndFeedAllowsCompletionWithoutOpeningIt() {
        rule.onNodeWithTag("catalog").performClick()
        rule.onNodeWithTag("catalog_query").performTextReplacement("4641")
        rule.onNodeWithTag("catalog_$E6_A").performClick()
        rule.onNodeWithTag("quest_state_COMPLETED").assertIsDisplayed()
        val actions=rule.onNodeWithTag("quest_state_COMPLETED").fetchSemanticsNode().boundsInRoot
        val description=rule.onNodeWithTag("entry_description").fetchSemanticsNode().boundsInRoot
        assertTrue("Quest actions must precede description",actions.top<description.top)
        shot("detail",true)
        rule.onNodeWithText("Zpět do katalogu").performClick()
        rule.onNodeWithTag("catalog_close").performClick()
        rule.runOnIdle { model.saveProfile(model.profile.copy(level=10)) }
        settled { model.profile.level==10 }
        rule.onNodeWithTag("nav_NOW").performClick()
        rule.onNodeWithTag("advice_list").performScrollToNode(hasTestTag("advice_done_$E6_A"))
        rule.onNodeWithTag("advice_done_$E6_A").performClick()
        settled { model.questStates[E6_A]==QuestState.COMPLETED }
        rule.runOnIdle { assertTrue(model.recommendations.all.none { it.entry.key==E6_A }) }
        shot("suggestions")
    }
    @Test fun unusedFieldsAreHiddenAndNorthButtonIsGone() {
        rule.onNodeWithTag("north").assertDoesNotExist()
        rule.onNodeWithTag("nav_CHARACTER").performClick()
        rule.onNodeWithTag("profile_mode").assertDoesNotExist()
        rule.onNodeWithTag("profile_professions").assertDoesNotExist()
        rule.onNodeWithTag("save_profile").performScrollTo().assertIsDisplayed()
    }
}
