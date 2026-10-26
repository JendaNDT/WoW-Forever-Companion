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
            Text("${model.pointsOnMap().size} viditelných bodů v oblasti. Filtr platí i pro panel V této oblasti.")
            FilterChip(selected=model.showCompletedQuests,onClick=model::toggleCompletedQuests,label={Text("Zobrazit hotové úkoly")},modifier=Modifier.testTag("show_completed_quests"))
            PointKind.entries.filter { kind -> kind == PointKind.NOTE || model.allPoints.any { it.kind == kind } }.forEach { kind ->
                val count=model.allPoints.count { it.kind == kind && it.position.uiMapId == model.mapId && it.position.floorId == model.floorId }
                FilterChip(selected=kind.name in model.visibleKinds,onClick={model.toggleLayer(kind)},
                    label={Text("${kind.label} ($count)")},modifier=Modifier.testTag("layer_${kind.name}"))
            }
            FilterChip(selected=model.onlyBookmarks,onClick=model::toggleBookmarkFilter,
                label={Text("Jen záložky katalogu")},modifier=Modifier.testTag("layer_bookmarks"))
            Text("! zadání · ? odevzdání · ◆ cíl nebo zdroj · ✎ poznámka · čísla výpravy. Postavy zahrnují i předměty a objekty. Původ a přesnost jsou v detailu.",style=MaterialTheme.typography.bodySmall)
            Text("Poloha z bety zůstává nepotvrzená ve vydané hře bez ohledu na přesnost souřadnic.",style=MaterialTheme.typography.bodySmall)
        }
    },confirmButton={TextButton(onClick=onClose){Text("Hotovo")}},
        dismissButton={TextButton(onClick=model::resetLayers,modifier=Modifier.testTag("reset_layers")){Text("Zobrazit vše")}})
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
