package cz.jenda.wowforever.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import cz.jenda.wowforever.GuideViewModel
import cz.jenda.wowforever.data.searchEntries
import cz.jenda.wowforever.content.normalizeSearch
import cz.jenda.wowforever.model.*
import java.util.Locale

fun pointCoordinates(model: GuideViewModel, point: MapPoint): String {
    val map = model.atlas.map(point.position.uiMapId,point.position.floorId)
    return "${map.name} · ${map.floorLabel} · " + String.format(Locale.forLanguageTag("cs-CZ"),"%.1f / %.1f",point.position.xPercent,point.position.yPercent)
}
fun precisionLabel(point: MapPoint) = if (point.kind == PointKind.NOTE) "Vlastní poznámka" else
    (if (point.approximate) "Přibližná poloha" else "Podrobná poloha") + String.format(Locale.forLanguageTag("cs-CZ")," · údaj zdroje ±%.1f",point.precisionPercent)

@Composable fun LayerDialog(model: GuideViewModel, onClose: () -> Unit) {
    AlertDialog(onDismissRequest=onClose,title={Text("Vrstvy mapy")},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("${model.pointsOnMap().size} viditelných bodů v oblasti. Filtr platí i pro seznam Body.")
            PointKind.entries.filter { kind -> kind == PointKind.NOTE || model.allPoints.any { it.kind == kind } }.forEach { kind ->
                val count=model.allPoints.count { it.kind == kind && it.position.uiMapId == model.mapId && it.position.floorId == model.floorId }
                FilterChip(selected=kind.name in model.visibleKinds,onClick={model.toggleLayer(kind)},
                    label={Text("${kind.label} ($count)")},modifier=Modifier.testTag("layer_${kind.name}"))
            }
            FilterChip(selected=model.onlyBookmarks,onClick=model::toggleBookmarkFilter,
                label={Text("Jen záložky katalogu")},modifier=Modifier.testTag("layer_bookmarks"))
            Text("Červená značka: katalog. Zelená: tvoje poznámka. Symbol ≈ označuje přibližnou polohu.",style=MaterialTheme.typography.bodySmall)
            Text("Poloha z bety zůstává nepotvrzená ve vydané hře bez ohledu na přesnost souřadnic.",style=MaterialTheme.typography.bodySmall)
        }
    },confirmButton={TextButton(onClick=onClose){Text("Hotovo")}},
        dismissButton={TextButton(onClick=model::resetLayers,modifier=Modifier.testTag("reset_layers")){Text("Zobrazit vše")}})
}

@Composable fun SearchDialog(model: GuideViewModel, onClose: () -> Unit, onEntry: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var localOnly by rememberSaveable { mutableStateOf(false) }
    val localKeys=remember(model.catalogSearchIndex,model.mapId,model.floorId) {
        model.atlas.points.filter { it.position.uiMapId==model.mapId && it.position.floorId==model.floorId }.mapNotNull { it.entryKey }.toSet()
    }
    val entries=remember(model.catalogSearchIndex,query,localOnly,localKeys) {
        model.searchCatalog(query).filter { !localOnly || it.key in localKeys }
    }
    val notes=remember(model.notes,model.catalogSearchIndex,query,localOnly,model.mapId,model.floorId) {
        val normalized=normalizeSearch(query.trim());val byId=model.notes.associateBy { it.id }
        model.allPoints.filter { it.kind==PointKind.NOTE && normalized in normalizeSearch(byId[it.id]?.text.orEmpty()) &&
            (!localOnly || it.position.uiMapId==model.mapId && it.position.floorId==model.floorId) }
    }
    AlertDialog(onDismissRequest=onClose,title={Text("Hledat v průvodci")},text={
            LazyColumn(Modifier.heightIn(max=420.dp).testTag("search_results"),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                item(key="query") { Column {
                    Text("Název, český popis nebo poznámka")
                    OutlinedTextField(query,{query=it.take(120)},singleLine=true,
                        modifier=Modifier.fillMaxWidth().testTag("map_search_query").semantics { contentDescription="Název, český popis nebo poznámka" })
                } }
                item(key="filter") { FilterChip(selected=localOnly,onClick={localOnly=!localOnly},label={Text("Jen tato oblast a patro")},modifier=Modifier.testTag("search_local")) }
                if (entries.isEmpty() && notes.isEmpty()) item(key="empty") { Text("Nic nenalezeno. Zkus jiné slovo nebo vypni omezení oblasti.") }
                items(notes,key={it.id}) { point ->
                    TextButton(onClick={model.showPoint(point.id);onClose()},modifier=Modifier.fillMaxWidth().testTag("search_${point.id}")) {
                        Column(Modifier.fillMaxWidth()) { Text("✎ ${point.name}");Text(pointCoordinates(model,point),style=MaterialTheme.typography.bodySmall) }
                    }
                }
                items(entries,key={it.key}) { entry ->
                    val locations=model.pointsForEntry(entry.key)
                    TextButton(onClick={
                        if (locations.size == 1) { model.showPoint(locations.single().id);onClose() } else onEntry(entry.key)
                    },modifier=Modifier.fillMaxWidth().testTag("search_${entry.key}")) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(entry.nameOriginal)
                            Text(when(locations.size) { 0 -> "Bez doložené polohy • otevřít katalog";1 -> pointCoordinates(model,locations.single());else -> "${locations.size} poloh • vybrat v katalogu" },style=MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
    },confirmButton={TextButton(onClick=onClose){Text("Zavřít")}})
}

@Composable fun NoteEditorDialog(model: GuideViewModel) {
    val draft=model.noteDraft ?: return
    val existing=model.notes.firstOrNull { it.id == draft.id }
    var text by rememberSaveable(draft.id,draft.position.uiMapId,draft.position.floorId,draft.position.xPercent,draft.position.yPercent) { mutableStateOf(existing?.text.orEmpty()) }
    var confirmDelete by rememberSaveable(draft.id) { mutableStateOf(false) }
    if (confirmDelete && draft.id != null) {
        AlertDialog(onDismissRequest={confirmDelete=false},title={Text("Smazat poznámku?")},text={Text("Poznámka i její značka zmizí z mapy.")},
            confirmButton={TextButton(onClick={model.deleteNote(draft.id)},enabled=!model.savingNote,modifier=Modifier.testTag("confirm_delete_note")){Text("Smazat")}},
            dismissButton={TextButton(onClick={confirmDelete=false}){Text("Zpět")}})
        return
    }
    val map=model.atlas.map(draft.position.uiMapId,draft.position.floorId)
    AlertDialog(onDismissRequest={if(!model.savingNote)model.cancelNote()},title={Text(if(draft.id==null) "Nová poznámka" else "Upravit poznámku")},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("${map.name} · ${map.floorLabel} · "+String.format(Locale.forLanguageTag("cs-CZ"),"%.1f / %.1f",draft.position.xPercent,draft.position.yPercent),style=MaterialTheme.typography.bodySmall)
            OutlinedTextField(text,{text=it.take(2000)},label={Text("Tvoje poznámka")},minLines=3,maxLines=7,
                supportingText={Text("${text.length}/2000")},modifier=Modifier.fillMaxWidth().testTag("note_text"))
            if(draft.id!=null) TextButton(onClick={confirmDelete=true},enabled=!model.savingNote,modifier=Modifier.testTag("delete_note")){Text("Smazat poznámku")}
        }
    },confirmButton={TextButton(onClick={model.saveNote(text)},enabled=text.isNotBlank() && !model.savingNote,modifier=Modifier.testTag("save_note")) {
        Text(if(model.savingNote) "Ukládám…" else "Uložit")
    }},dismissButton={TextButton(onClick=model::cancelNote,enabled=!model.savingNote){Text("Zrušit")}})
}
