package cz.jenda.wowforever

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import cz.jenda.wowforever.data.Destination
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

abstract class E11LargeTextChecks {
    // Configure real font resources before Activity/window creation.
    @get:Rule(order=0) val fonts=object:ExternalResource() {
        override fun before(){RuntimeEnvironment.setFontScale(2f)}
        override fun after(){RuntimeEnvironment.setFontScale(1f)}
    }
    @get:Rule(order=1) val rule=createAndroidComposeRule<MainActivity>()
    @Before fun ready(){
        rule.waitUntil(30_000){rule.onAllNodesWithTag("nav_MAP").fetchSemanticsNodes().isNotEmpty()}
        rule.runOnIdle { ViewModelProvider(rule.activity)[GuideViewModel::class.java].navigate(Destination.MAP) }
    }
    protected fun shot(name:String,dialog:Boolean=false) {
        rule.runOnIdle {
            val view=if(dialog)org.robolectric.shadows.ShadowDialog.getShownDialogs().last { it.isShowing }.window!!.decorView else rule.activity.window.decorView
            val image=captureView(view)
            File("build/reports/e11-$name.png").apply { parentFile.mkdirs();outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) } }
            image.recycle()
        }
    }
}
@RunWith(RobolectricTestRunner::class)
// Robolectric #8460: explicit w/h qualifiers oscillate Dialog/TextField constraints.
// Keep the default small phone window for dialog checks, with actual 200% font resources.
@Config(sdk=[28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class E11LargeTextTest:E11LargeTextChecks() {
    @Test fun catalogueAtTwoHundredPercentStillAllowsSearchingAndOpeningTheResult() {
        rule.openSearch()
        assertTrue(rule.onNodeWithTag("catalog_query").fetchSemanticsNode().boundsInRoot.height>=40f)
        rule.onNodeWithTag("catalog_query").performTextReplacement("Ruzan")
        rule.onNodeWithTag("catalog_list").performScrollToNode(hasTestTag("catalog_wow-forever:npc:5765"))
        rule.onNodeWithTag("catalog_wow-forever:npc:5765").assertIsDisplayed().performClick()
        rule.onNodeWithText("Ruzan").assertExists();shot("large-text-detail",true)
    }
    @Test fun mapSearchAtTwoHundredPercentCanReachTheNpcAndItsMap() {
        rule.onNodeWithTag("map_search").performClick()
        rule.onNodeWithTag("catalog_query").performTextReplacement("Ruzan")
        rule.onNodeWithTag("catalog_list").performScrollToNode(hasTestTag("result_map_wow-forever:npc:5765"))
        rule.onNodeWithTag("result_map_wow-forever:npc:5765").assertIsDisplayed();shot("large-text-search")
        rule.onNodeWithTag("result_map_wow-forever:npc:5765").performClick()
        rule.onNodeWithText("Ruzan").assertExists();rule.onNodeWithTag("native_map").assertIsDisplayed()
        shot("large-text-map")
        rule.onNodeWithTag("selected_point_detail").performClick()
        rule.onNodeWithTag("entry_sheet").assertExists()
        rule.onNodeWithTag("entry_location_0").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("detail_back").performClick()
    }
}
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],qualifiers="w640dp-h360dp-land")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class E11LandscapeTextTest:E11LargeTextChecks() {
    @Test fun shortLandscapeAtTwoHundredPercentCanReachCharacterAndSave() {
        rule.onNodeWithTag("navigation_rail").assertExists()
        rule.openCharacter()
        rule.onNodeWithTag("character_form").performScrollToNode(hasTestTag("save_profile"))
        rule.onNodeWithTag("save_profile").assertIsDisplayed().performClick();shot("large-text-landscape")
    }
}
