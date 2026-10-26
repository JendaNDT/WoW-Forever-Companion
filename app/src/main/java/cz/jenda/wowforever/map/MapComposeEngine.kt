package cz.jenda.wowforever.map

import android.content.res.AssetManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.jenda.wowforever.model.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.ui.state.MapState
import ovh.plrapps.mapcompose.ui.state.markers.model.RenderingStrategy

class MapComposeEngine(
    private val assets: AssetManager,
    private val atlas: Atlas,
    private val restored: (Int, Int) -> Viewport?,
    private val save: (Int, Int, Viewport) -> Unit,
    private val tileStream:(String)->java.io.InputStream={path->assets.open(path).buffered()},
) : MapEngine {
    private val states = linkedMapOf<String, MapState>()
    private var pointsByFrame = atlas.points.groupBy { "${it.position.uiMapId}:${it.position.floorId}" }
    internal val retainedFrameCount get() = states.size
    private val installed = mutableMapOf<String, Map<String, MapPoint>>()
    private val focused = mutableMapOf<String,String>()

    private fun state(map: MapDescriptor): MapState {
        states.remove(map.frameKey)?.let { states[map.frameKey]=it; return it }
        // Keep the active and most recent frame. Other tile workers/bitmaps are released.
        if(states.size>=2) {
            val key=states.keys.first()
            val old=atlas.maps.first { it.frameKey==key }
            viewport(old.uiMapId,old.floorId)?.let { save(old.uiMapId,old.floorId,it) }
            states.remove(key)!!.shutdown(); installed.remove(key); focused.remove(key)
        }
        val initial = restored(map.uiMapId, map.floorId)
        return MapState(map.levels, map.width, map.height, workerCount = 2) {
            scale(initial?.scale ?: 1.0)
            maxScale(8.0)
            rotation(0f)
            scroll(initial?.x ?: 0.5, initial?.y ?: 0.5)
        }.apply {
            addLayer(TileStreamProvider { row, col, zoom ->
                tileStream("${map.tilePath}/$zoom/$row/$col.png")
            })
            setMapBackground(Color(0xFF252728))
            addClusterer("npcs", clusteringThreshold = 48.dp) { ids ->
                { Pin(ids.size.toString(), "Skupina ${ids.size} bodů") }
            }
            syncMarkers(map, this)
        }.also { states[map.frameKey]=it }
    }

    private fun syncMarkers(map: MapDescriptor, state: MapState) {
        val wanted = pointsByFrame[map.frameKey].orEmpty().associateBy { it.id }
        val old = installed[map.frameKey].orEmpty()
        old.forEach { (id, point) -> if (wanted[id] != point) state.removeMarker(id) }
        wanted.forEach { (id, point) -> if (old[id] != point) {
            val (x,y) = point.position.normalized(map)
            state.addMarker(id,x,y,relativeOffset=Offset(-0.5f,-0.5f),renderingStrategy=RenderingStrategy.Clustering("npcs")) {
                val label=if(point.planNumbers.size==1)point.planNumbers.single().toString() else if(point.planNumbers.size==2)point.planNumbers.joinToString("·") else point.planNumbers.firstOrNull()?.let { "$it+" }
                Pin(label ?: point.markerRole.symbol,
                    if(point.planNumbers.isEmpty())point.name else "${point.name} · zastávky ${point.planNumbers.joinToString()}",
                    point.kind == PointKind.NOTE, point.approximate,point.planNumbers.isNotEmpty(),point.markerRole.label)
            }
        } }
        installed[map.frameKey] = wanted
        state.setClustererExemptList("npcs",wanted.values.filter { it.planNumbers.isNotEmpty() }.map { it.id }.toSet()+setOfNotNull(focused[map.frameKey]?.takeIf { it in wanted }))
    }
    override fun updatePoints(points: List<MapPoint>) {
        pointsByFrame = points.groupBy { "${it.position.uiMapId}:${it.position.floorId}" }
        atlas.maps.forEach { map -> states[map.frameKey]?.let { syncMarkers(map,it) } }
    }

    @OptIn(FlowPreview::class)
    @Composable override fun Render(map: MapDescriptor, modifier: Modifier, onLongPress: (MapPosition) -> Unit, onSelect: (String) -> Unit) {
        val mapState = remember(map.frameKey) { state(map) }
        val callback by rememberUpdatedState(onSelect)
        val longPress by rememberUpdatedState(onLongPress)
        DisposableEffect(mapState) {
            mapState.onMarkerClick { id, _, _ -> callback(id) }
            fun addNote(x: Double, y: Double) {
                val (px,py) = map.transform.percent(x,y)
                if (px.isFinite() && py.isFinite() && px in 0.0..100.0 && py in 0.0..100.0)
                    longPress(MapPosition(map.uiMapId,px,py,map.floorId))
            }
            mapState.onLongPress(::addNote)
            mapState.onMarkerLongPress { _,x,y -> addNote(x,y) }
            onDispose { viewport(map.uiMapId, map.floorId)?.let { save(map.uiMapId, map.floorId, it) } }
        }
        // MapState retains zoom and adjusts the centre when its layout size changes.
        // SavedStateHandle restores normalized coordinates after system process recreation.
        LaunchedEffect(mapState) {
            if (restored(map.uiMapId, map.floorId) == null) {
                mapState.getLayoutSize()
                mapState.scale = mapState.minScale
                mapState.snapScrollTo(0.5, 0.5)
            }
        }
        LaunchedEffect(mapState) {
            snapshotFlow { viewport(map.uiMapId, map.floorId) }.filterNotNull().debounce(400)
                .collect { save(map.uiMapId, map.floorId, it) }
        }
        MapUI(modifier, mapState)
    }

    override suspend fun zoom(mapId: Int, factor: Double, floorId: Int) {
        states["$mapId:$floorId"]?.let { it.scrollTo(it.centroidX, it.centroidY, it.scale * factor) }
    }
    override suspend fun fit(mapId: Int, floorId: Int) {
        states["$mapId:$floorId"]?.let {
            it.rotation = 0f
            it.scrollTo(0.5, 0.5, it.minScale)
        }
    }
    override suspend fun focus(point: MapPoint) {
        states["${point.position.uiMapId}:${point.position.floorId}"]?.let {
            val frame="${point.position.uiMapId}:${point.position.floorId}";focused[frame]=point.id
            it.setClustererExemptList("npcs", installed[frame].orEmpty().values.filter { p->p.planNumbers.isNotEmpty() }.map { p->p.id }.toSet()+point.id)
            val (x, y) = point.position.normalized(atlas.map(point.position.uiMapId,point.position.floorId))
            it.scrollTo(x, y, maxOf(it.scale, it.minScale * 3.0))
        }
    }
    override suspend fun north(mapId: Int, floorId: Int) { states["$mapId:$floorId"]?.rotateTo(0f) }
    override fun viewport(mapId: Int, floorId: Int): Viewport? = states["$mapId:$floorId"]?.let {
        Viewport(it.centroidX, it.centroidY, it.scale, it.rotation)
    }
    override fun trimMemory(activeMapId:Int,activeFloorId:Int) {
        states.keys.filter { it!="$activeMapId:$activeFloorId" }.forEach { key ->
            val map=atlas.maps.first { it.frameKey==key }
            viewport(map.uiMapId,map.floorId)?.let { save(map.uiMapId,map.floorId,it) }
            states.remove(key)!!.shutdown();installed.remove(key);focused.remove(key)
        }
    }
    override fun close() { states.values.forEach { it.shutdown() }; states.clear(); installed.clear();focused.clear() }
}

@Composable private fun Pin(label: String, description: String, note: Boolean = false, approximate: Boolean = false,planned:Boolean=false,role:String?=null) {
    val shape = if (note || planned) RoundedCornerShape(8.dp) else CircleShape
    Box(Modifier.size(48.dp).semantics { contentDescription = description;role?.let { stateDescription=it } },contentAlignment=Alignment.Center) {
    Box(Modifier.size(32.dp)
        .background(if(planned)Color(0xFF244B76) else if (note) Color(0xFF245A50) else Color(0xFF742A38), shape).border(if (approximate) 3.dp else 2.dp, Color(0xFFF4D398), shape),
        contentAlignment = Alignment.Center) {
        Text(label, color = Color(0xFFFFF3D9), fontSize = if(label.length>3)10.sp else 17.sp, maxLines=1, softWrap=false)
    }
    }
}
