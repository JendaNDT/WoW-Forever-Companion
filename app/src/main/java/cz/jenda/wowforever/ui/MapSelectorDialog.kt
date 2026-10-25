package cz.jenda.wowforever.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import cz.jenda.wowforever.content.normalizeSearch
import cz.jenda.wowforever.model.MapDescriptor

@Composable fun MapSelectorDialog(maps:List<MapDescriptor>,selected:Int,onChoose:(Int)->Unit,onClose:()->Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val shown=remember(maps,query) { maps.filter { normalizeSearch(query.trim()) in normalizeSearch(it.name) } }
    AlertDialog(onDismissRequest=onClose,title={Text("Oblasti a mapy (${maps.size})")},text={
        Column {
            OutlinedTextField(query,{query=it.take(80)},label={Text("Najít oblast")},singleLine=true,modifier=Modifier.fillMaxWidth().testTag("map_selector_query"))
            LazyColumn(Modifier.heightIn(max=380.dp).testTag("map_selector_list")) {
                if(shown.isEmpty())item { Text("Žádná oblast neodpovídá hledání.") }
                items(shown,key={it.uiMapId}) { map ->
                    TextButton(onClick={onChoose(map.uiMapId);onClose()},modifier=Modifier.fillMaxWidth().testTag("select_map_${map.uiMapId}")) {
                        Column(Modifier.fillMaxWidth()) {
                            Text((if(map.uiMapId==selected)"✓  " else "")+map.name)
                            Text(map.floorLabel,style=MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    },confirmButton={TextButton(onClick=onClose){Text("Zavřít výběr")}})
}
