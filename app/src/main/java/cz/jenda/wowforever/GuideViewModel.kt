package cz.jenda.wowforever

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import cz.jenda.wowforever.content.EntryRecord
import cz.jenda.wowforever.data.*
import cz.jenda.wowforever.data.db.ProfileEntity
import cz.jenda.wowforever.data.db.PackageEntity
import cz.jenda.wowforever.map.MapComposeEngine
import cz.jenda.wowforever.map.MapEngine
import cz.jenda.wowforever.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import cz.jenda.wowforever.data.db.*
import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.domain.*
import android.net.Uri
import cz.jenda.wowforever.data.db.NoteEntity
import cz.jenda.wowforever.data.db.DEFAULT_PROFILE_ID
import java.util.UUID
import cz.jenda.wowforever.packages.PackageCandidate
import cz.jenda.wowforever.packages.PackageStatus

data class PendingPersonalChange(val profileId:String,val quest:String,val state:QuestState?=null,val rewardItem:String?=null,val rewardChange:Boolean=false,val message:String)

data class NoteDraft(val position: MapPosition, val id: String? = null)
private data class QuickProfileEdit(val profileId:String,val level:Int,val area:Int)

class GuideViewModel(application: Application, private val saved: SavedStateHandle) : AndroidViewModel(application) {
    private val store = (application as GuideApplication).store
    private val appAssets = application.assets
    private var loading = false
    private var session = Session()
    lateinit var atlas: Atlas
        private set
    lateinit var engine: MapEngine
        private set
    var ready by mutableStateOf(false)
        private set
    var dataError by mutableStateOf<String?>(null)
        private set
    var catalogEntries by mutableStateOf<List<EntryRecord>>(emptyList())
        private set
    var catalogSearchIndex by mutableStateOf(CatalogSearchIndex(emptyList()))
        private set
    private var pointsByEntry:Map<String,List<MapPoint>> = emptyMap()
    var bookmarks by mutableStateOf<Set<String>>(emptySet())
        private set
    var profile by mutableStateOf(ProfileEntity())
        private set
    var savingProfile by mutableStateOf(false)
        private set
    var profileSaved by mutableStateOf(false)
        private set
    private var quickProfileEdit by mutableStateOf<QuickProfileEdit?>(null)
    private var quickProfileJob:Job?=null
    private var quickProfileWriting=false
    val headerLevel get()=quickProfileEdit?.level ?: profile.level
    val headerArea get()=quickProfileEdit?.area ?: profile.currentUiMapId
    val canQuickEdit get()=ready && (!operationBusy || quickProfileWriting)
    var contentMetadata by mutableStateOf<PackageEntity?>(null)
        private set
    var sourceUrls by mutableStateOf<Map<String,String>>(emptyMap())
        private set
    var mapId by mutableIntStateOf(1411)
        private set
    var selectedId by mutableStateOf<String?>(null)
        private set
    var destination by mutableStateOf(Destination.MAP)
        private set
    var settingsPage by mutableStateOf(saved.get<String>("settingsPage") ?: "MENU")
        private set
    var questTab by mutableStateOf("NEXT")
        private set
    var recentSearches by mutableStateOf<List<String>>(emptyList())
        private set
    var spoilers by mutableStateOf(Spoilers.FULL)
        private set
    var floorId by mutableIntStateOf(0)
        private set
    var visibleKinds by mutableStateOf(PointKind.entries.map { it.name }.toSet())
        private set
    var onlyBookmarks by mutableStateOf(false)
        private set
    var notes by mutableStateOf<List<NoteEntity>>(emptyList())
        private set
    var noteDraft by mutableStateOf<NoteDraft?>(null)
        private set
    var savingNote by mutableStateOf(false)
        private set
    var focusTarget by mutableStateOf<String?>(null)
        private set
    var personalState by mutableStateOf(ProfileBackup(ProfileEntity()))
        private set
    var profileChoices by mutableStateOf<List<ProfileEntity>>(emptyList())
        private set
    var undoAction by mutableStateOf<UndoEntity?>(null)
        private set
    var snapshots by mutableStateOf<List<PersonalSnapshotEntity>>(emptyList())
        private set
    var guideRules by mutableStateOf<List<GuideRule>>(emptyList())
        private set
    var guideFocus by mutableStateOf(GuideFocus.BALANCED)
        private set
    var recommendations by mutableStateOf(AdviceReport())
        private set
    private var recommendationEngine:RecommendationEngine?=null
    private var adviceTimer:Job?=null
    private var adviceJob:Job?=null
    var relations by mutableStateOf<List<RelationRecord>>(emptyList())
        private set
    var transportGraph by mutableStateOf(TransportGraph())
        private set
    var contentLocations by mutableStateOf<List<LocationRecord>>(emptyList())
        private set
    val expeditionStops get()=resolveStops(personalState.stops,catalogEntries,contentLocations)
    val expeditionLegs get()=TransportRouter(transportGraph).legs(expeditionStops,personalState)
    val questRules get()=QuestRules(relations)
    val questStates get()=personalState.quests.associate { it.questKey to QuestState.valueOf(it.state) }
    val acquiredItems get()=personalState.items.map { it.itemKey }.toSet()
    val rewardChoices get()=personalState.rewards.associateBy { it.questKey }
    var personalBusy by mutableStateOf(false)
        private set
    var backupBusy by mutableStateOf(false)
        private set
    var backupPreview by mutableStateOf<BackupFile?>(null)
        private set
    var backupMessage by mutableStateOf<String?>(null)
        private set
    var pendingChange by mutableStateOf<PendingPersonalChange?>(null)
        private set
    private var personalJob:Job?=null
    var packageBusy by mutableStateOf(false);private set
    var packageCanCancel by mutableStateOf(false);private set
    var packageBytes by mutableLongStateOf(0);private set
    var packageMessage by mutableStateOf<String?>(null);private set
    var packageStatus by mutableStateOf<PackageStatus?>(null);private set
    var packagePreview by mutableStateOf<PackageCandidate?>(null);private set
    var packageDownloadOffer by mutableStateOf<cz.jenda.wowforever.packages.DownloadOffer?>(null);private set
    var packageRemovedEntries by mutableIntStateOf(0);private set
    var packageAffectedRecords by mutableIntStateOf(0);private set
    var contentEpoch by mutableIntStateOf(0);private set
    private var packageJob:Job?=null
    private val operationBusy get()=savingProfile || savingNote || personalBusy || backupBusy || packageBusy
    val isBusy get()=operationBusy || quickProfileEdit!=null
    val allPoints get() = if (!::atlas.isInitialized) emptyList() else atlas.points + notes.mapNotNull { note ->
        val map = atlas.maps.firstOrNull { it.uiMapId == note.uiMapId && it.floorId == note.floorId } ?: return@mapNotNull null
        MapPoint(note.id,note.text.lineSequence().first().take(80),MapPosition(map.uiMapId,note.xPercent!!,note.yPercent!!,map.floorId),
            "",null,PointKind.NOTE,0.0,cz.jenda.wowforever.content.Verification.UNVERIFIED)
    }
    val filteredPoints get():List<MapPoint> {
        val numbers=expeditionStops.filter { it.location!=null }.groupBy { it.stored.entryKey }.mapValues { (_,rows)->rows.map { it.number } }
        return filterPoints(allPoints,visibleKinds,onlyBookmarks,bookmarks).map { point -> point.copy(planNumbers=numbers[point.entryKey].orEmpty()) }
    }
    fun pointsOnMap() = filteredPoints.filter { it.position.uiMapId == mapId && it.position.floorId == floorId }
    fun pointsForEntry(key: String) = pointsByEntry[key].orEmpty()
    fun searchCatalog(query:String):List<EntryRecord> {
        android.os.Trace.beginSection("forever.catalog.search")
        return try { catalogSearchIndex.search(query) } finally { android.os.Trace.endSection() }
    }
    val selectedPoint get() = if (ready) pointsOnMap().firstOrNull { it.id == selectedId } else null

    init { load() }
    fun load() {
        if (loading || ready) return
        loading = true; dataError = null
        if(android.os.Build.VERSION.SDK_INT>=29)android.os.Trace.beginAsyncSection("forever.startup",1)
        viewModelScope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) {
                    store.initialize()
                    val entries=store.entries()
                    Triple(store.atlas(entries), entries, store.personal.personal().profile()!!)
                }
                atlas = loaded.first; catalogEntries = loaded.second; profile = loaded.third
                pointsByEntry=atlas.points.filter { it.entryKey!=null }.groupBy { it.entryKey!! }
                catalogSearchIndex=withContext(Dispatchers.Default){CatalogSearchIndex(loaded.second)}
                contentMetadata = withContext(Dispatchers.IO) { store.content.catalog().metadata() }
                sourceUrls = withContext(Dispatchers.IO) { store.content.catalog().sourceLinks().associate { it.id to it.url } }
                session = store.session.read()
                val active=withContext(Dispatchers.IO) { store.personal.personal().profile(session.activeProfileId) ?: store.personal.personal().profile()!! }
                applyPersonal(withContext(Dispatchers.IO) { store.journal.profileBackup(active.id) })
                relations=withContext(Dispatchers.IO) { store.relations() }
                guideRules=withContext(Dispatchers.IO) { store.guideRules() }
                transportGraph=withContext(Dispatchers.IO) { store.transportGraph() }
                contentLocations=withContext(Dispatchers.IO) { store.locations() }
                guideFocus=GuideFocus.entries.firstOrNull { it.name==session.guideFocus } ?: GuideFocus.BALANCED
                recommendationEngine=RecommendationEngine(catalogEntries,relations,guideRules,atlas.points.filter { it.entryKey!=null }.groupBy { it.entryKey!! }.mapValues { it.value.map { p->p.position.uiMapId }.toSet() })
                refreshRecommendations()
                mapId = (saved.get<Int>("mapId") ?: session.mapId).takeIf { id -> atlas.maps.any { it.uiMapId == id } } ?: 1411
                floorId = (saved.get<Int>("floorId") ?: session.floorId).takeIf { floor -> atlas.maps.any { it.uiMapId == mapId && it.floorId == floor } }
                    ?: atlas.maps.first { it.uiMapId == mapId }.floorId
                visibleKinds = session.visibleKinds.filter { id -> PointKind.entries.any { it.name == id } }.toSet().let { kinds ->
                    if(kinds.containsAll(PointKind.entries.filter { it!=PointKind.OBJECT }.map { it.name }))kinds+PointKind.OBJECT.name else kinds
                }
                onlyBookmarks = session.onlyBookmarks

                selectedId = (if (saved.contains("pointId")) saved.get<String>("pointId") else session.selectedId)?.takeIf { id -> pointsOnMap().any { it.id == id } }
                val rawHistory=saved.get<ArrayList<String>>("history") ?: session.history
                val history=migrateDestinationHistory(rawHistory)
                questTab=(saved.get<String>("questTab") ?: if(rawHistory.lastOrNull { it in setOf("NOW","JOURNEY") }=="JOURNEY")"JOURNAL" else session.questTab).takeIf { it in setOf("NEXT","JOURNAL") } ?: "NEXT"
                recentSearches=session.recentSearches.filter { it.isNotBlank() && it.length<=120 }.distinct().take(5)
                session = session.copy(history = history)
                destination = Destination.valueOf(session.history.last())
                spoilers = Spoilers.entries.firstOrNull { it.name == session.spoilers } ?: Spoilers.FULL
                engine = MapComposeEngine(appAssets, atlas, ::restore, ::save,store.packages.tileOpener())
                packageStatus=withContext(Dispatchers.IO){store.packages.status()}
                packageMessage=packageStatus?.recoveryMessage
                ready = true
                syncPoints()
                saved.get<DoubleArray>("noteDraftPosition")?.takeIf { it.size == 4 && it.all(Double::isFinite) }?.let { v ->
                    if (atlas.maps.any { it.uiMapId == v[0].toInt() && it.floorId == v[1].toInt() } && v[2] in 0.0..100.0 && v[3] in 0.0..100.0)
                        noteDraft = NoteDraft(MapPosition(v[0].toInt(),v[2],v[3],v[1].toInt()),saved.get<String>("noteDraftId"))
                }
                viewModelScope.launch { store.session.error.collect { if (it != null) dataError = it } }
                watchPersonal()

            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { dataError = "Uložená data se nepodařilo načíst. Zkus to znovu. Tvé záznamy nebyly smazány." }
            finally {
                loading = false
                if(android.os.Build.VERSION.SDK_INT>=29)android.os.Trace.endAsyncSection("forever.startup",1)
            }
        }
    }
    fun toggleBookmark(entry: EntryRecord) { mutatePersonal { id -> store.toggleBookmark(entry,id) } }
    fun changeLevel(delta:Int) { queueQuickProfile((headerLevel+delta).coerceIn(1,60),headerArea) }
    fun changeCurrentArea(id:Int) {
        if(atlas.maps.any { it.uiMapId==id })queueQuickProfile(headerLevel,id)
    }
    private fun queueQuickProfile(level:Int,area:Int) {
        if(!canQuickEdit || (level==headerLevel && area==headerArea))return
        quickProfileEdit=QuickProfileEdit(profile.id,level,area)
        profileSaved=false
        if(quickProfileJob?.isActive==true)return
        quickProfileJob=viewModelScope.launch {
            try {
                while(quickProfileEdit!=null) {
                    val edit=quickProfileEdit!!
                    delay(300)
                    if(quickProfileEdit!=edit)continue
                    // Other writes cannot start while a quick edit is pending. Read the latest
                    // profile so changing level/area never overwrites its name, race or extras.
                    savingProfile=true;quickProfileWriting=true
                    val changedArea=profile.currentUiMapId!=edit.area
                    val updated=withContext(Dispatchers.IO) {
                        val dao=store.personal.personal()
                        val current=checkNotNull(dao.profile(edit.profileId))
                        current.copy(level=edit.level,currentUiMapId=edit.area).also { check(dao.updateProfile(it)==1) }
                    }
                    profile=updated;personalState=personalState.copy(profile=updated)
                    if(quickProfileEdit==edit)quickProfileEdit=null
                    if(changedArea)chooseMap(updated.currentUiMapId)
                    refreshRecommendations();profileSaved=quickProfileEdit==null
                    savingProfile=false;quickProfileWriting=false
                }
            } catch(e:CancellationException) { throw e }
            catch(_:Exception) {
                quickProfileEdit=null
                dataError="Úroveň nebo oblast se nepodařila uložit. Zkus změnu zopakovat."
            } finally { savingProfile=false;quickProfileWriting=false }
        }
    }
    fun saveProfile(updated: ProfileEntity) {
        require(updated.id == profile.id && updated.faction == "HORDE" && updated.playerClass == "WARLOCK")
        require(updated.race in listOf("ORC", "TROLL", "UNDEAD") && updated.displayName.length <= 60)
        require(atlas.maps.any { it.uiMapId == updated.currentUiMapId })
        if (isBusy) return
        val changedArea = profile.currentUiMapId != updated.currentUiMapId
        savingProfile = true; profileSaved = false
        viewModelScope.launch {
            try {
                check(withContext(Dispatchers.IO) { store.personal.personal().updateProfile(updated) } == 1)
                profile = updated;personalState=personalState.copy(profile=updated);refreshRecommendations()
                if (changedArea) chooseMap(updated.currentUiMapId)
                profileSaved = true
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { dataError = "Profil se nepodařilo uložit. Zkus to znovu." }
            finally { savingProfile = false }
        }
    }
    fun chooseGuideFocus(value:GuideFocus) { guideFocus=value;refreshRecommendations();persist() }
    fun refreshRecommendations() {
        adviceJob?.cancel()
        val engine=recommendationEngine;val personal=personalState;val focus=guideFocus
        fun apply(report:AdviceReport) {
            recommendations=report
            adviceTimer?.cancel()
            report.nextRefreshAtMillis?.let { deadline -> adviceTimer=viewModelScope.launch {
                delay((deadline-System.currentTimeMillis()).coerceAtLeast(1));refreshRecommendations()
            } }
        }
        if(catalogEntries.size<=1000)apply(engine?.evaluate(personal,focus) ?: AdviceReport())
        else adviceJob=viewModelScope.launch {
            val report=withContext(Dispatchers.Default){engine?.evaluate(personal,focus) ?: AdviceReport()}
            apply(report)
        }
    }
    fun profileEdited() { profileSaved = false }
    fun chooseSpoilers(value: Spoilers) { spoilers = value; persist() }
    fun chooseSettingsPage(value:String) {
        if(value !in setOf("MENU","CHARACTER","DISPLAY"))return
        settingsPage=value;saved["settingsPage"]=value
    }
    fun openSettings() { chooseSettingsPage("MENU");navigate(Destination.SETTINGS) }
    fun chooseQuestTab(value:String) {
        if(value !in setOf("NEXT","JOURNAL"))return
        questTab=value;saved["questTab"]=value;persist()
    }
    fun showSuggestions() { chooseQuestTab("NEXT");navigate(Destination.QUESTS) }
    fun showJournal() { chooseQuestTab("JOURNAL");navigate(Destination.QUESTS) }
    fun rememberSearch(query:String) {
        val value=query.trim().take(120)
        if(value.isBlank())return
        recentSearches=(listOf(value)+recentSearches.filter { normalizeSearch(it)!=normalizeSearch(value) }).take(5)
        persist()
    }
    fun navigate(target: Destination) {
        if (target == destination) return
        checkpoint()
        val history = if (target == Destination.MAP) listOf("MAP") else session.history.filter { it != target.name } + target.name
        session = session.copy(history = history)
        destination = target; persist()
    }
    fun back() {
        if (session.history.size > 1) {
            session = session.copy(history = session.history.dropLast(1))
            destination = Destination.valueOf(session.history.last()); persist()
        }
    }
    fun dismissError() { dataError = null }
    fun chooseMap(id: Int, floor: Int? = null) {
        val selected = atlas.maps.firstOrNull { it.uiMapId == id && (floor == null || it.floorId == floor) } ?: return
        checkpoint(); mapId = id; floorId = selected.floorId; select(null)
    }
    fun select(id: String?) {
        selectedId = id?.takeIf { key -> pointsOnMap().any { it.id == key } }
        persist()
    }
    fun showPoint(id: String) {
        val point = allPoints.firstOrNull { it.id == id } ?: return
        visibleKinds = visibleKinds + point.kind.name
        if (point.entryKey !in bookmarks) onlyBookmarks = false
        syncPoints()
        if (point.position.uiMapId != mapId || point.position.floorId != floorId) chooseMap(point.position.uiMapId,point.position.floorId)
        navigate(Destination.MAP); select(id); focusTarget = id
    }
    fun consumedFocus(id: String) { if (focusTarget == id) focusTarget = null }
    fun toggleLayer(kind: PointKind) {
        visibleKinds = if (kind.name in visibleKinds) visibleKinds - kind.name else visibleKinds + kind.name
        syncPoints(); persist()
    }
    fun toggleBookmarkFilter() { onlyBookmarks = !onlyBookmarks; syncPoints(); persist() }
    fun resetLayers() { visibleKinds = PointKind.entries.map { it.name }.toSet(); onlyBookmarks = false; syncPoints(); persist() }
    private fun syncPoints() {
        if (!::engine.isInitialized) return
        engine.updatePoints(filteredPoints)
        if (selectedId != null && pointsOnMap().none { it.id == selectedId }) { selectedId = null; persist() }
    }
    fun requestNote(position: MapPosition, id: String? = null) {
        require(atlas.maps.any { it.uiMapId == position.uiMapId && it.floorId == position.floorId })
        noteDraft = NoteDraft(position,id)
        saved["noteDraftPosition"] = doubleArrayOf(position.uiMapId.toDouble(),position.floorId.toDouble(),position.xPercent,position.yPercent)
        saved["noteDraftId"] = id
    }
    fun cancelNote() { noteDraft = null; saved["noteDraftPosition"] = null; saved["noteDraftId"] = null }
    fun saveNote(text: String) {
        val draft = noteDraft ?: return
        if (isBusy || text.isBlank() || text.trim().length > 2000) return
        val previous = notes.firstOrNull { it.id == draft.id }
        if (draft.id != null && previous == null) { dataError = "Poznámka už neexistuje."; return }
        val row = previous?.copy(text = text.trim(),updatedAt = System.currentTimeMillis()) ?: NoteEntity(
            "note:${UUID.randomUUID()}",profile.id,text.trim(),null,draft.position.uiMapId,draft.position.floorId,
            draft.position.xPercent,draft.position.yPercent,System.currentTimeMillis())
        savingNote = true
        viewModelScope.launch {
            try {
                notes = withContext(Dispatchers.IO) { store.personal.personal().saveNote(row); store.personal.personal().notes(profile.id) }
                cancelNote(); syncPoints(); showPoint(row.id)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { dataError = "Poznámku se nepodařilo uložit. Zkus to znovu." }
            finally { savingNote = false }
        }
    }
    fun deleteNote(id: String) {
        if (isBusy) return
        savingNote = true
        viewModelScope.launch {
            try {
                notes = withContext(Dispatchers.IO) { store.personal.personal().deleteNote(id,profile.id); store.personal.personal().notes(profile.id) }
                cancelNote(); syncPoints()
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { dataError = "Poznámku se nepodařilo smazat. Zkus to znovu." }
            finally { savingNote = false }
        }
    }
    fun checkpoint() { if (ready) atlas.maps.forEach { map -> engine.viewport(map.uiMapId,map.floorId)?.let { save(map.uiMapId,map.floorId,it) } } }
    fun trimMapMemory() { if(ready)engine.trimMemory(mapId,floorId) }
    private fun persist() {
        if (!ready) return
        session = session.copy(mapId = mapId,floorId = floorId,selectedId = selectedId,spoilers = spoilers.name,
            visibleKinds = visibleKinds.toList(),onlyBookmarks = onlyBookmarks,activeProfileId = profile.id,guideFocus = guideFocus.name,questTab=questTab,recentSearches=recentSearches)
        saved["mapId"] = mapId; saved["floorId"] = floorId; saved["pointId"] = selectedId
        saved["history"] = ArrayList(session.history)
        store.session.save(session)
    }
    private fun save(id: Int, floor: Int, v: Viewport) {
        saved["viewport_${id}_$floor"] = doubleArrayOf(v.x,v.y,v.scale,v.rotation.toDouble())
        session = session.copy(floorViewports = session.floorViewports + ("$id:$floor" to SavedViewport(v.x,v.y,v.scale,v.rotation)))
        persist()
    }
    private fun restore(id: Int, floor: Int): Viewport? {
        val savedValue = saved.get<DoubleArray>("viewport_${id}_$floor") ?: if (floor == 0) saved.get<DoubleArray>("viewport_$id") else null
        savedValue?.let { v ->
            if (v.size == 4 && v.all { it.isFinite() } && v[2] > 0.0) return Viewport(v[0],v[1],v[2],v[3].toFloat())
        }
        return (session.floorViewports["$id:$floor"] ?: if (floor == 0) session.viewports[id] else null)?.viewport()
    }
    private fun applyPersonal(value:ProfileBackup) {
        personalState=value;profile=value.profile;notes=value.notes;bookmarks=value.bookmarks.map { it.entryKey }.toSet();syncPoints();refreshRecommendations()
    }
    private fun watchPersonal() {
        personalJob?.cancel()
        val id=profile.id
        personalJob=viewModelScope.launch {
            try {
                launch { store.personal.personal().observeProfiles().collect { values -> profileChoices=values;values.firstOrNull { it.id==id }?.let { if(profile.id==id){profile=it;personalState=personalState.copy(profile=it);refreshRecommendations()} } } }
                launch { store.personal.personal().observeUndo(id).collect { if(profile.id==id)undoAction=it } }
                combine(listOf(store.personal.personal().observeProgress(id),store.personal.personal().observeItems(id),store.personal.personal().observeBookmarks(id),
                    store.personal.personal().observeNotes(id),store.personal.personal().observeRewards(id),store.personal.personal().observeStops(id),store.personal.personal().observeTravel(id))) { Unit }.collect {
                    val value=withContext(Dispatchers.IO) { store.journal.profileBackup(id) }
                    if(profile.id==id)applyPersonal(value)
                }
            } catch(e:CancellationException) { throw e }
            catch(_:Exception) { dataError="Osobní záznamy se nepodařilo znovu načíst. Zavři a otevři aplikaci." }
        }
    }
    private fun mutatePersonal(action:suspend (String)->Unit) {
        if(isBusy)return
        val id=profile.id;personalBusy=true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { action(id) }
                if(profile.id==id)applyPersonal(withContext(Dispatchers.IO) { store.journal.profileBackup(id) })
            } catch(e:CancellationException) { throw e }
            catch(_:Exception) { dataError="Změnu se nepodařilo uložit. Původní záznamy zůstaly zachované." }
            finally { personalBusy=false }
        }
    }
    fun historyEntry(key:String):EntryRecord? {
        catalogEntries.firstOrNull { it.key==key }?.let { return it }
        val kind=EntryKind.entries.firstOrNull { key.startsWith("wow-forever:${it.name.lowercase()}:") } ?: return null
        val name=personalState.quests.firstOrNull { it.questKey==key }?.nameSnapshot ?: personalState.items.firstOrNull { it.itemKey==key }?.nameSnapshot
            ?: personalState.bookmarks.firstOrNull { it.entryKey==key }?.nameSnapshot
            ?: personalState.stops.firstOrNull { it.entryKey==key }?.nameSnapshot
            ?: personalState.travel.firstOrNull { it.travelKey==key }?.nameSnapshot ?: return null
        return EntryRecord(key,kind,nameOriginal=name,descriptionCs="Osobní historie: položka momentálně chybí v herním katalogu.",verification=Verification.UNVERIFIED,sourceIds=emptyList(),payload="{}")
    }
    fun requestQuest(key:String,state:QuestState) {
        val entry=historyEntry(key)?.takeIf { it.kind==EntryKind.QUEST } ?: return
        if(isBusy || questStates[key]==state)return
        if(state in setOf(QuestState.IN_PROGRESS,QuestState.COMPLETED) && questRules.excludedBy(key,questStates).isNotEmpty()) {
            dataError="Tato volba se vylučuje s již dokončeným úkolem. Nejprve uprav jeho stav.";return
        }
        val affected=if(questStates[key]==QuestState.COMPLETED && state!=QuestState.COMPLETED)questRules.descendants(key).filter { questStates[it] in setOf(QuestState.COMPLETED,QuestState.IN_PROGRESS) } else emptyList()
        if(affected.isNotEmpty()) {
            pendingChange=PendingPersonalChange(profile.id,key,state=state,message="Změní se splnění předchůdce. ${affected.size} navazujících záznamů zůstane uložených a jejich podmínky se přepočítají. Pokračovat?")
        } else mutatePersonal { store.journal.setQuest(it,key,entry.nameOriginal,state) }
    }
    fun requestReward(quest:String,item:String?) {
        val q=historyEntry(quest)?.takeIf { it.kind==EntryKind.QUEST } ?: return
        if(isBusy || rewardChoices[quest]?.itemKey==item)return
        if(item!=null && questRules.rewardCandidates(quest).none { it.ownerKey==item })return
        if(rewardChoices[quest]!=null) pendingChange=PendingPersonalChange(profile.id,quest,rewardItem=item,rewardChange=true,
            message="Změnit zaznamenanou volbu odměny u ${q.nameOriginal}? Evidence získaných předmětů se tím nemění.")
        else mutatePersonal { store.journal.chooseReward(it,quest,q.nameOriginal,item,item?.let { k -> historyEntry(k)?.nameOriginal }) }
    }
    fun cancelChange() { pendingChange=null }
    fun confirmChange() {
        val change=pendingChange ?: return
        if(change.profileId!=profile.id || isBusy)return
        pendingChange=null
        if(change.rewardChange) {
            val quest=historyEntry(change.quest) ?: return
            mutatePersonal { store.journal.chooseReward(it,change.quest,quest.nameOriginal,change.rewardItem,change.rewardItem?.let { k -> historyEntry(k)?.nameOriginal }) }
        } else { val entry=historyEntry(change.quest) ?: return;mutatePersonal { store.journal.setQuest(it,entry.key,entry.nameOriginal,change.state!!) } }
    }
    fun setAcquired(key:String,value:Boolean) {
        val item=historyEntry(key)?.takeIf { it.kind==EntryKind.ITEM } ?: return
        if((key in acquiredItems)==value)return
        mutatePersonal { store.journal.setItem(it,key,item.nameOriginal,value) }
    }
    fun undoPersonal() { mutatePersonal { store.journal.undo(it) } }
    fun addPlanStop(key:String) {
        val entry=historyEntry(key) ?: return
        if(personalState.stops.size>=200) { dataError="Výprava už má 200 cílů. Nejdřív některý odeber.";return }
        mutatePersonal { store.journal.addStop(it,key,entry.nameOriginal) }
    }
    fun movePlanStop(position:Int,delta:Int) { mutatePersonal { store.journal.moveStop(it,position,delta) } }
    fun removePlanStop(position:Int) { mutatePersonal { store.journal.removeStop(it,position) } }
    fun showPlanStop(number:Int) {
        val stop=expeditionStops.firstOrNull { it.number==number && it.location!=null } ?: return
        val point=atlas.points.singleOrNull { it.entryKey==stop.stored.entryKey } ?: return
        showPoint(point.id)
    }
    fun setTravelDiscovered(key:String,discovered:Boolean) {
        val entry=historyEntry(key)?.takeIf { it.kind==EntryKind.TRAVEL } ?: return
        mutatePersonal { store.journal.setTravel(it,key,entry.nameOriginal,discovered) }
    }
    fun switchProfile(id:String) {
        if(isBusy || id==profile.id)return
        personalBusy=true
        viewModelScope.launch {
            try { activateProfile(id) }
            catch(e:CancellationException) { throw e }
            catch(_:Exception) { dataError="Profil se nepodařilo otevřít." }
            finally { personalBusy=false }
        }
    }
    private suspend fun activateProfile(id:String) {
        val value=withContext(Dispatchers.IO) { store.journal.profileBackup(id) }
        personalJob?.cancel();cancelNote();pendingChange=null;selectedId=null;focusTarget=null;undoAction=null
        applyPersonal(value)
        chooseMap(profile.currentUiMapId);profileSaved=false;watchPersonal();persist()
        snapshots=withContext(Dispatchers.IO) { store.personal.personal().snapshots(id) }
    }
    fun refreshSnapshots() {
        val id=profile.id
        viewModelScope.launch { try { val list=withContext(Dispatchers.IO) { store.personal.personal().snapshots(id) };if(profile.id==id)snapshots=list }
            catch(e:CancellationException){throw e}catch(_:Exception){dataError="Místní snímky se nepodařilo načíst."} }
    }
    fun clearBackupPreview() { if(!backupBusy)backupPreview=null }
    fun readBackup(uri:Uri) {
        if(isBusy)return
        backupBusy=true;backupPreview=null;backupMessage=null
        viewModelScope.launch {
            try { backupPreview=withContext(Dispatchers.IO) { getApplication<Application>().contentResolver.openInputStream(uri)?.use(BackupCodec::read) ?: error("Soubor nelze otevřít.") } }
            catch(e:CancellationException){throw e}
            catch(e:BackupFormatException){dataError="${e.message} Nic nebylo změněno."}
            catch(_:Exception){dataError="Zálohu nelze načíst. Ověř, že jde o nepoškozený soubor Forever ve formátu 1 do 5 MB. Nic nebylo změněno."}
            finally { backupBusy=false }
        }
    }
    fun exportBackup(uri:Uri) {
        if(isBusy)return
        val id=profile.id;backupBusy=true;backupMessage=null
        viewModelScope.launch {
            try {
                val count=withContext(Dispatchers.IO) {
                    val bytes=store.journal.export(id)
                    val out=getApplication<Application>().contentResolver.openOutputStream(uri,"wt") ?: error("Soubor nelze vytvořit.")
                    out.use { it.write(bytes);it.flush() };bytes.size
                }
                backupMessage="Záloha uložená (${count/1024+1} kB). Obsahuje všechny uložené profily."
            } catch(e:CancellationException){throw e}
            catch(_:Exception){dataError="Zálohu se nepodařilo uložit. Zvol soubor znovu; případný nedokončený soubor nepoužívej k obnově."}
            finally { backupBusy=false }
        }
    }
    fun importBackup(replace:Boolean,sourceId:String) {
        val file=backupPreview ?: return
        if(isBusy)return
        val target=profile.id;backupBusy=true
        viewModelScope.launch {
            var committed=false
            try {
                val imported=withContext(Dispatchers.IO) {
                    if(replace) { store.journal.replace(target,file.profiles.first { it.profile.id==sourceId });target } else store.journal.importNew(file)
                }
                committed=true;backupPreview=null;activateProfile(imported);backupMessage=if(replace) "Profil obnovený. Předchozí stav najdeš v místních snímcích." else "Záloha načtená do nových profilů. Původní profily zůstaly zachované."
            } catch(e:CancellationException){throw e}
            catch(_:Exception){dataError=if(committed) "Data zálohy jsou uložená. Obnovený profil se nepodařilo znovu načíst; zavři a otevři aplikaci." else "Obnova se nepodařila. Všechny změny byly vráceny a původní profily zůstaly zachované."}
            finally { backupBusy=false }
        }
    }
    fun recoverSnapshot(id:String) {
        if(isBusy)return
        val target=profile.id;backupBusy=true
        viewModelScope.launch {
            var committed=false
            try { withContext(Dispatchers.IO) { store.journal.recover(target,id) };committed=true;activateProfile(target);backupMessage="Místní snímek obnovený." }
            catch(e:CancellationException){throw e}
            catch(_:Exception){dataError=if(committed) "Snímek je uložený, ale profil se nepodařilo znovu načíst. Zavři a otevři aplikaci." else "Snímek se nepodařilo obnovit. Původní profil zůstal zachovaný."}
            finally { backupBusy=false }
        }
    }
    fun refreshPackages() {
        if(packageBusy)return
        viewModelScope.launch {runCatching{withContext(Dispatchers.IO){store.packages.status()}}.onSuccess{packageStatus=it}}
    }
    private fun preparePackage(action:suspend()->PackageCandidate) {
        if(isBusy)return
        packageBusy=true;packageCanCancel=true;packageMessage="Připravuji a ověřuji balíček…";packageBytes=0;packagePreview=null
        packageJob=viewModelScope.launch {
            try {
                withContext(Dispatchers.IO){store.packages.discard()}
                val candidate=withContext(Dispatchers.IO){action()}
                val removed=catalogEntries.map{it.key}.toSet()-candidate.catalog.entries.map{it.key}.toSet()
                packageRemovedEntries=removed.size
                packageAffectedRecords=withContext(Dispatchers.IO){store.personal.personal().profiles().sumOf{p->
                    val b=store.journal.profileBackup(p.id)
                    b.quests.count{it.questKey in removed}+b.items.count{it.itemKey in removed}+b.bookmarks.count{it.entryKey in removed}+
                        b.stops.count{it.entryKey in removed}+b.travel.count{it.travelKey in removed}+b.rewards.count{it.questKey in removed || it.itemKey in removed}
                }}
                packagePreview=candidate
                packageMessage=if(candidate.unchanged)"Tento obsah už používáš. Nic se nezměnilo." else "Balíček je ověřený. Před přepnutím zkontroluj náhled."
            }catch(e:CancellationException){packageMessage="Příprava zrušená. Dosavadní obsah zůstal zachovaný.";throw e}
            catch(e:Exception){packageMessage=e.message?.take(250) ?: "Balíček se nepodařilo připravit. Dosavadní obsah zůstal zachovaný."}
            finally{packageBusy=false;packageCanCancel=false}
        }
    }
    fun readContentPackage(uri:Uri)=preparePackage {
        val input=requireNotNull(getApplication<Application>().contentResolver.openInputStream(uri)){"Soubor se nepodařilo otevřít."}
        store.packages.stage(input){bytes->viewModelScope.launch{packageBytes=bytes}}
    }
    fun downloadContentPackage(url:String,wifiOnly:Boolean)=preparePackage {
        val offer=requireNotNull(packageDownloadOffer?.takeIf{it.url==url.trim()}){"Nejprve zjisti velikost souboru."}
        store.packages.download(url,wifiOnly,offer.bytes){bytes->viewModelScope.launch{packageBytes=bytes}}
    }
    fun inspectContentDownload(url:String,wifiOnly:Boolean){
        if(isBusy)return
        packageBusy=true;packageCanCancel=true;packageDownloadOffer=null;packageMessage="Zjišťuji velikost stažení…";packageBytes=0
        packageJob=viewModelScope.launch {
            try{packageDownloadOffer=withContext(Dispatchers.IO){store.packages.inspectDownload(url,wifiOnly)};packageMessage="Velikost je známá. Stažení spustíš tlačítkem níže."}
            catch(e:CancellationException){packageMessage="Kontrola zrušená.";throw e}
            catch(e:Exception){packageMessage=e.message?.take(250) ?: "Server neodpovídá. Stažený obsah zůstal zachovaný."}
            finally{packageBusy=false;packageCanCancel=false}
        }
    }
    fun cancelPackageWork(){if(packageCanCancel)packageJob?.cancel()}
    fun clearPackagePreview(){
        if(packageBusy)return
        packagePreview=null
        // A new preparation or next launch removes abandoned candidates under the manager's lock.
    }
    private fun changePackage(message:String,action:suspend()->Unit) {
        if(isBusy)return
        checkpoint();packageBusy=true;packageCanCancel=false;packageMessage="Přepínám ověřený obsah…"
        val original=store.packages.state
        packageJob=viewModelScope.launch {
            try {
                // Completion belongs to the application: a rotation or close must not interrupt an accepted commit.
                withContext(NonCancellable){
                    withContext(Dispatchers.IO){action();store.selectActiveContent()}
                    val entries=withContext(Dispatchers.IO){store.entries()}
                    val newAtlas=withContext(Dispatchers.IO){store.atlas(entries)}
                    val newRelations=withContext(Dispatchers.IO){store.relations()}
                    val rules=withContext(Dispatchers.IO){store.guideRules()}
                    val graph=withContext(Dispatchers.IO){store.transportGraph()}
                    val locations=withContext(Dispatchers.IO){store.locations()}
                    val metadata=withContext(Dispatchers.IO){store.content.catalog().metadata()}
                    val sources=withContext(Dispatchers.IO){store.content.catalog().sourceLinks().associate{it.id to it.url}}
                    val status=withContext(Dispatchers.IO){store.packages.status()}
                    val newSearch=withContext(Dispatchers.Default){CatalogSearchIndex(entries)}
                    engine.close();atlas=newAtlas;catalogEntries=entries;relations=newRelations;guideRules=rules;transportGraph=graph;contentLocations=locations
                    pointsByEntry=atlas.points.filter { it.entryKey!=null }.groupBy { it.entryKey!! };catalogSearchIndex=newSearch
                    contentMetadata=metadata;sourceUrls=sources;packageStatus=status
                    engine=MapComposeEngine(appAssets,atlas,::restore,::save,store.packages.tileOpener());contentEpoch++
                    if(atlas.maps.none{it.uiMapId==mapId && it.floorId==floorId}){mapId=atlas.maps.first().uiMapId;floorId=atlas.maps.first().floorId}
                    if(pointsOnMap().none{it.id==selectedId})selectedId=null
                    focusTarget=null;packagePreview=null
                    recommendationEngine=RecommendationEngine(catalogEntries,relations,guideRules,atlas.points.filter{it.entryKey!=null}.groupBy{it.entryKey!!}.mapValues{it.value.map{p->p.position.uiMapId}.toSet()})
                    syncPoints();refreshRecommendations();persist()
                    val cleaned=withContext(Dispatchers.IO){try{store.packages.prune();true}catch(_:Exception){false}}
                    packageMessage=message+if(cleaned)"" else " Úklid starých souborů se zkusí při příštím spuštění."
                }
            }catch(e:Exception){
                val restored=withContext(NonCancellable+Dispatchers.IO){runCatching{store.packages.restoreSelection(original);store.selectActiveContent()}.isSuccess}
                packageMessage=if(restored)"Změnu se nepodařilo dokončit. Původní obsah i osobní záznamy zůstaly zachované." else "Změnu se nepodařilo dokončit. Znovu otevři aplikaci pro obnovu ověřeného obsahu."
            }
            finally{packageBusy=false}
        }
    }
    fun activateContentPackage(){val candidate=packagePreview ?: return;changePackage("Používáš generaci ${candidate.manifest.generation}. Osobní záznamy zůstaly zachované."){store.packages.activate(candidate)}}
    fun rollbackContentPackage()=changePackage("Předchozí obsah obnovený. Osobní záznamy zůstaly zachované."){store.packages.rollback()}
    fun resetContentPackages()=changePackage("Používáš přibalený obsah. Stažené balíčky byly smazané; osobní záznamy zůstaly zachované."){store.packages.reset()}
    override fun onCleared() { if (::engine.isInitialized) engine.close() }
}
