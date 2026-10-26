package cz.jenda.wowforever.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import cz.jenda.wowforever.BuildConfig
import cz.jenda.wowforever.GuideViewModel

/** Offline operating guide. Counts describe the active catalog, not a promised content milestone. */
@Composable fun HelpDialog(model:GuideViewModel,onClose:()->Unit,onBackup:()->Unit,onPackages:()->Unit,onCoverage:()->Unit) {
    var showLicenses by remember { mutableStateOf(false) }
    val list=rememberLazyListState()
    if(!showLicenses) AlertDialog(onDismissRequest=onClose,title={Text("Návod k použití")},text={
        LazyColumn(Modifier.heightIn(max=440.dp).testTag("help_list"),state=list,verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item(key="current") {
                Text("Tvoje vydání",style=MaterialTheme.typography.titleMedium)
                Text("Aplikace ${BuildConfig.VERSION_NAME}. Data ${model.contentMetadata?.generation ?: 2} · ${model.catalogEntries.size} položek · ${model.atlas.maps.size} map.",modifier=Modifier.testTag("help_current"))
                Text("Podklady z bety a Classicu slouží jako reference; správnost ve vydané Forever zatím není potvrzená.")
            }
            item(key="start") {
                Text("1. Postava",style=MaterialTheme.typography.titleMedium)
                Text("Úroveň měň tlačítky − / + v horní liště, oblast klepnutím na její název. Změny se ukládají automaticky. Rasu a jméno upravíš v Nastavení → Postava tlačítkem Uložit postavu.")
            }
            item(key="quests") {
                Text("2. Co dělat dál",style=MaterialTheme.typography.titleMedium)
                Text("Úkoly → Co dál nabízí úkoly a kouzla podle úrovně a oblasti. Brzy a neúplné údaje jsou sbalené. Beru zaznamená rozpracovaný úkol, Hotovo jeho dokončení; obě tlačítka jsou i nahoře v detailu. Podmínky a zdroje zkontroluj podle hry.")
            }
            item(key="map") {
                Text("3. Mapa a hledání",style=MaterialTheme.typography.titleMedium)
                Text("Mapa se drží severem nahoru. Přibližuj prsty nebo tlačítky. Prohlížení jiné mapy nemění oblast postavy. Podržením volného místa vytvoříš poznámku; Jedna záložka Hledat najde úkoly, NPC, předměty i poznámky. Klepnutí otevře společný detail; Na mapě ukáže doloženou polohu.")
                Text("Značky C jsou reference z Classicu, ? komunitní podklady. Čísla výpravy jsou tvoje pořadí cílů. Chybějící polohy ani průchozí cesty se nedomýšlejí.")
            }
            item(key="coverage") {
                Text("4. Obsah bez internetu",style=MaterialTheme.typography.titleMedium)
                Text("Obsah je přímo v APK a při prvním spuštění se připraví automaticky bez internetu. Aktuální rozsah, zdroje a mezery ukazuje Pokrytí. Otevření webového zdroje potřebuje připojení.")
                OutlinedButton(onClick=onCoverage,modifier=Modifier.testTag("help_coverage")){Text("Otevřít pokrytí")}
            }
            item(key="updates") {
                Text("Aktualizace",style=MaterialTheme.typography.titleMedium)
                Text("Před aktualizací vyexportuj zálohu. Aktualizace přes stávající instalaci vyžaduje shodný podpis APK. Při odlišném podpisu nejdřív zazálohuj postup a po čisté instalaci jej obnov. Balíčky a mapy slouží také pro volitelné další aktualizace obsahu.")
                OutlinedButton(onClick=onPackages,modifier=Modifier.testTag("help_packages")){Text("Otevřít balíčky a mapy")}
            }
            item(key="recovery") {
                Text("5. Záloha a obnova postupu",style=MaterialTheme.typography.titleMedium)
                Text("Záloha obsahuje uložené změny. Rozpracované změny jména a rasy nejdřív ulož. Soubor JSON schovej také mimo telefon. Při obnově je výchozí volbou nový profil; potvrzené nahrazení má místní vratný snímek.")
                Text("Při problému aplikaci nejdřív znovu otevři a zkontroluj aktivní profil. Návrat ke starším herním datům provedeš ve správci balíčků; osobní historie zůstane zachovaná. Poznámka z chybějící mapy čeká na opětovné zapnutí správného obsahu.")
                Text("Neodinstalovávej aplikaci kvůli návratu ke starší verzi APK: odinstalování může odstranit místní postup. Pro změnu dat použij návrat balíčku, pro obnovu postupu vlastní zálohu.")
                OutlinedButton(onClick=onBackup,modifier=Modifier.testTag("help_backup")){Text("Otevřít profily a zálohu")}
            }
            item(key="offline") {
                Text("Bez internetu a při potížích",style=MaterialTheme.typography.titleMedium)
                Text("Stažené mapy, katalog, poznámky a postup fungují offline. Otevření webového zdroje vyžaduje internet. Prázdný seznam doporučení nepotvrzuje dokončení oblasti.")
                Text("Při hlášení problému uveď verzi aplikace, generaci dat a akci, po které nastal. Zvětšené písmo a nízké okno mohou změnit rozložení; panely posouvej a detail mapového bodu otevři tlačítkem s jeho názvem.")
            }
            item(key="licenses") { TextButton(onClick={showLicenses=true}) { Text("Původ dat a licence") } }
            item(key="changes") {
                Text("Poslední změny",style=MaterialTheme.typography.titleMedium)
                Text("Tři záložky Mapa / Úkoly / Hledat, přepínač Co dál / Deník, společný vytahovací detail a Nastavení v horní liště. Herní data ani formát zálohy se nemění.")
                Text("Sestavení ${BuildConfig.SOURCE_REVISION.take(8)}. Telefonní přejímka a měření výkonu zůstávají samostatnou kontrolou.",style=MaterialTheme.typography.bodySmall)
            }
        }
    },confirmButton={TextButton(onClick=onClose){Text("Zavřít návod")}})
    if(showLicenses) DataLicensesDialog(onClose={showLicenses=false})
}
