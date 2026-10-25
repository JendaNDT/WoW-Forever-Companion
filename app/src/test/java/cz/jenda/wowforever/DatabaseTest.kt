package cz.jenda.wowforever

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.data.*
import cz.jenda.wowforever.data.db.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class DatabaseTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private lateinit var catalog: ContentDatabase
    private lateinit var personal: PersonalDatabase
    private val seed=samplePackage()
    private fun bytes(p:ContentPackage)=ContentJson.encodeToString(p).toByteArray()
    @Before fun open() = runBlocking {
        catalog=Room.inMemoryDatabaseBuilder(context,ContentDatabase::class.java).build()
        personal=Room.inMemoryDatabaseBuilder(context,PersonalDatabase::class.java).build()
        personal.personal().createProfile(ProfileEntity())
        CatalogInstaller(catalog).install(bytes(seed))
        Unit
    }
    @After fun close() { catalog.close();personal.close() }
    @Test fun importIsIdempotentAndSearchUsesAliasesWithoutAccents()=runBlocking {
        assertEquals(InstallResult.UNCHANGED,CatalogInstaller(catalog).install(bytes(seed)))
        assertEquals(21,catalog.catalog().entries().size)
        assertEquals("wow-forever:spell:688",catalog.catalog().search(normalizeSearch("PŘIVOLÁNÍ IMPA")).single().key)
        assertEquals(25,catalog.catalog().relations().size)
    }
    @Test fun renamingContentPreservesProgressAndPersonalHistory()=runBlocking {
        val dao=personal.personal();val quest="wow-forever:quest:1485"
        dao.saveProgress(QuestProgressEntity(DEFAULT_PROFILE_ID,quest,"Vile Familiars",QuestState.COMPLETED.name,1))
        dao.saveBookmark(BookmarkEntity(DEFAULT_PROFILE_ID,quest,"Vile Familiars","QUEST",1))
        dao.saveCollectedItem(CollectedItemEntity(DEFAULT_PROFILE_ID,"wow-forever:item:5778","Primitive Walking Stick",1))
        dao.saveNote(NoteEntity("note:1",DEFAULT_PROFILE_ID,"Vlastní poznámka",quest,1411,0,42.7,69.0,1))
        val changed=seed.copy(generation=2,entries=seed.entries.map { if(it.key==quest) it.copy(nameOriginal="Nový název") else it })
        CatalogInstaller(catalog).install(bytes(changed))
        assertEquals("Nový název",catalog.catalog().entry(quest)!!.nameOriginal)
        assertEquals(QuestState.COMPLETED.name,dao.progress(DEFAULT_PROFILE_ID,quest)!!.state)
        assertNotNull(dao.bookmark(DEFAULT_PROFILE_ID,quest));assertEquals(1,dao.notes(DEFAULT_PROFILE_ID).size)
        assertEquals(1,dao.collectedItems(DEFAULT_PROFILE_ID).size)
    }
    @Test fun removingEntryRetainsItsPersonalSnapshot()=runBlocking {
        val key="wow-forever:travel:durotar-undercity"
        personal.personal().saveBookmark(BookmarkEntity(DEFAULT_PROFILE_ID,key,"Durotar → Undercity","TRAVEL",1))
        CatalogInstaller(catalog).install(bytes(seed.copy(generation=2,entries=seed.entries.filter { it.key!=key })))
        assertNull(catalog.catalog().entry(key))
        assertEquals("Durotar → Undercity",personal.personal().bookmark(DEFAULT_PROFILE_ID,key)!!.nameSnapshot)
    }
    @Test fun invalidUpdateDoesNotChangeEitherDatabase()=runBlocking {
        val old=catalog.catalog().metadata()
        val invalid=seed.copy(generation=2,locations=seed.locations.map { it.copy(xPercent=101.0) })
        assertThrows(InvalidContent::class.java) { runBlocking { CatalogInstaller(catalog).install(bytes(invalid)) } }
        assertEquals(old,catalog.catalog().metadata());assertEquals(21,catalog.catalog().entries().size)
        assertNotNull(personal.personal().profile())
    }
    @Test fun databaseFailureRollsBackTheWholeImport()=runBlocking {
        catalog.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_test BEFORE INSERT ON entries WHEN NEW.nameOriginal='FAIL_IMPORT' BEGIN SELECT RAISE(ABORT,'simulated failure'); END")
        val before=catalog.catalog().metadata()
        val changed=seed.copy(generation=2,entries=seed.entries.mapIndexed { i,e -> if(i==0) e.copy(nameOriginal="FAIL_IMPORT") else e })
        assertThrows(Exception::class.java) { runBlocking { CatalogInstaller(catalog).install(bytes(changed)) } }
        assertEquals(before,catalog.catalog().metadata());assertEquals(21,catalog.catalog().entries().size)
        assertEquals(25,catalog.catalog().relations().size)
    }
    @Test fun conflictingGenerationAndDowngradeAreRefused()=runBlocking {
        val installer=CatalogInstaller(catalog)
        assertThrows(IllegalArgumentException::class.java) { runBlocking { installer.install(bytes(seed.copy(checkedOn="2026-10-22"))) } }
        installer.install(bytes(seed.copy(generation=2)))
        assertThrows(IllegalArgumentException::class.java) { runBlocking { installer.install(bytes(seed)) } }
        assertEquals(2,catalog.catalog().metadata()!!.generation)
    }
    @Test fun profilesAndCompletionStatesStaySeparate()=runBlocking {
        val dao=personal.personal();val quest="wow-forever:quest:1485"
        dao.createProfile(ProfileEntity(id="profile:second"))
        dao.saveProgress(QuestProgressEntity(DEFAULT_PROFILE_ID,quest,"Vile Familiars",QuestState.DEFERRED.name,1))
        assertEquals(QuestState.DEFERRED.name,dao.progress(DEFAULT_PROFILE_ID,quest)!!.state)
        assertNull(dao.progress("profile:second",quest))
        assertTrue(dao.collectedItems(DEFAULT_PROFILE_ID).isEmpty())
    }
    @Test fun personalFileReopensWithoutLosingBookmark()=runBlocking {
        val name="e02-reopen-${System.nanoTime()}.db"
        val file=Room.databaseBuilder(context,PersonalDatabase::class.java,name).build()
        file.personal().createProfile(ProfileEntity())
        file.personal().saveBookmark(BookmarkEntity(DEFAULT_PROFILE_ID,"wow-forever:npc:5765","Ruzan","NPC",1));file.close()
        val reopened=Room.databaseBuilder(context,PersonalDatabase::class.java,name).build()
        try { assertEquals("Ruzan",reopened.personal().observeBookmarks().first().single().nameSnapshot) }
        finally { reopened.close();context.deleteDatabase(name) }
    }
    @Test fun unsupportedSchemaVersionRefusesToDeletePersonalData()=runBlocking {
        val name="e02-version-${System.nanoTime()}.db"
        val file=Room.databaseBuilder(context,PersonalDatabase::class.java,name).build()
        file.personal().createProfile(ProfileEntity())
        file.personal().saveBookmark(BookmarkEntity(DEFAULT_PROFILE_ID,"wow-forever:npc:5765","Ruzan","NPC",1))
        file.openHelper.writableDatabase.version=3
        file.close()
        val reopened=Room.databaseBuilder(context,PersonalDatabase::class.java,name).build()
        try {
            assertThrows(Exception::class.java) { runBlocking { reopened.personal().profile() } }
        } finally { reopened.close() }
        val raw=android.database.sqlite.SQLiteDatabase.openDatabase(context.getDatabasePath(name).path,null,android.database.sqlite.SQLiteDatabase.OPEN_READONLY)
        try { raw.rawQuery("SELECT COUNT(*) FROM bookmarks",null).use { it.moveToFirst();assertEquals(1,it.getInt(0)) } }
        finally { raw.close();context.deleteDatabase(name) }
    }
}
