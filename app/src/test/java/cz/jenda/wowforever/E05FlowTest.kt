package cz.jenda.wowforever

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import cz.jenda.wowforever.data.*
import cz.jenda.wowforever.content.ContentJson
import kotlinx.serialization.encodeToString
import cz.jenda.wowforever.data.db.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class E05FlowTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private val model get()=ViewModelProvider(rule.activity)[GuideViewModel::class.java]
    private val store get()=(rule.activity.application as GuideApplication).store
    @Before fun ready() {
        rule.waitUntil(20_000){rule.onAllNodesWithTag("nav_MAP").fetchSemanticsNodes().isNotEmpty()}
        rule.runOnIdle {model.navigate(Destination.MAP);model.chooseMap(1411);model.resetLayers();model.chooseSpoilers(Spoilers.FULL)}
    }
    @After fun reset(){rule.runOnIdle{model.cancelChange();model.cancelNote();model.navigate(Destination.MAP)}}
    private fun awaitState(key:String,state:QuestState) {rule.waitUntil(10_000){rule.onAllNodesWithTag("bookmark_toggle").fetchSemanticsNodes();!model.isBusy && model.questStates[key]==state}}
    private fun open(key:String,query:String) {
        rule.openSearch();rule.onNodeWithTag("catalog_query").performTextReplacement(query)
        rule.onNodeWithTag("catalog_list").performScrollToNode(hasTestTag("catalog_$key"))
        rule.onNodeWithTag("catalog_$key").performClick()
    }
    private fun shot(name:String,dialog:Boolean=true){
        lateinit var b:android.graphics.Bitmap
        rule.runOnIdle{b=captureView(if(dialog)org.robolectric.shadows.ShadowDialog.getShownDialogs().last { it.isShowing }.window!!.decorView else rule.activity.window.decorView)}
        val f=File("build/reports/e05-$name.png");f.parentFile!!.mkdirs();f.outputStream().use{b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
    }
    @Test fun questStatesUndoRecreationAndJourneyFilterAreConnected() {
        val key="wow-forever:quest:4641";open(key,"Your Place")
        rule.onNodeWithTag("quest_state_IN_PROGRESS").performScrollTo().performClick();awaitState(key,QuestState.IN_PROGRESS)
        rule.activityRule.scenario.recreate()
        rule.onNodeWithTag("quest_state_IN_PROGRESS").assertIsSelected()
        rule.onNodeWithTag("quest_state_COMPLETED").performScrollTo().performClick();awaitState(key,QuestState.COMPLETED)
        rule.onNodeWithTag("undo_personal").performScrollTo().performClick();awaitState(key,QuestState.IN_PROGRESS)
        rule.onNodeWithTag("quest_state_DEFERRED").performScrollTo().performClick();awaitState(key,QuestState.DEFERRED)
        rule.onNodeWithTag("quest_state_DEFERRED").performScrollTo();shot("quest")
        rule.onNodeWithTag("entry_journey").performScrollTo().performClick()
        rule.onNodeWithTag("journey_list").performScrollToNode(hasTestTag("journal_filter_DEFERRED"))
        rule.onNodeWithTag("journal_filter_DEFERRED").performClick()
        rule.onNodeWithTag("journey_list").performScrollToNode(hasTestTag("journal_$key"))
        rule.onNodeWithTag("journal_$key").assertIsDisplayed();shot("journey",false)
    }
    @Test fun rewardChangeNeedsConfirmationAndAcquisitionStaysIndependent() {
        val key="wow-forever:quest:792";val item="wow-forever:item:5778"
        open(key,"Vile Familiars")
        rule.onNodeWithTag("reward_$item").performScrollTo().performClick()
        rule.waitUntil(10_000){rule.onAllNodesWithTag("reward_none").fetchSemanticsNodes();!model.isBusy && model.rewardChoices[key]?.itemKey==item}
        rule.runOnIdle{assertTrue(model.acquiredItems.isEmpty());assertNull(model.questStates[key])}
        rule.onNodeWithTag("reward_none").performScrollTo().performClick()
        rule.onNodeWithText("Zrušit").performClick()
        rule.runOnIdle{assertEquals(item,model.rewardChoices[key]!!.itemKey)}
        rule.onNodeWithTag("reward_none").performScrollTo().performClick()
        rule.onNodeWithTag("confirm_personal_change").performClick()
        rule.waitUntil(10_000){rule.onAllNodesWithTag("reward_none").fetchSemanticsNodes();!model.isBusy && model.rewardChoices[key]==null}
        rule.onNodeWithTag("undo_personal").performScrollTo().performClick()
        rule.waitUntil(10_000){rule.onAllNodesWithTag("reward_none").fetchSemanticsNodes();!model.isBusy && model.rewardChoices[key]!=null}
        rule.onNodeWithText("Otevřít předmět").performScrollTo().performClick()
        rule.onNodeWithTag("item_acquired").performScrollTo().performClick()
        rule.waitUntil(10_000){rule.onAllNodesWithTag("item_acquired").fetchSemanticsNodes();!model.isBusy && item in model.acquiredItems}
        rule.onNodeWithTag("bookmark_toggle").performScrollTo().performClick()
        rule.waitUntil(10_000){rule.onAllNodesWithTag("bookmark_toggle").fetchSemanticsNodes();!model.isBusy && item in model.bookmarks}
        rule.onNodeWithTag("item_acquired").performScrollTo();shot("item")
    }
    @Test fun reversingCompletedPredecessorNeedsConfirmationAndKeepsDescendantRecord() {
        val first="wow-forever:quest:1485";val next="wow-forever:quest:1499"
        rule.runOnIdle{model.requestQuest(first,QuestState.COMPLETED)}
        awaitState(first,QuestState.COMPLETED)
        rule.runOnIdle{model.requestQuest(next,QuestState.COMPLETED)}
        awaitState(next,QuestState.COMPLETED)
        open(first,"Vile Familiars")
        rule.onNodeWithTag("quest_state_DEFERRED").performScrollTo().performClick()
        rule.onNodeWithText("Zrušit").performClick()
        rule.runOnIdle{assertEquals(QuestState.COMPLETED,model.questStates[first])}
        rule.onNodeWithTag("quest_state_DEFERRED").performScrollTo().performClick()
        rule.onNodeWithTag("confirm_personal_change").performClick();awaitState(first,QuestState.DEFERRED)
        rule.runOnIdle{assertEquals(QuestState.COMPLETED,model.questStates[next]);assertEquals(cz.jenda.wowforever.domain.PrerequisiteStatus.UNVERIFIED,model.questRules.status(next,model.questStates))}
    }
    @Test @Config(sdk=[28],qualifiers="w560dp-h360dp-land")
    fun replacementAndSnapshotRecoveryAreReachableInShortLandscapeDialog() {
        val original=model.profile
        backupDialog()
        val uri=Uri.parse("content://e05.test/replacement.json")
        shadowOf(rule.activity.contentResolver).registerInputStream(uri,BackupCodec.encode(backupFile()).inputStream())
        rule.runOnIdle{model.readBackup(uri)}
        rule.waitUntil(10_000){rule.onAllNodesWithTag("restore_replace").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("restore_replace").performScrollTo().performClick()
        rule.onNodeWithTag("confirm_import").performScrollTo().performClick()
        rule.waitUntil(15_000){rule.onAllNodesWithTag("backup_message").fetchSemanticsNodes();!model.isBusy && model.profile.level==12}
        assertEquals(original.id,model.profile.id)
        val snapshot=model.snapshots.single().id
        rule.onNodeWithText("Zavřít").performClick()
        rule.openJournal()
        rule.onNodeWithTag("journey_list").performScrollToNode(hasTestTag("journal_wow-forever:quest:990001"))
        rule.onNodeWithTag("journal_wow-forever:quest:990001").performClick()
        rule.onNodeWithText("Úkol 990001").assertExists()
        rule.onNodeWithText("Zpět").performClick()
        rule.openSettings()
        rule.onNodeWithTag("backup").performScrollTo().performClick()
        rule.onNodeWithTag("snapshot_$snapshot").performScrollTo().performClick()
        shot("snapshot-confirm")
        rule.onNodeWithTag("confirm_recover").performClick()
        rule.waitUntil(15_000){rule.onAllNodesWithTag("backup_message").fetchSemanticsNodes();!model.isBusy && model.profile==original}
        assertTrue(model.personalState.quests.isEmpty())
    }
    @Test fun verifiedCatalogExclusiveChoiceBlocksOtherBranchUntilPriorStateIsChanged() {
        val a="wow-forever:quest:1485";val b="wow-forever:quest:1499"
        // A synthetic higher-generation package uses the real installer/loader path, never a production asset.
        runBlocking { CatalogInstaller(store.content).install(ContentJson.encodeToString(exclusiveFixture().copy(generation=currentPackage().generation+1)).encodeToByteArray()) }
        val originalStore=androidx.lifecycle.ViewModelStore()
        lateinit var fresh:GuideViewModel
        rule.runOnIdle{
            fresh=GuideViewModel(rule.activity.application,androidx.lifecycle.SavedStateHandle());originalStore.put("exclusive",fresh)
            rule.activity.setContent { cz.jenda.wowforever.ui.GuideTheme { cz.jenda.wowforever.ui.GuideScreen(fresh) } }
        }
        try {
            rule.waitUntil(20_000){rule.onAllNodesWithTag("nav_MAP").fetchSemanticsNodes();fresh.ready || fresh.dataError!=null}
            rule.runOnIdle{fresh.requestQuest(a,QuestState.COMPLETED)}
            rule.waitUntil(10_000){rule.onAllNodesWithTag("nav_MAP").fetchSemanticsNodes();!fresh.isBusy && fresh.questStates[a]==QuestState.COMPLETED}
            rule.runOnIdle{fresh.requestQuest(b,QuestState.IN_PROGRESS)}
            rule.runOnIdle{assertNotNull(fresh.dataError);assertNull(fresh.questStates[b]);fresh.dismissError();fresh.requestQuest(a,QuestState.DEFERRED)}
            rule.waitUntil(10_000){rule.onAllNodesWithTag("nav_MAP").fetchSemanticsNodes();!fresh.isBusy && fresh.questStates[a]==QuestState.DEFERRED}
            rule.runOnIdle{fresh.requestQuest(b,QuestState.IN_PROGRESS)}
            rule.waitUntil(10_000){rule.onAllNodesWithTag("nav_MAP").fetchSemanticsNodes();!fresh.isBusy && fresh.questStates[b]==QuestState.IN_PROGRESS}
        } finally {rule.runOnIdle{originalStore.clear()}}
    }
    private fun backupDialog(){rule.openSettings();rule.onNodeWithTag("backup").performScrollTo().performClick()}
    @Test fun systemDocumentContractsExportPreviewAndDefaultNewProfileRestore() {
        rule.runOnIdle{model.requestQuest("wow-forever:quest:4641",QuestState.DEFERRED)}
        rule.waitUntil(10_000){rule.onAllNodesWithTag("nav_MAP").fetchSemanticsNodes();!model.isBusy && model.questStates.isNotEmpty()}
        val originalId=model.profile.id
        backupDialog()
        rule.onNodeWithTag("export_backup").performScrollTo().performClick()
        val exportIntent=shadowOf(rule.activity).nextStartedActivityForResult
        assertEquals(Intent.ACTION_CREATE_DOCUMENT,exportIntent.intent.action)
        assertEquals("application/json",exportIntent.intent.type)
        val out=ByteArrayOutputStream();val exportUri=Uri.parse("content://e05.test/export.json")
        shadowOf(rule.activity.contentResolver).registerOutputStream(exportUri,out)
        rule.runOnIdle{shadowOf(rule.activity).receiveResult(exportIntent.intent,Activity.RESULT_OK,Intent().setData(exportUri))}
        rule.waitUntil(10_000){rule.onAllNodesWithTag("backup_message").fetchSemanticsNodes().isNotEmpty() && !model.backupBusy}
        val exported=BackupCodec.decode(out.toByteArray());assertEquals(originalId,exported.activeProfileId)
        val before=runBlocking{store.journal.profileBackup(originalId)}
        rule.onNodeWithTag("open_backup").performScrollTo().performClick()
        val importIntent=shadowOf(rule.activity).nextStartedActivityForResult
        assertEquals(Intent.ACTION_OPEN_DOCUMENT,importIntent.intent.action)
        val importUri=Uri.parse("content://e05.test/import.json")
        shadowOf(rule.activity.contentResolver).registerInputStream(importUri,out.toByteArray().inputStream())
        rule.runOnIdle{shadowOf(rule.activity).receiveResult(importIntent.intent,Activity.RESULT_OK,Intent().setData(importUri))}
        rule.waitUntil(10_000){rule.onAllNodesWithTag("restore_new").fetchSemanticsNodes().isNotEmpty()}
        assertEquals(before,runBlocking{store.journal.profileBackup(originalId)})
        rule.onNodeWithTag("restore_new").performScrollTo().assertIsSelected()
        shot("backup-preview")
        rule.onNodeWithTag("confirm_import").performScrollTo().performClick()
        rule.waitUntil(15_000){rule.onAllNodesWithTag("backup_message").fetchSemanticsNodes();!model.isBusy && model.profile.id!=originalId}
        assertEquals(before,runBlocking{store.journal.profileBackup(originalId)})
        assertEquals(QuestState.DEFERRED,model.questStates["wow-forever:quest:4641"])
        rule.activityRule.scenario.recreate()
        rule.onNodeWithTag("profile_choice_${model.profile.id}").performScrollTo().assertIsSelected()
        rule.onNodeWithTag("profile_choice_$originalId").performScrollTo().performClick()
        rule.waitUntil(10_000){rule.onAllNodesWithTag("backup_message").fetchSemanticsNodes();!model.isBusy && model.profile.id==originalId}
    }
    @Test fun invalidDocumentOnlyShowsErrorAndKeepsPersonalDatabaseUntouched() {
        backupDialog();val before=runBlocking{store.journal.profileBackup(model.profile.id)}
        rule.onNodeWithTag("open_backup").performScrollTo().performClick()
        val cancelled=shadowOf(rule.activity).nextStartedActivityForResult
        rule.runOnIdle{shadowOf(rule.activity).receiveResult(cancelled.intent,Activity.RESULT_CANCELED,null)}
        assertNull(model.backupPreview);assertEquals(before,runBlocking{store.journal.profileBackup(model.profile.id)})
        val uri=Uri.parse("content://e05.test/broken.json")
        shadowOf(rule.activity.contentResolver).registerInputStream(uri,"{\"formatVersion\":2}".byteInputStream())
        rule.runOnIdle{model.readBackup(uri)}
        rule.waitUntil(10_000){rule.onAllNodesWithTag("settings").fetchSemanticsNodes();!model.backupBusy && model.dataError!=null}
        assertNull(model.backupPreview);assertEquals(before,runBlocking{store.journal.profileBackup(model.profile.id)})
    }
}
