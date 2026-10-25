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
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class E06StorageTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    @Test fun guideRulesSurviveInstallationAndReopenAndKeepRecursiveGroups()=runBlocking {
        val name="e06-rules-${System.nanoTime()}.db"
        val pack=e06Fixture();var db=Room.databaseBuilder(context,ContentDatabase::class.java,name).build()
        try {
            ContentValidator.validate(pack)
            File("build/reports/e06-guide-fixture.json").also { it.parentFile!!.mkdirs();it.writeText(ContentJson.encodeToString(pack)) }
            CatalogInstaller(db).install(ContentJson.encodeToString(pack).encodeToByteArray());db.close()
            db=Room.databaseBuilder(context,ContentDatabase::class.java,name).build()
            val rules=db.catalog().guideRules().map { ContentJson.decodeFromString<GuideRule>(it.recordJson) }
            assertEquals(pack.guideRules.toSet(),rules.toSet())
            assertTrue(rules.first { it.entryKey==E6_C }.condition is GuideCondition.AnyOf)
            assertEquals(25,db.relationRecords().size)
        } finally {db.close();context.deleteDatabase(name)}
    }
    @Test fun invalidUpdateAndMidTransactionFailureLeaveOldCatalogWhole()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,ContentDatabase::class.java).build()
        try {
            val seed=samplePackage();CatalogInstaller(db).install(ContentJson.encodeToString(seed).encodeToByteArray())
            val before=db.catalog().metadata();val entries=db.catalog().entries();val relations=db.relationRecords()
            val future=e06Fixture().copy(generation=2)
            val invalid=future.copy(guideRules=future.guideRules.map { it.copy(sourceIds=listOf("missing")) })
            assertThrows(InvalidContent::class.java){runBlocking{CatalogInstaller(db).install(ContentJson.encodeToString(invalid).encodeToByteArray())}}
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_e06_rule BEFORE INSERT ON guide_rules BEGIN SELECT RAISE(ABORT,'simulated rule write failure'); END")
            assertThrows(Exception::class.java){runBlocking{CatalogInstaller(db).install(ContentJson.encodeToString(future).encodeToByteArray())}}
            assertEquals(before,db.catalog().metadata());assertEquals(entries,db.catalog().entries());assertEquals(relations,db.relationRecords());assertTrue(db.catalog().guideRules().isEmpty())
        } finally {db.close()}
    }
    @Test fun actualVersionOneCatalogMigratesWithAllOriginalRowsAndMetadata()=runBlocking {
        val name="e06-catalog-v1-${System.nanoTime()}.db";val path=context.getDatabasePath(name);path.parentFile!!.mkdirs()
        val schema=JSONObject(File("schemas/cz.jenda.wowforever.data.db.ContentDatabase/1.json").readText()).getJSONObject("database")
        val raw=SQLiteDatabase.openOrCreateDatabase(path,null);val p=samplePackage()
        try {
            val tables=schema.getJSONArray("entities")
            for(i in 0 until tables.length()) {
                val table=tables.getJSONObject(i);val name=table.getString("tableName")
                raw.execSQL(table.getString("createSql").replace("\${TABLE_NAME}",name))
                val indices=table.optJSONArray("indices") ?: org.json.JSONArray()
                for(j in 0 until indices.length())raw.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}",name))
            }
            val queries=schema.getJSONArray("setupQueries");for(i in 0 until queries.length())raw.execSQL(queries.getString(i))
            p.sources.forEach { raw.execSQL("INSERT INTO sources VALUES (?,?,?,?)",arrayOf<Any?>(it.id,it.url,it.kind,ContentJson.encodeToString(it))) }
            p.maps.forEach { raw.execSQL("INSERT INTO maps VALUES (?,?,?,?,?,?)",arrayOf<Any?>(it.key,it.uiMapId,it.floorId,it.coordinateVersion,it.sourceId,ContentJson.encodeToString(it))) }
            p.entries.forEach { raw.execSQL("INSERT INTO entries VALUES (?,?,?,?,?,?,?)",arrayOf<Any?>(it.key,it.kind.name,it.gameId,it.nameOriginal,it.minimumLevel,it.searchText,ContentJson.encodeToString(it)))
                it.sourceIds.forEach { s->raw.execSQL("INSERT INTO entry_sources VALUES (?,?)",arrayOf<Any?>(it.key,s)) } }
            p.locations.forEach { raw.execSQL("INSERT INTO locations VALUES (?,?,?,?,?,?,?,?,?)",arrayOf<Any?>(it.key,it.entryKey,it.mapKey,it.sourceId,it.floorId,it.xPercent,it.yPercent,it.precisionPercent,it.coordinateVersion)) }
            p.relations.forEach { r->raw.execSQL("INSERT INTO relations VALUES (?,?,?,?,?,?)",arrayOf<Any?>(r.key,r.ownerKey,r.kind.name,r.mode.name,r.verification.name,if(r.routingEnabled)1 else 0))
                r.targetKeys.forEach { raw.execSQL("INSERT INTO relation_targets VALUES (?,?)",arrayOf<Any?>(r.key,it)) }
                r.sourceIds.forEach { raw.execSQL("INSERT INTO relation_sources VALUES (?,?)",arrayOf<Any?>(r.key,it)) } }
            raw.execSQL("INSERT INTO package_metadata VALUES (1,?,?,?,?,?)",arrayOf<Any?>(p.packageId,1,1,"old-hash",p.checkedOn));raw.version=1
        } finally {raw.close()}
        val db=Room.databaseBuilder(context,ContentDatabase::class.java,name).build()
        try {
            assertEquals(21,db.catalog().entries().size);assertEquals(26,db.catalog().sources().size)
            assertEquals(4,db.catalog().maps().size);assertEquals(8,db.catalog().locations().size);assertEquals(p.relations.toSet(),db.relationRecords().toSet())
            assertEquals("old-hash",db.catalog().metadata()!!.sha256);assertTrue(db.catalog().guideRules().isEmpty());assertEquals(3,db.openHelper.readableDatabase.version)
        } finally {db.close();context.deleteDatabase(name)}
    }
    @Test fun originalE05SessionGetsBalancedFocusWithoutLosingActiveProfile()=runBlocking {
        val json="""{"mapId":1454,"activeProfileId":"profile:import:keep","history":["MAP","NOW"],"spoilers":"HINTS"}"""
        val session=SessionSerializer.readFrom(json.byteInputStream())
        assertEquals("BALANCED",session.guideFocus);assertEquals("profile:import:keep",session.activeProfileId);assertEquals(1454,session.mapId);assertEquals("HINTS",session.spoilers)
    }
}
