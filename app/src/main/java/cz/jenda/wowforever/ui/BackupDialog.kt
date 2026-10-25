package cz.jenda.wowforever.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import cz.jenda.wowforever.GuideViewModel
import java.text.DateFormat
import java.util.Date

@Composable fun BackupDialog(model:GuideViewModel,onClose:()->Unit) {
    val create=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> if(uri!=null)model.exportBackup(uri) }
    val open=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if(uri!=null)model.readBackup(uri) }
    var recoverId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(model.profile.id) { model.refreshSnapshots() }
    if(recoverId!=null) {
        AlertDialog(onDismissRequest={recoverId=null},title={Text("Vrátit místní snímek?")},text={Text("Aktivní profil se nahradí zvoleným snímkem. Před nahrazením se uloží další snímek současného stavu.")},
            confirmButton={TextButton(onClick={val id=recoverId!!;recoverId=null;model.recoverSnapshot(id)},enabled=!model.isBusy,modifier=Modifier.testTag("confirm_recover")){Text("Vrátit snímek")}},
            dismissButton={TextButton(onClick={recoverId=null}){Text("Zrušit")}})
        return
    }
    AlertDialog(onDismissRequest={if(!model.backupBusy){model.clearBackupPreview();onClose()}},title={Text("Profily a záloha")},text={
        Column(Modifier.heightIn(max=430.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("Zálohují se všechny uložené profily: postava, úkoly, získané předměty, odměny, záložky, poznámky a plán. Mapové obrázky se nekopírují.",style=MaterialTheme.typography.bodySmall)
            Button(onClick={create.launch("Forever_zaloha_${System.currentTimeMillis()}.json")},enabled=!model.isBusy,modifier=Modifier.testTag("export_backup")){Text("Uložit zálohu do souboru")}
            OutlinedButton(onClick={open.launch(arrayOf("application/json","application/octet-stream","text/plain"))},enabled=!model.isBusy,modifier=Modifier.testTag("open_backup")){Text("Vybrat zálohu k obnově")}
            if(model.backupBusy) { LinearProgressIndicator(Modifier.fillMaxWidth());Text("Pracuji se zálohou…") }
            model.backupMessage?.let { Text(it,modifier=Modifier.testTag("backup_message")) }
            val file=model.backupPreview
            if(file!=null) {
                var replace by remember(file) { mutableStateOf(false) }
                var sourceId by remember(file) { mutableStateOf(file.activeProfileId) }
                val source=file.profiles.first { it.profile.id==sourceId }
                HorizontalDivider();Text("Náhled před obnovou",style=MaterialTheme.typography.titleLarge)
                Text("${file.profiles.size} profilů · ${file.profiles.sumOf { it.quests.size }} záznamů úkolů · ${file.profiles.sumOf { it.notes.size }} poznámek")
                file.profiles.forEach { p -> Text("${p.profile.displayName} · úroveň ${p.profile.level} · ${p.rowCount} záznamů",style=MaterialTheme.typography.bodySmall) }
                FilterChip(selected=!replace,onClick={replace=false},enabled=!model.isBusy,label={Text("Načíst do nových profilů")},modifier=Modifier.testTag("restore_new"))
                FilterChip(selected=replace,onClick={replace=true},enabled=!model.isBusy,label={Text("Nahradit aktivní profil")},modifier=Modifier.testTag("restore_replace"))
                if(replace) {
                    Text("Cíl: ${model.profile.displayName}. ${model.personalState.rowCount} dosavadních záznamů se nahradí ${source.rowCount} záznamy vybraného profilu. Současný stav se nejprve uloží do místního snímku.")
                    if(file.profiles.size>1) file.profiles.forEach { p -> FilterChip(selected=sourceId==p.profile.id,onClick={sourceId=p.profile.id},enabled=!model.isBusy,label={Text(p.profile.displayName)}) }
                } else Text("Vznikne ${file.profiles.size} nových profilů. Dosavadní záznamy se nepřepisují.")
                Button(onClick={model.importBackup(replace,sourceId)},enabled=!model.isBusy,modifier=Modifier.testTag("confirm_import")){Text(if(replace) "Nahradit a uchovat snímek" else "Načíst nové profily")}
                TextButton(onClick=model::clearBackupPreview,enabled=!model.isBusy){Text("Zrušit obnovu")}
            }
            HorizontalDivider();Text("Aktivní profil",style=MaterialTheme.typography.titleMedium)
            model.profileChoices.forEach { p -> FilterChip(selected=p.id==model.profile.id,onClick={model.switchProfile(p.id)},enabled=!model.isBusy,
                label={Text("${p.displayName} · úroveň ${p.level}")},modifier=Modifier.testTag("profile_choice_${p.id}")) }
            if(model.snapshots.isNotEmpty()) {
                HorizontalDivider();Text("Místní snímky před obnovou",style=MaterialTheme.typography.titleMedium)
                Text("Poslední tři snímky tohoto profilu. Pro přenos na jiné zařízení použij soubor zálohy.",style=MaterialTheme.typography.bodySmall)
                model.snapshots.forEach { snapshot ->
                    TextButton(onClick={recoverId=snapshot.id},enabled=!model.isBusy,modifier=Modifier.testTag("snapshot_${snapshot.id}")) {
                        Text(snapshot.label+" · "+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(Date(snapshot.createdAt)))
                    }
                }
            }
        }
    },confirmButton={TextButton(onClick={model.clearBackupPreview();onClose()},enabled=!model.backupBusy){Text("Zavřít")}})
}
