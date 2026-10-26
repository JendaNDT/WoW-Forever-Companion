package cz.jenda.wowforever.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cz.jenda.wowforever.GuideViewModel
import cz.jenda.wowforever.content.*

@OptIn(ExperimentalLayoutApi::class,ExperimentalMaterial3Api::class)
@Composable fun SearchScreen(model:GuideViewModel,onEntry:(String)->Unit,onPoint:(String)->Unit,onCoverage:()->Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    var category by rememberSaveable { mutableStateOf("ALL") }
    var levelBand by rememberSaveable { mutableIntStateOf(0) }
    var onlyArea by rememberSaveable { mutableStateOf(false) }
    var myLevel by rememberSaveable { mutableStateOf(false) }
    var onlyBookmarks by rememberSaveable { mutableStateOf(false) }
    val classKeys=remember(model.guideRules) { model.guideRules.filter { it.importance==Importance.CLASS }.map { it.entryKey }.toSet()+WarlockQuestIndex.keys }
    val adviceByEntry=remember(model.recommendations) { model.recommendations.all.associateBy { it.entry.key } }
    val areaKeys=remember(model.recommendations,model.profile.currentUiMapId) { model.recommendations.all.filter { model.profile.currentUiMapId in it.areaIds }.map { it.entry.key }.toSet() }
    val listState=rememberLazyListState()
    val requester=remember { FocusRequester() }
    val keyboard=LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { requester.requestFocus() }
    val filterSignature=query
    var previousFilters by rememberSaveable { mutableStateOf(filterSignature) }
    LaunchedEffect(filterSignature) { if(previousFilters!=filterSignature){listState.scrollToItem(0);previousFilters=filterSignature} }
    val entries=remember(model.catalogSearchIndex,query,onlyBookmarks,model.bookmarks,category,levelBand,onlyArea,myLevel,model.profile.level,classKeys,areaKeys) {
        if(query.isBlank() && category=="ALL" && !onlyBookmarks && !onlyArea && !myLevel && levelBand==0)emptyList()
        else model.searchCatalog(query).filter { e->
            val level=listOfNotNull(e.reportedLevel,e.minimumLevel).filter { it>0 }.maxOrNull()
            (!onlyBookmarks || e.key in model.bookmarks) && (!onlyArea || e.key in areaKeys) &&
            (!myLevel || level?.let { it<=model.profile.level+2 }==true) &&
            (when(category) {
                "CLASS"->e.key in classKeys;"DUNGEON"->e.key.startsWith("wow-forever:travel:dungeon_")
                "QUEST"->e.kind==EntryKind.QUEST;"NPC"->e.kind==EntryKind.NPC
                "ITEM"->e.kind==EntryKind.ITEM;"SPELL"->e.kind==EntryKind.SPELL;else->true
            }) && (levelBand==0 || level?.let { it in ((levelBand-1)*10+1)..(levelBand*10) }==true)
        }
    }
    val notes=remember(model.notes,query,onlyArea,model.profile.currentUiMapId,category,onlyBookmarks) {
        val normalized=normalizeSearch(query.trim())
        if(category!="ALL" || onlyBookmarks)emptyList() else model.notes.filter {
            normalized in normalizeSearch(it.text) && (!onlyArea || it.uiMapId==model.profile.currentUiMapId)
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text("Hledat",style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f).padding(top=10.dp))
                TextButton(onClick={keyboard?.hide();model.back()},modifier=Modifier.testTag("catalog_close")) { Text("Zavřít") }
            }
            OutlinedTextField(query,{query=it.take(120)},label={Text("Název, český popis nebo poznámka")},singleLine=true,
                keyboardActions=KeyboardActions(onDone={model.rememberSearch(query);keyboard?.hide()}),
                modifier=Modifier.fillMaxWidth().focusRequester(requester).testTag("catalog_query").semantics { contentDescription="Hledat v průvodci" })
        LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("catalog_list"),state=listState,verticalArrangement=Arrangement.spacedBy(8.dp)) {
        item(key="types") {
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                listOf("ALL" to "Vše","QUEST" to "Úkoly","NPC" to "Postavy","ITEM" to "Předměty","SPELL" to "Kouzla","CLASS" to "Warlock","DUNGEON" to "Dungeony").forEach { (id,label)->
                    FilterChip(selected=category==id,onClick={category=id},label={Text(label)},modifier=Modifier.testTag("catalog_category_$id"))
                }
            }
            FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                FilterChip(selected=onlyBookmarks,onClick={onlyBookmarks=!onlyBookmarks},label={Text("★ Moje (${model.bookmarks.size})")},modifier=Modifier.testTag("only_bookmarks"))
                FilterChip(selected=onlyArea,onClick={onlyArea=!onlyArea},label={Text("Oblast postavy")},modifier=Modifier.testTag("catalog_current_area"))
                FilterChip(selected=myLevel,onClick={myLevel=!myLevel},label={Text("Moje úroveň")},modifier=Modifier.testTag("search_my_level"))
                TextButton(onClick={filtersOpen=!filtersOpen},modifier=Modifier.testTag("catalog_filters")) { Text("Další filtry") }
            }
        }
        if(filtersOpen)item(key="advanced") {
            Text("Pásmo úrovní podle podkladů",style=MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) { (0..6).forEach { band->
                FilterChip(selected=levelBand==band,onClick={levelBand=band},label={Text(if(band==0)"1–60" else "${(band-1)*10+1}–${band*10}")},modifier=Modifier.testTag("catalog_level_$band"))
            } }
            TextButton(onClick={filtersOpen=false},modifier=Modifier.testTag("apply_catalog_filters")) { Text("Použít filtry") }
        }
        if(query.isBlank() && model.recentSearches.isNotEmpty())item(key="recent") {
            Text("Poslední hledání",style=MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) { model.recentSearches.forEach { q->SuggestionChip(onClick={query=q},label={Text(q)},modifier=Modifier.testTag("recent_$q")) } }
        }
        items(notes,key={"note:"+it.id}) { note->
            OutlinedCard(onClick={keyboard?.hide();model.rememberSearch(query);onPoint(note.id)},modifier=Modifier.fillMaxWidth().testTag("search_${note.id}")) {
                Text("✎ ${note.text}",modifier=Modifier.padding(12.dp))
            }
        }
        items(entries,key={it.key}) { e->
            OutlinedCard(onClick={keyboard?.hide();model.rememberSearch(query);onEntry(e.key)},modifier=Modifier.fillMaxWidth().testTag("catalog_${e.key}")) {
                Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text((if(e.key in model.bookmarks)"★ " else "")+e.nameOriginal,style=MaterialTheme.typography.titleMedium)
                    val level=listOfNotNull(e.reportedLevel,e.minimumLevel).filter { it>0 }.maxOrNull()
                    Text(entryLabel(e)+(level?.let { " · úroveň $it" } ?: ""),style=MaterialTheme.typography.labelMedium)
                    val points=model.pointsForEntry(e.key)
                    val areaNames=if(points.isNotEmpty())points.map { model.atlas.map(it.position.uiMapId,it.position.floorId).name }.distinct()
                        else adviceByEntry[e.key]?.areaIds.orEmpty().mapNotNull { id->model.atlas.maps.firstOrNull { it.uiMapId==id }?.name }.distinct()
                    if(areaNames.isNotEmpty())Text(areaNames.joinToString(),style=MaterialTheme.typography.bodySmall)
                    if(points.isNotEmpty())TextButton(onClick={keyboard?.hide();model.rememberSearch(query);if(points.size==1){model.showPoint(points.single().id)}else onEntry(e.key)},modifier=Modifier.testTag("result_map_${e.key}")) { Text("Na mapě") }
                }
            }
        }
        if(entries.isEmpty() && notes.isEmpty())item(key="empty") { Text(if(query.isBlank())"Napiš název nebo vyber filtr." else "Nic nenalezeno. Zkus jiné slovo nebo změň filtry.") }
        item(key="coverage") {
            TextButton(onClick=onCoverage,modifier=Modifier.testTag("content_coverage")) { Text("Co průvodce pokrývá") }
            Text("${model.catalogEntries.size} položek · podklady z bety a Classicu",style=MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
        }
        }
    }
}
fun entryLabel(e:EntryRecord):String=when(e.kind) {
    EntryKind.OBJECT->"Herní objekt";EntryKind.NPC->"NPC";EntryKind.QUEST->"Úkol ${e.gameId ?: e.key.substringAfterLast(':')}"
    EntryKind.ITEM->"Předmět";EntryKind.SPELL->"Kouzlo";EntryKind.TRAVEL->"Doprava"
}
fun verificationLabel(v:Verification):String=when(v) {
    Verification.BETA->"Údaj z bety — ve vydané hře nepotvrzeno"
    Verification.CLASSIC->"Reference z Classicu — Forever čeká na ověření"
    Verification.COMMUNITY->"Komunitní reference — čeká na ověření"
    Verification.CONFLICT->"Zdroje se v některých údajích rozcházejí"
    Verification.UNVERIFIED->"Neověřený údaj";Verification.RELEASE->"Potvrzeno ve vydané hře"
}
