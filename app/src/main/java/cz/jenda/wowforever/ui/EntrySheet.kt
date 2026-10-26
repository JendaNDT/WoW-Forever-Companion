package cz.jenda.wowforever.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import cz.jenda.wowforever.GuideViewModel
import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.data.Spoilers
import cz.jenda.wowforever.model.MapPoint
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class,ExperimentalLayoutApi::class,androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable fun EntrySheet(model:GuideViewModel,entryKey:String?,pointId:String?,backLabel:String,onBack:()->Unit,onDismiss:()->Unit,onExit:()->Unit,onRelated:(String)->Unit,onShowPoint:(MapPoint)->Unit,onJournal:()->Unit) {
    val entry=entryKey?.let(model::historyEntry)
    val point=pointId?.let { id->model.allPoints.firstOrNull { it.id==id } }
    val note=pointId?.let { id->model.notes.firstOrNull { it.id==id } }
    val density=LocalDensity.current
    val window=LocalWindowInfo.current.containerSize
    val expandedOnly=density.fontScale>=1.5f || with(density) { window.height.toDp()<480.dp }
    val sheet=rememberModalBottomSheetState(skipPartiallyExpanded=expandedOnly)
    val detailStates=rememberSaveableStateHolder()
    val scope=rememberCoroutineScope()
    val uri=LocalUriHandler.current
    var sourceError by remember(entryKey,pointId) { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest=onDismiss,sheetState=sheet,modifier=Modifier.testTag("entry_sheet")) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.94f).navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                TextButton(onClick=onBack,modifier=Modifier.testTag("detail_back")) { Text(backLabel) }
                Spacer(Modifier.weight(1f))
                if(!expandedOnly)TextButton(onClick={scope.launch { if(sheet.currentValue==SheetValue.Expanded && sheet.hasPartiallyExpandedState)sheet.partialExpand() else sheet.expand() }},modifier=Modifier.testTag("detail_expand")) { Text(if(sheet.currentValue==SheetValue.Expanded)"Zmenšit" else "Celý detail") }
            }
            detailStates.SaveableStateProvider(entryKey ?: pointId ?: "missing") {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text(entry?.nameOriginal ?: note?.text?.lineSequence()?.first()?.take(80) ?: point?.name ?: "Detail",style=MaterialTheme.typography.titleLarge,modifier=Modifier.testTag("detail_title"))
                if(entry!=null)key(entry.key) {
                    if(entry.kind==EntryKind.QUEST)QuestStateActions(model,entry)
                    if(entry.kind==EntryKind.ITEM)FilterChip(selected=entry.key in model.acquiredItems,onClick={model.setAcquired(entry.key,entry.key !in model.acquiredItems)},enabled=!model.isBusy,label={Text(if(entry.key in model.acquiredItems)"Mám" else "Označit jako získaný")},modifier=Modifier.testTag("item_acquired"))
                    val shownLevel=listOfNotNull(entry.reportedLevel,entry.minimumLevel).filter { it>0 }.maxOrNull()
                    Text(entryLabel(entry)+(shownLevel?.let { " · úroveň $it" } ?: ""),style=MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick={model.toggleBookmark(entry)},enabled=!model.isBusy,modifier=Modifier.testTag("bookmark_toggle")) { Text(if(entry.key in model.bookmarks)"Odebrat záložku" else "Přidat záložku") }
                        TextButton(onClick={model.addPlanStop(entry.key)},enabled=!model.isBusy,modifier=Modifier.testTag("plan_add_entry")) { Text("Do výpravy") }
                        model.pointsForEntry(entry.key).firstOrNull()?.let { p->TextButton(onClick={onExit();model.requestNote(p.position)},modifier=Modifier.testTag("entry_note")) { Text("Poznámka") } }
                    }
                    if(entry.kind==EntryKind.ITEM)TextButton(onClick=onJournal,modifier=Modifier.testTag("entry_journey")) { Text("Otevřít deník") }
                    DetailSection("Kde", "detail_locations") {
                        val locations=model.pointsForEntry(entry.key)
                        if(locations.isEmpty())Text("Přímá poloha není doložená.",style=MaterialTheme.typography.bodySmall)
                        locations.forEachIndexed { index,p->TextButton(onClick={onShowPoint(p)},modifier=Modifier.testTag("entry_location_$index")) { Text("Na mapě: "+pointCoordinates(model,p)) } }
                        RelatedEntryLinks(model,entry,onRelated)
                    }
                    DetailSection("Návaznosti a odměna", "detail_progress") {
                        ProgressPanel(model,entry,onJournal,onRelated,includeItemAction=false)
                    }
                    DetailSection("Popis", "detail_description") {
                        var revealed by rememberSaveable(entry.key,model.spoilers) { mutableStateOf(false) }
                        if(model.spoilers==Spoilers.FULL || revealed)Text(entry.descriptionCs,modifier=Modifier.testTag("entry_description"))
                        else TextButton(onClick={revealed=true},modifier=Modifier.testTag("reveal_description")) { Text("Odkrýt tento popis") }
                    }
                    DetailSection("Podmínky a zdroje", "detail_conditions") {
                        Text(verificationLabel(entry.verification),style=MaterialTheme.typography.bodySmall)
                        var revealed by rememberSaveable(entry.key,model.spoilers) { mutableStateOf(false) }
                        val advice=model.recommendations.all.firstOrNull { it.entry.key==entry.key }
                        Column(Modifier.testTag("advice_explanation")) {
                            advice?.let { Text(it.availability.label) }
                            if(model.spoilers==Spoilers.FULL || revealed) {
                                advice?.ruleReason?.let { Text(it) };advice?.explanation?.forEach { Text(it,style=MaterialTheme.typography.bodySmall) }
                            } else TextButton(onClick={revealed=true},modifier=Modifier.testTag("reveal_advice")) { Text("Odkrýt tyto podrobnosti") }
                        }
                        entry.sourceIds.forEachIndexed { i,id->model.sourceUrls[id]?.let { url->TextButton(onClick={runCatching { uri.openUri(url) }.onFailure { sourceError=true }}) { Text("Zdroj ${i+1} ↗") } } }
                        if(sourceError)Text("Otevření zdroje vyžaduje prohlížeč a připojení.")
                    }
                } else {
                    Text(note?.text ?: point?.let { pointCoordinates(model,it) } ?: "Položka momentálně není v aktivních datech.")
                    if(point!=null) {
                        TextButton(onClick={onShowPoint(point)},modifier=Modifier.testTag("focus")) { Text("Na mapě") }
                        if(note!=null)TextButton(onClick={onExit();model.requestNote(point.position,note.id)},modifier=Modifier.testTag("edit_note")) { Text("Upravit") }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            }
            PersonalUndoBar(model)
        }
    }
}
@Composable private fun DetailSection(title:String,tag:String,content:@Composable ()->Unit) {
    var open by rememberSaveable { mutableStateOf(true) }
    HorizontalDivider()
    TextButton(onClick={open=!open},modifier=Modifier.fillMaxWidth().testTag(tag)) { Text((if(open)"▾ " else "▸ ")+title,style=MaterialTheme.typography.titleMedium) }
    if(open)content()
}
