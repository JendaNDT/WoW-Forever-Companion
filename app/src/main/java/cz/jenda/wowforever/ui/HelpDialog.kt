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
    AlertDialog(onDismissRequest=onClose,title={Text("Návod k použití")},text={
        LazyColumn(Modifier.heightIn(max=440.dp).testTag("help_list"),state=list,verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item(key="current") {
                Text("Tvoje vydání",style=MaterialTheme.typography.titleMedium)
                Text("Aplikace ${BuildConfig.VERSION_NAME}. Data ${model.contentMetadata?.generation ?: 2} · ${model.catalogEntries.size} položek · ${model.atlas.maps.size} map.",modifier=Modifier.testTag("help_current"))
                Text("Testovací průvodce pro Hordu a Warlocka. Data 7 přinášejí referenční katalog cesty 1–60. Rozsah závisí na aktivním balíčku; úplnost vydané hry není potvrzená. Údaje z bety a Classicu nejsou potvrzením vydané hry.")
            }
            item(key="start") {
                Text("Začínám hrát",style=MaterialTheme.typography.titleMedium)
                Text("V Postavě vyber rasu, nastav skutečnou úroveň a oblast a ulož je. Samotné prohlížení jiné mapy oblast postavy nezmění. Profese a režim můžeš doplnit později.")
                Text("Hledej český popis nebo původní název. V detailu ověř zdroj, podmínky a stav ověření. Hotový úkol označ ručně; získaný předmět a výběr odměny se evidují zvlášť.")
            }
            item(key="map") {
                Text("Mapa a výprava",style=MaterialTheme.typography.titleMedium)
                Text("Vrstvy mohou body skrýt. Body s otazníkem jsou komunitní reference. Body s C a názvem Classic jsou pouze referenční polohy, ve Forever čekají na ověření. Hledání umí ukázat správnou oblast; chybějící poloha nedostane vymyšlenou značku. Podržením volného místa vytvoříš poznámku.")
                Text("Čísla ve výpravě jsou tvoje pořadí cílů. Neprokazují průchozí cestu. Objevený letový bod sám nepotvrdí dopravní spojení.")
            }
            item(key="coverage") {
                Text("Co je zpracované",style=MaterialTheme.typography.titleMedium)
                Text("Rozsah zjistíš z právě aktivních dat. Aplikace obsahuje data 7: úkoly Hordy / Warlocka 1–60, související postavy, objekty a předměty, služby, přípravu na dungeony a rozšířený atlas. V katalogu použij filtry Warlock, Dungeony, oblast a pásmo úrovní. Oblasti na mapě můžeš hledat podle názvu. Obsah se při prvním spuštění připraví automaticky bez internetu. Aktuální rozsah ukazuje Pokrytí.")
                OutlinedButton(onClick=onCoverage,modifier=Modifier.testTag("help_coverage")){Text("Otevřít pokrytí")}
            }
            item(key="updates") {
                Text("Aktualizace aplikace a dat",style=MaterialTheme.typography.titleMedium)
                Text("Nejdřív vyexportuj zálohu. Nové APK instaluj přes E09 nebo novější verzi bez odinstalování. Pokud máš ještě E08, ponech ji a přenes její JSON zálohu do novější aplikace.")
                Text("Pro běžné používání stačí nainstalovat toto APK. Data 7 už máš uvnitř. Samostatný soubor .wfp je volitelný způsob dalších aktualizací přes Balíčky a mapy; před aktivací ukáže náhled.")
                OutlinedButton(onClick=onPackages,modifier=Modifier.testTag("help_packages")){Text("Otevřít balíčky a mapy")}
            }
            item(key="recovery") {
                Text("Záloha a obnova postupu",style=MaterialTheme.typography.titleMedium)
                Text("Záloha obsahuje uložené změny. Rozpracovanou Postavu nejdřív ulož. Soubor JSON schovej také mimo telefon. Při obnově je výchozí volbou nový profil; potvrzené nahrazení má místní vratný snímek.")
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
                Text("Oprava 0.15.1: menší spotřeba paměti při přípravě obsahu a spuštění, opravené společné zdroje předmětů v doporučeních. Data i osobní postup zůstávají zachované. E10 celek: referenční katalog Hordy / Warlocka 1–60, 61 offline map, cíle a zdroje předmětů, přehled dungeonů a filtry katalogu. Data 7 jsou přímo v APK a připraví se automaticky; první spuštění může trvat déle. Zachovává se postup i novější importovaný obsah. E12: návod, dohledatelný původ sestavení a kontrola původního podpisu. E11: rychlejší hledání, uvolňování neaktivních map, velké písmo a obnova nastavení. E10: první rozšířený obsah v balíčku 4.")
                Text("Sestavení ${BuildConfig.SOURCE_REVISION.take(8)}. Telefonní přejímka a měření výkonu zůstávají samostatnou kontrolou.",style=MaterialTheme.typography.bodySmall)
            }
        }
    },confirmButton={TextButton(onClick=onClose){Text("Zavřít návod")}})
    if(showLicenses) DataLicensesDialog(onClose={showLicenses=false})
}
