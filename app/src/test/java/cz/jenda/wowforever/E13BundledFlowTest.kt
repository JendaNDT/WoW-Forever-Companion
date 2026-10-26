package cz.jenda.wowforever

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import cz.jenda.wowforever.data.GuideStore
import cz.jenda.wowforever.data.db.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

open class E13HistoricalApplication:GuideApplication() {
    override val store by lazy { GuideStore(this,bundledAsset=E13_BUNDLE) }
}

class E13UpgradeApplication:E13HistoricalApplication() {
    override fun onCreate(){
        super.onCreate()
        runBlocking {
            val prior=GuideStore(this@E13UpgradeApplication)
            try{
                prior.initialize()
                prior.personal.personal().updateProfile(ProfileEntity(displayName="Jenda",level=8))
                prior.journal.setQuest(DEFAULT_PROFILE_ID,"wow-forever:quest:788","Cutting Teeth",QuestState.COMPLETED)
                prior.personal.personal().saveNote(NoteEntity("note:old",DEFAULT_PROFILE_ID,"Moje stará poznámka",null,1411,0,43.0,68.0,1))
            }finally{prior.close()}
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=E13HistoricalApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class E13BundledFlowTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private val model get()=ViewModelProvider(rule.activity)[GuideViewModel::class.java]
    private fun ready(){rule.waitUntil(30_000){rule.onAllNodesWithTag("nav_MAP").fetchSemanticsNodes().isNotEmpty()}}
    private fun shot(name:String){rule.runOnIdle{
        val dialog=org.robolectric.shadows.ShadowDialog.getShownDialogs().lastOrNull{it.isShowing}
        val bitmap=captureView(dialog?.window?.decorView ?: rule.activity.window.decorView)
        File("build/reports/e10c-$name.png").apply{parentFile.mkdirs();outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}};bitmap.recycle()
    }}
    @Test fun productionFirstLaunchShowsAllDataAndOfflineHelpWithoutImport(){
        ready();rule.runOnIdle{assertEquals(6,model.contentMetadata!!.generation);assertEquals(216,model.catalogEntries.size);assertEquals(5,model.atlas.maps.size)}
        rule.openSearch();rule.onNodeWithTag("catalog_list").performScrollToNode(hasTestTag("content_coverage"));rule.onNodeWithTag("content_coverage").performClick()
        rule.onNodeWithTag("coverage_counts").assertTextContains("216 položek · 84 přímých poloh · 5 offline map");shot("integrated-coverage")
        rule.onNodeWithText("Zavřít pokrytí").performClick();rule.onNodeWithTag("catalog_close").performClick()
        rule.openSettings();rule.onNodeWithTag("open_help").performScrollTo().performClick()
        rule.onNodeWithTag("help_current").assertTextContains("Data 6 · 216 položek · 5 map",substring=true);shot("integrated-help")
    }
    @Test @Config(application=E13UpgradeApplication::class)
    fun savedE12ProfileQuestAndNoteArePresentAfterAutomaticUpgradeAndRecreation(){
        ready()
        rule.runOnIdle{
            assertEquals("Jenda",model.profile.displayName);assertEquals(8,model.profile.level)
            assertEquals(QuestState.COMPLETED,model.questStates["wow-forever:quest:788"])
            assertTrue(model.notes.any{it.text=="Moje stará poznámka"});assertEquals(6,model.contentMetadata!!.generation)
        }
        rule.activityRule.scenario.recreate();ready()
        rule.runOnIdle{assertEquals("Jenda",model.profile.displayName);assertEquals(QuestState.COMPLETED,model.questStates["wow-forever:quest:788"]);assertEquals(216,model.catalogEntries.size)}
        shot("preserved-profile")
    }
    @Test fun restoreButtonReturnsAllIntegratedDataAfterRollbackWithoutAFilePicker(){
        ready()
        val store=(rule.activity.application as GuideApplication).store
        val before=runBlocking{store.journal.profileBackup(model.profile.id)}
        rule.openSettings()
        rule.onNodeWithTag("packages").performScrollTo()
        rule.onNodeWithTag("packages").performClick()
        fun scroll(tag:String){rule.onNodeWithTag("packages_list").performScrollToNode(hasTestTag(tag))}
        fun settled(generation:Int){rule.waitUntil(30_000){rule.waitForIdle();!model.isBusy && model.contentMetadata?.generation==generation}}
        scroll("package_rollback");rule.onNodeWithTag("package_rollback").performClick()
        rule.onNodeWithTag("package_confirm").performClick();settled(2)
        scroll("package_reset");rule.onNodeWithTag("package_reset").assertIsEnabled().assertTextContains("Obnovit přibalený obsah")
        rule.onNodeWithTag("package_reset").performClick();rule.onNodeWithTag("package_confirm").performClick();settled(6)
        assertEquals(before,runBlocking{store.journal.profileBackup(model.profile.id)})
        scroll("package_generation");rule.onNodeWithTag("package_generation").assertTextContains("6",substring=true)
        shot("restored-bundled")
        rule.onNodeWithText("Zavřít balíčky").performClick()
    }
}
