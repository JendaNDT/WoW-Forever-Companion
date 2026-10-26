package cz.jenda.wowforever

import android.content.Context
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cz.jenda.wowforever.data.CatalogSearchIndex
import cz.jenda.wowforever.data.GuideStore
import cz.jenda.wowforever.map.MapComposeEngine
import cz.jenda.wowforever.model.Viewport
import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class E11FullCatalogDeviceTest {
    @get:Rule val rule = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun report(name: String, value: JSONObject) {
        value.put("environment", "Android emulator / debug APK")
            .put("physical_phone_performance_verified", false)
        File(context.filesDir, "e11/$name.json").apply { parentFile.mkdirs(); writeText(value.toString(2)) }
    }
    @Test fun fullCatalogSearchRetainsResultsAcrossRepeatedQueries() = runBlocking {
        val store = GuideStore(context, bundledAsset = BUNDLED_CONTENT_ASSET)
        try {
            store.initialize()
            val entries = store.entries()
            assertEquals(11912, entries.size)
            val index = CatalogSearchIndex(entries)
            val queries = listOf("Dreadsteed of Xoroth", "Ruzan", "Summon Imp", "Winterspring", "e11-no-such-record-123")
            val expected = queries.associateWith { q -> index.search(q).map { it.key } }
            assertTrue(index.search(queries.first()).any { it.nameOriginal == queries.first() })
            assertTrue(expected.getValue(queries.last()).isEmpty())
            val samples = mutableListOf<Double>()
            repeat(40) { queries.forEach { q ->
                val started = System.nanoTime()
                assertEquals(expected.getValue(q), index.search(q).map { it.key })
                samples.add((System.nanoTime() - started) / 1e6)
            } }
            samples.sort()
            report("full-catalog-search", JSONObject().put("entries", entries.size).put("queries", samples.size)
                .put("p50_ms", samples[samples.size / 2]).put("max_ms", samples.last())
                .put("scope", "stable search results and emulator search kernel only; not phone UI latency"))
        } finally { store.close() }
    }
    @Test fun allBundledMapsSurviveSoakAndMemoryTrimWithRestoredViewports() = runBlocking {
        val store = GuideStore(context, bundledAsset = BUNDLED_CONTENT_ASSET)
        var engine: MapComposeEngine? = null
        try {
            store.initialize()
            val atlas = store.atlas()
            assertEquals(61, atlas.maps.size)
            assertTrue(atlas.points.size > 10000)
            val saved = mutableMapOf<String, Viewport>()
            val requested = ConcurrentHashMap.newKeySet<String>()
            val opened = AtomicInteger()
            val tile = store.packages.tileOpener()
            val mapEngine = MapComposeEngine(context.assets, atlas,
                { id, floor -> saved["$id:$floor"] }, { id, floor, v -> saved["$id:$floor"] = v },
                { path -> tile(path).also { opened.incrementAndGet(); requested.add(path) } })
            engine = mapEngine
            var current by mutableStateOf(atlas.maps.first())
            rule.setContent { key(current.frameKey) {
                mapEngine.Render(current, Modifier.requiredSize(360.dp, 240.dp).testTag("e11-full-map")) {}
            } }
            val seconds = InstrumentationRegistry.getArguments().getString("e11SoakSeconds", "1200").toLong()
            require(seconds in 0..1800)
            val started = android.os.SystemClock.elapsedRealtime()
            var changes = 0
            var cycles = 0
            var peak = 0
            do {
                for (map in atlas.maps) {
                    rule.runOnIdle { current = map }
                    rule.waitForIdle()
                    rule.waitUntil(30000) { requested.any { it.startsWith("${map.tilePath}/") } }
                    rule.runOnIdle {
                        assertTrue(mapEngine.retainedFrameCount <= 2)
                        peak = maxOf(peak, mapEngine.retainedFrameCount)
                        val v = requireNotNull(mapEngine.viewport(map.uiMapId, map.floorId))
                        assertTrue(v.x.isFinite() && v.y.isFinite() && v.scale.isFinite() && v.scale > 0)
                        if (cycles > 0) {
                            val before = requireNotNull(saved[map.frameKey])
                            assertEquals(before.x, v.x, 0.02)
                            assertEquals(before.y, v.y, 0.02)
                            assertEquals(before.scale, v.scale, 0.01)
                        }
                        changes++
                    }
                }
                cycles++
                rule.runOnIdle {
                    mapEngine.trimMemory(current.uiMapId, current.floorId)
                    assertEquals(1, mapEngine.retainedFrameCount)
                }
            } while (cycles < 2 || android.os.SystemClock.elapsedRealtime() - started < seconds * 1000)
            rule.onNodeWithTag("e11-full-map").assertIsDisplayed()
            report("full-map-soak", JSONObject().put("maps", atlas.maps.size).put("points", atlas.points.size)
                .put("area_changes", changes).put("complete_cycles", cycles).put("peak_retained_frames", peak)
                .put("tile_streams_opened", opened.get()).put("duration_ms", android.os.SystemClock.elapsedRealtime() - started)
                .put("scope", "emulator stability, all bundled frames, state retention and memory trim; not physical-phone FPS/PSS acceptance"))
        } finally {
            engine?.let { e -> rule.runOnIdle { e.close(); e.close(); assertEquals(0, e.retainedFrameCount) } }
            store.close()
        }
    }
}
