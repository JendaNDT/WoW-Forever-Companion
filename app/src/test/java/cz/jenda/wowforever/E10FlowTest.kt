package cz.jenda.wowforever

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import cz.jenda.wowforever.data.Destination
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
class E10FlowTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private val model get()=ViewModelProvider(rule.activity)[GuideViewModel::class.java]
    private val store get()=(rule.activity.application as GuideApplication).store
    @Before fun ready(){rule.waitUntil(30_000){rule.onAllNodesWithTag("nav_MAP").fetchSemanticsNodes().isNotEmpty()}}
    private fun settled(check:()->Boolean){rule.waitUntil(30_000){rule.onAllNodesWithTag("nav_MAP").fetchSemanticsNodes();!model.isBusy && check()}}
    private fun openPackages(){rule.openSettings();rule.onNodeWithTag("packages").performScrollTo();rule.onNodeWithTag("packages").performClick()}
    private fun scroll(tag:String){rule.onNodeWithTag("packages_list").performScrollToNode(hasTestTag(tag))}
    private fun install(){
        openPackages();rule.onNodeWithTag("e10_package_hint").assertExists()
        val file=File(rule.activity.cacheDir,"real-e10.wfp").apply{writeBytes(e10Distribution(rule.activity))}
        rule.runOnIdle{model.readContentPackage(Uri.fromFile(file))};settled{model.packagePreview!=null}
        scroll("package_activate");rule.onNodeWithTag("package_activate").performClick();rule.onNodeWithTag("package_confirm").performClick()
        settled{model.contentMetadata?.generation==4};file.delete()
    }
    private fun shot(name:String,dialog:Boolean=true){
        lateinit var bitmap:android.graphics.Bitmap
        rule.runOnIdle{bitmap=captureView(if(dialog)org.robolectric.shadows.ShadowDialog.getShownDialogs().last{it.isShowing}.window!!.decorView else rule.activity.window.decorView)}
        File("build/reports/e10-$name.png").also{it.parentFile!!.mkdirs();it.outputStream().use{out->bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out)}}
    }
    @Test fun realSignedDistributionShowsBarrensTerrainItsActualNpcAndNumberedPlan(){
        install();scroll("package_generation");rule.onNodeWithTag("package_generation").assertTextContains("4",substring=true);shot("generation4")
        rule.onNodeWithText("Zavřít balíčky").performClick()
        rule.runOnIdle{model.addPlanStop("wow-forever:npc:3338")};settled{model.personalState.stops.size==1}
        rule.runOnIdle{model.showPoint("wow-forever:npc:3338")}
        rule.onNodeWithTag("map_1413").assertIsSelected()
        rule.waitUntil(15_000){rule.onAllNodesWithContentDescription("Sergra Darkthorn · zastávky 1",useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
        rule.waitUntil(15_000){
            val bounds=rule.onNodeWithTag("native_map").fetchSemanticsNode().boundsInRoot;var brown=0
            rule.runOnIdle{
                val bitmap=captureView(rule.activity.window.decorView)
                for(y in bounds.top.toInt().coerceAtLeast(0) until bounds.bottom.toInt().coerceAtMost(bitmap.height))
                    for(x in bounds.left.toInt().coerceAtLeast(0) until bounds.right.toInt().coerceAtMost(bitmap.width)){
                        val color=bitmap.getPixel(x,y);val r=android.graphics.Color.red(color);val g=android.graphics.Color.green(color);val b=android.graphics.Color.blue(color)
                        if(r in 25..190 && r>g*1.05 && g>b*1.05)brown++
                    }
                bitmap.recycle()
            };brown>800
        }
        shot("barrens-map",false)
        rule.runOnIdle{assertEquals(139,model.catalogEntries.size);assertEquals(5,model.atlas.maps.size);assertEquals(1413,model.selectedPoint!!.position.uiMapId)}
        rule.activityRule.scenario.recreate();rule.onNodeWithTag("map_1413").assertIsSelected()
        rule.runOnIdle{assertEquals("wow-forever:npc:3338",model.selectedPoint!!.entryKey);assertEquals(1,model.expeditionStops.size)}
    }
    @Test fun rollbackKeepsBarrensNoteCompletedQuestPlanAndMissingAreaDoesNotOverwriteProfile(){
        install();rule.onNodeWithText("Zavřít balíčky").performClick()
        rule.onNodeWithTag("character_form").performScrollToNode(hasTestTag("profile_level"))
        rule.onNodeWithTag("profile_level").performTextReplacement("15")
        rule.onNodeWithTag("character_form").performScrollToNode(hasTestTag("area_1413"))
        rule.onNodeWithTag("area_1413").performClick()
        rule.onNodeWithTag("character_form").performScrollToNode(hasTestTag("save_profile"))
        rule.onNodeWithTag("save_profile").performClick();settled{model.profile.currentUiMapId==1413 && model.profile.level==15}
        rule.runOnIdle{model.requestQuest("wow-forever:quest:842",QuestState.COMPLETED)};settled{model.questStates["wow-forever:quest:842"]==QuestState.COMPLETED}
        rule.runOnIdle{model.addPlanStop("wow-forever:npc:3338")};settled{model.personalState.stops.size==1}
        rule.runOnIdle{model.requestNote(cz.jenda.wowforever.model.MapPosition(1413,51.7,30.3));model.saveNote("Nová poznámka v Crossroads")};settled{model.notes.size==1}
        val before=runBlocking{store.journal.profileBackup(model.profile.id)}
        openPackages();scroll("package_rollback");rule.onNodeWithTag("package_rollback").performClick();rule.onNodeWithTag("package_confirm").performClick();settled{model.contentMetadata?.generation==2}
        rule.onNodeWithText("Zavřít balíčky").performClick()
        rule.onNodeWithTag("character_form").performScrollToNode(hasTestTag("profile_missing_area"));rule.onNodeWithTag("profile_missing_area").assertExists()
        rule.onNodeWithTag("character_form").performScrollToNode(hasTestTag("save_profile"));rule.onNodeWithTag("save_profile").assertIsNotEnabled();shot("missing-area",false)
        rule.runOnIdle{assertEquals(1413,model.profile.currentUiMapId);assertNull(model.expeditionStops.single().location);assertTrue(model.allPoints.none{it.kind==cz.jenda.wowforever.model.PointKind.NOTE})}
        assertEquals(before,runBlocking{store.journal.profileBackup(model.profile.id)})
    }
    @Test fun coverageUsesActiveAreaScopeAndUnlocatedEbruNeverGetsAFakePin(){
        install();rule.onNodeWithText("Zavřít balíčky").performClick();rule.onNodeWithTag("nav_MAP").performClick()
        rule.openSearch();rule.onNodeWithTag("content_coverage").performClick()
        rule.onNodeWithTag("coverage_counts").assertTextContains("139 položek · 62 přímých poloh · 5 offline map")
        rule.onNodeWithTag("coverage_scope_1413").performScrollTo().assertTextContains("Crossroads",substring=true);shot("coverage-barrens")
        rule.onNodeWithTag("coverage_release").performScrollTo().assertTextContains("0 položek a 0 návazností",substring=true)
        rule.onNodeWithText("Zavřít pokrytí").performClick()
        rule.onNodeWithTag("catalog_query").performTextReplacement("Ebru")
        rule.onNodeWithTag("catalog_wow-forever:npc:5768").performClick()
        rule.onNodeWithTag("entry_description").assertTextContains("přesná místní poloha zatím chybí",substring=true)
        rule.onAllNodesWithTag("entry_location_0").assertCountEquals(0);shot("ebru-unlocated")
    }
}
