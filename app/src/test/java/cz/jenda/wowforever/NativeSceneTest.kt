package cz.jenda.wowforever

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Real Compose/Android view rendering on Robolectric, additional to device instrumentation. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NativeSceneTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Before fun awaitDatabase() {
        rule.waitUntil(20_000) { rule.onAllNodesWithTag("nav_MAP").fetchSemanticsNodes().isNotEmpty() }
        rule.runOnIdle {
            val model=ViewModelProvider(rule.activity)[GuideViewModel::class.java]
            model.navigate(cz.jenda.wowforever.data.Destination.MAP);model.chooseMap(1411);model.resetLayers()
        }
        rule.waitUntil(20_000) { rule.onAllNodesWithTag("native_map").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun mapsAndNpcListWorkWithNoNetworkPermission() {
        rule.onNodeWithTag("native_map").assertIsDisplayed()
        rule.onNodeWithTag("map_1454").performClick()
        rule.onNodeWithTag("points").performClick()
        rule.onNodeWithText("V této mapě zatím nejsou doložené body. Mapu můžeš prohlížet offline.").assertExists()
        rule.onNodeWithText("Zavřít").performClick()
        rule.onNodeWithTag("map_1411").performClick()
        rule.onNodeWithTag("points").performClick()
        rule.onNodeWithTag("points_list").performScrollToNode(hasTestTag("point_5765"))
        rule.onNodeWithTag("point_5765").performClick()
        rule.onNodeWithText("Ruzan").assertExists()
        rule.onNodeWithTag("focus").assertIsDisplayed()
        // MapCompose clusters on Dispatchers.Default after throttled viewport updates;
        // Compose idleness alone does not mean that its marker has been rendered.
        rule.waitUntil(10_000) {
            rule.onAllNodesWithContentDescription("Ruzan", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        val mapBounds = rule.onNodeWithTag("native_map").fetchSemanticsNode().boundsInRoot
        assertTrue("Map remains useful with the detail card open", mapBounds.height >= 100 * rule.density.density)
        val marker = rule.onNodeWithContentDescription("Ruzan", useUnmergedTree = true).fetchSemanticsNode()
        val centre = marker.positionInRoot + androidx.compose.ui.geometry.Offset(marker.size.width/2f, marker.size.height/2f)
        assertTrue("Selected NPC anchor stays inside the short map viewport", mapBounds.contains(centre))
        lateinit var screenshot: android.graphics.Bitmap
        rule.runOnIdle { screenshot = captureView(rule.activity.window.decorView) }
        val output = File("build/reports/e01-native-scene.png")
        output.parentFile!!.mkdirs()
        output.outputStream().use { screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun zoomAndSelectedPointRemainAfterActivityRecreation() {
        rule.onNodeWithTag("points").performClick()
        rule.onNodeWithTag("points_list").performScrollToNode(hasTestTag("point_5765"))
        rule.onNodeWithTag("point_5765").performClick()
        rule.onNodeWithTag("zoom_in").performClick()
        rule.waitForIdle()
        var scaleBefore = 0.0
        rule.runOnIdle { scaleBefore = ViewModelProvider(rule.activity)[GuideViewModel::class.java].engine.viewport(1411)!!.scale }
        rule.activityRule.scenario.recreate()
        rule.onNodeWithText("Ruzan").assertExists()
        rule.runOnIdle {
            val model = ViewModelProvider(rule.activity)[GuideViewModel::class.java]
            assertEquals(1411, model.mapId)
            assertEquals(scaleBefore, model.engine.viewport(1411)!!.scale, 0.001)
        }
    }

    @Test
    @Config(sdk = [28], qualifiers = "w800dp-h400dp-land")
    fun landscapeKeepsMapAndAllControlsVisible() {
        rule.onNodeWithTag("points").performClick()
        rule.onNodeWithTag("points_list").performScrollToNode(hasTestTag("point_5765"))
        rule.onNodeWithTag("point_5765").performClick()
        rule.onNodeWithTag("native_map").assertIsDisplayed()
        listOf("zoom_in", "zoom_out", "north", "fit", "focus").forEach {
            rule.onNodeWithTag(it).assertIsDisplayed()
        }
        rule.onAllNodesWithText("Ruzan").assertCountEquals(1)
        lateinit var screenshot: android.graphics.Bitmap
        rule.runOnIdle { screenshot = captureView(rule.activity.window.decorView) }
        File("build/reports/e01-landscape.png").outputStream().use {
            screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
