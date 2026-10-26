package cz.jenda.wowforever

import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import cz.jenda.wowforever.data.Destination
import cz.jenda.wowforever.ui.GuideScreen
import cz.jenda.wowforever.ui.GuideTheme
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
class E04MapFlowTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private val model get()=ViewModelProvider(rule.activity)[GuideViewModel::class.java]
    @Before fun ready() {rule.finishWelcomeIfShown();
        rule.waitUntil(20_000) { rule.onAllNodesWithTag("nav_MAP").fetchSemanticsNodes().isNotEmpty() }
        rule.runOnIdle { model.navigate(Destination.MAP);model.chooseMap(1411);model.resetLayers() }
        rule.waitUntil(20_000) { rule.onAllNodesWithTag("native_map").fetchSemanticsNodes().isNotEmpty() }
    }
    @After fun reset() { rule.runOnIdle { model.cancelNote();model.resetLayers();model.navigate(Destination.MAP);model.chooseMap(1411) } }
    private fun shot(name:String,dialog:Boolean=false) {
        lateinit var bitmap:android.graphics.Bitmap
        rule.runOnIdle { bitmap=captureView(if(dialog) org.robolectric.shadows.ShadowDialog.getLatestDialog().window!!.decorView else rule.activity.window.decorView) }
        val file=File("build/reports/e04-$name.png");file.parentFile!!.mkdirs()
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
    }
    @Test fun globalSearchOpensTheCorrectMapAndLinksToCatalog() {
        rule.chooseMap(1454)
        rule.onNodeWithTag("map_search").performClick()
        rule.onNodeWithTag("catalog_query").performTextReplacement("ruzan")
        rule.onNodeWithTag("result_map_wow-forever:npc:5765").performScrollTo().performClick()
        rule.onNodeWithTag("current_map_1411").assertExists()
        rule.onNodeWithTag("point_catalog").performClick()
        rule.onNodeWithTag("entry_location_0").performScrollTo().assertExists()
        shot("catalog-location",true)
        rule.onNodeWithTag("entry_location_0").performClick()
        rule.onNodeWithTag("focus").assertIsDisplayed()
        rule.waitUntil(10_000) { rule.onAllNodesWithContentDescription("Ruzan",useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty() }
        shot("search-map")
    }
    @Test fun layersRemoveMarkersAndBookmarkFilterContainsOnlyTheSavedEntry() {
        rule.onNodeWithTag("map_layers").performClick()
        rule.onNodeWithTag("layer_NPC").performClick()
        rule.onNodeWithText("Hotovo").performClick()
        rule.activityRule.scenario.recreate()
        rule.onNodeWithTag("map_layers").performClick()
        rule.onNodeWithTag("layer_NPC").assertIsNotSelected()
        rule.onNodeWithTag("reset_layers").assertIsDisplayed().performClick()
        rule.onNodeWithTag("layer_NPC").assertIsSelected()
        rule.onNodeWithText("Hotovo").performClick()
        rule.openSearch()
        rule.onNodeWithTag("catalog_query").performTextReplacement("ruzan")
        rule.onNodeWithTag("catalog_wow-forever:npc:5765").performClick()
        rule.onNodeWithTag("bookmark_toggle").performScrollTo().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Odebrat záložku").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Zpět do hledání").performClick();rule.onNodeWithTag("catalog_close").performClick()
        rule.onNodeWithTag("map_layers").performClick()
        rule.onNodeWithTag("layer_bookmarks").performClick()
        shot("layers",true)
        rule.onNodeWithText("Hotovo").performClick()
        rule.runOnIdle { assertEquals(listOf("wow-forever:npc:5765"),model.pointsOnMap().map { it.entryKey }) }
        rule.waitUntil(10_000) { rule.onAllNodesWithContentDescription("Ruzan",useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("Kaltunk",useUnmergedTree=true).assertDoesNotExist()
    }
    @Test fun longPressNoteDraftSurvivesRotationAndSavedNoteCanBeEditedAndDeleted() {
        rule.chooseMap(1454)
        rule.onNodeWithTag("fit").performClick()
        rule.waitForIdle()
        lateinit var before:cz.jenda.wowforever.model.Viewport
        rule.runOnIdle { before=model.engine.viewport(1454)!! }
        rule.onNodeWithTag("native_map").performTouchInput { longClick(center) }
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("note_text").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("note_text").performTextInput("Tady se vrátím")
        rule.activityRule.scenario.recreate()
        rule.onNodeWithTag("note_text").assertTextContains("Tady se vrátím")
        shot("note-editor",true)
        rule.onNodeWithTag("save_note").performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("edit_note").fetchSemanticsNodes().isNotEmpty() }
        lateinit var noteId:String
        rule.runOnIdle {
            val note=model.notes.single { it.text == "Tady se vrátím" };noteId=note.id
            assertEquals(1454,note.uiMapId)
            assertEquals(before.x*100,note.xPercent!!,1.0);assertEquals(before.y*100,note.yPercent!!,1.0)
        }
        shot("note-map")
        rule.onNodeWithTag("map_layers").performClick();rule.onNodeWithTag("layer_NPC").performClick();rule.onNodeWithText("Hotovo").performClick()
        val app=rule.activity.application as GuideApplication
        rule.waitUntil(10_000) { kotlinx.coroutines.runBlocking { app.store.session.read().let { it.selectedId == noteId && "NPC" !in it.visibleKinds } } }
        lateinit var fresh:GuideViewModel
        val freshStore=androidx.lifecycle.ViewModelStore()
        rule.runOnIdle { fresh=GuideViewModel(app,SavedStateHandle());freshStore.put("cold",fresh) }
        try {
            rule.waitUntil(20_000) { rule.onAllNodesWithTag("native_map").fetchSemanticsNodes();fresh.ready || fresh.dataError != null }
            rule.runOnIdle {
                assertNull(fresh.dataError);assertEquals(1454,fresh.mapId);assertEquals(noteId,fresh.selectedPoint!!.id)
                assertEquals("Tady se vrátím",fresh.notes.single { it.id == noteId }.text)
                assertFalse("NPC" in fresh.visibleKinds)
            }
        } finally { rule.runOnIdle { freshStore.clear() } }
        rule.openJournal()
        rule.onNodeWithTag("journey_list").performScrollToNode(hasTestTag("journey_$noteId"))
        rule.onNodeWithTag("journey_$noteId").performClick()
        rule.onNodeWithTag("edit_note").performClick()
        rule.onNodeWithTag("note_text").performTextReplacement("Upravená poznámka")
        rule.onNodeWithTag("save_note").performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("edit_note").fetchSemanticsNodes();!model.savingNote && model.noteDraft==null && model.notes.any { it.id==noteId && it.text=="Upravená poznámka" } }
        rule.onNodeWithTag("edit_note").performClick();rule.onNodeWithTag("delete_note").performClick()
        rule.onNodeWithTag("confirm_delete_note").performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("note_text").fetchSemanticsNodes().isEmpty() && model.notes.none { it.id == noteId } }
        rule.onNodeWithContentDescription("Upravená poznámka",useUnmergedTree=true).assertDoesNotExist()
    }
}
