package cz.jenda.wowforever.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cz.jenda.wowforever.GuideViewModel
import cz.jenda.wowforever.content.ContentJson
import cz.jenda.wowforever.data.Destination
import cz.jenda.wowforever.data.Spoilers
import kotlinx.serialization.encodeToString

fun raceLabel(id: String) = when (id) { "ORC" -> "Ork"; "TROLL" -> "Troll"; "UNDEAD" -> "Nemrtvý"; else -> id }

@OptIn(ExperimentalLayoutApi::class)
@Composable fun CharacterScreen(model: GuideViewModel, onBackup:()->Unit,onPackages:()->Unit = {}) {
    var showAreaSelector by rememberSaveable { mutableStateOf(false) }
    val p = model.profile
    var name by rememberSaveable(p.id) { mutableStateOf(p.displayName) }
    var level by rememberSaveable(p.id) { mutableStateOf(p.level.toString()) }
    var race by rememberSaveable(p.id) { mutableStateOf(p.race) }
    var area by rememberSaveable(p.id) { mutableIntStateOf(p.currentUiMapId) }
    var mode by rememberSaveable(p.id) { mutableStateOf(p.ruleset.orEmpty()) }
    var professions by rememberSaveable(p.id) { mutableStateOf(runCatching { ContentJson.decodeFromString<List<String>>(p.professionsJson).joinToString(", ") }.getOrDefault("")) }
    val validLevel = level.toIntOrNull()?.takeIf { it in 1..60 }
    val validArea=model.atlas.maps.any{it.uiMapId==area}
    val focus = LocalFocusManager.current
    val dirty = name.trim() != p.displayName || level != p.level.toString() || race != p.race || area != p.currentUiMapId || mode.trim() != p.ruleset.orEmpty() ||
        professions.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct() != runCatching { ContentJson.decodeFromString<List<String>>(p.professionsJson) }.getOrDefault(emptyList())
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp).testTag("character_form"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Tvůj Warlock", style = MaterialTheme.typography.headlineSmall)
        Text("Horda • Warlock. Údaje zadáváš ručně podle své postavy.", style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick=onBackup,modifier=Modifier.testTag("backup")){Text("Profily a záloha")}
        OutlinedButton(onClick=onPackages,modifier=Modifier.testTag("packages")){Text("Balíčky a mapy")}
        Text("Záloha obsahuje uložené údaje. Rozepsané změny nejdřív potvrď tlačítkem Uložit postavu.",style=MaterialTheme.typography.bodySmall)
        OutlinedTextField(name, { name = it.take(60); model.profileEdited() }, label = { Text("Jméno nebo název cesty") }, singleLine = true,
            isError = name.isBlank(), modifier = Modifier.fillMaxWidth().testTag("profile_name"))
        OutlinedTextField(level, { level = it.take(3); model.profileEdited() }, label = { Text("Úroveň (1–60)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = validLevel == null,
            supportingText = { if (validLevel == null) Text("Zadej celé číslo od 1 do 60.") }, modifier = Modifier.fillMaxWidth().testTag("profile_level"))
        Text("Rasa", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ORC", "TROLL", "UNDEAD").forEach { id -> FilterChip(selected = race == id, onClick = { race = id; model.profileEdited() },
                label = { Text(raceLabel(id)) }, modifier = Modifier.testTag("race_$id")) }
        }
        Text("Aktuální oblast", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if(model.atlas.maps.size>8) {
                OutlinedButton(onClick={showAreaSelector=true},modifier=Modifier.testTag("choose_profile_area")){Text(model.atlas.maps.firstOrNull { it.uiMapId==area }?.name ?: "Vybrat oblast")}
            } else model.atlas.maps.forEach { map -> FilterChip(selected = area == map.uiMapId,
                onClick = { area = map.uiMapId; model.profileEdited() }, label = { Text(map.name) }, modifier = Modifier.testTag("area_${map.uiMapId}")) }
        }
        Text("Dostupných offline map: ${model.atlas.maps.size}. Přesný rozsah bodů najdeš v katalogu přes Co průvodce pokrývá.", style = MaterialTheme.typography.bodySmall)
        if(!validArea)Text("Uložená oblast není v aktivním balíčku. Pro uložení změn vyber dostupnou oblast; původní údaje zatím zůstávají zachované.",modifier=Modifier.testTag("profile_missing_area"))
        OutlinedTextField(mode, { mode = it.take(60); model.profileEdited() }, label = { Text("Režim (nepovinné)") },
            supportingText = { Text("Můžeš nechat prázdné a doplnit později.") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("profile_mode"))
        OutlinedTextField(professions, { professions = it.take(160); model.profileEdited() }, label = { Text("Profese (nepovinné)") },
            supportingText = { Text("Odděl čárkou, nebo nech prázdné.") }, modifier = Modifier.fillMaxWidth().testTag("profile_professions"))
        Button(onClick = {
            focus.clearFocus()
            model.saveProfile(p.copy(displayName = name.trim(), level = validLevel!!, race = race, currentUiMapId = area,
                ruleset = mode.trim().ifBlank { null }, professionsJson = ContentJson.encodeToString(professions.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct())))
        }, enabled = validLevel != null && name.isNotBlank() && validArea && !model.isBusy,
            modifier = Modifier.fillMaxWidth().testTag("save_profile")) { Text(if (model.savingProfile) "Ukládám…" else "Uložit postavu") }
        if (model.profileSaved && !dirty) Text("Postava uložená.", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.testTag("profile_saved"))
        else if (dirty) Text("Změny nejsou uložené. Můžeš se k nim vrátit v části Postava.", style = MaterialTheme.typography.bodySmall)
        HorizontalDivider()
        Text("Kolik chceš prozradit?", style = MaterialTheme.typography.titleLarge)
        Text("Nastavení se ukládá ihned. Názvy a polohy zůstávají viditelné.", style = MaterialTheme.typography.bodySmall)
        Spoilers.entries.forEach { choice ->
            Row(Modifier.fillMaxWidth()) {
                RadioButton(selected = model.spoilers == choice, onClick = { model.chooseSpoilers(choice) }, modifier = Modifier.testTag("spoilers_${choice.name}"))
                TextButton(onClick = { model.chooseSpoilers(choice) }) { Text(choice.label) }
            }
        }
        Text("Při volbě „Jen názvy a místa“ zůstanou popisy v katalogu skryté. Jednotlivý popis si můžeš odkrýt.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
    }
    if(showAreaSelector) MapSelectorDialog(model.atlas.maps,area,onChoose={area=it;model.profileEdited()},onClose={showAreaSelector=false})
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun JourneyScreen(model: GuideViewModel, onCatalog: () -> Unit, onBackup:()->Unit = {},onPlan:()->Unit = {}, onEntry: (String) -> Unit) {
    val entries = model.catalogEntries.filter { it.key in model.bookmarks }
    var questFilter by rememberSaveable(model.profile.id) { mutableStateOf("RECORDS") }
    val questCatalog=model.catalogEntries.filter { it.kind==cz.jenda.wowforever.content.EntryKind.QUEST }
    val quests=(questCatalog+model.personalState.quests.mapNotNull { model.historyEntry(it.questKey) }).distinctBy { it.key }.filter {
        when(questFilter) { "ALL"->true;"RECORDS"->it.key in model.questStates;else->(model.questStates[it.key] ?: cz.jenda.wowforever.data.db.QuestState.NOT_STARTED).name==questFilter }
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp).testTag("journey_list"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Moje záložky (${model.bookmarks.size})", style = MaterialTheme.typography.headlineSmall)
            OutlinedButton(onClick=onPlan,modifier=Modifier.testTag("journey_plan")){Text("Plán výpravy (${model.personalState.stops.size})")}
            Text("Místa a položky, ke kterým se chceš vrátit. Záložka neoznačuje úkol za splněný.", Modifier.padding(vertical = 12.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCatalog) { Text("Otevřít katalog") }
                OutlinedButton(onClick=onBackup,modifier=Modifier.testTag("backup")){Text("Záloha")}
            }
            if(model.undoAction!=null) TextButton(onClick=model::undoPersonal,enabled=!model.isBusy,modifier=Modifier.testTag("undo_personal")){Text("Vrátit poslední změnu")}

        }
        if (entries.isEmpty()) item { Text("První záložku si přidej v detailu položky v katalogu.", Modifier.padding(vertical = 16.dp)) }
        items(entries, key = { it.key }) { entry ->
            OutlinedButton(onClick = { onEntry(entry.key) }, modifier = Modifier.fillMaxWidth().testTag("journey_${entry.key}")) { Text("★  ${entry.nameOriginal}") }
        }
        item { Text("Moje poznámky (${model.notes.size})",style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(top=16.dp)) }
        items(model.allPoints.filter { it.kind == cz.jenda.wowforever.model.PointKind.NOTE },key={it.id}) { point ->
            OutlinedButton(onClick={model.showPoint(point.id)},modifier=Modifier.fillMaxWidth().testTag("journey_${point.id}")) { Text("✎ ${point.name}") }
        }
        item { Text("Úkoly",style=MaterialTheme.typography.headlineSmall,modifier=Modifier.padding(top=16.dp)) }
        item {
            FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                listOf("RECORDS" to "Moje záznamy","ALL" to "Všechny úkoly","IN_PROGRESS" to "Rozpracované","COMPLETED" to "Hotové","DEFERRED" to "Odložené").forEach { (key,label) ->
                    FilterChip(selected=questFilter==key,onClick={questFilter=key},label={Text(label)},modifier=Modifier.testTag("journal_filter_$key"))
                }
            }
            if(quests.isEmpty())Text("Žádný úkol v tomto filtru. Stav si nastav v detailu katalogu.",style=MaterialTheme.typography.bodySmall)
        }
        items(quests,key={"quest:"+it.key}) { entry ->
            OutlinedButton(onClick={onEntry(entry.key)},modifier=Modifier.fillMaxWidth().testTag("journal_${entry.key}")) {
                Column(Modifier.fillMaxWidth()) { Text(entry.nameOriginal+" · "+entry.key.substringAfterLast(':'))
                    Text(stateLabel(model.questStates[entry.key] ?: cz.jenda.wowforever.data.db.QuestState.NOT_STARTED),style=MaterialTheme.typography.labelMedium) }
            }
        }
        item { Text("Přání a získané předměty",style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(top=16.dp)) }
        items(model.personalState.items,key={"item:"+it.itemKey}) { item ->
            TextButton(onClick={onEntry(item.itemKey)},modifier=Modifier.fillMaxWidth().testTag("journal_${item.itemKey}")){Text("✓ ${item.nameSnapshot} · získáno")}
        }
        items(model.catalogEntries.filter { it.kind==cz.jenda.wowforever.content.EntryKind.ITEM && it.key in model.bookmarks && it.key !in model.acquiredItems },key={"wish:"+it.key}) { item ->
            TextButton(onClick={onEntry(item.key)},modifier=Modifier.fillMaxWidth()){Text("☆ ${item.nameOriginal} · chci získat")}
        }
        items(model.personalState.bookmarks.filter { b -> model.catalogEntries.none { it.key==b.entryKey } },key={"history:"+it.entryKey}) { item ->
            TextButton(onClick={onEntry(item.entryKey)}){Text("★ ${item.nameSnapshot} · momentálně mimo katalog")}
        }
        items(model.notes.filter { n -> model.allPoints.none { it.id==n.id } },key={"unlocated:"+it.id}) { note ->
            Text("✎ ${note.text} · mapa není stažená nebo poloha chybí",style=MaterialTheme.typography.bodyMedium)
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}
