package cz.jenda.wowforever.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import cz.jenda.wowforever.data.Destination
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.jenda.wowforever.BuildConfig
import cz.jenda.wowforever.GuideViewModel
import cz.jenda.wowforever.model.MapPoint
import cz.jenda.wowforever.model.PointKind
import kotlinx.coroutines.launch
import java.util.Locale

@Composable fun GuideScreen(model: GuideViewModel) {
    if (!model.ready) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.Center) {
                Text("Forever průvodce", style = MaterialTheme.typography.headlineSmall)
                if (model.dataError == null) { CircularProgressIndicator(); Text("Připravuji offline obsah…") }
                else { Text(model.dataError!!); Button(onClick = model::load) { Text("Zkusit znovu") } }
            }
        }
        return
    }
    if(model.welcomeRequired)WelcomeScreen(model) else GuideReadyScreen(model)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable private fun GuideReadyScreen(model:GuideViewModel) {
    KeepMapAwake(model.keepMapScreenOn && model.destination==Destination.MAP)
    val window=LocalWindowInfo.current.containerSize
    val wide=with(LocalDensity.current) { window.width.toDp()>=600.dp || (window.width.toDp()>=480.dp && window.height.toDp()<480.dp) }
    val compactHeader=LocalDensity.current.fontScale>=1.5f
    var showQuickArea by rememberSaveable { mutableStateOf(false) }
    var overlay by rememberSaveable { mutableStateOf<String?>(null) }
    var overlayReturn by rememberSaveable { mutableStateOf<String?>(null) }
    var detailKey by rememberSaveable(model.profile.id) { mutableStateOf<String?>(null) }
    var detailPoint by rememberSaveable(model.profile.id) { mutableStateOf<String?>(null) }
    var detailTrail by rememberSaveable(model.profile.id) { mutableStateOf(emptyList<String>()) }
    var detailReturn by rememberSaveable { mutableStateOf<String?>(null) }
    val holder=rememberSaveableStateHolder()
    val closeDetail:()->Unit={detailKey=null;detailPoint=null;detailTrail=emptyList();detailReturn=null}
    val openEntry:(String)->Unit={key->
        detailKey=key;detailPoint=null;detailTrail=emptyList();detailReturn=overlay;overlay=null
    }
    val openPoint:(String)->Unit={id->
        val p=model.allPoints.firstOrNull { it.id==id }
        if(p?.entryKey!=null)openEntry(p.entryKey) else { detailKey=null;detailPoint=id;detailTrail=emptyList();detailReturn=overlay;overlay=null }
    }
    val openOverlay:(String)->Unit={next->overlayReturn=overlay;overlay=next}
    val closeOverlay:()->Unit={overlay=overlayReturn;overlayReturn=null}
    val openSearch:()->Unit={model.navigate(Destination.SEARCH)}
    BackHandler(enabled=model.destination!=Destination.MAP && overlay==null && detailKey==null && detailPoint==null && model.noteDraft==null) { model.back() }
    Surface(Modifier.fillMaxSize().semantics { testTagsAsResourceId=true }) {
        Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
            if(wide)NavigationRail(Modifier.fillMaxHeight().testTag("navigation_rail")) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Destination.tabs.forEach { target->NavigationRailItem(selected=model.destination==target,onClick={model.navigate(target)},icon={NavigationIcon(target)},label={Text(target.label)},modifier=Modifier.testTag("nav_${target.name}")) }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Row(Modifier.fillMaxWidth().padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
                    if(!compactHeader)Text(raceLabel(model.profile.race),style=MaterialTheme.typography.labelMedium,modifier=Modifier.padding(horizontal=8.dp))
                    IconButton(onClick={model.changeLevel(-1)},enabled=model.canQuickEdit && model.headerLevel>1,modifier=Modifier.size(48.dp).testTag("quick_level_down").semantics { contentDescription="Snížit úroveň" }) { Text("−",fontSize=24.sp) }
                    Text("${model.headerLevel}",style=MaterialTheme.typography.titleMedium,modifier=Modifier.testTag("quick_level").semantics { contentDescription="Úroveň ${model.headerLevel}" })
                    IconButton(onClick={model.changeLevel(1)},enabled=model.canQuickEdit && model.headerLevel<60,modifier=Modifier.size(48.dp).testTag("quick_level_up").semantics { contentDescription="Zvýšit úroveň" }) { Text("+",fontSize=24.sp) }
                    TextButton(onClick={showQuickArea=true},enabled=model.canQuickEdit,modifier=Modifier.weight(1f).testTag("quick_area")) { Text((model.atlas.maps.firstOrNull { it.uiMapId==model.headerArea }?.name ?: "Oblast")+" ▾",maxLines=1,overflow=TextOverflow.Ellipsis) }
                    IconButton(onClick={model.openSettings()},modifier=Modifier.size(48.dp).testTag("settings").semantics { contentDescription="Nastavení" }) { NavigationIcon(Destination.SETTINGS) }
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    holder.SaveableStateProvider(model.destination.name) {
                        when(model.destination) {
                            Destination.MAP->MapScreen(model,wide,openSearch,onPlan={openOverlay("EXPEDITION")},onPoint=openPoint)
                            Destination.QUESTS->QuestsScreen(model,openSearch,openOverlay,openEntry,openPoint)
                            Destination.SEARCH->SearchScreen(model,openEntry,openPoint,onCoverage={openOverlay("COVERAGE")})
                            Destination.SETTINGS->SettingsScreen(model,openOverlay)
                        }
                    }
                }
                if(!wide)NavigationBar(windowInsets=WindowInsets(0,0,0,0),modifier=Modifier.testTag("navigation_bar")) {
                    Destination.tabs.forEach { target->NavigationBarItem(selected=model.destination==target,onClick={model.navigate(target)},icon={NavigationIcon(target)},label={Text(target.label)},modifier=Modifier.testTag("nav_${target.name}")) }
                }
            }
        }
        if(detailKey==null && detailPoint==null && overlay==null)Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom=if(wide)0.dp else 80.dp)) { PersonalUndoBar(model) }
        }
    }
    when {
        model.dataError!=null->AlertDialog(onDismissRequest=model::dismissError,title={Text("Změnu se nepodařilo dokončit")},text={Text(model.dataError!!)},confirmButton={TextButton(onClick=model::dismissError){Text("Rozumím")}})
        model.pendingChange!=null->AlertDialog(onDismissRequest=model::cancelChange,title={Text("Potvrdit změnu?")},text={Text(model.pendingChange!!.message)},confirmButton={TextButton(onClick=model::confirmChange,enabled=!model.isBusy,modifier=Modifier.testTag("confirm_personal_change")){Text("Potvrdit")}},dismissButton={TextButton(onClick=model::cancelChange){Text("Zrušit")}})
        model.noteDraft!=null->NoteEditorDialog(model)
        detailKey!=null || detailPoint!=null->EntrySheet(model,detailKey,detailPoint,
            backLabel=if(detailTrail.isNotEmpty())"Zpět na předchozí" else if(model.destination==Destination.SEARCH)"Zpět do hledání" else "Zpět",
            onBack={if(detailTrail.isNotEmpty()){detailKey=detailTrail.last();detailTrail=detailTrail.dropLast(1)}else{val previous=detailReturn;closeDetail();overlay=previous}},
            onDismiss={val previous=detailReturn;closeDetail();overlay=previous},onExit=closeDetail,
            onRelated={key->detailKey?.let { detailTrail=detailTrail+it };detailKey=key;detailPoint=null},
            onShowPoint={p->closeDetail();model.showPoint(p.id)},onJournal={closeDetail();model.showJournal()})
        showQuickArea->MapSelectorDialog(model.atlas.maps,model.headerArea,model::changeCurrentArea,onClose={showQuickArea=false})
        overlay=="BACKUP"->BackupDialog(model,closeOverlay)
        overlay=="PACKAGES"->PackagesDialog(model,closeOverlay)
        overlay=="COVERAGE"->ContentCoverageDialog(model,closeOverlay)
        overlay=="HELP"->HelpDialog(model,closeOverlay,onBackup={openOverlay("BACKUP")},onPackages={openOverlay("PACKAGES")},onCoverage={openOverlay("COVERAGE")})
        overlay=="ABOUT"->AboutDialog(model,onHelp={openOverlay("HELP")},onClose=closeOverlay)
        overlay=="EXPEDITION"->ExpeditionDialog(model,closeOverlay,openEntry)
        overlay=="LEAVING"->BeforeLeavingSheet(model,closeOverlay,openEntry)
    }
}

@Composable private fun NavigationIcon(destination:Destination) {
    val color=MaterialTheme.colorScheme.onSurface
    androidx.compose.foundation.Canvas(Modifier.size(24.dp)) {
        val stroke=androidx.compose.ui.graphics.drawscope.Stroke(width=2.dp.toPx())
        val w=size.width;val h=size.height
        when(destination) {
            Destination.SEARCH->{drawCircle(color,w*0.28f,androidx.compose.ui.geometry.Offset(w*0.40f,h*0.40f),style=stroke);drawLine(color,androidx.compose.ui.geometry.Offset(w*.60f,h*.60f),androidx.compose.ui.geometry.Offset(w*.90f,h*.90f),2.dp.toPx())}
            Destination.QUESTS->{drawRect(color,androidx.compose.ui.geometry.Offset(w*.18f,h*.12f),androidx.compose.ui.geometry.Size(w*.64f,h*.76f),style=stroke);(1..3).forEach { i->drawLine(color,androidx.compose.ui.geometry.Offset(w*.32f,h*(.2f+i*.16f)),androidx.compose.ui.geometry.Offset(w*.68f,h*(.2f+i*.16f)),2.dp.toPx()) }}
            Destination.MAP->{drawRect(color,androidx.compose.ui.geometry.Offset(w*.1f,h*.2f),androidx.compose.ui.geometry.Size(w*.8f,h*.6f),style=stroke);listOf(.36f,.64f).forEach { x->drawLine(color,androidx.compose.ui.geometry.Offset(w*x,h*.2f),androidx.compose.ui.geometry.Offset(w*x,h*.8f),2.dp.toPx()) }}
            Destination.SETTINGS->{drawCircle(color,w*.28f,style=stroke);drawCircle(color,w*.09f,style=stroke);(0..7).forEach { i->val a=i*Math.PI/4;drawLine(color,androidx.compose.ui.geometry.Offset(w/2+(kotlin.math.cos(a)*w*.3).toFloat(),h/2+(kotlin.math.sin(a)*h*.3).toFloat()),androidx.compose.ui.geometry.Offset(w/2+(kotlin.math.cos(a)*w*.43).toFloat(),h/2+(kotlin.math.sin(a)*h*.43).toFloat()),2.dp.toPx()) }}
        }
    }
}

@Composable private fun AboutDialog(model: GuideViewModel, onHelp:()->Unit, onClose: () -> Unit) {
    AlertDialog(onDismissRequest = onClose, title = { Text("Forever průvodce") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Verze ${BuildConfig.VERSION_NAME} • data ${model.contentMetadata?.generation ?: 2}")
            OutlinedButton(onClick=onHelp,modifier=Modifier.testTag("open_help")){Text("Návod a obnova postupu")}
            Text("Osobní průvodce pro Warlocka za Hordu od první úrovně. Úroveň a oblast měníš přímo v horní liště; rasu a jméno v Nastavení → Postava.")
            Text("${model.atlas.maps.size} mapy, ${model.atlas.points.size} poloh, vlastní poznámky a ${model.catalogEntries.size} položek katalogu fungují bez internetu. Záložky se ukládají do tvého profilu. Stavy úkolů, získané předměty a poznámky najdeš v Úkoly → Deník. Záloha, Pokrytí i další volby jsou v Nastavení.")
            Text("Obsah: generace ${model.contentMetadata?.generation ?: 1}, kontrola ${model.contentMetadata?.checkedOn ?: "2026-10-23"}.")
            Text("Polohy pocházejí z veřejných záznamů bety. Nejsou potvrzené ve vydané hře. Přiblížení nezvyšuje rozlišení podkladu 1002 × 668.")
            Text("Mapy © Blizzard Entertainment. Zdroj: warcraftforever.games. Neoficiální osobní prototyp bez spojení s Blizzardem.")
        }
    }, confirmButton = { TextButton(onClick = onClose) { Text("Rozumím") } })
}

@Composable private fun MapButton(label: String, description: String, tag: String, action: () -> Unit) {
    androidx.compose.material3.FilledTonalIconButton(onClick = action, modifier = Modifier.size(48.dp).testTag(tag)) {
        Text(label, fontSize = 23.sp, modifier = Modifier.semanticsDescription(description))
    }
}

private fun Modifier.semanticsDescription(text: String): Modifier = this.then(Modifier.semantics { contentDescription = text })
