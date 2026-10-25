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
@Config(sdk=[28],application=GuideApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class E10FullFlowTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private val model get()=ViewModelProvider(rule.activity)[GuideViewModel::class.java]
    private fun ready(){rule.waitUntil(180_000){rule.onAllNodesWithTag("nav_MAP").fetchSemanticsNodes().isNotEmpty()};rule.onNodeWithTag("nav_MAP").performClick()}
    private fun shot(name:String){rule.runOnIdle{
        val dialog=org.robolectric.shadows.ShadowDialog.getShownDialogs().lastOrNull{it.isShowing}
        val bitmap=captureView(dialog?.window?.decorView ?: rule.activity.window.decorView)
        File("build/reports/e10full-$name.png").apply{parentFile.mkdirs();outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}};bitmap.recycle()
    }}
    @Test fun actualBundledAppSupportsLateWarlockContentAndSearchableSurfaceAndDungeonMaps(){
        ready();rule.runOnIdle{assertEquals(8,model.contentMetadata!!.generation);assertEquals(11912,model.catalogEntries.size)}
        rule.onNodeWithTag("choose_map").performClick();rule.onNodeWithTag("map_selector_query").performTextInput("Dalaran")
        rule.onNodeWithTag("select_map_902959").performClick();rule.onNodeWithTag("map_902959").assertIsSelected();shot("dalaran")
        rule.onNodeWithTag("choose_map").performClick();rule.onNodeWithTag("map_selector_query").performTextInput("Winterspring")
        rule.onNodeWithTag("select_map_1452").performClick();rule.onNodeWithTag("map_1452").assertIsSelected();shot("winterspring")
        rule.onNodeWithTag("catalog").performClick();rule.onNodeWithTag("catalog_query").performTextInput("Dreadsteed of Xoroth")
        rule.onNodeWithTag("catalog_list").performScrollToNode(hasTestTag("catalog_filters"));rule.onNodeWithTag("catalog_filters").performClick()
        rule.onNodeWithTag("catalog_category_CLASS").performClick();rule.onNodeWithTag("apply_catalog_filters").performClick()
        val target=model.catalogEntries.first { it.nameOriginal=="Dreadsteed of Xoroth" }.key
        rule.onNodeWithTag("catalog_list").performScrollToNode(hasTestTag("catalog_$target"));rule.onNodeWithTag("catalog_$target").performClick()
        rule.onNodeWithTag("entry_description").assertTextContains("Warlock",substring=true);shot("warlock60")
        rule.onNodeWithText("Zpět do katalogu").performClick();rule.onNodeWithTag("catalog_close").performClick()
        rule.onNodeWithTag("nav_CHARACTER").performClick();rule.onNodeWithTag("character_form").performScrollToNode(hasTestTag("choose_profile_area"))
        rule.onNodeWithTag("choose_profile_area").performClick();rule.onNodeWithTag("map_selector_query").performTextInput("Winterspring");rule.onNodeWithTag("select_map_1452").performClick()
        rule.onNodeWithTag("save_profile").performScrollTo().performClick();rule.waitUntil(30_000){model.profile.currentUiMapId==1452 && !model.savingProfile}
        rule.activityRule.scenario.recreate();ready();rule.runOnIdle{assertEquals(1452,model.profile.currentUiMapId);assertEquals(8,model.contentMetadata!!.generation)}
    }
}
