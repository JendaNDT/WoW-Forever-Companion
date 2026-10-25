package cz.jenda.wowforever

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.assertEquals
import cz.jenda.wowforever.model.Viewport
import org.junit.Rule
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Before fun awaitDatabase() {
        rule.waitUntil(20_000) { rule.onAllNodesWithTag("native_map").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun offlineMapSelectionAndNpcDetailSurviveRecreation() {
        rule.onNodeWithTag("map_1454").performClick()
        rule.onNodeWithTag("points").performClick()
        rule.onNodeWithText("V této mapě zatím nejsou doložené body. Mapu můžeš prohlížet offline.").assertExists()
        rule.onNodeWithText("Zavřít").performClick()
        rule.onNodeWithTag("map_1411").performClick()
        rule.onNodeWithTag("points").performClick()
        rule.onNodeWithTag("points_list").performScrollToNode(hasTestTag("point_5765"))
        rule.onNodeWithTag("point_5765").performClick()
        rule.onNodeWithText("Ruzan").assertExists()
        rule.onNodeWithTag("zoom_in").performClick()
        rule.waitForIdle()
        lateinit var before: Viewport
        rule.runOnIdle { before = ViewModelProvider(rule.activity)[GuideViewModel::class.java].engine.viewport(1411)!! }
        rule.activityRule.scenario.recreate()
        rule.onNodeWithText("Ruzan").assertExists()
        rule.onNodeWithTag("map_1411").assertIsSelected()
        rule.runOnIdle {
            val after = ViewModelProvider(rule.activity)[GuideViewModel::class.java].engine.viewport(1411)!!
            assertEquals(before.scale, after.scale, 0.001)
            assertEquals(before.x, after.x, 0.01)
            assertEquals(before.y, after.y, 0.01)
        }
        rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        rule.waitForIdle()
        rule.onNodeWithText("Ruzan").assertExists()
        rule.onNodeWithTag("native_map").assertIsDisplayed()
        rule.runOnIdle {
            val rotated = ViewModelProvider(rule.activity)[GuideViewModel::class.java].engine.viewport(1411)!!
            assertEquals(before.scale, rotated.scale, 0.001)
            assertEquals(before.x, rotated.x, 0.01)
            assertEquals(before.y, rotated.y, 0.01)
        }
    }
}
