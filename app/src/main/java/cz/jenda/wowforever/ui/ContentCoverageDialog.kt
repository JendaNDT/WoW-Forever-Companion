package cz.jenda.wowforever.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import cz.jenda.wowforever.GuideViewModel
import cz.jenda.wowforever.content.EntryKind
import cz.jenda.wowforever.content.RelationKind
import cz.jenda.wowforever.content.Verification
import cz.jenda.wowforever.domain.areaCoverage

@Composable fun ContentCoverageDialog(model:GuideViewModel,onClose:()->Unit) {
    val entries=model.catalogEntries
    val positions=model.atlas.points.filter { it.entryKey!=null }
    val uri=LocalUriHandler.current
    AlertDialog(onDismissRequest=onClose,title={Text("Pokrytí průvodce")},text={
        Column(Modifier.verticalScroll(rememberScrollState()).testTag("coverage_details"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(if(model.atlas.maps.size>8)"Referenční průvodce Horda / Warlock 1–60" else "Zpracovaný výběr pro Hordu a Warlocka",style=MaterialTheme.typography.titleMedium)
            Text("${entries.size} položek · ${positions.size} přímých poloh · ${model.atlas.maps.size} offline map",modifier=Modifier.testTag("coverage_counts"))
            Text("NPC: ${entries.count { it.kind==EntryKind.NPC }} · úkoly: ${entries.count { it.kind==EntryKind.QUEST }} · předměty: ${entries.count { it.kind==EntryKind.ITEM }} · objekty: ${entries.count { it.kind==EntryKind.OBJECT }} · kouzla: ${entries.count { it.kind==EntryKind.SPELL }} · doprava: ${entries.count { it.kind==EntryKind.TRAVEL }}")
            Text(if(model.atlas.maps.size>8)"Katalog zahrnuje referenční úkoly Hordy a Warlocka 1–60, související předměty, služby a přípravu na dungeony. Nejde o potvrzení úplnosti vydané Forever ani o optimální XP trasu." else "Rozsah se počítá z právě aktivního balíčku. Celý svět ani cesta do úrovně 60 nejsou zpracované.")
            if(model.atlas.maps.size>8)Text("QuestieDB: připnutý snímek z 25. 10. 2026. Použitý je převedený Classic základ, generovaný Forever rozdíl a statické Forever opravy. Živé skripty, starší zděděné opravy a další trasovací vrstvy nejsou spuštěné. U cílů s mnoha výskyty se zobrazuje nejvýše šest explicitních bodů na mapu; zadavatelé úkolů mají všechny doložené polohy.")
            areaCoverage(model.atlas,entries,model.relations,model.guideRules).forEach { area ->
                Text(area.map.name,style=MaterialTheme.typography.titleMedium,modifier=Modifier.testTag("coverage_area_${area.map.uiMapId}"))
                Text("${area.npcCount} NPC s polohou · ${area.objectCount} objektů · ${area.questCount} souvisejících úkolů · ${area.itemCount} souvisejících předmětů · ${area.travelCount} dopravních bodů")
                Text(area.map.scopeCs ?: if(area.npcCount==0)"Mapový přehled; podrobný obsah této oblasti není naplněný." else "Výběr doložených bodů a navázaných záznamů. Seznam oblasti není úplný.",modifier=Modifier.testTag("coverage_scope_${area.map.uiMapId}"))
            }
            Text("Úkoly jsou přiřazené přes doložené NPC nebo zařazení v průvodci. Počet úkolů není počet přesných questových souřadnic; jeden úkol může souviset s více oblastmi.",style=MaterialTheme.typography.bodySmall)
            Text("Podklady: zveřejněná pozorování bety, klientské záznamy a označené komunitní reference. Dokončení úkolu v aplikaci nepotvrdí správnost zdroje.")
            val confirmed=entries.count { it.releaseConfirmed && it.verification==Verification.RELEASE }
            val links=model.relations.count { it.kind==RelationKind.PREREQUISITE && it.routingEnabled && it.verification==Verification.RELEASE }
            Text("Potvrzeno ve vydané hře: $confirmed položek a $links návazností.",modifier=Modifier.testTag("coverage_release"))
            Text("Co ještě ověřit",style=MaterialTheme.typography.titleMedium)
            Text("Podmínky přijímání a návaznosti úkolů, nabídku trenérů a odměny v aktuálním klientu. U Vile Familiars 1485 zůstává rozpor. Přesná místa cílů uvnitř jeskyní nejsou doložená.")
            Text("Příprava na dungeon není mapa jeho vnitřku. Záznam letového bodu nepotvrzuje dopravní linku. Úplný soupis tajných míst, dropů a výbavy není doložený; prázdná kontrola před odchodem nepotvrzuje dokončení oblasti.")
            TextButton(onClick={runCatching { uri.openUri("https://foreverchanges.pro/class/warlock") }}) { Text("ForeverChanges: změny Warlocka ↗") }
            Text("Odkaz potřebuje prohlížeč a internet. Místní katalog a mapy fungují offline.",style=MaterialTheme.typography.bodySmall)
        }
    },confirmButton={TextButton(onClick=onClose){Text("Zavřít pokrytí")}})
}
