package cz.jenda.wowforever.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cz.jenda.wowforever.GuideViewModel
import cz.jenda.wowforever.data.Spoilers
import cz.jenda.wowforever.domain.GuideFocus
import cz.jenda.wowforever.domain.backupReminderDue
import cz.jenda.wowforever.domain.lastBackupDays

@OptIn(ExperimentalLayoutApi::class)
@Composable fun SettingsScreen(model:GuideViewModel,onOverlay:(String)->Unit) {
    val page=model.settingsPage
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer=LifecycleEventObserver { _,event->if(event==Lifecycle.Event.ON_RESUME)now=System.currentTimeMillis() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    BackHandler(enabled=page!="MENU") { model.chooseSettingsPage("MENU") }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp)) {
            TextButton(onClick={if(page=="MENU")model.back() else model.chooseSettingsPage("MENU")},modifier=Modifier.testTag("settings_back")) { Text("Zpět") }
            Text(if(page=="CHARACTER")"Postava" else if(page=="DISPLAY")"Zobrazení" else "Nastavení",style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(12.dp))
        }
        if(page=="CHARACTER") CharacterScreen(model)
        else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            if(page=="DISPLAY") {
                Text("Kolik chceš prozradit?",style=MaterialTheme.typography.titleMedium)
                Spoilers.entries.forEach { choice->FilterChip(selected=model.spoilers==choice,onClick={model.chooseSpoilers(choice)},label={Text(choice.label)},modifier=Modifier.testTag("spoilers_${choice.name}")) }
                Text("Řazení doporučení",style=MaterialTheme.typography.titleMedium)
                GuideFocus.entries.forEach { focus->FilterChip(selected=model.guideFocus==focus,onClick={model.chooseGuideFocus(focus)},label={Text(focus.label)},modifier=Modifier.testTag("focus_${focus.name}")) }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text("Nezhasínat displej na mapě",modifier=Modifier.weight(1f))
                    Switch(checked=model.keepMapScreenOn,onCheckedChange=model::chooseKeepScreenOn,modifier=Modifier.testTag("keep_map_awake"))
                }
                Text("Nastavení se ukládá ihned.",style=MaterialTheme.typography.bodySmall)
            } else {
                SettingsAction("Postava", "Jméno a rasa", "settings_character") { model.chooseSettingsPage("CHARACTER") }
                SettingsAction("Profily a záloha", "Uložit, obnovit nebo přepnout profil", "backup") { onOverlay("BACKUP") }
                Text(model.backupState.lastBackupDays(now)?.let { when(it) { 0L->"Poslední záloha: dnes";1L->"Poslední záloha: včera";else->"Poslední záloha: před $it dny" } } ?: "Poslední záloha: dosud neuložená",modifier=Modifier.testTag("last_backup"),style=MaterialTheme.typography.bodySmall)
                if(model.backupState.backupReminderDue(now))OutlinedCard(modifier=Modifier.fillMaxWidth().testTag("backup_reminder")) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Máš nové změny. Ulož si zálohu také mimo telefon.")
                        TextButton(onClick={onOverlay("BACKUP")}){Text("Uložit zálohu")}
                    }
                }
                SettingsAction("Zobrazení", "Popisy a řazení doporučení", "focus_settings") { model.chooseSettingsPage("DISPLAY") }
                SettingsAction("Balíčky a mapy", "Obsah a návrat k předchozím datům", "packages") { onOverlay("PACKAGES") }
                SettingsAction("Pokrytí", "Rozsah, zdroje a nejisté údaje", "content_coverage") { onOverlay("COVERAGE") }
                SettingsAction("Návod", "Použití aplikace a obnova postupu", "open_help") { onOverlay("HELP") }
                SettingsAction("O aplikaci", "Verze a licence", "about") { onOverlay("ABOUT") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
@Composable private fun SettingsAction(title:String,subtitle:String,tag:String,onClick:()->Unit) {
    OutlinedButton(onClick=onClick,modifier=Modifier.fillMaxWidth().testTag(tag),contentPadding=PaddingValues(14.dp)) {
        Column(Modifier.fillMaxWidth()) { Text(title,style=MaterialTheme.typography.titleMedium);Text(subtitle,style=MaterialTheme.typography.bodySmall) }
    }
}

@Composable fun QuestsScreen(model:GuideViewModel,onSearch:()->Unit,onOverlay:(String)->Unit,onEntry:(String)->Unit,onPoint:(String)->Unit) {
    val holder=androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            FilterChip(selected=model.questTab=="NEXT",onClick={model.chooseQuestTab("NEXT")},label={Text("Co dál")},modifier=Modifier.testTag("quests_next"))
            FilterChip(selected=model.questTab=="JOURNAL",onClick={model.chooseQuestTab("JOURNAL")},label={Text("Deník")},modifier=Modifier.testTag("quests_journal"))
        }
        Box(Modifier.weight(1f)) {
            holder.SaveableStateProvider(model.questTab) {
                if(model.questTab=="JOURNAL") JourneyScreen(model,onSearch,onPlan={onOverlay("EXPEDITION")},onPoint=onPoint,onEntry=onEntry)
                else NowScreen(model,onSearch,onEntry,onCoverage={onOverlay("COVERAGE")},onLeave={onOverlay("LEAVING")})
            }
        }
    }
}
