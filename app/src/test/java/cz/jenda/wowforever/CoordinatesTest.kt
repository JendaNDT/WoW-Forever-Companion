package cz.jenda.wowforever

import cz.jenda.wowforever.model.*
import org.junit.Assert.*
import org.junit.Test

class CoordinatesTest {
    private val durotar = MapDescriptor(1411, "Durotar", 1002, 668, 3, "beta", "https://example.test")
    @Test fun `percentage frame uses top left origin and correct dimensions`() {
        assertEquals(0.0 to 0.0, MapPosition(1411, 0.0, 0.0).pixels(durotar))
        assertEquals(1002.0 to 668.0, MapPosition(1411, 100.0, 100.0).pixels(durotar))
        val (x, y) = MapPosition(1411, 43.1, 68.7).pixels(durotar)
        assertEquals(431.862, x, 0.00001)
        assertEquals(458.916, y, 0.00001)
    }
    @Test fun `area ID cannot replace UI map ID`() {
        assertThrows(IllegalArgumentException::class.java) { MapPosition(14, 43.1, 68.7).pixels(durotar) }
    }
    @Test fun `invalid and nonfinite coordinates are rejected`() {
        listOf(-0.1, 100.1, Double.NaN, Double.POSITIVE_INFINITY).forEach { value ->
            assertThrows(IllegalArgumentException::class.java) { MapPosition(1411, value, 50.0) }
            assertThrows(IllegalArgumentException::class.java) { MapPosition(1411, 50.0, value) }
        }
    }
}
