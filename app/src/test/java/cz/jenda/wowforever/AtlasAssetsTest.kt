package cz.jenda.wowforever

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import cz.jenda.wowforever.data.AtlasRepository
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.ceil

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AtlasAssetsTest {
    private val assets = ApplicationProvider.getApplicationContext<Context>().assets
    @Test fun `all four offline pyramids decode and cover every edge`() {
        val atlas = AtlasRepository(assets).load()
        assertEquals(setOf(947, 1414, 1411, 1454), atlas.maps.map { it.uiMapId }.toSet())
        var count = 0
        atlas.maps.forEach { map ->
            repeat(map.levels) { level ->
                val divisor = 1 shl (map.levels-level-1)
                repeat(ceil(map.height.toDouble()/divisor/256).toInt()) { row ->
                    repeat(ceil(map.width.toDouble()/divisor/256).toInt()) { col ->
                        assets.open("maps/${map.uiMapId}/$level/$row/$col.png").buffered().use {
                            val bitmap = BitmapFactory.decodeStream(it)
                            assertNotNull(bitmap)
                            assertEquals(256, bitmap.width); assertEquals(256, bitmap.height)
                            bitmap.recycle(); count++
                        }
                    }
                }
            }
        }
        assertEquals(68, count)
    }
    @Test fun `legacy atlas keeps its original eight sourced locations`() {
        val atlas = AtlasRepository(assets).load()
        assertEquals(8, atlas.points.size)
        assertEquals(8, atlas.pointsFor(1411).size)
        assertTrue(atlas.pointsFor(1454).isEmpty())
        assertFalse(atlas.points.any { it.id.endsWith(":3156") })
        atlas.points.forEach { assertTrue(it.sourceUrl.startsWith("https://warcraftforever.games/npcs/")) }
    }
}
