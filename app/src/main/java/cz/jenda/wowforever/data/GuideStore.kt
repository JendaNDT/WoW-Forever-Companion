package cz.jenda.wowforever.data

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.data.db.*
import cz.jenda.wowforever.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GuideStore(context: Context,publisherKey:ByteArray?=null,packageFault:(String)->Unit={},bundledAsset:String?=null) {
    private val application = context.applicationContext
    val session = SessionStore(application)
    private val builtinContent = Room.databaseBuilder(application, ContentDatabase::class.java, "forever-content.db").build()
    var content = builtinContent;private set
    val packages=publisherKey?.let{cz.jenda.wowforever.packages.PackageManager(application,it,fault=packageFault,bundledAsset=bundledAsset)} ?: cz.jenda.wowforever.packages.PackageManager(application,fault=packageFault,bundledAsset=bundledAsset)
    val personal = Room.databaseBuilder(application, PersonalDatabase::class.java, "forever-personal.db").build()

    suspend fun initialize() {
        val bytes = application.assets.open("content/starter.json").use { it.readBytes() }
        val seed = decodePackage(bytes)
        val current = builtinContent.catalog().metadata()
        if (current == null || current.generation <= seed.generation) CatalogInstaller(builtinContent).install(bytes)
        packages.initialize()
        selectActiveContent()
        personal.personal().createProfile(ProfileEntity())
    }
    fun selectActiveContent() {
        val next=packages.databaseFile()?.let{Room.databaseBuilder(application,ContentDatabase::class.java,it.absolutePath).build()} ?: builtinContent
        val previous=content;content=next
        if(previous!==builtinContent && previous!==next)previous.close()
    }
    suspend fun entries():List<EntryRecord> = withContext(Dispatchers.IO) {
        content.catalog().entryRecordCursor().use { cursor ->
            buildList { while(cursor.moveToNext())add(ContentJson.decodeFromString<EntryRecord>(cursor.getString(0))) }
        }
    }
    suspend fun atlas():Atlas=atlas(entries())
    suspend fun atlas(entries:List<EntryRecord>): Atlas {
        val maps = content.catalog().maps().map { ContentJson.decodeFromString<MapRecord>(it.recordJson) }
        val sources = content.catalog().sourceLinks().associate { it.id to it.url }
        return buildAtlas(maps, entries, sources, content.catalog().locations().map {
            LocationRecord(it.key,it.entryKey,it.mapKey,it.coordinateVersion,it.floorId,it.xPercent,it.yPercent,it.precisionPercent,it.sourceId)
        })
    }
    suspend fun toggleBookmark(entry: EntryRecord, profileId: String = DEFAULT_PROFILE_ID) = personal.withTransaction {
        val dao = personal.personal()
        if (dao.bookmark(profileId, entry.key) == null) dao.saveBookmark(
            BookmarkEntity(profileId, entry.key, entry.nameOriginal, entry.kind.name, System.currentTimeMillis()))
        else dao.deleteBookmark(profileId, entry.key)
    }
    suspend fun guideRules():List<GuideRule> = content.catalog().guideRules().map { ContentJson.decodeFromString(it.recordJson) }
    suspend fun transportGraph():TransportGraph = content.catalog().metadata()?.let { ContentJson.decodeFromString<TransportGraph>(it.transportJson) } ?: TransportGraph()
    suspend fun locations():List<LocationRecord> = content.catalog().locations().map { LocationRecord(it.key,it.entryKey,it.mapKey,it.coordinateVersion,it.floorId,it.xPercent,it.yPercent,it.precisionPercent,it.sourceId) }
    suspend fun relations(): List<RelationRecord> = content.relationRecords()
    val journal by lazy { PersonalRepository(personal) }
    fun close() { if(content!==builtinContent)content.close();builtinContent.close();personal.close() }
}

/** Shared decoding path for installed relation rows, including exclusive quest groups. */
suspend fun ContentDatabase.relationRecords():List<RelationRecord> {
    val targets=catalog().targets().groupBy { it.relationKey }
    val sources=catalog().relationSources().groupBy { it.relationKey }
    return catalog().relations().map { r -> RelationRecord(r.key,RelationKind.valueOf(r.kind),r.ownerKey,
        targets[r.key].orEmpty().map { it.targetKey },MatchMode.valueOf(r.mode),Verification.valueOf(r.verification),r.routingEnabled,
        sources[r.key].orEmpty().map { it.sourceId }) }
}
