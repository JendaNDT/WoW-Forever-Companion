package cz.jenda.wowforever

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.data.*
import cz.jenda.wowforever.data.db.QuestState
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

class E09TestApplication:GuideApplication(){override val store by lazy{GuideStore(this,E09Fixture.pair.public.encoded)}}

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=E09TestApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class E09FlowTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private val model get()=ViewModelProvider(rule.activity)[GuideViewModel::class.java]
    private val store get()=(rule.activity.application as GuideApplication).store
    @Before fun ready(){rule.finishWelcomeIfShown();rule.waitUntil(30_000){rule.onAllNodesWithTag("nav_MAP").fetchSemanticsNodes().isNotEmpty()}}
    private fun settled(check:()->Boolean){rule.waitUntil(30_000){rule.onAllNodesWithTag("nav_MAP").fetchSemanticsNodes();!model.isBusy && check()}}
    private fun open(){rule.openSettings();rule.onNodeWithTag("packages").performScrollTo();rule.onNodeWithTag("packages").performClick()}
    private fun scroll(tag:String){rule.onNodeWithTag("packages_list").performScrollToNode(hasTestTag(tag))}
    private fun import(bytes:ByteArray){
        val file=File(rule.activity.cacheDir,"e09-test-${System.nanoTime()}.wfp").apply{writeBytes(bytes)}
        rule.runOnIdle{model.readContentPackage(Uri.fromFile(file))}
        settled{model.packagePreview!=null || model.packageMessage?.contains("balíčk",true)==true}
        file.delete()
    }
    private fun capture(name:String,dialog:Boolean=true){
        lateinit var bitmap:android.graphics.Bitmap
        rule.runOnIdle{bitmap=captureView(if(dialog)org.robolectric.shadows.ShadowDialog.getShownDialogs().last{it.isShowing}.window!!.decorView else rule.activity.window.decorView)}
        File("build/reports/e09-$name.png").also{it.parentFile!!.mkdirs();it.outputStream().use{out->bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out)}}
    }
    @Test fun previewConfirmActualMapSwitchRecreateAndRollbackPreservePersonalProgress(){
        rule.runOnIdle{model.requestQuest("wow-forever:quest:788",QuestState.COMPLETED)};settled{model.questStates["wow-forever:quest:788"]==QuestState.COMPLETED}
        rule.runOnIdle{model.addPlanStop("wow-forever:npc:5765")};settled{model.personalState.stops.size==1}
        val before=runBlocking{store.journal.profileBackup(model.profile.id)}
        open();rule.onNodeWithTag("package_generation").assertTextContains("2",substring=true);capture("manager")
        val seed=E09Fixture.seed(rule.activity)
        val renamed=seed.copy(generation=3,entries=seed.entries.map{if(it.key=="wow-forever:npc:5765")it.copy(nameOriginal="Ruzan — cvičná změna E09") else it})
        import(E09Fixture.zip(rule.activity,renamed))
        scroll("package_preview");rule.onNodeWithTag("package_preview").assertExists();capture("preview")
        rule.runOnIdle{assertEquals(2,model.contentMetadata!!.generation)}
        scroll("package_activate");rule.onNodeWithTag("package_activate").performClick();rule.onNodeWithTag("package_confirm").performClick()
        settled{model.contentMetadata?.generation==3};scroll("package_generation");rule.onNodeWithTag("package_generation").assertTextContains("3",substring=true);capture("active")
        rule.activityRule.scenario.recreate();scroll("package_generation");rule.onNodeWithTag("package_generation").assertTextContains("3",substring=true)
        rule.onNodeWithText("Zavřít balíčky").performClick()
        rule.runOnIdle{model.showPoint("wow-forever:npc:5765")}
        rule.waitUntil(15_000){rule.onAllNodesWithContentDescription("Ruzan — cvičná změna E09 · zastávky 1",useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
        // Markers and tile workers finish independently. Require the Durotar terrain,
        // not just pins over an empty background, before recording the updated map.
        rule.waitUntil(15_000){
            val bounds=rule.onNodeWithTag("native_map").fetchSemanticsNode().boundsInRoot
            var brown=0
            rule.runOnIdle{
                val bitmap=captureView(rule.activity.window.decorView)
                for(y in bounds.top.toInt().coerceAtLeast(0) until bounds.bottom.toInt().coerceAtMost(bitmap.height))
                    for(x in bounds.left.toInt().coerceAtLeast(0) until bounds.right.toInt().coerceAtMost(bitmap.width)){
                        val pixel=bitmap.getPixel(x,y);val r=android.graphics.Color.red(pixel);val g=android.graphics.Color.green(pixel);val b=android.graphics.Color.blue(pixel)
                        if(r in 25..190 && r>g*1.05 && g>b*1.05)brown++
                    }
                bitmap.recycle()
            }
            brown>800
        }
        capture("updated-map",false)
        open();scroll("package_rollback");rule.onNodeWithTag("package_rollback").performClick();rule.onNodeWithTag("package_confirm").performClick()
        settled{model.contentMetadata?.generation==2};scroll("package_generation");rule.onNodeWithTag("package_generation").assertTextContains("2",substring=true);capture("rollback")
        assertEquals(before,runBlocking{store.journal.profileBackup(model.profile.id)})
    }
    @Test fun removedCatalogRecordStaysInPersonalHistoryAndResetRetainsIt(){
        val key="wow-forever:npc:5765"
        rule.runOnIdle{model.addPlanStop(key)};settled{model.personalState.stops.size==1}
        val seed=E09Fixture.seed(rule.activity)
        val reduced=seed.copy(generation=3,entries=seed.entries.filter{it.key!=key},locations=seed.locations.filter{it.entryKey!=key},
            relations=seed.relations.filter{it.ownerKey!=key && key !in it.targetKeys},guideRules=seed.guideRules.filter{it.entryKey!=key})
        open();import(E09Fixture.zip(rule.activity,reduced))
        rule.runOnIdle{assertEquals(1,model.packageRemovedEntries);assertEquals(1,model.packageAffectedRecords)}
        scroll("package_activate");rule.onNodeWithTag("package_activate").performClick();rule.onNodeWithTag("package_confirm").performClick()
        settled{model.contentMetadata?.generation==3}
        rule.runOnIdle{assertEquals("Ruzan",model.expeditionStops.single().name);assertNotNull(model.expeditionStops.single().limitation);assertNull(model.expeditionStops.single().location)}
        scroll("package_reset");rule.onNodeWithTag("package_reset").performClick();rule.onNodeWithTag("package_confirm").performClick();settled{model.contentMetadata?.generation==2}
        rule.runOnIdle{assertEquals(key,model.personalState.stops.single().entryKey);assertNotNull(model.expeditionStops.single().location)}
    }
    @Test fun wrongSignatureShowsAUsefulErrorAndKeepsTheOriginalMapAndPlan(){
        open();import(E09Fixture.zip(rule.activity,archiveChange={it["manifest.sig"]=ByteArray(256)}))
        scroll("package_message");rule.onNodeWithTag("package_message").assertTextContains("Podpis",substring=true);capture("rejected")
        rule.runOnIdle{assertEquals(2,model.contentMetadata!!.generation);assertNull(model.packagePreview);assertEquals(27,model.atlas.points.size)}
    }
}
