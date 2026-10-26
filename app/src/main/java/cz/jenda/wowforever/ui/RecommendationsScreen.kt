package cz.jenda.wowforever.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import cz.jenda.wowforever.GuideViewModel
import cz.jenda.wowforever.data.Destination
import cz.jenda.wowforever.data.Spoilers
import cz.jenda.wowforever.domain.*
import cz.jenda.wowforever.content.EntryKind
import cz.jenda.wowforever.content.Importance
import cz.jenda.wowforever.content.Verification
import cz.jenda.wowforever.data.db.QuestState

@OptIn(ExperimentalLayoutApi::class)
@Composable fun NowScreen(model:GuideViewModel,onCatalog:()->Unit,onEntry:(String)->Unit,onCoverage:()->Unit = {},onLeave:()->Unit = {}) {
    val p=model.profile
    val report=remember(model.recommendations) {
        model.recommendations.copy(all=model.recommendations.all.filter { it.entry.kind in setOf(EntryKind.QUEST,EntryKind.SPELL) })
    }
    val suggestions=remember(report,p.level) { report.questSuggestions(p.level) }
    var showSoon by rememberSaveable(p.id) { mutableStateOf(false) }
    var showUnknown by rememberSaveable(p.id) { mutableStateOf(false) }
    var showNotice by rememberSaveable(model.contentMetadata?.generation) { mutableStateOf(true) }
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer=LifecycleEventObserver { _,event->if(event==Lifecycle.Event.ON_RESUME)model.refreshRecommendations() }
        lifecycle.addObserver(observer);model.refreshRecommendations()
        onDispose{lifecycle.removeObserver(observer)}
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp).testTag("advice_list"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Text(model.atlas.maps.firstOrNull { it.uiMapId==p.currentUiMapId }?.name?.let { "Co dál v $it" } ?: "Co dál · vyber dostupnou oblast",style=MaterialTheme.typography.titleLarge)
        }
        if(showNotice && model.catalogEntries.any { it.verification!=Verification.RELEASE })item {
            Surface(color=MaterialTheme.colorScheme.secondaryContainer,shape=MaterialTheme.shapes.medium,modifier=Modifier.testTag("reference_notice")) {
                Column(Modifier.padding(12.dp)) {
                    Text("Data z bety a Classicu, ve hře zatím neověřená.",style=MaterialTheme.typography.bodySmall)
                    FlowRow {
                        TextButton(onClick=onCoverage,modifier=Modifier.testTag("advice_coverage")) { Text("Pokrytí a zdroje") }
                        TextButton(onClick={showNotice=false},modifier=Modifier.testTag("dismiss_reference_notice")) { Text("Skrýt upozornění") }
                    }
                }
            }
        }
        adviceSection("Rozpracované a tvoje cíle",suggestions.personal,"Rozpracované úkoly a záložky úkolů se objeví tady.",model,onEntry,onEntry)
        adviceSection("Warlock",suggestions.current.filter { it.importance==Importance.CLASS },"Pro tuto úroveň tu nemám další třídní krok.",model,onEntry,onEntry)
        adviceSection("Tady",suggestions.current.filter { it.importance!=Importance.CLASS },"Pro tuto úroveň a oblast zatím nemám vhodný úkol v podkladech.",model,onEntry,onEntry)
        if(p.level<58)item { TextButton(onClick={showSoon=!showSoon},modifier=Modifier.testTag("advice_soon")) { Text("Brzy · úroveň ${p.level+3}–${minOf(60,p.level+5)} (${suggestions.soon.size})") } }
        if(showSoon)adviceRows(suggestions.soon,model,onEntry,onEntry)
        item { TextButton(onClick={showUnknown=!showUnknown},modifier=Modifier.testTag("advice_unknown")) { Text("Bez známé úrovně nebo oblasti (${suggestions.unknown.size})") } }
        if(showUnknown)adviceRows(suggestions.unknown,model,onEntry,onEntry)
        item {
            OutlinedButton(onClick=onLeave,modifier=Modifier.testTag("before_leaving")) { Text("Než odejdeš z "+(model.atlas.maps.firstOrNull { it.uiMapId==p.currentUiMapId }?.name ?: "oblasti")) }
        }
        item {
            Text("${model.catalogEntries.size} položek v katalogu. Seznam není úplný přehled hry. Položky s nedoloženou oblastí mohou být uvedené i zde.",style=MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick=onCatalog){Text("Hledat další úkol")}
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun LazyListScope.adviceSection(title:String,rows:List<Advice>,empty:String,model:GuideViewModel,onEntry:(String)->Unit,onWhy:(String)->Unit) {
    item { Text(title,style=MaterialTheme.typography.titleLarge);if(rows.none { it.entry.kind in setOf(EntryKind.QUEST,EntryKind.SPELL) })Text(empty,style=MaterialTheme.typography.bodyMedium) }
    adviceRows(rows,model,onEntry,onWhy)
}
private fun LazyListScope.adviceRows(rows:List<Advice>,model:GuideViewModel,onEntry:(String)->Unit,onWhy:(String)->Unit) {
    items(rows.filter { it.entry.kind in setOf(EntryKind.QUEST,EntryKind.SPELL) },key={"advice:"+it.entry.key}) { a -> AdviceCard(model,a,onEntry,onWhy) }
}
@OptIn(ExperimentalLayoutApi::class,ExperimentalMaterial3Api::class)
@Composable private fun AdviceCard(model:GuideViewModel,a:Advice,onEntry:(String)->Unit,onWhy:(String)->Unit) {
    ElevatedCard(onClick={onEntry(a.entry.key)},modifier=Modifier.fillMaxWidth().testTag("advice_${a.entry.key}")) {
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text(a.entry.nameOriginal,style=MaterialTheme.typography.titleMedium)
            if(a.availability!=Availability.UNVERIFIED || a.inProgress || a.wish)Text(
                if(a.inProgress) "Rozpracováno" else if(a.wish) "Tvoje záložka" else a.availability.label,
                color=MaterialTheme.colorScheme.primary,modifier=Modifier.testTag("availability_${a.entry.key}"))
            Text(a.suggestedLevel?.let { "Úroveň $it" } ?: "Úroveň není známá",style=MaterialTheme.typography.labelMedium)
            a.riskLabel?.let { Text(it,style=MaterialTheme.typography.labelLarge) }
            if(a.entry.verification in setOf(Verification.CONFLICT,Verification.RELEASE))Text(verificationLabel(a.entry.verification),style=MaterialTheme.typography.labelSmall)
            Text(if(a.areaIds.isEmpty()) "Oblast položky není doložená." else "Vazba na oblast: "+a.areaIds.sorted().joinToString { id->model.atlas.maps.firstOrNull { it.uiMapId==id }?.name ?: id.toString() },style=MaterialTheme.typography.bodySmall)
            if(model.spoilers==Spoilers.FULL && a.ruleReason!=null)Text(a.ruleReason,style=MaterialTheme.typography.bodyMedium)
            if(a.entry.kind==EntryKind.QUEST)FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                TextButton(onClick={model.requestQuest(a.entry.key,QuestState.IN_PROGRESS)},enabled=!model.isBusy && !a.inProgress,
                    modifier=Modifier.testTag("advice_take_${a.entry.key}")) { Text("Beru") }
                TextButton(onClick={model.requestQuest(a.entry.key,QuestState.COMPLETED)},enabled=!model.isBusy,
                    modifier=Modifier.testTag("advice_done_${a.entry.key}")) { Text("Hotovo") }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                TextButton(onClick={onEntry(a.entry.key)},modifier=Modifier.testTag("advice_open_${a.entry.key}")){Text("Otevřít detail")}
            }
        }
    }
}
