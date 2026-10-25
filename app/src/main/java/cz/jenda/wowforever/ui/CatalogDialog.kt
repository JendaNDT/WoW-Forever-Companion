package cz.jenda.wowforever.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import cz.jenda.wowforever.data.Spoilers
import cz.jenda.wowforever.data.searchEntries
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cz.jenda.wowforever.GuideViewModel
import cz.jenda.wowforever.content.*

@OptIn(ExperimentalLayoutApi::class)
@Composable fun CatalogDialog(model: GuideViewModel, onClose: () -> Unit, initialEntryKey: String? = null) {
    var selectedKey by rememberSaveable(initialEntryKey) { mutableStateOf(initialEntryKey) }
    var query by rememberSaveable { mutableStateOf("") }
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    var category by rememberSaveable { mutableStateOf("ALL") }
    var levelBand by rememberSaveable { mutableIntStateOf(0) }
    var onlyArea by rememberSaveable { mutableStateOf(false) }
    val classKeys=remember(model.guideRules) { model.guideRules.filter { it.importance==Importance.CLASS }.map { it.entryKey }.toSet()+WarlockQuestIndex.keys }
    val areaKeys=remember(model.recommendations,model.profile.currentUiMapId) { model.recommendations.all.filter { model.profile.currentUiMapId in it.areaIds }.map { it.entry.key }.toSet() }
    var onlyBookmarks by rememberSaveable { mutableStateOf(false) }
    var coverageOpen by rememberSaveable { mutableStateOf(false) }
    val listState=rememberLazyListState()
    LaunchedEffect(onlyBookmarks,category,levelBand,onlyArea) { listState.scrollToItem(0) }
    var trail by rememberSaveable(initialEntryKey) { mutableStateOf(listOf<String>()) }
    val entry = selectedKey?.let(model::historyEntry)
    val backEntry:()->Unit = {
        if(trail.isNotEmpty()) { selectedKey=trail.last();trail=trail.dropLast(1) }
        else if(initialEntryKey!=null)onClose() else selectedKey=null
    }
    val related:(String)->Unit = { key -> if(model.historyEntry(key)!=null){selectedKey?.let { trail=trail+it };selectedKey=key} }
    val uri = LocalUriHandler.current
    var sourceError by remember { mutableStateOf(false) }
    if (entry != null) {
        val detailScroll=key(entry.key) { rememberScrollState() }
        AlertDialog(onDismissRequest = backEntry, title = { Text(entry.nameOriginal) }, text = {
            Column(Modifier.verticalScroll(detailScroll), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(entryLabel(entry))
                var revealed by rememberSaveable(entry.key, model.spoilers) { mutableStateOf(false) }
                if (model.spoilers == Spoilers.FULL || revealed) Text(entry.descriptionCs, modifier = Modifier.testTag("entry_description"))
                else {
                    Text("Popis je skrytý podle tvého nastavení.")
                    TextButton(onClick = { revealed = true }, modifier = Modifier.testTag("reveal_description")) { Text("Odkrýt tento popis") }
                }
                Text(verificationLabel(entry.verification), color = MaterialTheme.colorScheme.primary)
                if(model.catalogEntries.none { it.key==entry.key }) Text("Osobní historie · položka momentálně chybí v katalogu.")
                ProgressPanel(model,entry,onClose,related)
                OutlinedButton(onClick={model.addPlanStop(entry.key)},enabled=!model.isBusy,modifier=Modifier.testTag("plan_add_entry")) {
                    Text(if(model.personalState.stops.any { it.entryKey==entry.key })"Přidat znovu do výpravy" else "Přidat cíl do výpravy")
                }
                if(model.personalState.stops.any { it.entryKey==entry.key })Text("Ve výpravě: "+model.expeditionStops.filter { it.stored.entryKey==entry.key }.joinToString { it.number.toString() },style=MaterialTheme.typography.bodySmall)
                entry.sourceIds.forEachIndexed { index, id ->
                    model.sourceUrls[id]?.let { url ->
                        TextButton(onClick = { runCatching { uri.openUri(url) }.onFailure { sourceError = true } }) { Text("Zdroj ${index+1} ↗") }
                    }
                }
                if(sourceError) Text("Otevření zdroje vyžaduje prohlížeč a připojení.")
                if (entry.kind == EntryKind.QUEST) Text(entry.minimumLevel?.let { "Minimální úroveň: $it" } ?: "Minimální úroveň není ověřená.")
                Text("Záložka ukládá položku pro pozdější návrat.")
                Button(onClick = { model.toggleBookmark(entry) }, enabled=!model.isBusy,modifier = Modifier.testTag("bookmark_toggle")) {
                    Text(if (entry.key in model.bookmarks) "Odebrat záložku" else "Přidat záložku")
                }
                RelatedEntryLinks(model,entry,related)
                val locations = model.pointsForEntry(entry.key)
                if (locations.isEmpty()) Text("Přímá poloha není doložená. Odkaz na mapu se nenabízí.",style=MaterialTheme.typography.bodySmall)
                locations.forEachIndexed { index, point ->
                    TextButton(onClick={model.showPoint(point.id);onClose()},modifier=Modifier.testTag("entry_location_$index")) {
                        Column { Text("Ukázat na mapě: "+pointCoordinates(model,point));Text(precisionLabel(point),style=MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = backEntry) { Text(if(trail.isNotEmpty()) "Zpět na předchozí" else if (initialEntryKey == null) "Zpět do katalogu" else "Zpět") } })
    } else AlertDialog(onDismissRequest = onClose, title = { Text("Herní katalog") }, text = {
            val entries = remember(model.catalogSearchIndex,query,onlyBookmarks,model.bookmarks,category,levelBand,onlyArea,classKeys,areaKeys) {
                model.searchCatalog(query).filter { e ->
                    (!onlyBookmarks || e.key in model.bookmarks) && (!onlyArea || e.key in areaKeys) &&
                    (when(category) { "CLASS" -> e.key in classKeys; "DUNGEON" -> e.key.startsWith("wow-forever:travel:dungeon_"); "ITEM" -> e.kind==EntryKind.ITEM; else -> true }) &&
                    (levelBand==0 || (e.reportedLevel ?: e.minimumLevel)?.let { it in ((levelBand-1)*10+1)..(levelBand*10) }==true)
                }
            }
            LazyColumn(Modifier.heightIn(max=420.dp).testTag("catalog_list"),state=listState) {
                item(key="search") { Column {
                    Text("Hledat v katalogu")
                    OutlinedTextField(query,{query=it.take(120)},singleLine=true,modifier=Modifier.fillMaxWidth().testTag("catalog_query").semantics { contentDescription="Hledat v katalogu" })
                } }
                item(key="coverage") { TextButton(onClick={coverageOpen=true},modifier=Modifier.testTag("content_coverage")) { Text("Co průvodce pokrývá") } }
                item(key="summary") { Text("${model.catalogEntries.size} položek z bety a komunitních podkladů. Úplnost ani dostupnost v aktuální hře nejsou potvrzené.", style = MaterialTheme.typography.bodySmall) }
                if(model.atlas.maps.size>8) item(key="route_filters") {
                    TextButton(onClick={filtersOpen=true},modifier=Modifier.testTag("catalog_filters")) { Text("Filtry cesty 1–60 · nalezeno ${entries.size}") }
                }
                item(key="filter") { FilterChip(selected = onlyBookmarks, onClick = { onlyBookmarks = !onlyBookmarks },
                    label = { Text("Moje záložky (${model.bookmarks.size})") }, modifier = Modifier.testTag("only_bookmarks")) }
                if (entries.isEmpty()) item(key="empty") { Text(if (query.isNotBlank()) "Nic nenalezeno." else "Zatím tu nemáš žádné záložky.") }
                items(entries, key = { it.key }) { e ->
                    TextButton(onClick = { selectedKey = e.key }, modifier = Modifier.fillMaxWidth().testTag("catalog_${e.key}")) {
                        Column(Modifier.fillMaxWidth()) {
                            Text((if (e.key in model.bookmarks) "★  " else "") + e.nameOriginal)
                            Text(entryLabel(e), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
    }, confirmButton = { TextButton(onClick = onClose,modifier=Modifier.testTag("catalog_close")) { Text("Zavřít") } })
    if(filtersOpen) AlertDialog(onDismissRequest={filtersOpen=false},title={Text("Filtry katalogu")},text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                listOf("ALL" to "Vše","CLASS" to "Warlock","DUNGEON" to "Dungeony","ITEM" to "Předměty").forEach { (id,label) ->
                    FilterChip(selected=category==id,onClick={category=id},label={Text(label)},modifier=Modifier.testTag("catalog_category_$id"))
                }
            }
            FilterChip(selected=onlyArea,onClick={onlyArea=!onlyArea},label={Text("Oblast postavy")},modifier=Modifier.testTag("catalog_current_area"))
            Text("Úrovně podle podkladu; filtr nepotvrzuje dostupnost.",style=MaterialTheme.typography.labelSmall)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                (0..6).forEach { band -> FilterChip(selected=levelBand==band,onClick={levelBand=band},label={Text(if(band==0)"1–60" else "${(band-1)*10+1}–${band*10}")},modifier=Modifier.testTag("catalog_level_$band")) }
            }
        }
    },confirmButton={TextButton(onClick={filtersOpen=false},modifier=Modifier.testTag("apply_catalog_filters")){Text("Použít filtry")}},dismissButton={TextButton(onClick={category="ALL";levelBand=0;onlyArea=false;filtersOpen=false}){Text("Vymazat filtry")}})
    if(coverageOpen) ContentCoverageDialog(model,onClose={coverageOpen=false})
}

private fun entryLabel(e: EntryRecord): String = when(e.kind) {
    EntryKind.OBJECT -> "Herní objekt"; EntryKind.NPC -> "NPC"; EntryKind.QUEST -> "Úkol ${e.gameId ?: e.key.substringAfterLast(':')}"
    EntryKind.ITEM -> "Předmět"; EntryKind.SPELL -> "Kouzlo"; EntryKind.TRAVEL -> "Doprava — údaj k ověření"
}
fun verificationLabel(v: Verification): String = when(v) {
    Verification.BETA -> "Údaj z bety — ve vydané hře nepotvrzeno"
    Verification.CLASSIC -> "Reference z Classicu — Forever čeká na ověření"
    Verification.COMMUNITY -> "Komunitní reference — čeká na ověření"
    Verification.CONFLICT -> "Zdroje se v některých údajích rozcházejí"
    Verification.UNVERIFIED -> "Neověřený údaj"
    Verification.RELEASE -> "Potvrzeno ve vydané hře"
}
