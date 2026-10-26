package cz.jenda.wowforever.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cz.jenda.wowforever.GuideViewModel
import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.data.searchEntries
import cz.jenda.wowforever.domain.*

@Composable fun ExpeditionDialog(model:GuideViewModel,onClose:()->Unit,onEntry:(String)->Unit) {
    var mode by rememberSaveable(model.profile.id) { mutableStateOf("PLAN") }
    var query by rememberSaveable(model.profile.id) { mutableStateOf("") }
    val stops=model.expeditionStops;val legs=model.expeditionLegs
    val uri=LocalUriHandler.current
    val listState=rememberLazyListState()
    val searchResults=remember(model.catalogSearchIndex,query,mode){if(mode=="ADD")model.searchCatalog(query) else emptyList()}
    LaunchedEffect(mode,query,model.profile.id) { listState.scrollToItem(0) }
    AlertDialog(onDismissRequest=onClose,title={Text("Plán výpravy (${stops.size})")},text={
        LazyColumn(Modifier.heightIn(max=440.dp).testTag("expedition_list"),state=listState,verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item {
                Text("${model.profile.displayName} · vlastní pořadí cílů",style=MaterialTheme.typography.titleSmall)
                Text("Čísla na mapě označují pořadí zastávek. Nejsou navigací průchozím terénem.",modifier=Modifier.testTag("plan_warning"))
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick={mode=if(mode=="ADD")"PLAN" else "ADD"},modifier=Modifier.testTag("plan_add")){Text(if(mode=="ADD")"Zpět k plánu" else "Přidat cíl")}
                    TextButton(onClick={mode=if(mode=="TRAVEL")"PLAN" else "TRAVEL"},modifier=Modifier.testTag("plan_travel")){Text(if(mode=="TRAVEL")"Zpět k plánu" else "Objevená doprava")}
                }
            }
            if(mode=="ADD") {
                item { OutlinedTextField(query,{query=it.take(120)},label={Text("Hledat cíl")},singleLine=true,modifier=Modifier.fillMaxWidth().testTag("plan_query"))
                    Text("Pro mapové číslo vyber konkrétní NPC. Úkol bez přímé polohy zůstane v seznamu; jeho související postavy najdeš v detailu.",style=MaterialTheme.typography.bodySmall) }
                val entries=searchResults
                if(entries.isEmpty())item { Text("Nic nenalezeno.") }
                items(entries,key={it.key}) { entry ->
                    OutlinedButton(onClick={model.addPlanStop(entry.key)},enabled=!model.isBusy,modifier=Modifier.fillMaxWidth().testTag("plan_pick_${entry.key}")) {
                        Text(entry.nameOriginal+if(stops.any { it.stored.entryKey==entry.key })" · přidat znovu" else " · přidat")
                    }
                }
            } else if(mode=="TRAVEL") {
                item { Text("Zaznamenej jen body nebo dopravu, které jsi skutečně objevil ve hře. Tento osobní záznam nepotvrzuje správnost dopravního zdroje.") }
                val entries=(model.catalogEntries.filter { it.kind==EntryKind.TRAVEL }+model.personalState.travel.mapNotNull { model.historyEntry(it.travelKey) }).distinctBy { it.key }
                if(entries.isEmpty())item { Text("V katalogu zatím nejsou dopravní body.") }
                items(entries,key={it.key}) { entry ->
                    val discovered=model.personalState.travel.any { it.travelKey==entry.key }
                    Column {
                        FilterChip(selected=discovered,onClick={model.setTravelDiscovered(entry.key,!discovered)},enabled=!model.isBusy,
                            label={Text((if(discovered)"✓ " else "○ ")+entry.nameOriginal)},modifier=Modifier.testTag("discovery_${entry.key}"))
                        Text(verificationLabel(entry.verification),style=MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                if(stops.isEmpty())item { Text("Výprava je zatím prázdná. Přidej první cíl pomocí Hledat nebo z detailu položky.") }
                items(stops,key={"stop:${it.stored.position}"}) { stop ->
                    OutlinedCard(Modifier.fillMaxWidth().testTag("plan_stop_${stop.number}")) {
                        Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                            Text("${stop.number}. ${stop.name}",style=MaterialTheme.typography.titleMedium,modifier=Modifier.testTag("plan_name_${stop.number}"))
                            stop.limitation?.let { Text(it,style=MaterialTheme.typography.bodySmall) }
                            stop.location?.let { Text("${model.atlas.maps.firstOrNull { m->m.uiMapId.toString()==it.mapKey.substringAfterLast(':') }?.name ?: it.mapKey} · ${if(it.floorId==0)"Povrch" else "Patro ${it.floorId}"}",style=MaterialTheme.typography.bodySmall) }
                            Row {
                                TextButton(onClick={model.movePlanStop(stop.stored.position,-1)},enabled=stop.number>1 && !model.isBusy,
                                    modifier=Modifier.testTag("plan_up_${stop.number}").semantics { contentDescription="Posunout zastávku ${stop.number} nahoru" }){Text("↑")}
                                TextButton(onClick={model.movePlanStop(stop.stored.position,1)},enabled=stop.number<stops.size && !model.isBusy,
                                    modifier=Modifier.testTag("plan_down_${stop.number}").semantics { contentDescription="Posunout zastávku ${stop.number} dolů" }){Text("↓")}
                                TextButton(onClick={model.removePlanStop(stop.stored.position)},enabled=!model.isBusy,
                                    modifier=Modifier.testTag("plan_remove_${stop.number}").semantics { contentDescription="Odebrat zastávku ${stop.number}" }){Text("Odebrat")}
                            }
                            Row {
                                if(stop.location!=null)TextButton(onClick={model.showPlanStop(stop.number);onClose()},enabled=!model.isBusy,modifier=Modifier.testTag("plan_map_${stop.number}")){Text("Mapa")}
                                TextButton(onClick={onEntry(stop.stored.entryKey)},modifier=Modifier.testTag("plan_detail_${stop.number}")){Text("Detail")}
                            }
                        }
                    }
                }
                item { HorizontalDivider();Text("Ověřená spojení",style=MaterialTheme.typography.titleMedium)
                    Text("Doložených úseků v katalogu: ${model.transportGraph.edges.count { it.verification==Verification.RELEASE }}. Výpočet respektuje směr, podmínky a oba objevené letecké body.",modifier=Modifier.testTag("transport_coverage")) }
                items(legs,key={"leg:${it.from}"}) { leg ->
                    Column(Modifier.testTag("plan_leg_${leg.from}")) {
                        Text("${leg.from} → ${leg.to}: "+when(leg.status) {
                            LegStatus.VERIFIED -> "Ověřené spojení podle podkladů"
                            LegStatus.SAME_PLACE -> "Stejné uložené místo"
                            LegStatus.NO_POSITION -> "Chybí jednoznačná poloha cíle"
                            LegStatus.NO_NODE -> "Spojení těchto míst není doložené"
                            LegStatus.NO_VERIFIED_PATH -> "Není doložená přístupná cesta pro tvoji postavu"
                        },modifier=Modifier.testTag("plan_leg_status_${leg.from}"))
                        if(leg.status==LegStatus.VERIFIED) {
                            Text("${leg.edges.size} úseků · nejméně doložených spojů, nikoli nejkratší vzdálenost.",style=MaterialTheme.typography.bodySmall)
                            leg.edges.forEach { edge ->
                                val names=model.transportGraph.nodes.associate { it.key to it.nameCs }
                                Text("${names[edge.from]} → ${names[edge.to]} · "+when(edge.mode) { TravelMode.WALK->"pěšky";TravelMode.FLIGHT->"letový bod";TravelMode.BOAT->"loď";TravelMode.ZEPPELIN->"vzducholoď";TravelMode.PORTAL->"portál";TravelMode.ENTRANCE->"vstup" },style=MaterialTheme.typography.bodySmall)
                            }
                            val times=leg.edges.map { it.estimatedSeconds }
                            Text(if(times.all { it!=null })"Odhad přepravy: ${times.sumOf { it!! }} s." else "Čas přepravy není doložený.",style=MaterialTheme.typography.bodySmall)
                            leg.edges.flatMap { it.sourceIds }.distinct().forEach { id -> model.sourceUrls[id]?.let { url -> TextButton(onClick={runCatching{uri.openUri(url)}}){Text("Zdroj spojení ↗")} } }
                        }
                    }
                }
                item { Text("Chybějící spojení neznamená, že se tam ve hře nedostaneš. Aplikace nepřidává odhadnuté průchody, souřadnice ani čas pěšího přesunu.",style=MaterialTheme.typography.bodySmall) }
            }
        }
    },confirmButton={TextButton(onClick=onClose){Text("Zavřít výpravu")}})
}
