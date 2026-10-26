package cz.jenda.wowforever

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import cz.jenda.wowforever.data.*
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
class E07FlowTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private val model get()=ViewModelProvider(rule.activity)[GuideViewModel::class.java]
    @Before fun ready() {
        rule.waitUntil(20_000){rule.onAllNodesWithTag("nav_MAP").fetchSemanticsNodes().isNotEmpty()}
        rule.runOnIdle{model.navigate(Destination.MAP);model.chooseMap(1411);model.resetLayers();model.chooseSpoilers(Spoilers.FULL)}
    }
    @After fun reset(){rule.runOnIdle{model.navigate(Destination.MAP);model.chooseMap(1411)}}
    private fun shot(name:String,dialog:Boolean=true) {
        lateinit var bitmap:android.graphics.Bitmap
        rule.runOnIdle{bitmap=captureView(if(dialog)org.robolectric.shadows.ShadowDialog.getShownDialogs().last { it.isShowing }.window!!.decorView else rule.activity.window.decorView)}
        File("build/reports/e07-$name.png").also { it.parentFile!!.mkdirs();it.outputStream().use { stream->bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,stream) } }
    }
    @Test fun pilotCoverageDisclosesActualScopeAndNewDemonTrainerOpensCorrectMap() {
        rule.openSearch();rule.onNodeWithTag("catalog_list").performScrollToNode(hasTestTag("content_coverage"));rule.onNodeWithTag("content_coverage").performClick()
        rule.onNodeWithTag("coverage_counts").assertTextContains("53 položek · 27 přímých poloh · 4 offline map")
        shot("coverage")
        rule.onNodeWithTag("coverage_release").performScrollTo().assertTextContains("0 položek a 0 návazností",substring=true)
        rule.onNodeWithText("Zavřít pokrytí").performClick()
        rule.onNodeWithTag("catalog_query").performTextReplacement("trenér démonů")
        rule.onNodeWithTag("catalog_wow-forever:npc:12776").performClick()
        rule.onNodeWithTag("entry_description").assertTextContains("Trenér démonů",substring=true)
        rule.onNodeWithTag("entry_location_0").performScrollTo();shot("hraug")
        rule.onNodeWithTag("entry_location_0").performClick()
        rule.onNodeWithTag("map_1411").assertIsSelected()
        rule.runOnIdle{assertEquals("wow-forever:npc:12776",model.selectedPoint!!.entryKey)}
        shot("map",false)
    }
    @Test fun galgarShowsThreeDistinctManualChoicesAndWishSurvivesRecreation() {
        rule.runOnIdle{assertEquals(53,model.catalogEntries.size)}
        rule.openSearch();rule.onNodeWithTag("catalog_query").performTextReplacement("4402")
        rule.onNodeWithTag("catalog_wow-forever:quest:4402").assertIsDisplayed().performClick()
        listOf(247840,247841,247846).forEach { id->rule.onNodeWithTag("reward_wow-forever:item:$id").performScrollTo().assertExists() }
        shot("reward-choice")
        rule.onNodeWithText("Zpět do hledání").performClick()
        rule.onNodeWithTag("catalog_query").performTextReplacement("247841")
        rule.onNodeWithTag("catalog_wow-forever:item:247841").performClick()
        rule.onNodeWithTag("bookmark_toggle").performScrollTo().performClick()
        rule.waitUntil(10_000){rule.onAllNodesWithText("Odebrat záložku").fetchSemanticsNodes().isNotEmpty()}
        rule.activityRule.scenario.recreate()
        rule.onNodeWithTag("bookmark_toggle").performScrollTo().assertTextContains("Odebrat záložku",substring=true)
        rule.runOnIdle{assertTrue("wow-forever:item:247841" in model.bookmarks);assertFalse("wow-forever:item:247841" in model.acquiredItems)}
    }
}
