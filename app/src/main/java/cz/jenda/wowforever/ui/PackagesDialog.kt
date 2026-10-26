package cz.jenda.wowforever.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import cz.jenda.wowforever.GuideViewModel
import java.util.Locale

fun packageSize(bytes:Long)=String.format(Locale.forLanguageTag("cs-CZ"),"%.1f MiB",bytes/1048576.0)

@Composable fun PackagesDialog(model:GuideViewModel,onClose:()->Unit) {
    var url by rememberSaveable{mutableStateOf("")}
    var wifiOnly by rememberSaveable{mutableStateOf(true)}
    var confirm by rememberSaveable{mutableStateOf<String?>(null)}
    val pick=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)model.readContentPackage(uri)}
    LaunchedEffect(Unit){model.refreshPackages()}
    val status=model.packageStatus
    if(confirm==null) AlertDialog(onDismissRequest={if(!model.packageBusy)onClose()},title={Text("Balíčky a mapy")},text={
        LazyColumn(Modifier.heightIn(max=500.dp).testTag("packages_list"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item {
                Text("Obsah generace ${status?.generation ?: "…"}",style=MaterialTheme.typography.titleMedium,modifier=Modifier.testTag("package_generation"))
                Text(if(status?.downloaded==true)"Ověřený stažený balíček · ${packageSize(status.downloadBytes)}" else "Přibalený katalog a mapy · fungují offline")
                Text("Herní obsah se mění odděleně od APK. Tvůj postup, poznámky a výprava zůstávají uložené zvlášť.",style=MaterialTheme.typography.bodySmall)
                if((status?.generation ?: 0)<4)Text("Úplný dosavadní obsah je uvnitř aplikace. Pro návrat k němu použij Obnovit přibalený obsah. Samostatné soubory slouží k dalším aktualizacím.",modifier=Modifier.testTag("e10_package_hint"))
            }
            items(status?.maps.orEmpty(),key={it.first}) { map -> Text("${map.first} · ${packageSize(map.second)}",modifier=Modifier.testTag("package_map_${map.first}")) }
            item {
                HorizontalDivider()
                Text("Aktualizovat obsah",style=MaterialTheme.typography.titleMedium)
                Text("Vyber podepsaný soubor .wfp od vydavatele průvodce. Běžný ZIP, webová stránka ani osobní záloha sem nepatří.")
                OutlinedButton(onClick={pick.launch(arrayOf("*/*"))},enabled=!model.isBusy,modifier=Modifier.testTag("package_import")){Text("Vybrat balíček ze souboru")}
                OutlinedTextField(url,{url=it.take(2000)},singleLine=true,label={Text("Přímý HTTPS odkaz na balíček")},enabled=!model.isBusy,modifier=Modifier.fillMaxWidth().testTag("package_url"))
                Row {Checkbox(wifiOnly,{wifiOnly=it},enabled=!model.isBusy,modifier=Modifier.testTag("package_wifi"));Text("Stahovat pouze přes Wi-Fi",modifier=Modifier.padding(top=12.dp))}
                Text("Automatická kontrola aktualizací není zapnutá. Stažení spustíš ručně; server musí nabídnout přímý soubor. Osobní záloha se na server neodesílá.",style=MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick={model.inspectContentDownload(url,wifiOnly)},enabled=!model.isBusy && url.isNotBlank(),modifier=Modifier.testTag("package_download_info")){Text("Zjistit velikost stažení")}
                model.packageDownloadOffer?.takeIf{it.url==url.trim()}?.let{offer->
                    Text("Ke stažení: ${packageSize(offer.bytes)}")
                    OutlinedButton(onClick={model.downloadContentPackage(url,wifiOnly)},enabled=!model.isBusy,modifier=Modifier.testTag("package_download")){Text("Stáhnout a ověřit")}
                }
            }
            if(model.packageBusy)item {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Přeneseno ${packageSize(model.packageBytes)}")
                if(model.packageCanCancel)TextButton(onClick=model::cancelPackageWork,modifier=Modifier.testTag("package_cancel")){Text("Zrušit přípravu")}
            }
            model.packageMessage?.let { message -> item {Text(message,modifier=Modifier.testTag("package_message"))} }
            model.packagePreview?.let { candidate -> item {
                OutlinedCard(Modifier.fillMaxWidth().testTag("package_preview")) {
                    Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text("Náhled generace ${candidate.manifest.generation}",style=MaterialTheme.typography.titleMedium)
                        Text("${candidate.catalog.entries.size} položek · ${candidate.catalog.maps.size} map · ${packageSize(candidate.bytes)}")
                        Text("Kontrola podkladů: ${candidate.manifest.checkedOn}. Podpis vydavatele potvrzuje původ souboru; beta a neověřené herní údaje zůstávají tak označené.")
                        Text("Položek mizících z katalogu: ${model.packageRemovedEntries}. Dotčených osobních záznamů: ${model.packageAffectedRecords}. Jejich historie zůstane zachovaná.")
                        if(!candidate.unchanged)Button(onClick={confirm="ACTIVATE"},enabled=!model.isBusy,modifier=Modifier.testTag("package_activate")){Text("Použít tento obsah")}
                        TextButton(onClick=model::clearPackagePreview,enabled=!model.isBusy){Text("Zavřít náhled")}
                    }
                }
            } }
            item {
                HorizontalDivider()
                Text("Návrat a místo v telefonu",style=MaterialTheme.typography.titleMedium)
                Text(status?.previousGeneration?.let{"Předchozí funkční obsah: generace $it."} ?: "Předchozí generace zatím není uložená.")
                OutlinedButton(onClick={confirm="ROLLBACK"},enabled=!model.isBusy && status?.previousGeneration!=null,modifier=Modifier.testTag("package_rollback")){Text("Návrat k předchozímu obsahu")}
                OutlinedButton(onClick={confirm="RESET"},enabled=!model.isBusy && status!=null,modifier=Modifier.testTag("package_reset")){Text("Obnovit přibalený obsah")}
                Text("Vrátí obsah dodávaný v tomto APK, uvolní dříve stažené generace a zachová tvůj postup, poznámky a výpravu. Přibalené mapy z APK smazat nelze.",style=MaterialTheme.typography.bodySmall)
            }
        }
    },confirmButton={TextButton(onClick=onClose,enabled=!model.packageBusy){Text("Zavřít balíčky")}})
    confirm?.let { action -> AlertDialog(onDismissRequest={confirm=null},title={Text("Potvrdit změnu obsahu?")},text={Text(when(action){
        "ACTIVATE"->"Přepnu celý katalog a mapy na ověřenou novou generaci. Dosavadní obsah zůstane uložený pro návrat. Osobní záznamy se nemění."
        "ROLLBACK"->"Obnovím předchozí katalog a mapy. Osobní záznamy, včetně nově dokončených úkolů, zůstanou zachované."
        else->"Obnovím obsah dodávaný v APK a uvolním dříve stažené generace. Tvůj postup, poznámky a výprava zůstanou zachované."
    })},confirmButton={TextButton(onClick={confirm=null;when(action){"ACTIVATE"->model.activateContentPackage();"ROLLBACK"->model.rollbackContentPackage();else->model.resetContentPackages()}},enabled=!model.isBusy,modifier=Modifier.testTag("package_confirm")){Text("Potvrdit")}},dismissButton={TextButton(onClick={confirm=null}){Text("Zrušit")}}) }
}
