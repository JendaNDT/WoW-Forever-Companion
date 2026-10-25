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
class E12ReleaseFlowTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private val model get()=ViewModelProvider(rule.activity)[GuideViewModel::class.java]
    private val store get()=(rule.activity.application as GuideApplication).store
    @Before fun ready(){rule.waitUntil(30_000){rule.onAllNodesWithTag("nav_MAP").fetchSemanticsNodes().isNotEmpty()}}
    private fun settled(check:()->Boolean){rule.waitUntil(30_000){rule.waitForIdle();!model.isBusy && check()}}
    private fun help(){rule.onNodeWithTag("about").performClick();rule.onNodeWithTag("open_help").performScrollTo().performClick()}
    private fun scroll(tag:String){rule.onNodeWithTag("help_list").performScrollToNode(hasTestTag(tag))}
    private fun shot(name:String){rule.runOnIdle {
        val bitmap=captureView(org.robolectric.shadows.ShadowDialog.getShownDialogs().last{it.isShowing}.window!!.decorView)
        File("build/reports/e12-$name.png").apply {parentFile.mkdirs();outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}};bitmap.recycle()
    }}
    @Test fun offlineHelpOpensBackupWithoutChangingQuestHistoryAndSurvivesRecreation(){
        rule.runOnIdle {model.requestQuest("wow-forever:quest:4641",QuestState.COMPLETED)};settled{model.questStates["wow-forever:quest:4641"]==QuestState.COMPLETED}
        val before=runBlocking{store.journal.profileBackup(model.profile.id)}
        help();rule.onNodeWithTag("help_current").assertTextContains("Data 2",substring=true);shot("help-start")
        rule.activityRule.scenario.recreate();rule.onNodeWithTag("help_list").assertExists()
        scroll("help_backup");shot("help-recovery");rule.onNodeWithTag("help_backup").performClick()
        rule.onNodeWithTag("export_backup").assertExists();assertEquals(before,runBlocking{store.journal.profileBackup(model.profile.id)})
    }
    @Test fun helpUsesActuallyActivatedSignedDataAndOpensItsPackageManager(){
        val file=File(rule.activity.cacheDir,"e12-data4.wfp").apply{writeBytes(e10Distribution(rule.activity))}
        rule.runOnIdle{model.readContentPackage(Uri.fromFile(file))};settled{model.packagePreview!=null}
        rule.runOnIdle{model.activateContentPackage()};settled{model.contentMetadata?.generation==4}
        val before=runBlocking{store.journal.profileBackup(model.profile.id)}
        help();rule.onNodeWithTag("help_current").assertTextContains("Data 4 · 139 položek · 5 map",substring=true);shot("help-data4")
        scroll("help_packages");rule.onNodeWithTag("help_packages").performClick()
        rule.onNodeWithTag("package_generation").assertTextContains("4",substring=true)
        assertEquals(before,runBlocking{store.journal.profileBackup(model.profile.id)});file.delete()
    }
    @Test fun coverageCanReturnToHelpAndBothCloseThroughTheirOwnActions(){
        help();scroll("help_coverage");rule.onNodeWithTag("help_coverage").performClick()
        rule.onNodeWithTag("coverage_counts").assertTextContains("53 položek",substring=true)
        rule.onNodeWithText("Zavřít pokrytí").performClick();rule.onNodeWithTag("help_list").assertExists()
        rule.onNodeWithText("Zavřít návod").performClick();rule.onNodeWithTag("native_map").assertIsDisplayed()
        rule.runOnIdle {assertEquals(Destination.MAP,model.destination)}
    }
}
