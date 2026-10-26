package cz.jenda.wowforever.model

import cz.jenda.wowforever.content.TransformRecord
import cz.jenda.wowforever.content.Verification
import cz.jenda.wowforever.domain.MarkerRole

/** uiMapId identifies the coordinate frame. An areaId must never be substituted. */
data class MapDescriptor(
    val uiMapId: Int, val name: String, val width: Int, val height: Int,
    val levels: Int, val build: String, val sourceUrl: String,
    val transform: TransformRecord = TransformRecord(), val floorId: Int = 0,
    val scopeCs:String? = null,
) {
    val frameKey get() = "$uiMapId:$floorId"
    val floorLabel get() = if (uiMapId>=900000) "Přehled" else if (floorId == 0) "Povrch" else "Patro $floorId"
    val tilePath get() = if (floorId == 0) "maps/$uiMapId" else "maps/$uiMapId/floor_$floorId"
}

data class MapPosition(val uiMapId: Int, val xPercent: Double, val yPercent: Double, val floorId: Int = 0) {
    init {
        require(uiMapId > 0)
        require(xPercent.isFinite() && xPercent in 0.0..100.0)
        require(yPercent.isFinite() && yPercent in 0.0..100.0)
    }
    val x: Double get() = xPercent / 100.0
    val y: Double get() = yPercent / 100.0
    fun pixels(map: MapDescriptor): Pair<Double, Double> {
        val (nx, ny) = normalized(map)
        return nx * map.width to ny * map.height
    }
    fun normalized(map: MapDescriptor): Pair<Double, Double> {
        require(map.uiMapId == uiMapId && map.floorId == floorId) { "Coordinate frame mismatch" }
        return map.transform.normalized(xPercent, yPercent)
    }
}

enum class PointKind(val label: String) {
    NPC("Postavy"), QUEST("Úkoly"), ITEM("Předměty"), SPELL("Kouzla"), TRAVEL("Doprava"), OBJECT("Objekty"), NOTE("Moje poznámky")
}
data class MapPoint(val id: String, val name: String, val position: MapPosition, val sourceUrl: String,
    val entryKey: String? = id, val kind: PointKind = PointKind.NPC, val precisionPercent: Double = 0.1,
    val verification: Verification = Verification.BETA, val planNumbers:List<Int> = emptyList(),
    val markerRole:MarkerRole = if(kind==PointKind.NOTE)MarkerRole.NOTE else MarkerRole.PERSON) {
    val approximate get() = precisionPercent > 1.0
}
data class Atlas(val maps: List<MapDescriptor>, val points: List<MapPoint>) {
    init {
        require(maps.isNotEmpty())
        require(maps.map { it.frameKey }.distinct().size == maps.size)
        require(points.map { it.id }.distinct().size == points.size)
        require(points.all { point -> maps.any { it.uiMapId == point.position.uiMapId && it.floorId == point.position.floorId } })
    }
    fun map(id: Int, floor: Int = 0) = maps.first { it.uiMapId == id && it.floorId == floor }
    fun pointsFor(id: Int, floor: Int = 0) = points.filter { it.position.uiMapId == id && it.position.floorId == floor }
}

data class Viewport(val x: Double = 0.5, val y: Double = 0.5, val scale: Double = 1.0, val rotation: Float = 0f)
