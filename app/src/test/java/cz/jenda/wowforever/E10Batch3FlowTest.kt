package cz.jenda.wowforever

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=E13HistoricalApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class E10Batch3FlowTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private val model get()=ViewModelProvider(rule.activity)[GuideViewModel::class.java]
    private fun ready(){rule.waitUntil(30_000){rule.onAllNodesWithTag("nav_MAP").fetchSemanticsNodes().isNotEmpty()};rule.onNodeWithTag("nav_MAP").performClick()}
    private fun find(query:String,key:String){rule.onNodeWithTag("catalog_query").performTextReplacement(query);rule.onNodeWithTag("catalog_$key").performClick()}
    private fun shot(name:String){rule.runOnIdle{
        val dialog=org.robolectric.shadows.ShadowDialog.getShownDialogs().lastOrNull{it.isShowing}
        val bitmap=captureView(dialog?.window?.decorView ?: rule.activity.window.decorView)
        File("build/reports/e10c-$name.png").apply{parentFile.mkdirs();outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}};bitmap.recycle()
    }}
    @Test fun newStaffAndLargerBagAreAvailableOfflineWithoutImport(){
        ready();rule.openSearch()
        find("Makeshift Stormcaller","wow-forever:item:281274")
        rule.onNodeWithTag("entry_description").assertTextContains("+4 Spell Power",substring=true).assertTextContains("pevnou odměnu",substring=true);shot("stormcaller")
        rule.onNodeWithText("Zpět do hledání").performClick()
        find("šest míst","wow-forever:item:11845")
        rule.onNodeWithTag("entry_description").assertTextContains("šest míst",substring=true).assertTextContains("nikoli volbu",substring=true);shot("bag")
    }
    @Test fun orcVoidwalkerQuestLinksAndClassicNpcPointAreUsable(){
        ready();rule.openSearch()
        find("Creature of the Void","wow-forever:quest:1501")
        rule.onNodeWithTag("entry_description").assertTextContains("Tablet of Verga",substring=true).assertTextContains("Undercity",substring=true);shot("voidwalker")
        rule.onNodeWithText("Zpět do hledání").performClick()
        find("5875","wow-forever:npc:5875")
        rule.onNodeWithTag("entry_description").assertTextContains("Classic reference",substring=true)
        rule.onNodeWithTag("entry_location_0").performScrollTo().performClick()
        rule.onNodeWithTag("map_1454").assertIsSelected()
        rule.runOnIdle{assertEquals("wow-forever:npc:5875",model.selectedPoint!!.entryKey);assertEquals(6,model.contentMetadata!!.generation)}
        shot("classic-map")
    }
}
