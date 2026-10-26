package cz.jenda.wowforever.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.jenda.wowforever.GuideViewModel
import cz.jenda.wowforever.model.PointKind
import kotlinx.coroutines.launch

@Composable fun MapScreen(model:GuideViewModel,wide:Boolean,onSearch:()->Unit,onPlan:()->Unit,onPoint:(String)->Unit,onArea:(()->Unit)?=null) {
    val scope=rememberCoroutineScope()
    var showMapSelector by rememberSaveable { mutableStateOf(false) }
    var showArea by rememberSaveable { mutableStateOf(false) }
    var showLayers by rememberSaveable { mutableStateOf(false) }
    val map=model.atlas.map(model.mapId,model.floorId)
    val points=model.pointsOnMap()
    BackHandler(enabled=model.selectedPoint!=null && !showArea && !showLayers && !showMapSelector && model.noteDraft==null) { model.select(null) }
    LaunchedEffect(model.focusTarget,map.frameKey) {
        val id=model.focusTarget
        val point=model.selectedPoint
        if(id!=null && point?.id==id) {
            withFrameNanos { };withFrameNanos { }
            model.engine.focus(point);model.consumedFocus(id)
        }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.weight(1f).testTag("current_map_${map.uiMapId}")) {
                FilterChip(selected=true,onClick={showMapSelector=true},label={Text(map.name+" ▾",maxLines=1,overflow=TextOverflow.Ellipsis)},modifier=Modifier.fillMaxWidth().testTag("choose_map"))
            }
            TextButton(onClick=onSearch,modifier=Modifier.testTag("map_search")){Text("Hledat")}
            TextButton(onClick={showLayers=true},modifier=Modifier.testTag("map_layers").semantics { contentDescription="Další filtry mapy" }){Text("⋯",fontSize=24.sp)}
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=8.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf("Úkoly" to setOf(PointKind.QUEST,PointKind.SPELL),"Postavy" to setOf(PointKind.NPC,PointKind.ITEM,PointKind.OBJECT),"Doprava" to setOf(PointKind.TRAVEL),"Moje" to setOf(PointKind.NOTE)).forEachIndexed { i,(label,kinds)->
                FilterChip(selected=kinds.all { it.name in model.visibleKinds },onClick={model.toggleMapGroup(kinds)},label={Text(label)},modifier=Modifier.testTag("map_group_$i"))
            }
        }
        val floors=model.atlas.maps.filter { it.uiMapId==map.uiMapId }
        if(floors.size>1)Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal=8.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            floors.forEach { floor->FilterChip(selected=floor.floorId==model.floorId,onClick={model.chooseMap(map.uiMapId,floor.floorId)},label={Text(floor.floorLabel)},modifier=Modifier.testTag("floor_${floor.floorId}")) }
        }
        Box(Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)) {
            key(map.frameKey,model.contentEpoch) {
                model.engine.Render(map,Modifier.fillMaxSize().testTag("native_map").semantics {
                    customActions=listOf(CustomAccessibilityAction("Přiblížit"){scope.launch { model.engine.zoom(model.mapId,1.7,model.floorId) };true},
                        CustomAccessibilityAction("Oddálit"){scope.launch { model.engine.zoom(model.mapId,1.0/1.7,model.floorId) };true})
                },onLongPress={model.requestNote(it)}) { id->model.select(id);onPoint(id) }
            }
            FilledTonalIconButton(onClick={scope.launch { model.engine.fit(model.mapId,model.floorId) }},modifier=Modifier.align(Alignment.TopEnd).padding(8.dp).size(48.dp).testTag("fit").semantics { contentDescription="Celá mapa" }) { Text("↔",fontSize=24.sp) }
            if(model.headerArea!=map.uiMapId)FilledTonalButton(onClick={model.changeCurrentArea(map.uiMapId)},enabled=model.canQuickEdit,
                modifier=Modifier.align(Alignment.TopStart).padding(8.dp).testTag("i_am_here")) { Text("Jsem tady") }
            if(!model.mapHintDismissed && model.headerArea==map.uiMapId)Surface(Modifier.align(Alignment.TopStart).padding(start=8.dp,top=8.dp,end=64.dp).widthIn(max=280.dp),shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surface.copy(alpha=.96f)) {
                Column(Modifier.padding(horizontal=10.dp)) {
                    Text("Podrž volné místo pro vlastní poznámku.",style=MaterialTheme.typography.bodySmall)
                    TextButton(onClick=model::dismissMapHint,modifier=Modifier.testTag("dismiss_map_hint")){Text("Rozumím")}
                }
            }
            Text("Mapy © Blizzard Entertainment",style=MaterialTheme.typography.labelSmall.copy(fontSize=9.sp),color=MaterialTheme.colorScheme.onSurface,
                modifier=Modifier.align(Alignment.BottomStart).background(MaterialTheme.colorScheme.surface.copy(alpha=.8f)).padding(horizontal=4.dp,vertical=2.dp))
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
            TextButton(onClick={if(onArea!=null)onArea() else showArea=true},modifier=Modifier.testTag("points")){Text("V této oblasti (${points.size})")}
            FilledTonalButton(onClick=onPlan,modifier=Modifier.testTag("expedition").semantics { contentDescription="Výprava (${model.personalState.stops.size})" }){Text("Výprava (${model.personalState.stops.size})")}
            if(model.personalState.stops.isNotEmpty())Text("Čísla = pořadí cílů",style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(start=8.dp).testTag("plan_map_legend"))
        }
        model.selectedPoint?.let { selected->
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
                TextButton(onClick={onPoint(selected.id)},modifier=Modifier.testTag("selected_point_detail")){Text(selected.name,maxLines=1)}
                TextButton(onClick={scope.launch { model.engine.focus(selected) }},modifier=Modifier.testTag("focus")){Text("Na mapě")}
                if(selected.entryKey!=null)TextButton(onClick={onPoint(selected.id)},modifier=Modifier.testTag("point_catalog")){Text("Detail")}
                else TextButton(onClick={model.requestNote(selected.position,selected.id)},modifier=Modifier.testTag("edit_note")){Text("Upravit")}
                TextButton(onClick={model.select(null)},modifier=Modifier.testTag("close_selected_point")){Text("Zavřít")}
            }
        }
    }
    when {
        showMapSelector->MapSelectorDialog(model.atlas.maps,model.mapId,{model.chooseMap(it)},onClose={showMapSelector=false})
        showLayers->LayerDialog(model){showLayers=false}
        showArea->AreaPointsSheet(model,onClose={showArea=false},onPoint={id->showArea=false;onPoint(id)})
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun AreaPointsSheet(model:GuideViewModel,onClose:()->Unit,onPoint:(String)->Unit) {
    ModalBottomSheet(onDismissRequest=onClose,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),modifier=Modifier.testTag("area_points_sheet")) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(.92f).navigationBarsPadding()) {
            AreaPointsPanel(model,onPoint,onClose)
        }
    }
}

@Composable internal fun AreaPointsPanel(model:GuideViewModel,onPoint:(String)->Unit,onClose:(()->Unit)?=null) {
    val map=model.atlas.map(model.mapId,model.floorId)
    val points=model.pointsOnMap()
        Column(Modifier.fillMaxSize().testTag("area_points_panel")) {
            if(onClose!=null)TextButton(onClick=onClose,modifier=Modifier.testTag("area_points_close")){Text("Zpět na mapu")}
            Text("V této oblasti · ${map.name}",style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(horizontal=20.dp,vertical=12.dp))
            LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("points_list"),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                item {
                    Text("! zadání · ? odevzdání · ◆ cíl nebo zdroj · ✎ poznámka · čísla výpravy",style=MaterialTheme.typography.bodySmall)
                    FilterChip(selected=model.showCompletedQuests,onClick=model::toggleCompletedQuests,label={Text("Zobrazit hotové úkoly")},modifier=Modifier.testTag("show_completed_quests"))
                    if(points.isEmpty())Text(if(model.allPoints.none { it.position.uiMapId==map.uiMapId && it.position.floorId==map.floorId })"V této mapě zatím nejsou doložené body. Mapu můžeš prohlížet offline." else "Filtr skrývá všechny body. Uprav vrstvy mapy.")
                }
                items(points,key={it.id}) { p->
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(horizontal=8.dp)) {
                            TextButton(onClick={model.showPoint(p.id);onClose?.invoke()},modifier=Modifier.fillMaxWidth().testTag("point_${p.id.substringAfterLast(':')}")) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(p.name)
                                    Text((if(p.planNumbers.isNotEmpty())p.planNumbers.joinToString("·") else p.markerRole.symbol)+" · "+p.markerRole.label,style=MaterialTheme.typography.labelSmall)
                                    Text(pointCoordinates(model,p),style=MaterialTheme.typography.labelSmall)
                                }
                            }
                            TextButton(onClick={onPoint(p.id)},modifier=Modifier.testTag("area_detail_${p.id}")){Text("Detail")}
                        }
                    }
                }
            }
        }
}
