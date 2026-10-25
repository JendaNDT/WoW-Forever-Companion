package cz.jenda.wowforever.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable fun DataLicensesDialog(onClose:()->Unit) {
    val context=LocalContext.current
    val notices=remember { listOf("LICENSE.md","PROVENANCE.md","gpl-3.0.md").joinToString("\n\n") { name ->
        context.assets.open("licenses/questiedb/$name").bufferedReader().use { it.readText() }
    } }
    AlertDialog(onDismissRequest=onClose,title={Text("Původ dat a licence")},text={
        Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Questie / QuestieDB, snímek 5c094f6e (24. 10. 2026). Převzaté a odvozené referenční údaje mají zachované původní licence a autorství. Zdrojový archiv obsahuje přesný snímek i převodní nástroje.")
            Text("Mapové podklady © Blizzard Entertainment, extrakce Warcraft Recorder / warcraftforever.games. Neoficiální osobní průvodce.")
            Text(notices,style=MaterialTheme.typography.bodySmall)
        }
    },confirmButton={TextButton(onClick=onClose){Text("Zavřít licence")}})
}
