package cz.jenda.wowforever

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import cz.jenda.wowforever.data.AtlasRepository
import cz.jenda.wowforever.map.MapComposeEngine
import cz.jenda.wowforever.model.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalTestApi::class)
abstract class NativeMapChecks {
    @get:Rule val rule = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val original = AtlasRepository(context.assets).load()

    @Test fun allEightBetaAnchorsRenderAtTheirExpectedScreenPosition() {
        var active by mutableIntStateOf(0)
        val engines = original.points.map { point ->
            MapComposeEngine(context.assets, Atlas(original.maps, listOf(point)), { _, _ -> null }, { _, _, _ -> })
        }
        lateinit var renderedView: android.view.View
        rule.setContent {
            renderedView = LocalView.current
            key(active) {
                engines[active].Render(original.map(1411), Modifier.requiredSize(300.dp, 200.dp).testTag("calibration")) {}
            }
        }
        try {
            original.points.forEachIndexed { index, point ->
                rule.runOnIdle { active = index }
                rule.waitForIdle()
                mapAction { engines[index].fit(1411) }
                rule.waitForIdle()
                rule.waitUntilAtLeastOneExists(hasContentDescription(point.name), timeoutMillis = 10_000)
                val bounds = rule.onNodeWithTag("calibration").fetchSemanticsNode().boundsInRoot
                lateinit var bitmap: Bitmap
                rule.runOnIdle {
                    val full = captureView(renderedView)
                    bitmap = Bitmap.createBitmap(full, bounds.left.roundToInt(), bounds.top.roundToInt(),
                        bounds.width.roundToInt(), bounds.height.roundToInt())
                }
                val viewport = engines[index].viewport(1411)!!
                val expectedX = ((point.position.x - viewport.x) * 1002 * viewport.scale + bitmap.width/2.0).toInt()
                val expectedY = ((point.position.y - viewport.y) * 668 * viewport.scale + bitmap.height/2.0).toInt()
                val offset = (bitmap.width / 300.0 * 12).toInt()
                // Four samples around the expected anchor avoid the white letter in the centre.
                listOf(offset to 0, -offset to 0, 0 to offset, 0 to -offset).forEach { (dx, dy) ->
                    assertTrue("${point.name} native pin around anchor", redPin(bitmap.getPixel(expectedX+dx, expectedY+dy)))
                }
            }
        } finally { rule.runOnIdle { engines.forEach { it.close() } } }
    }

    @Test fun fiveHundredMarkersAllowRepeatedMapOperations() {
        val points = (0 until 500).map { index ->
            MapPoint("stress:$index", "Test $index", MapPosition(1411,
                2.0 + (index % 25)*3.9, 2.0 + (index/25)*4.9), "https://example.test")
        }
        val engine = MapComposeEngine(context.assets, Atlas(original.maps, points), { _, _ -> null }, { _, _, _ -> })
        rule.setContent { engine.Render(original.map(1411), Modifier.fillMaxSize().testTag("stress")) {} }
        try {
            repeat(15) { index ->
                mapAction { engine.focus(points[index*31]); engine.zoom(1411, 1.1); engine.north(1411); engine.fit(1411) }
                rule.waitForIdle()
            }
            rule.onNodeWithTag("stress").assertIsDisplayed()
            assertTrue(engine.viewport(1411)!!.scale.isFinite())
        } finally { rule.runOnIdle { engine.close() } }
    }

    private fun redPin(color: Int): Boolean =
        abs(android.graphics.Color.red(color)-116) < 25 &&
            abs(android.graphics.Color.green(color)-42) < 25 &&
            abs(android.graphics.Color.blue(color)-56) < 25

    private fun mapAction(action: suspend () -> Unit) {
        val job = CoroutineScope(Dispatchers.Main).async { action() }
        rule.waitForIdle()
        rule.waitUntil(60_000) { job.isCompleted }
        runBlocking { job.await() }
    }
}
