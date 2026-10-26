package cz.jenda.wowforever

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
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
class E10Batch2FlowTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private val model get()=ViewModelProvider(rule.activity)[GuideViewModel::class.java]
    private val store get()=(rule.activity.application as GuideApplication).store
    private fun settled(check:()->Boolean){rule.waitUntil(30_000){rule.waitForIdle();!model.isBusy && check()}}
    private fun shot(name:String){rule.runOnIdle{
        val bitmap=captureView(org.robolectric.shadows.ShadowDialog.getShownDialogs().last{it.isShowing}.window!!.decorView)
        File("build/reports/e10b-$name.png").apply{parentFile.mkdirs();outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}};bitmap.recycle()
    }}
    @Test fun actualSignedGenerationFiveImportsIntoE12ShowsCoverageAndFindsNewNpc(){
        rule.waitUntil(30_000){rule.onAllNodesWithTag("nav_MAP").fetchSemanticsNodes().isNotEmpty()}
        rule.runOnIdle{model.requestQuest("wow-forever:quest:788",QuestState.COMPLETED)}
        settled{model.questStates["wow-forever:quest:788"]==QuestState.COMPLETED}
        val before=runBlocking{store.journal.profileBackup(model.profile.id)}
        val file=File(rule.activity.cacheDir,"e10b-real.wfp").apply{writeBytes(e10Batch2Distribution(rule.activity))}
        rule.runOnIdle{model.readContentPackage(Uri.fromFile(file))};settled{model.packagePreview!=null}
        rule.runOnIdle{model.activateContentPackage()};settled{model.contentMetadata?.generation==5}
        file.delete();assertEquals(before,runBlocking{store.journal.profileBackup(model.profile.id)})
        rule.onNodeWithTag("nav_MAP").performClick()
        rule.openSearch();rule.onNodeWithTag("catalog_list").performScrollToNode(hasTestTag("content_coverage"));rule.onNodeWithTag("content_coverage").performClick()
        rule.onNodeWithTag("coverage_counts").assertTextContains("182 položek · 74 přímých poloh · 5 offline map");shot("coverage")
        rule.onNodeWithTag("coverage_scope_1411").performScrollTo().assertTextContains("Startovní výběr 1–10",substring=true)
        rule.onNodeWithText("Zavřít pokrytí").performClick()
        rule.onNodeWithTag("catalog_query").performTextReplacement("Legging It")
        rule.onNodeWithTag("catalog_wow-forever:quest:96821").performClick()
        rule.onNodeWithTag("entry_description").assertTextContains("Gar’Thokovi",substring=true);shot("quest")
        rule.onNodeWithText("Zpět do hledání").performClick()
        rule.onNodeWithTag("catalog_query").performTextReplacement("Pal'juh")
        rule.onNodeWithTag("catalog_wow-forever:npc:275811").performClick()
        rule.onNodeWithTag("entry_location_0").performScrollTo().performClick()
        rule.onNodeWithTag("map_1411").assertIsSelected()
        rule.runOnIdle{assertEquals("wow-forever:npc:275811",model.selectedPoint!!.entryKey)}
        rule.activityRule.scenario.recreate();rule.onNodeWithTag("map_1411").assertIsSelected()
        rule.runOnIdle{assertEquals("wow-forever:npc:275811",model.selectedPoint!!.entryKey)}
        assertEquals(before,runBlocking{store.journal.profileBackup(model.profile.id)})
    }
}
