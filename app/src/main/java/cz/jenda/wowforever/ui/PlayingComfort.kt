package cz.jenda.wowforever.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cz.jenda.wowforever.GuideViewModel
import cz.jenda.wowforever.data.db.QuestState
import cz.jenda.wowforever.content.EntryKind
import cz.jenda.wowforever.domain.*
import kotlinx.coroutines.delay

@Composable fun KeepMapAwake(enabled:Boolean) {
    val view=LocalView.current
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(view,lifecycle,enabled) {
        val previous=view.keepScreenOn
        fun update() { view.keepScreenOn=enabled && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        val observer=LifecycleEventObserver { _,_->update() }
        lifecycle.addObserver(observer);update()
        onDispose { lifecycle.removeObserver(observer);view.keepScreenOn=previous }
    }
}

@Composable fun PersonalUndoBar(model:GuideViewModel) {
    val notice=model.undoNotice ?: return
    LaunchedEffect(notice.token) {
        delay((notice.deadline-android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0))
        model.dismissUndo(notice.token)
    }
    Snackbar(modifier=Modifier.fillMaxWidth().padding(8.dp).testTag("undo_bar"),
        action={TextButton(onClick={model.undoFromNotice(notice.token)},enabled=!model.isBusy,modifier=Modifier.testTag("undo_personal")){Text("Vrátit")}}) {
        Text(notice.label)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun WelcomeScreen(model:GuideViewModel) {
    var race by rememberSaveable { mutableStateOf("ORC") }
    var level by rememberSaveable { mutableStateOf("1") }
    var area by rememberSaveable { mutableIntStateOf(1411) }
    var chooseArea by rememberSaveable { mutableStateOf(false) }
    var backup by rememberSaveable { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text("Vítej ve Forever průvodci",style=MaterialTheme.typography.headlineSmall)
            Text("Warlock za Hordu · postavu můžeš kdykoli změnit v Nastavení.")
            Text("Rasa",style=MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf("ORC","TROLL","UNDEAD").forEach { value->FilterChip(selected=race==value,onClick={race=value},label={Text(raceLabel(value))},modifier=Modifier.testTag("welcome_race_$value")) }
            }
            OutlinedTextField(level,{level=it.filter(Char::isDigit).take(2)},label={Text("Úroveň (1–60)")},singleLine=true,
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth().testTag("welcome_level"))
            OutlinedButton(onClick={chooseArea=true},modifier=Modifier.fillMaxWidth().testTag("welcome_area")) { Text(model.atlas.maps.firstOrNull { it.uiMapId==area }?.name ?: "Vybrat oblast") }
            Text("Podržením volného místa na mapě přidáš vlastní poznámku.",style=MaterialTheme.typography.bodySmall)
            model.dataError?.let { Text(it,color=MaterialTheme.colorScheme.error);TextButton(onClick=model::dismissError){Text("Rozumím")} }
            Button(onClick={model.completeWelcome(race,level.toIntOrNull() ?: 1,area)},enabled=!model.isBusy && level.toIntOrNull() in 1..60,
                modifier=Modifier.fillMaxWidth().testTag("welcome_start")) { Text(if(model.savingProfile)"Ukládám…" else "Začít") }
            OutlinedButton(onClick={backup=true},modifier=Modifier.fillMaxWidth().testTag("welcome_restore")) { Text("Obnovit vlastní zálohu") }
        }
    }
    if(backup)BackupDialog(model){backup=false}
    else if(chooseArea)MapSelectorDialog(model.atlas.maps,area,{area=it},onClose={chooseArea=false})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun BeforeLeavingSheet(model:GuideViewModel,onClose:()->Unit,onEntry:(String)->Unit) {
    val area=model.atlas.maps.firstOrNull { it.uiMapId==model.profile.currentUiMapId }?.name ?: "oblasti"
    val choices=remember(model.recommendations,model.profile.level) { model.recommendations.beforeLeaving(model.profile.level) }
    ModalBottomSheet(onDismissRequest=onClose,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),modifier=Modifier.testTag("before_leaving_sheet")) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.94f).navigationBarsPadding()) {
            TextButton(onClick=onClose,modifier=Modifier.testTag("before_leaving_close")){Text("Zpět")}
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("Než odejdeš z $area",style=MaterialTheme.typography.titleLarge)
                Text("Výběr podle známých vazeb na oblast; údaje z bety a Classicu ověř ve hře.",style=MaterialTheme.typography.bodySmall)
                listOf("Rozpracované" to choices.inProgress,"Nesplněné třídní úkoly do úrovně ${minOf(60,model.profile.level+2)}" to choices.classQuests,"Tvoje přání a záložky" to choices.wishes).forEach { (title,rows)->
                    Text(title,style=MaterialTheme.typography.titleMedium)
                    if(rows.isEmpty())Text("Žádný další záznam ve známých podkladech.",style=MaterialTheme.typography.bodySmall)
                    rows.forEach { a->
                        OutlinedCard(onClick={onEntry(a.entry.key)},modifier=Modifier.fillMaxWidth().testTag("leaving_${a.entry.key}")) {
                            Column(Modifier.padding(12.dp)) {
                                Text(a.entry.nameOriginal,style=MaterialTheme.typography.titleSmall)
                                Text(a.suggestedLevel?.let { "Úroveň $it" } ?: entryLabel(a.entry),style=MaterialTheme.typography.bodySmall)
                                if(a.entry.kind==EntryKind.QUEST)TextButton(onClick={model.requestQuest(a.entry.key,QuestState.COMPLETED)},enabled=!model.isBusy,modifier=Modifier.testTag("leaving_done_${a.entry.key}")){Text("Hotovo")}
                            }
                        }
                    }
                }
                Text("Prázdný seznam nepotvrzuje dokončení oblasti.",style=MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(16.dp))
            }
            PersonalUndoBar(model)
        }
    }
}
