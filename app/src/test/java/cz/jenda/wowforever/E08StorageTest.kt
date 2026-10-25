package cz.jenda.wowforever

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.data.*
import cz.jenda.wowforever.data.db.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class E08StorageTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    @Test fun orderedDuplicateStopsReopenAndDoNotChangeQuestNotesOrItems()=runBlocking {
        val name="e08-plan-${System.nanoTime()}";var db=Room.databaseBuilder(context,PersonalDatabase::class.java,name).build()
        try {
            val dao=db.personal();dao.createProfile(ProfileEntity());val repo=PersonalRepository(db)
            repo.setQuest(DEFAULT_PROFILE_ID,"wow-forever:quest:788","Cutting Teeth",QuestState.IN_PROGRESS)
            val before=repo.profileBackup(DEFAULT_PROFILE_ID)
            repo.addStop(DEFAULT_PROFILE_ID,"wow-forever:npc:12776","Hraug");repo.addStop(DEFAULT_PROFILE_ID,"wow-forever:npc:6928","Grosk");repo.addStop(DEFAULT_PROFILE_ID,"wow-forever:npc:12776","Hraug")
            repo.moveStop(DEFAULT_PROFILE_ID,2,-1);repo.removeStop(DEFAULT_PROFILE_ID,0)
            assertEquals(listOf("Hraug","Grosk"),dao.plannedStops(DEFAULT_PROFILE_ID).map { it.nameSnapshot })
            assertEquals(listOf(0,1),dao.plannedStops(DEFAULT_PROFILE_ID).map { it.position })
            assertEquals(before,repo.profileBackup(DEFAULT_PROFILE_ID).copy(stops=before.stops))
            db.close();db=Room.databaseBuilder(context,PersonalDatabase::class.java,name).build()
            assertEquals(listOf("Hraug","Grosk"),db.personal().plannedStops(DEFAULT_PROFILE_ID).map { it.nameSnapshot })
        } finally {db.close();context.deleteDatabase(name)}
    }
    @Test fun importedGapsUnknownIdsAndDiscoveriesRoundTripAcrossIndependentProfiles()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,PersonalDatabase::class.java).build();val target=Room.inMemoryDatabaseBuilder(context,PersonalDatabase::class.java).build()
        try {
            val d=db.personal();d.createProfile(ProfileEntity());d.createProfile(ProfileEntity(id="second"))
            d.insertStops(listOf(PlannedStopEntity(DEFAULT_PROFILE_ID,3,"wow-forever:npc:99999","Historický cíl"),PlannedStopEntity(DEFAULT_PROFILE_ID,88,"wow-forever:npc:5765","Ruzan")))
            val repo=PersonalRepository(db);repo.moveStop(DEFAULT_PROFILE_ID,88,-1)
            repo.setTravel(DEFAULT_PROFILE_ID,"wow-forever:travel:future","Historický letový bod",true)
            assertTrue(d.plannedStops("second").isEmpty());assertTrue(d.discoveredTravel("second").isEmpty())
            val file=BackupCodec.decode(repo.export(DEFAULT_PROFILE_ID));assertEquals(1,file.formatVersion)
            val dest=PersonalRepository(target);val id=dest.importNew(file);val restored=dest.profileBackup(id)
            assertEquals(listOf("Ruzan","Historický cíl"),restored.stops.map { it.nameSnapshot });assertEquals(1,restored.travel.size)
            repo.setTravel(DEFAULT_PROFILE_ID,"wow-forever:travel:future","Historický letový bod",false);assertTrue(d.discoveredTravel(DEFAULT_PROFILE_ID).isEmpty())
            assertEquals(1,dest.profileBackup(id).travel.size)
        } finally {db.close();target.close()}
    }
    @Test fun failedReorderRollsBackTheWholeListAndInvalidMovesKeepExistingRows()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,PersonalDatabase::class.java).build()
        try {
            val dao=db.personal();dao.createProfile(ProfileEntity());val repo=PersonalRepository(db)
            repo.addStop(DEFAULT_PROFILE_ID,"wow-forever:npc:5765","Ruzan");repo.addStop(DEFAULT_PROFILE_ID,"wow-forever:npc:6928","Grosk")
            val before=dao.plannedStops(DEFAULT_PROFILE_ID)
            repo.moveStop(DEFAULT_PROFILE_ID,0,-1);repo.moveStop(DEFAULT_PROFILE_ID,999,1);assertEquals(before,dao.plannedStops(DEFAULT_PROFILE_ID))
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_e08_stop BEFORE INSERT ON planned_stops WHEN NEW.position=1 BEGIN SELECT RAISE(ABORT,'simulated plan write failure'); END")
            assertThrows(Exception::class.java){runBlocking{repo.moveStop(DEFAULT_PROFILE_ID,0,1)}}
            assertEquals(before,dao.plannedStops(DEFAULT_PROFILE_ID))
            assertThrows(IllegalArgumentException::class.java){runBlocking{repo.moveStop(DEFAULT_PROFILE_ID,0,9)}}
            Unit
        } finally {db.close()}
    }
    @Test fun actualContentSchemaTwoMigratesWithoutLosingRowsThenPersistsValidatedGraph()=runBlocking {
        val name="e08-v2-${System.nanoTime()}";val path=context.getDatabasePath(name);path.parentFile!!.mkdirs()
        val schema=JSONObject(File("schemas/cz.jenda.wowforever.data.db.ContentDatabase/2.json").readText()).getJSONObject("database")
        val p=currentPackage();val source=p.sources.first();val entry=p.entries.first()
        val raw=SQLiteDatabase.openOrCreateDatabase(path,null)
        try {
            val tables=schema.getJSONArray("entities")
            for(i in 0 until tables.length()) { val t=tables.getJSONObject(i);val table=t.getString("tableName")
                raw.execSQL(t.getString("createSql").replace("\${TABLE_NAME}",table));val indices=t.optJSONArray("indices") ?: org.json.JSONArray()
                for(j in 0 until indices.length())raw.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}",table)) }
            val setup=schema.getJSONArray("setupQueries");for(i in 0 until setup.length())raw.execSQL(setup.getString(i))
            raw.execSQL("INSERT INTO sources VALUES (?,?,?,?)",arrayOf<Any?>(source.id,source.url,source.kind,ContentJson.encodeToString(source)))
            raw.execSQL("INSERT INTO entries VALUES (?,?,?,?,?,?,?)",arrayOf<Any?>(entry.key,entry.kind.name,entry.gameId,entry.nameOriginal,entry.minimumLevel,entry.searchText,ContentJson.encodeToString(entry)))
            raw.execSQL("INSERT INTO package_metadata VALUES (1,?,?,?,?,?)",arrayOf<Any?>(p.packageId,p.generation,p.schemaVersion,"e07-hash",p.checkedOn));raw.version=2
        } finally {raw.close()}
        var db=Room.databaseBuilder(context,ContentDatabase::class.java,name).build()
        try {
            assertEquals(3,db.openHelper.readableDatabase.version);assertEquals("e07-hash",db.catalog().metadata()!!.sha256)
            assertEquals(1,db.catalog().entries().size);assertEquals(1,db.catalog().sources().size)
            assertEquals(TransportGraph(),ContentJson.decodeFromString<TransportGraph>(db.catalog().metadata()!!.transportJson))
            val graph=t8Fixture();CatalogInstaller(db).install(ContentJson.encodeToString(graph).encodeToByteArray());db.close()
            db=Room.databaseBuilder(context,ContentDatabase::class.java,name).build()
            assertEquals(graph.transportGraph,ContentJson.decodeFromString<TransportGraph>(db.catalog().metadata()!!.transportJson))
        } finally {db.close();context.deleteDatabase(name)}
    }
}
