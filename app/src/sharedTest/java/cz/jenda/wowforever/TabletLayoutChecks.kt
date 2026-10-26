package cz.jenda.wowforever

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import cz.jenda.wowforever.data.Destination
import cz.jenda.wowforever.data.Spoilers
import org.junit.*
import org.junit.Assert.*
import java.io.File

abstract class TabletLayoutChecks {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    protected val model get()=ViewModelProvider(rule.activity)[GuideViewModel::class.java]
    protected open fun outputDirectory()=File(rule.activity.getExternalFilesDir(null),"tablet-qa")
    @Before fun readyTablet() {
        rule.finishWelcomeIfShown()
        rule.runOnIdle { model.navigate(Destination.MAP);model.chooseMap(1411);model.dismissMapHint();model.chooseSpoilers(Spoilers.FULL) }
        rule.waitUntil(30_000) { rule.onAllNodesWithTag("native_map").fetchSemanticsNodes().isNotEmpty() }
    }
    protected fun shot(name:String) { rule.runOnIdle {
        val image=captureView(rule.activity.window.decorView)
        outputDirectory().resolve("$name.png").apply { parentFile.mkdirs();outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) } }
        image.recycle()
    } }
    protected fun openRuzanFromArea() {
        rule.onNodeWithTag("points_list").performScrollToNode(hasTestTag("area_detail_wow-forever:npc:5765"))
        rule.onNodeWithTag("area_detail_wow-forever:npc:5765").performClick()
        rule.onNodeWithTag("detail_title").assertTextEquals("Ruzan")
    }
    @Test fun mapAndAreaRemainSideBySideAndDetailBackPreservesViewport() {
        rule.onNodeWithTag("navigation_rail").assertIsDisplayed()
        rule.onNodeWithTag("companion_pane").assertIsDisplayed()
        rule.onNodeWithTag("area_points_sheet").assertDoesNotExist()
        rule.zoomMap()
        val before=model.engine.viewport(1411)!!
        shot("landscape-map-area")
        openRuzanFromArea()
        rule.onNodeWithTag("entry_side_pane").assertIsDisplayed()
        rule.onNodeWithTag("native_map").assertIsDisplayed()
        shot("landscape-map-detail")
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag("entry_side_pane").assertDoesNotExist()
        rule.onNodeWithTag("area_points_panel").assertIsDisplayed()
        rule.runOnIdle {
            val after=model.engine.viewport(1411)!!
            assertEquals(before.scale,after.scale,.001);assertEquals(before.x,after.x,.01);assertEquals(before.y,after.y,.01)
            assertEquals(Destination.MAP,model.destination)
        }
    }
    @Test fun searchCanReplaceTheSideDetailAndKeepsItsQueryWhenClosed() {
        rule.openSearch()
        rule.onNodeWithTag("catalog_query").performTextReplacement("Ruzan")
        rule.onNodeWithTag("catalog_list").performScrollToNode(hasTestTag("catalog_wow-forever:npc:5765"))
        rule.onNodeWithTag("catalog_wow-forever:npc:5765").performClick()
        rule.onNodeWithTag("entry_side_pane").assertIsDisplayed()
        rule.onNodeWithTag("catalog_wow-forever:npc:5765").assertIsSelected()
        rule.onNodeWithTag("catalog_query").assertIsDisplayed().performTextReplacement("5778")
        rule.onNodeWithTag("catalog_list").performScrollToNode(hasTestTag("catalog_wow-forever:item:5778"))
        rule.onNodeWithTag("catalog_wow-forever:item:5778").performClick()
        rule.onNodeWithTag("item_acquired").assertIsDisplayed()
        rule.onNodeWithTag("catalog_wow-forever:item:5778").assertIsSelected()
        shot("landscape-search-detail")
        rule.onNodeWithTag("detail_back").performClick()
        rule.onNodeWithTag("entry_side_pane").assertDoesNotExist()
        rule.onNodeWithTag("catalog_query").assertTextContains("5778")
    }
    @Test fun changingTabsClosesTheDetailAndSettingsHaveComfortableLineWidth() {
        openRuzanFromArea()
        rule.onNodeWithTag("nav_QUESTS").performClick()
        rule.onNodeWithTag("entry_side_pane").assertDoesNotExist()
        rule.openCharacter()
        rule.onNodeWithTag("companion_pane").assertDoesNotExist()
        rule.onNodeWithTag("profile_name").assertIsDisplayed()
        val density=rule.activity.resources.displayMetrics.density
        assertTrue(rule.onNodeWithTag("character_form").fetchSemanticsNode().boundsInRoot.width/density<=840.5f)
        rule.onNodeWithTag("save_profile").performScrollTo().assertIsDisplayed()
        shot("landscape-character")
    }
}
