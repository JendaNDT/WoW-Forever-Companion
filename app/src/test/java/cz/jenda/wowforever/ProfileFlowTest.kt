package cz.jenda.wowforever

import androidx.compose.ui.test.*
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.SavedStateHandle
import cz.jenda.wowforever.ui.GuideScreen
import cz.jenda.wowforever.ui.GuideTheme
import cz.jenda.wowforever.data.Destination
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
class ProfileFlowTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    @Before fun ready() {
        rule.waitUntil(20_000) { rule.onAllNodesWithTag("nav_MAP").fetchSemanticsNodes().isNotEmpty() }
        rule.runOnIdle {
            val model=ViewModelProvider(rule.activity)[GuideViewModel::class.java]
            model.navigate(Destination.MAP);model.chooseMap(1411);model.chooseSpoilers(cz.jenda.wowforever.data.Spoilers.FULL)
        }
        rule.waitUntil(20_000) { rule.onAllNodesWithTag("native_map").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun shot(name:String) {
        lateinit var bitmap:android.graphics.Bitmap
        rule.runOnIdle { bitmap=captureView(rule.activity.window.decorView) }
        val file=File("build/reports/e03-$name.png");file.parentFile!!.mkdirs()
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
    }
    private fun back() { rule.runOnIdle {
        val dialog = org.robolectric.shadows.ShadowDialog.getLatestDialog()
        if (dialog?.isShowing == true && dialog is androidx.activity.ComponentDialog) dialog.onBackPressedDispatcher.onBackPressed()
        else rule.activity.onBackPressedDispatcher.onBackPressed()
    } }
    @Test fun profileValidatesAndRetainsDraftAcrossNavigationAndRecreation() {
        rule.openCharacter()
        rule.onNodeWithTag("profile_name").performTextReplacement("Jenda")
        rule.onNodeWithTag("profile_level").performTextReplacement("61")
        rule.onNodeWithTag("save_profile").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("profile_level").performScrollTo().performTextReplacement("12")
        rule.openJournal()
        back()
        rule.onNodeWithTag("profile_level").performScrollTo().assertTextContains("12")
        rule.activityRule.scenario.recreate()
        rule.onNodeWithTag("profile_name").performScrollTo().assertTextContains("Jenda")
        rule.onNodeWithTag("race_TROLL").performScrollTo().performClick()
        rule.onNodeWithTag("area_1454").performScrollTo().performClick()
        rule.onNodeWithTag("save_profile").performScrollTo().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("profile_saved").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("profile_level").performScrollTo()
        shot("profile")
        rule.openSuggestions()
        rule.onNodeWithTag("quick_level").assertTextEquals("12")
        rule.onNodeWithTag("quick_area").assertTextContains("Orgrimmar",substring=true)
        rule.onNodeWithTag("nav_MAP").performClick()
        rule.onNodeWithTag("map_1454").assertIsSelected()
    }
    @Test fun spoilerChoiceHidesDescriptionAndBookmarkOpensFromJourney() {
        rule.openDisplay()
        rule.onNodeWithTag("spoilers_HINTS").performScrollTo().performClick()
        rule.onNodeWithTag("nav_MAP").performClick()
        rule.openSearch()
        rule.onNodeWithTag("catalog_query").performTextReplacement("5778")
        rule.onNodeWithTag("catalog_wow-forever:item:5778").performClick()
        rule.onNodeWithTag("entry_description").assertDoesNotExist()
        rule.onNodeWithTag("reveal_description").performClick()
        rule.onNodeWithTag("entry_description").assertExists()
        rule.onNodeWithTag("bookmark_toggle").performScrollTo().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Odebrat záložku").fetchSemanticsNodes().isNotEmpty() }
        back(); back()
        rule.openJournal()
        rule.onNodeWithTag("journey_wow-forever:item:5778").performClick()
        rule.onNodeWithTag("entry_description").assertDoesNotExist()
        rule.onNodeWithText("Zpět").performClick()
        rule.onNodeWithTag("journey_list").assertIsDisplayed()
        shot("journey")
    }
    @Test @Config(sdk=[28],qualifiers="w560dp-h360dp-land")
    fun shortPhoneWindowKeepsMapControlsAndNavigationVisible() {
        rule.onNodeWithTag("navigation_rail").assertIsDisplayed()
        rule.onNodeWithTag("points").performClick()
        rule.onNodeWithTag("points_list").performScrollToNode(hasTestTag("point_5765"))
        rule.onNodeWithTag("point_5765").performClick()
        listOf("native_map","zoom_in","zoom_out","fit","focus","settings").forEach { rule.onNodeWithTag(it).assertIsDisplayed() }
        shot("small-landscape")
    }
    @Test @Config(sdk=[28],qualifiers="w800dp-h400dp-land")
    fun wideLayoutUsesRailAndNewViewModelReadsPersistedSession() {
        rule.onNodeWithTag("navigation_rail").assertIsDisplayed()
        rule.onNodeWithTag("map_1454").performClick()
        rule.openCharacter()
        rule.onNodeWithTag("save_profile").performScrollTo().assertIsDisplayed()
        rule.openJournal()
        shot("landscape")
        lateinit var app:GuideApplication
        rule.runOnIdle { app=rule.activity.application as GuideApplication }
        rule.waitUntil(10_000) { kotlinx.coroutines.runBlocking { app.store.session.read().history.last() == "QUESTS" } }
        // New ViewModel without retained objects or SavedStateHandle data: cold session read.
        lateinit var restored:GuideViewModel
        rule.runOnIdle {
            restored=GuideViewModel(app,SavedStateHandle())
            rule.activity.setContentForTest(restored)
        }
        rule.waitUntil(20_000) {
            rule.onAllNodesWithTag("nav_QUESTS").fetchSemanticsNodes()
            restored.ready || restored.dataError != null
        }
        assertNull("Cold load error",restored.dataError)
        rule.runOnIdle { assertEquals(Destination.QUESTS,restored.destination);assertEquals(1454,restored.mapId) }
        rule.onNodeWithTag("nav_QUESTS").assertIsSelected()
        rule.onNodeWithTag("nav_MAP").performClick()
        rule.onNodeWithTag("map_1454").assertIsSelected()
    }
}
private fun MainActivity.setContentForTest(model:GuideViewModel) {
    setContent { GuideTheme { GuideScreen(model) } }
}
