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

@OptIn(ExperimentalLayoutApi::class)
@Composable fun NowScreen(model:GuideViewModel,onCatalog:()->Unit,onEntry:(String)->Unit) {
    val p=model.profile;val report=model.recommendations
    var tab by rememberSaveable { mutableStateOf("NOW") }
    var showFocus by rememberSaveable { mutableStateOf(false) }
    var detailKey by rememberSaveable { mutableStateOf<String?>(null) }
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer=LifecycleEventObserver { _,event->if(event==Lifecycle.Event.ON_RESUME)model.refreshRecommendations() }
        lifecycle.addObserver(observer);model.refreshRecommendations()
        onDispose{lifecycle.removeObserver(observer)}
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp).testTag("advice_list"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Text(p.displayName,style=MaterialTheme.typography.titleLarge)
            Text("${raceLabel(p.race)} • Warlock • úroveň ${p.level}")
            Text("Oblast: ${model.atlas.map(p.currentUiMapId).name}",style=MaterialTheme.typography.titleMedium)
            Text("Podle uložené postavy, oblast zadáváš ručně.",style=MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(onClick={model.chooseMap(p.currentUiMapId);model.navigate(Destination.MAP)}){Text("Zobrazit oblast")}
                TextButton(onClick={model.navigate(Destination.CHARACTER)}){Text("Upravit postavu")}
            }
            TextButton(onClick={showFocus=true},modifier=Modifier.testTag("focus_settings")){Text("Řazení: ${model.guideFocus.label}")}
            FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                listOf("NOW" to "Teď","LEAVE" to "Před odchodem","UNCERTAIN" to "K ověření (${report.uncertain.size})","WAITING" to "Podmínky (${report.waiting.size})","DEFERRED" to "Odložené (${report.deferred.size})").forEach { (id,label) ->
                    FilterChip(selected=tab==id,onClick={tab=id},label={Text(label)},modifier=Modifier.testTag("advice_tab_$id"))
                }
            }
        }
        when(tab) {
            "NOW" -> {
                adviceSection("Osobní cíle",report.personal,"Rozpracované úkoly a nesplněná přání se objeví tady. Záložku přidej v katalogu.",model,onEntry,{detailKey=it})
                adviceSection("Dostupné kroky",report.now.filter { !it.personal },"Pro aktuální oblast nemám potvrzený další krok se všemi známými podmínkami. To neznamená, že ve hře nic dalšího není.",model,onEntry,{detailKey=it})
                if(report.uncertain.isNotEmpty())item {
                    OutlinedButton(onClick={tab="UNCERTAIN"},modifier=Modifier.testTag("show_uncertain")){Text("Prohlédnout podklady k ověření (${report.uncertain.size})")}
                }
            }
            "LEAVE" -> {
                item {
                    Text("Před odchodem z ${model.atlas.map(p.currentUiMapId).name}",style=MaterialTheme.typography.headlineSmall)
                    Text("Kontrola pokrývá jen nahraný katalog. Ani prázdný seznam nepotvrzuje, že máš celou oblast hotovou.",modifier=Modifier.testTag("coverage_warning"),style=MaterialTheme.typography.bodyMedium)
                }
                adviceSection("Dostupné důležité cíle a přání",report.leave,"Žádný doložený otevřený cíl v této skupině.",model,onEntry,{detailKey=it})
                adviceSection("Chybějící předchozí kroky",report.leaveMissing,"Žádná potvrzená chybějící podmínka v nahraných pravidlech.",model,onEntry,{detailKey=it})
                adviceSection("Nejistoty před odchodem",report.leaveUncertain,"Žádná další nejistota v této skupině. Rozsah katalogu zůstává omezený.",model,onEntry,{detailKey=it})
                item { Text("Odložené úkoly nejsou splněné. Zkontroluj je samostatně podle potřeby.",style=MaterialTheme.typography.bodySmall) }
            }
            "UNCERTAIN" -> {
                item { Text("Podklady k ověření",style=MaterialTheme.typography.headlineSmall);Text("Beta, Classic a komunitní podklady se nepovažují za potvrzenou dostupnost ve Forever. Chybějící údaje se nedoplňují odhadem.") }
                adviceRows(report.uncertain,model,onEntry,{detailKey=it})
                if(report.uncertain.isEmpty())item{Text("Žádné položky k ověření pro tuto oblast.")}
            }
            "WAITING" -> adviceSection("Později, chybějící podmínky a vyloučené volby",report.waiting,"Žádné takové potvrzené pravidlo pro tuto oblast.",model,onEntry,{detailKey=it})
            "DEFERRED" -> adviceSection("Tvoje odložené úkoly",report.deferred,"Žádný odložený úkol pro tuto oblast.",model,onEntry,{detailKey=it})
        }
        item {
            Text("${model.catalogEntries.size} položek v katalogu. Seznam není úplný přehled hry. Položky s nedoloženou oblastí mohou být uvedené i zde.",style=MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick=onCatalog){Text("Prohlédnout katalog")}
            Spacer(Modifier.height(16.dp))
        }
    }
    if(showFocus)AlertDialog(onDismissRequest={showFocus=false},title={Text("Čemu dát přednost?")},text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Řazení nemění podmínky dostupnosti. Doložené výlučné volby a důležité kroky Warlocka mají přednost.")
            GuideFocus.entries.forEach { focus->FilterChip(selected=model.guideFocus==focus,onClick={model.chooseGuideFocus(focus);showFocus=false},label={Text(focus.label)},modifier=Modifier.fillMaxWidth().testTag("focus_${focus.name}")) }
        }
    },confirmButton={TextButton(onClick={showFocus=false}){Text("Hotovo")}})
    val detail=report.all.firstOrNull { it.entry.key==detailKey }
    if(detail!=null)AdviceExplanation(model,detail,onClose={detailKey=null},onEntry={detailKey=null;onEntry(it)})
}

private fun LazyListScope.adviceSection(title:String,rows:List<Advice>,empty:String,model:GuideViewModel,onEntry:(String)->Unit,onWhy:(String)->Unit) {
    item { Text(title,style=MaterialTheme.typography.titleLarge);if(rows.isEmpty())Text(empty,style=MaterialTheme.typography.bodyMedium) }
    adviceRows(rows,model,onEntry,onWhy)
}
private fun LazyListScope.adviceRows(rows:List<Advice>,model:GuideViewModel,onEntry:(String)->Unit,onWhy:(String)->Unit) {
    items(rows,key={"advice:"+it.entry.key}) { a -> AdviceCard(model,a,onEntry,onWhy) }
}
@Composable private fun AdviceCard(model:GuideViewModel,a:Advice,onEntry:(String)->Unit,onWhy:(String)->Unit) {
    ElevatedCard(Modifier.fillMaxWidth().testTag("advice_${a.entry.key}")) {
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text(a.entry.nameOriginal,style=MaterialTheme.typography.titleMedium)
            Text(a.availability.label+(if(a.inProgress) " · Rozpracováno" else if(a.wish) " · Tvoje záložka / přání" else ""),color=MaterialTheme.colorScheme.primary,modifier=Modifier.testTag("availability_${a.entry.key}"))
            a.riskLabel?.let { Text(it,style=MaterialTheme.typography.labelLarge) }
            Text(verificationLabel(a.entry.verification),style=MaterialTheme.typography.labelSmall)
            if(a.ruleVerification!=null && a.ruleVerification!=a.entry.verification)Text("Pravidlo: "+verificationLabel(a.ruleVerification),style=MaterialTheme.typography.labelSmall)
            Text(if(a.areaIds.isEmpty()) "Oblast položky není doložená." else "Vazba na oblast: "+a.areaIds.sorted().joinToString { id->model.atlas.maps.firstOrNull { it.uiMapId==id }?.name ?: id.toString() },style=MaterialTheme.typography.bodySmall)
            if(model.spoilers==Spoilers.FULL) Text(a.ruleReason?.let { if(a.availability==Availability.UNVERIFIED) "Důvod z reference: $it" else it } ?: "Podmínky dostupnosti pro Forever zatím nejsou doložené.",style=MaterialTheme.typography.bodyMedium)
            else Text("Důvod a podmínky jsou skryté podle tvého nastavení.",style=MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                TextButton(onClick={onWhy(a.entry.key)},modifier=Modifier.testTag("why_${a.entry.key}")){Text("Proč a podmínky")}
                TextButton(onClick={onEntry(a.entry.key)},modifier=Modifier.testTag("advice_open_${a.entry.key}")){Text("Otevřít detail")}
            }
        }
    }
}
@Composable private fun AdviceExplanation(model:GuideViewModel,a:Advice,onClose:()->Unit,onEntry:(String)->Unit) {
    var revealed by rememberSaveable(a.entry.key,model.spoilers) { mutableStateOf(false) }
    val uri=LocalUriHandler.current
    var sourceError by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest=onClose,title={Text("Proč: ${a.entry.nameOriginal}")},text={
        Column(Modifier.heightIn(max=430.dp).verticalScroll(rememberScrollState()).testTag("advice_explanation"),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(a.availability.label);Text(verificationLabel(a.entry.verification))
            if(a.ruleVerification!=null)Text("Pravidlo: "+verificationLabel(a.ruleVerification))
            if(model.spoilers==Spoilers.FULL || revealed) {
                a.ruleReason?.let { Text(it,style=MaterialTheme.typography.titleMedium) }
                a.explanation.forEach { Text(it,style=MaterialTheme.typography.bodyMedium) }
                if(a.unlockCount>0)Text("Dokončení doplní předchozí kroky pro ${a.unlockCount} dalších úkolů. Jejich ostatní podmínky se posuzují zvlášť.")
                if(a.relatedKeys.isNotEmpty())Text("Související záznamy z podkladů",style=MaterialTheme.typography.titleSmall)
                a.relatedKeys.sorted().forEach { key->
                    model.historyEntry(key)?.let { e->TextButton(onClick={onEntry(key)},modifier=Modifier.testTag("advice_related_$key")){Text("Otevřít: ${e.nameOriginal}")} }
                }
            } else {
                Text("Podrobnosti jsou skryté podle tvého nastavení.")
                TextButton(onClick={revealed=true},modifier=Modifier.testTag("reveal_advice")){Text("Odkrýt tyto podrobnosti")}
            }
            Text("Podklady",style=MaterialTheme.typography.titleSmall)
            if(a.sourceIds.isEmpty())Text("Osobní historie nemá zdroj v aktuálním katalogu.")
            a.sourceIds.sorted().forEachIndexed { i,id->model.sourceUrls[id]?.let { url->TextButton(onClick={runCatching{uri.openUri(url)}.onFailure{sourceError=true}}){Text("Zdroj ${i+1} ↗")} } }
            if(sourceError)Text("Otevření zdroje vyžaduje prohlížeč a připojení.")
        }
    },confirmButton={TextButton(onClick=onClose){Text("Zavřít")}})
}
