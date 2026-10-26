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
@Composable fun CharacterScreen(model: GuideViewModel, onBackup:()->Unit = {},onPackages:()->Unit = {}) {
    var showAreaSelector by rememberSaveable { mutableStateOf(false) }
    val p = model.profile
    var name by rememberSaveable(p.id) { mutableStateOf(p.displayName) }
    var level by rememberSaveable(p.id,p.level) { mutableStateOf(p.level.toString()) }
    var race by rememberSaveable(p.id) { mutableStateOf(p.race) }
    var area by rememberSaveable(p.id,p.currentUiMapId) { mutableIntStateOf(p.currentUiMapId) }
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
        if(model.guideRules.any { it.condition.usesMode() }) OutlinedTextField(mode, { mode = it.take(60); model.profileEdited() }, label = { Text("Režim (nepovinné)") },
            supportingText = { Text("Můžeš nechat prázdné a doplnit později.") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("profile_mode"))
        if(model.guideRules.any { it.condition.usesProfession() }) OutlinedTextField(professions, { professions = it.take(160); model.profileEdited() }, label = { Text("Profese (nepovinné)") },
            supportingText = { Text("Odděl čárkou, nebo nech prázdné.") }, modifier = Modifier.fillMaxWidth().testTag("profile_professions"))
        Button(onClick = {
            focus.clearFocus()
            model.saveProfile(p.copy(displayName = name.trim(), level = validLevel!!, race = race, currentUiMapId = area,
                ruleset = mode.trim().ifBlank { null }, professionsJson = ContentJson.encodeToString(professions.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct())))
        }, enabled = validLevel != null && name.isNotBlank() && validArea && !model.isBusy,
            modifier = Modifier.fillMaxWidth().testTag("save_profile")) { Text(if (model.savingProfile) "Ukládám…" else "Uložit postavu") }
        if (model.profileSaved && !dirty) Text("Postava uložená.", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.testTag("profile_saved"))
        else if (dirty) Text("Změny nejsou uložené. Můžeš se k nim vrátit v části Postava.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
    }
    if(showAreaSelector) MapSelectorDialog(model.atlas.maps,area,onChoose={area=it;model.profileEdited()},onClose={showAreaSelector=false})
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun JourneyScreen(model: GuideViewModel, onCatalog: () -> Unit, onBackup:()->Unit = {},onPlan:()->Unit = {}, onPoint:(String)->Unit = {}, onEntry: (String) -> Unit) {
    val entries = model.catalogEntries.filter { it.key in model.bookmarks }
    var questFilter by rememberSaveable(model.profile.id) { mutableStateOf("RECORDS") }
    val questCatalog=model.catalogEntries.filter { it.kind==cz.jenda.wowforever.content.EntryKind.QUEST }
    val quests=(questCatalog+model.personalState.quests.mapNotNull { model.historyEntry(it.questKey) }).distinctBy { it.key }.filter {
        when(questFilter) { "RECORDS"->it.key in model.questStates;else->(model.questStates[it.key] ?: cz.jenda.wowforever.data.db.QuestState.NOT_STARTED).name==questFilter }
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp).testTag("journey_list"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Deník", style = MaterialTheme.typography.headlineSmall)
            OutlinedButton(onClick=onPlan,modifier=Modifier.testTag("journey_plan")){Text("Plán výpravy (${model.personalState.stops.size})")}

        }
        item { Text("Úkoly",style=MaterialTheme.typography.headlineSmall,modifier=Modifier.padding(top=16.dp)) }
        item {
            FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                listOf("RECORDS" to "Moje záznamy","IN_PROGRESS" to "Rozpracované","COMPLETED" to "Hotové","DEFERRED" to "Odložené").forEach { (key,label) ->
                    FilterChip(selected=questFilter==key,onClick={questFilter=key},label={Text(label)},modifier=Modifier.testTag("journal_filter_$key"))
                }
            }
            if(quests.isEmpty())Text("Žádný úkol v tomto filtru. Stav si nastav v detailu úkolu.",style=MaterialTheme.typography.bodySmall)
        }
        items(quests,key={"quest:"+it.key}) { entry ->
            OutlinedButton(onClick={onEntry(entry.key)},modifier=Modifier.fillMaxWidth().testTag("journal_${entry.key}")) {
                Column(Modifier.fillMaxWidth()) { Text(entry.nameOriginal+" · "+entry.key.substringAfterLast(':'))
                    Text(stateLabel(model.questStates[entry.key] ?: cz.jenda.wowforever.data.db.QuestState.NOT_STARTED),style=MaterialTheme.typography.labelMedium) }
            }
        }
        item { Text("Záložky a přání (${model.bookmarks.size})",style=MaterialTheme.typography.titleLarge) }
        if (entries.isEmpty()) item { Text("První záložku si přidej v detailu položky v Hledat.", Modifier.padding(vertical = 16.dp)) }
        items(entries, key = { it.key }) { entry ->
            OutlinedButton(onClick = { onEntry(entry.key) }, modifier = Modifier.fillMaxWidth().testTag("journey_${entry.key}")) { Text((if(entry.kind==cz.jenda.wowforever.content.EntryKind.ITEM && entry.key !in model.acquiredItems)"☆ Chci · " else "★  ")+entry.nameOriginal) }
        }
        item { Text("Moje poznámky (${model.notes.size})",style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(top=16.dp)) }
        items(model.allPoints.filter { it.kind == cz.jenda.wowforever.model.PointKind.NOTE },key={it.id}) { point ->
            OutlinedButton(onClick={onPoint(point.id)},modifier=Modifier.fillMaxWidth().testTag("journey_${point.id}")) { Text("✎ ${point.name}") }
        }
        item { Text("Získané předměty",style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(top=16.dp)) }
        items(model.personalState.items,key={"item:"+it.itemKey}) { item ->
            TextButton(onClick={onEntry(item.itemKey)},modifier=Modifier.fillMaxWidth().testTag("journal_${item.itemKey}")){Text("✓ ${item.nameSnapshot} · získáno")}
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

private fun cz.jenda.wowforever.content.GuideCondition.usesMode():Boolean=when(this) {
    is cz.jenda.wowforever.content.GuideCondition.Mode->true
    is cz.jenda.wowforever.content.GuideCondition.All->conditions.any { it.usesMode() }
    is cz.jenda.wowforever.content.GuideCondition.AnyOf->conditions.any { it.usesMode() }
    else->false
}
private fun cz.jenda.wowforever.content.GuideCondition.usesProfession():Boolean=when(this) {
    is cz.jenda.wowforever.content.GuideCondition.Profession->true
    is cz.jenda.wowforever.content.GuideCondition.All->conditions.any { it.usesProfession() }
    is cz.jenda.wowforever.content.GuideCondition.AnyOf->conditions.any { it.usesProfession() }
    else->false
}
