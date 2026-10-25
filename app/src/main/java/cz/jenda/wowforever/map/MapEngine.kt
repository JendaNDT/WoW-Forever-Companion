package cz.jenda.wowforever.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cz.jenda.wowforever.model.*

/** All renderer-specific types stay behind this interface. Calls run on the main thread. */
interface MapEngine {
    @Composable fun Render(map: MapDescriptor, modifier: Modifier, onLongPress: (MapPosition) -> Unit = {}, onSelect: (String) -> Unit)
    suspend fun zoom(mapId: Int, factor: Double, floorId: Int = 0)
    suspend fun fit(mapId: Int, floorId: Int = 0)
    suspend fun focus(point: MapPoint)
    suspend fun north(mapId: Int, floorId: Int = 0)
    fun viewport(mapId: Int, floorId: Int = 0): Viewport?
    fun updatePoints(points: List<MapPoint>)
    fun trimMemory(activeMapId:Int,activeFloorId:Int=0) {}
    fun close()
}
