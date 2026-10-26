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
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowSystemClock
import java.io.File
import java.time.Duration

class Ui3WelcomeApplication:GuideApplication() {
    override val store by lazy { cz.jenda.wowforever.data.GuideStore(this) }
}
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UiPhase3FlowTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private val model get()=ViewModelProvider(rule.activity)[GuideViewModel::class.java]
    private val store get()=(rule.activity.application as GuideApplication).store
    private fun settled(check:()->Boolean) { rule.waitUntil(30_000) { rule.waitForIdle();!model.isBusy && check() } }
    private fun ready() {
        rule.finishWelcomeIfShown()
        rule.runOnIdle { model.navigate(Destination.MAP);model.changeCurrentArea(1411) }
        settled { model.profile.currentUiMapId==1411 }
        rule.runOnIdle { model.chooseMap(1411);model.dismissMapHint() }
    }
    private fun shot(name:String,sheet:Boolean=false) { rule.runOnIdle {
        val view=if(sheet)ShadowDialog.getShownDialogs().last { it.isShowing }.window!!.decorView else rule.activity.window.decorView
        val image=captureView(view);File("build/reports/ui3-$name.png").apply { parentFile.mkdirs();outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) } };image.recycle()
    } }
    @Test @Config(sdk=[28],application=Ui3WelcomeApplication::class)
    fun welcomeValidatesAndSavesTheChosenCharacterOnceAcrossRecreation() {
        rule.waitUntil(30_000) { rule.onAllNodesWithTag("welcome_start").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("welcome_race_ORC").assertIsSelected()
        rule.onNodeWithTag("welcome_race_UNDEAD").performClick()
        rule.onNodeWithTag("welcome_level").performTextReplacement("61")
        rule.onNodeWithTag("welcome_start").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("welcome_level").performScrollTo().performTextReplacement("12")
        rule.activityRule.scenario.recreate()
        rule.onNodeWithTag("welcome_level").assertTextContains("12")
        shot("welcome")
        rule.onNodeWithTag("welcome_start").performScrollTo().performClick()
        settled { !model.welcomeRequired && model.profile.level==12 }
        rule.runOnIdle { assertEquals("UNDEAD",model.profile.race) }
        rule.activityRule.scenario.recreate();rule.onNodeWithTag("welcome_start").assertDoesNotExist()
        assertTrue(runBlocking { store.session.read().welcomeComplete })
    }
    @Test fun browsingMapDoesNotMoveTheCharacterUntilIAmHereAndAreaPanelOpensOneDetail() {
        ready();rule.chooseMap(1454)
        rule.runOnIdle { assertEquals(1411,model.profile.currentUiMapId) }
        rule.onNodeWithTag("i_am_here").performClick();settled { model.profile.currentUiMapId==1454 }
        rule.chooseMap(1411);rule.onNodeWithTag("i_am_here").assertIsDisplayed()
        rule.onNodeWithTag("points").performClick();rule.onNodeWithTag("area_points_sheet").assertExists()
        rule.onNodeWithTag("points_list").performScrollToNode(hasTestTag("area_detail_wow-forever:npc:5765"))
        shot("area",true)
        rule.onNodeWithTag("area_detail_wow-forever:npc:5765").performClick()
        rule.onNodeWithTag("detail_title").assertTextEquals("Ruzan")
        rule.runOnIdle { assertEquals(1,ShadowDialog.getShownDialogs().count { it.isShowing }) }
        rule.onNodeWithTag("detail_back").performClick();shot("map")
    }
    @Test fun undoBarReturnsTheExactChangeAndOldOrExpiredActionsDoNotUndoANewerOne() {
        ready()
        rule.runOnIdle { model.requestQuest(E6_A,QuestState.IN_PROGRESS) };settled { model.undoNotice!=null }
        val first=model.undoNotice!!.token
        rule.onNodeWithTag("undo_personal").performClick();settled { model.questStates[E6_A]==null }
        rule.runOnIdle { model.requestQuest(E6_A,QuestState.COMPLETED) };settled { model.undoNotice!=null }
        val second=model.undoNotice!!.token
        rule.runOnIdle { model.undoFromNotice(first) }
        rule.runOnIdle { assertEquals(QuestState.COMPLETED,model.questStates[E6_A]) }
        shot("undo")
        ShadowSystemClock.advanceBy(Duration.ofSeconds(6))
        rule.runOnIdle { model.undoFromNotice(second) }
        rule.runOnIdle { assertEquals(QuestState.COMPLETED,model.questStates[E6_A]) }
    }
    @Test fun keepAwakePreferenceOnlyAppliesOnMapAndSurvivesActivityRecreation() {
        ready()
        fun awake():Boolean {
            fun has(view:android.view.View):Boolean=view.keepScreenOn || (view is android.view.ViewGroup && (0 until view.childCount).any { has(view.getChildAt(it)) })
            return has(rule.activity.window.decorView)
        }
        rule.openDisplay();rule.onNodeWithTag("keep_map_awake").performScrollTo().performClick()
        rule.runOnIdle { assertFalse(awake()) }
        rule.onNodeWithTag("nav_MAP").performClick();rule.runOnIdle { assertTrue(awake()) }
        rule.activityRule.scenario.recreate();rule.runOnIdle { assertTrue(awake()) }
        rule.openSearch();rule.runOnIdle { assertFalse(awake()) }
        assertTrue(runBlocking { store.session.read().keepMapScreenOn })
    }
    @Test fun successfulBackupRecordsItsDateAndFailedExportDoesNotReplaceIt() {
        ready()
        rule.runOnIdle { model.requestQuest(E6_A,QuestState.IN_PROGRESS) };settled { model.questStates[E6_A]==QuestState.IN_PROGRESS }
        val output=File(rule.activity.cacheDir,"ui3-backup.json")
        rule.runOnIdle { model.exportBackup(Uri.fromFile(output)) };settled { model.backupState.lastBackupAt>0 }
        val before=model.backupState
        assertEquals(before.personalRevision,before.backedUpRevision)
        val decoded=output.inputStream().use(cz.jenda.wowforever.data.BackupCodec::read)
        assertTrue(decoded.profiles.any { it.quests.any { q->q.questKey==E6_A } })
        rule.runOnIdle { model.requestQuest(E6_A,QuestState.COMPLETED) };settled { model.questStates[E6_A]==QuestState.COMPLETED }
        rule.runOnIdle { model.exportBackup(Uri.fromFile(rule.activity.cacheDir)) };settled { model.dataError!=null }
        assertEquals(before.lastBackupAt,model.backupState.lastBackupAt)
        assertTrue(model.backupState.personalRevision>model.backupState.backedUpRevision)
    }
    @Test fun beforeLeavingReturnsThroughSharedDetailAndDoesNotMarkDataVerified() {
        ready()
        rule.runOnIdle { model.requestQuest(E6_A,QuestState.IN_PROGRESS) };settled { model.questStates[E6_A]==QuestState.IN_PROGRESS && model.recommendations.all.any { it.entry.key==E6_A && it.inProgress } }
        rule.openSuggestions();rule.onNodeWithTag("advice_list").performScrollToNode(hasTestTag("before_leaving"))
        rule.onNodeWithTag("before_leaving").performClick();rule.onNodeWithTag("leaving_$E6_A").performScrollTo().performClick()
        rule.onNodeWithTag("detail_title").assertTextEquals("Your Place in the World")
        rule.onNodeWithTag("detail_back").performClick();rule.onNodeWithTag("before_leaving_sheet").assertExists();shot("before-leaving",true)
        rule.runOnIdle { assertEquals(1,ShadowDialog.getShownDialogs().count { it.isShowing });assertFalse(model.historyEntry(E6_A)!!.releaseConfirmed) }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UiPhase3LargeTextTest:E11LargeTextChecks() {
    @Test fun areaPanelAndNewMapActionsRemainReachableAtTwoHundredPercent() {
        rule.onNodeWithTag("points").performClick();rule.onNodeWithTag("show_completed_quests").performScrollTo().assertIsDisplayed()
        shot("ui3-large-area",true);rule.onNodeWithTag("area_points_close").assertIsDisplayed().performClick()
        rule.onNodeWithTag("fit").assertIsDisplayed();rule.onNodeWithTag("map_layers").assertIsDisplayed()
    }
}
