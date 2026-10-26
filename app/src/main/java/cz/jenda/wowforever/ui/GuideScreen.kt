package cz.jenda.wowforever.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import cz.jenda.wowforever.data.Destination
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.jenda.wowforever.BuildConfig
import cz.jenda.wowforever.GuideViewModel
import cz.jenda.wowforever.model.MapPoint
import cz.jenda.wowforever.model.PointKind
import kotlinx.coroutines.launch
import java.util.Locale

@Composable fun GuideScreen(model: GuideViewModel) {
    if (!model.ready) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.Center) {
                Text("Forever průvodce", style = MaterialTheme.typography.headlineSmall)
                if (model.dataError == null) { CircularProgressIndicator(); Text("Připravuji offline obsah…") }
                else { Text(model.dataError!!); Button(onClick = model::load) { Text("Zkusit znovu") } }
            }
        }
        return
    }
    GuideReadyScreen(model)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable private fun GuideReadyScreen(model: GuideViewModel) {
    val window = LocalWindowInfo.current.containerSize
    val wide = with(LocalDensity.current) { window.width.toDp() >= 600.dp || (window.width.toDp() >= 480.dp && window.height.toDp() < 480.dp) }
    val compactHeader=LocalDensity.current.fontScale>=1.5f
    var showQuickArea by rememberSaveable { mutableStateOf(false) }
    var showBackup by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var showCoverage by rememberSaveable { mutableStateOf(false) }
    var coverageFromHelp by rememberSaveable { mutableStateOf(false) }
    var showCatalog by rememberSaveable { mutableStateOf(false) }
    var showExpedition by rememberSaveable { mutableStateOf(false) }
    var showPackages by rememberSaveable { mutableStateOf(false) }
    var catalogKey by rememberSaveable { mutableStateOf<String?>(null) }
    val holder = rememberSaveableStateHolder()
    BackHandler(enabled = model.destination != Destination.MAP && !showAbout && !showHelp && !showCoverage && !showCatalog && !showBackup && !showExpedition && !showPackages) { model.back() }
    val openCatalog: () -> Unit = { catalogKey = null; showCatalog = true }
    Surface(Modifier.fillMaxSize().semantics { testTagsAsResourceId=true }) {
        Row(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
            if (wide) NavigationRail(Modifier.fillMaxHeight().testTag("navigation_rail")) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(20.dp))
                Destination.entries.forEach { target ->
                    NavigationRailItem(selected = model.destination == target, onClick = { model.navigate(target) },
                        icon = { Text(target.symbol, fontSize = 24.sp) }, label = { Text(target.label) },
                        modifier = Modifier.testTag("nav_${target.name}"))
                }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Row(Modifier.fillMaxWidth().padding(horizontal=8.dp), verticalAlignment=Alignment.CenterVertically) {
                    Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically) {
                    if(!compactHeader)Text(raceLabel(model.profile.race),style=MaterialTheme.typography.labelMedium,modifier=Modifier.padding(horizontal=8.dp))
                    IconButton(onClick={model.changeLevel(-1)},enabled=model.canQuickEdit && model.headerLevel>1,
                        modifier=Modifier.size(48.dp).testTag("quick_level_down").semantics { contentDescription="Snížit úroveň" }) { Text("−",fontSize=24.sp) }
                    Text("${model.headerLevel}",style=MaterialTheme.typography.titleMedium,
                        modifier=Modifier.testTag("quick_level").semantics { contentDescription="Úroveň ${model.headerLevel}" })
                    IconButton(onClick={model.changeLevel(1)},enabled=model.canQuickEdit && model.headerLevel<60,
                        modifier=Modifier.size(48.dp).testTag("quick_level_up").semantics { contentDescription="Zvýšit úroveň" }) { Text("+",fontSize=24.sp) }
                    TextButton(onClick={showQuickArea=true},enabled=model.canQuickEdit,modifier=Modifier.weight(1f).testTag("quick_area")) {
                        Text((model.atlas.maps.firstOrNull { it.uiMapId==model.headerArea }?.name ?: "Vybrat oblast")+" ▾",maxLines=1,overflow=TextOverflow.Ellipsis)
                    }
                    }
                    IconButton(onClick={showAbout=true},modifier=Modifier.size(48.dp).testTag("about").semantics { contentDescription="O aplikaci" }) { Text("i") }
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    holder.SaveableStateProvider(model.destination.name) {
                        when (model.destination) {
                            Destination.MAP -> MapScreen(model, wide, openCatalog,onPlan={showExpedition=true}) { key -> catalogKey = key; showCatalog = true }
                            Destination.NOW -> NowScreen(model, openCatalog, onEntry={ key -> catalogKey=key;showCatalog=true },onCoverage={coverageFromHelp=false;showCoverage=true})
                            Destination.JOURNEY -> JourneyScreen(model, openCatalog, onBackup={showBackup=true},onPlan={showExpedition=true}) { key -> catalogKey = key; showCatalog = true }
                            Destination.CHARACTER -> CharacterScreen(model,onBackup={showBackup=true},onPackages={showPackages=true})
                        }
                    }
                }
                if (!wide) NavigationBar(windowInsets = WindowInsets(0, 0, 0, 0), modifier = Modifier.testTag("navigation_bar")) {
                    Destination.entries.forEach { target ->
                        NavigationBarItem(selected = model.destination == target, onClick = { model.navigate(target) },
                            icon = { Text(target.symbol, fontSize = 24.sp) }, label = { Text(target.label) },
                            modifier = Modifier.testTag("nav_${target.name}"))
                    }
                }
            }
        }
    }
    if(showQuickArea) MapSelectorDialog(model.atlas.maps,model.headerArea,model::changeCurrentArea,onClose={showQuickArea=false})
    if (model.noteDraft != null) NoteEditorDialog(model)
    if (showAbout) AboutDialog(model,onHelp={showAbout=false;showHelp=true}) { showAbout = false }
    if (showHelp) HelpDialog(model,onClose={showHelp=false},onBackup={showHelp=false;showBackup=true},onPackages={showHelp=false;showPackages=true},onCoverage={showHelp=false;coverageFromHelp=true;showCoverage=true})
    if (showCoverage) ContentCoverageDialog(model,onClose={showCoverage=false;showHelp=coverageFromHelp})
    if (showCatalog) CatalogDialog(model, onClose = { showCatalog = false }, initialEntryKey = catalogKey)
    if (showExpedition) ExpeditionDialog(model,onClose={showExpedition=false})
    if (showBackup) BackupDialog(model) { showBackup=false }
    if (showPackages) PackagesDialog(model) { showPackages=false }
    model.pendingChange?.let { pending ->
        AlertDialog(onDismissRequest=model::cancelChange,title={Text("Potvrdit změnu?")},text={Text(pending.message)},
            confirmButton={TextButton(onClick=model::confirmChange,enabled=!model.isBusy,modifier=Modifier.testTag("confirm_personal_change")){Text("Potvrdit")}},
            dismissButton={TextButton(onClick=model::cancelChange){Text("Zrušit")}})
    }
    if (model.dataError != null) AlertDialog(onDismissRequest = model::dismissError,
        title = { Text("Změnu se nepodařilo dokončit") }, text = { Text(model.dataError!!) },
        confirmButton = { TextButton(onClick = model::dismissError) { Text("Rozumím") } })
}

@Composable private fun MapScreen(model: GuideViewModel, wide: Boolean, onCatalog: () -> Unit,onPlan:()->Unit, onEntry: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var showMapSelector by rememberSaveable { mutableStateOf(false) }
    var showPoints by rememberSaveable { mutableStateOf(false) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showLayers by rememberSaveable { mutableStateOf(false) }
    var showCompactDetail by rememberSaveable(model.selectedId) { mutableStateOf(false) }
    BackHandler(enabled = model.selectedPoint != null && !showPoints && !showSearch && !showLayers && !showCompactDetail && model.noteDraft == null) { model.select(null) }
    val map = model.atlas.map(model.mapId,model.floorId)
    LaunchedEffect(model.focusTarget,map.frameKey) {
        val target=model.focusTarget
        val point=model.selectedPoint
        if(target != null && point?.id == target) {
            withFrameNanos { }; withFrameNanos { }
            model.engine.focus(point);model.consumedFocus(target)
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compactDetail = maxHeight < 420.dp
        val compactMap=LocalDensity.current.fontScale>=1.5f && maxHeight<420.dp
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if(model.atlas.maps.size>8) {
                    FilterChip(selected=true,onClick={showMapSelector=true},label={Text(map.name)},modifier=Modifier.testTag("map_${map.uiMapId}"))
                    OutlinedButton(onClick={showMapSelector=true},modifier=Modifier.testTag("choose_map")){Text("Oblasti (${model.atlas.maps.size})")}
                } else model.atlas.maps.distinctBy { it.uiMapId }.forEach { descriptor ->
                    FilterChip(selected = model.mapId == descriptor.uiMapId,
                        onClick = { model.chooseMap(descriptor.uiMapId) }, label = { Text(descriptor.name) },
                        modifier = Modifier.testTag("map_${descriptor.uiMapId}"))
                }
            }
            Row(Modifier.fillMaxWidth().then(if(compactMap)Modifier.horizontalScroll(rememberScrollState())else Modifier).padding(horizontal = 20.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                val actionModifier=if(compactMap)Modifier else Modifier.weight(1f)
                val actionPadding=PaddingValues(horizontal=if(compactMap)8.dp else 2.dp)
                TextButton(onClick = { showSearch = true }, modifier = actionModifier.testTag("map_search"), contentPadding = actionPadding) { Text("Hledat") }
                TextButton(onClick = { showLayers = true }, modifier = actionModifier.testTag("map_layers"), contentPadding = actionPadding) { Text("Vrstvy") }
                TextButton(onClick = onCatalog, modifier = actionModifier.testTag("catalog"), contentPadding = actionPadding) { Text("Katalog") }
                TextButton(onClick = { showPoints = true }, modifier = actionModifier.testTag("points"), contentPadding = actionPadding) { Text("Body (${model.pointsOnMap().size})",maxLines=1,softWrap=false) }
            }
            val floors = model.atlas.maps.filter { it.uiMapId == model.mapId }
            if (floors.size > 1) Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal=16.dp)) {
                floors.forEach { floor -> FilterChip(selected=floor.floorId == model.floorId,onClick={model.chooseMap(model.mapId,floor.floorId)},label={Text(floor.floorLabel)},modifier=Modifier.testTag("floor_${floor.floorId}")) }
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val compactControls = maxHeight < 220.dp
                val detailWidth = if (maxWidth >= 640.dp) 300.dp else 220.dp
                val compactDetailWidth=maxWidth*0.55f
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxHeight().padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                        .clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                        key(map.frameKey) {
                            key(model.contentEpoch){model.engine.Render(map, Modifier.fillMaxSize().testTag("native_map"), onLongPress = { model.requestNote(it) }) { id -> model.select(id) }}
                        }
                        val controls: @Composable () -> Unit = {
                            MapButton("+", "Přiblížit", "zoom_in") { scope.launch { model.engine.zoom(model.mapId, 1.7, model.floorId) } }
                            MapButton("−", "Oddálit", "zoom_out") { scope.launch { model.engine.zoom(model.mapId, 1.0 / 1.7, model.floorId) } }
                            MapButton("↔", "Celá mapa", "fit") { scope.launch { model.engine.fit(model.mapId,model.floorId) } }
                        }
                        if (compactControls) {
                            Row(Modifier.align(Alignment.TopEnd).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) { controls() }
                        } else {
                            Column(Modifier.align(Alignment.TopEnd).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { controls() }
                        }
                        FilledTonalButton(onClick=onPlan,modifier=Modifier.align(Alignment.BottomEnd).padding(10.dp).testTag("expedition").semantics { contentDescription="Výprava (${model.personalState.stops.size})" }) { Text(if(compactMap)"☆ ${model.personalState.stops.size}" else "Výprava (${model.personalState.stops.size})") }
                        if(compactMap)model.selectedPoint?.let { point ->
                            FilledTonalButton(onClick={showCompactDetail=true},modifier=Modifier.align(Alignment.BottomStart).padding(10.dp).widthIn(max=compactDetailWidth).testTag("compact_point_detail").semantics { contentDescription="Detail bodu ${point.name}" }) {
                                Text(point.name,maxLines=1,overflow=TextOverflow.Ellipsis)
                            }
                        }
                        if(!compactMap)Surface(Modifier.align(Alignment.BottomStart).padding(10.dp), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)) {
                            Column(Modifier.padding(horizontal=12.dp,vertical=8.dp)) {
                                Text("${map.name} · ${model.pointsOnMap().size} bodů",style=MaterialTheme.typography.labelMedium)
                                if(model.personalState.stops.isNotEmpty()) Text("Čísla = pořadí cílů",style=MaterialTheme.typography.labelSmall,modifier=Modifier.testTag("plan_map_legend"))
                            }
                        }
                    }
                    val selected = model.selectedPoint
                    if (wide && selected != null && !compactMap) {
                        PointDetail(model, selected, Modifier.width(detailWidth).fillMaxHeight(),
                            onClose = { model.select(null) }, onEntry = onEntry, onFocus = { scope.launch { model.engine.focus(selected) } })
                    }
                }
            }
            val selected = model.selectedPoint
            if (selected != null && !wide && !compactMap) {
                // A short card works in portrait and in short landscape windows.
                PointDetail(model, selected, Modifier.fillMaxWidth().heightIn(max = 210.dp), compact = compactDetail,
                    onClose = { model.select(null) }, onEntry = onEntry, onFocus = { scope.launch { model.engine.focus(selected) } })
            }
            if (!compactMap && (!compactDetail || selected == null)) Text("Mapy © Blizzard Entertainment • beta ${map.build}", Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if(showCompactDetail)model.selectedPoint?.let { point ->
        AlertDialog(onDismissRequest={showCompactDetail=false},title={Text("Vybraný bod")},text={
            PointDetail(model,point,Modifier.fillMaxWidth().heightIn(max=360.dp).testTag("compact_point_panel"),onClose={showCompactDetail=false},
                onEntry={showCompactDetail=false;onEntry(it)},onFocus={showCompactDetail=false;scope.launch { model.engine.focus(point) }})
        },confirmButton={TextButton(onClick={showCompactDetail=false}){Text("Zavřít detail")}})
    }
    if(showMapSelector) MapSelectorDialog(model.atlas.maps,model.mapId,{model.chooseMap(it)},onClose={showMapSelector=false})
    if (showSearch) SearchDialog(model,onClose={showSearch=false}) { key -> showSearch=false;onEntry(key) }
    if (showLayers) LayerDialog(model) { showLayers=false }
    if (showPoints) AlertDialog(onDismissRequest = { showPoints = false }, title = { Text("Body v oblasti ${map.name}") },
        text = {
            val points = model.pointsOnMap()
            if (points.isEmpty() && model.allPoints.none { it.position.uiMapId == model.mapId && it.position.floorId == model.floorId }) Text("V této mapě zatím nejsou doložené body. Mapu můžeš prohlížet offline.")
            else if (points.isEmpty()) Text("Filtr skrývá všechny body. Uprav vrstvy mapy.")
            else LazyColumn(Modifier.heightIn(max = 380.dp).testTag("points_list")) {
                items(points, key = { it.id }) { point ->
                    TextButton(onClick = {
                        model.showPoint(point.id); showPoints = false
                    }, modifier = Modifier.fillMaxWidth().testTag("point_${point.id.substringAfterLast(':')}")) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(point.name)
                            Text(pointCoordinates(model,point), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { showPoints = false }) { Text("Zavřít") } })
}

@Composable private fun AboutDialog(model: GuideViewModel, onHelp:()->Unit, onClose: () -> Unit) {
    AlertDialog(onDismissRequest = onClose, title = { Text("Forever průvodce") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Verze ${BuildConfig.VERSION_NAME} • data ${model.contentMetadata?.generation ?: 2}")
            OutlinedButton(onClick=onHelp,modifier=Modifier.testTag("open_help")){Text("Návod a obnova postupu")}
            Text("Osobní průvodce pro Warlocka za Hordu od první úrovně. Úroveň a oblast měníš přímo v horní liště; rasu a jméno v části Postava.")
            Text("${model.atlas.maps.size} mapy, ${model.atlas.points.size} poloh, vlastní poznámky a ${model.catalogEntries.size} položek katalogu fungují bez internetu. Záložky se ukládají do tvého profilu. Stavy úkolů, získané předměty a zálohu najdeš v katalogu a v části Moje cesta. Rozsah a mezery otevři v katalogu přes Co průvodce pokrývá.")
            Text("Obsah: generace ${model.contentMetadata?.generation ?: 1}, kontrola ${model.contentMetadata?.checkedOn ?: "2026-10-23"}.")
            Text("Polohy pocházejí z veřejných záznamů bety. Nejsou potvrzené ve vydané hře. Přiblížení nezvyšuje rozlišení podkladu 1002 × 668.")
            Text("Mapy © Blizzard Entertainment. Zdroj: warcraftforever.games. Neoficiální osobní prototyp bez spojení s Blizzardem.")
        }
    }, confirmButton = { TextButton(onClick = onClose) { Text("Rozumím") } })
}

@Composable private fun MapButton(label: String, description: String, tag: String, action: () -> Unit) {
    androidx.compose.material3.FilledTonalIconButton(onClick = action, modifier = Modifier.size(48.dp).testTag(tag)) {
        Text(label, fontSize = 23.sp, modifier = Modifier.semanticsDescription(description))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun PointDetail(model: GuideViewModel, point: MapPoint, modifier: Modifier, compact: Boolean = false, onClose: () -> Unit, onEntry: (String) -> Unit, onFocus: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    var sourceError by remember(point.id) { mutableStateOf(false) }
    Surface(modifier, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = if (compact) 0.dp else 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(point.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (compact) Text(pointCoordinates(model,point) + (if(point.kind == PointKind.NOTE) "" else (if(point.approximate) " • ≈ " else " • ") + when(point.verification) {
                        cz.jenda.wowforever.content.Verification.RELEASE -> "potvrzeno"
                        cz.jenda.wowforever.content.Verification.CLASSIC -> "Classic reference"
                        cz.jenda.wowforever.content.Verification.COMMUNITY -> "Komunitní reference"
                        else -> "beta"
                    }), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                TextButton(onClick = onClose) { Text("Zavřít") }
            }
            if (!compact) {
                Text(pointCoordinates(model,point), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                Text(if(point.kind == PointKind.NOTE) "Vlastní poznámka" else "${precisionLabel(point)} • ${verificationLabel(point.verification)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onFocus, modifier = Modifier.testTag("focus")) { Text("Ukázat na mapě") }
                if (point.entryKey != null) TextButton(onClick={onEntry(point.entryKey)},modifier=Modifier.testTag("point_catalog")) { Text("Katalog") }
                else TextButton(onClick={model.requestNote(point.position,point.id)},modifier=Modifier.testTag("edit_note")) { Text("Upravit") }
                if (!compact && point.sourceUrl.isNotBlank()) TextButton(onClick = { runCatching { uriHandler.openUri(point.sourceUrl) }.onFailure { sourceError = true } }) { Text("Zdroj ↗") }
            }
            if(point.kind == PointKind.NOTE) model.notes.firstOrNull { it.id == point.id }?.text?.takeIf { it != point.name }?.let { Text(it) }
            if (sourceError) Text("Zdroj vyžaduje prohlížeč a připojení k internetu.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun Modifier.semanticsDescription(text: String): Modifier = this.then(Modifier.semantics { contentDescription = text })
