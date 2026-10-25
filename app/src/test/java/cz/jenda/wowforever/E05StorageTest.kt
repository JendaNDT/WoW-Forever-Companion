package cz.jenda.wowforever

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cz.jenda.wowforever.data.*
import cz.jenda.wowforever.data.db.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class E05StorageTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db:PersonalDatabase
    private val dao get()=db.personal()
    private val repo get()=PersonalRepository(db)
    @Before fun open()=runBlocking { db=Room.inMemoryDatabaseBuilder(context,PersonalDatabase::class.java).build();dao.createProfile(ProfileEntity());Unit }
    @After fun close(){db.close()}
    private suspend fun insert(p:ProfileBackup) {
        dao.createProfile(p.profile);dao.updateProfile(p.profile)
        dao.insertProgress(p.quests);dao.insertItems(p.items);dao.insertBookmarks(p.bookmarks);dao.insertNotes(p.notes)
        dao.insertStops(p.stops);dao.insertTravel(p.travel);dao.insertRewards(p.rewards)
    }
    @Test fun progressAndUndoReopenFromDiskAndUndoKeepsUnrelatedNoteEdit()=runBlocking {
        val name="e05-state-${System.nanoTime()}.db"
        var disk=Room.databaseBuilder(context,PersonalDatabase::class.java,name).build()
        try {
            disk.personal().createProfile(ProfileEntity())
            PersonalRepository(disk).setQuest(DEFAULT_PROFILE_ID,"wow-forever:quest:4641","Your Place In The World",QuestState.COMPLETED)
            disk.personal().saveNote(backupProfile().notes.single().copy(text="Původní"))
            disk.close();disk=Room.databaseBuilder(context,PersonalDatabase::class.java,name).build()
            assertEquals(QuestState.COMPLETED.name,disk.personal().allProgress(DEFAULT_PROFILE_ID).single().state)
            disk.personal().saveNote(backupProfile().notes.single().copy(text="Nový text",updatedAt=9))
            PersonalRepository(disk).undo(DEFAULT_PROFILE_ID)
            assertTrue(disk.personal().allProgress(DEFAULT_PROFILE_ID).isEmpty())
            assertEquals("Nový text",disk.personal().notes(DEFAULT_PROFILE_ID).single().text)
            assertNull(disk.personal().undo(DEFAULT_PROFILE_ID))
        } finally {disk.close();context.deleteDatabase(name)}
    }
    @Test fun rewardChoiceAcquisitionWishAndQuestStateAreIndependentAndUndoable()=runBlocking {
        val q="wow-forever:quest:792";val i="wow-forever:item:5778"
        repo.chooseReward(DEFAULT_PROFILE_ID,q,"Vile Familiars",i,"Hůl")
        assertTrue(dao.collectedItems(DEFAULT_PROFILE_ID).isEmpty());assertTrue(dao.allProgress(DEFAULT_PROFILE_ID).isEmpty())
        dao.saveBookmark(BookmarkEntity(DEFAULT_PROFILE_ID,i,"Hůl","ITEM",1))
        repo.setItem(DEFAULT_PROFILE_ID,i,"Hůl",true)
        assertEquals(1,dao.rewards(DEFAULT_PROFILE_ID).size);assertEquals(1,dao.collectedItems(DEFAULT_PROFILE_ID).size)
        repo.undo(DEFAULT_PROFILE_ID)
        assertTrue(dao.collectedItems(DEFAULT_PROFILE_ID).isEmpty());assertNotNull(dao.bookmark(DEFAULT_PROFILE_ID,i))
        repo.chooseReward(DEFAULT_PROFILE_ID,q,"Vile Familiars","wow-forever:item:999","Jiná volba")
        assertEquals(1,dao.rewards(DEFAULT_PROFILE_ID).size)
        repo.undo(DEFAULT_PROFILE_ID)
        assertEquals(i,dao.rewards(DEFAULT_PROFILE_ID).single().itemKey)
    }
    @Test fun exportFileMovesAllTablesAndTwoProfilesToIndependentDatabaseWithoutCollisions()=runBlocking {
        insert(backupProfile())
        val second=backupProfile("profile:second").copy(notes=emptyList())
        insert(second)
        val transfer=File(context.cacheDir,"e05-transfer.json")
        transfer.writeBytes(repo.export(DEFAULT_PROFILE_ID))
        // Close the source before opening an independent destination; only the portable file crosses.
        db.close()
        val target=Room.inMemoryDatabaseBuilder(context,PersonalDatabase::class.java).build()
        try {
            val destination=PersonalRepository(target);val d=target.personal()
            d.createProfile(ProfileEntity())
            d.saveNote(backupProfile().notes.single().copy(text="Na cílovém zařízení"))
            val before=destination.profileBackup(DEFAULT_PROFILE_ID)
            val active=destination.importNew(BackupCodec.read(transfer.inputStream()))
            assertEquals(before,destination.profileBackup(DEFAULT_PROFILE_ID))
            assertEquals(3,d.profiles().size);assertNotEquals(DEFAULT_PROFILE_ID,active)
            val restored=destination.profileBackup(active)
            assertEquals(7,restored.rowCount);assertEquals(12,restored.profile.level)
            assertEquals("Historický úkol",restored.quests.single().nameSnapshot)
            assertEquals(QuestState.DEFERRED.name,restored.quests.single().state)
            assertEquals("Přání mimo katalog",restored.bookmarks.single().nameSnapshot)
            assertEquals(backupProfile().notes.single().copy(id=restored.notes.single().id,profileId=active),restored.notes.single())
            assertNotEquals("note:shared",restored.notes.single().id)
            val again=BackupCodec.decode(destination.export(active))
            assertEquals(3,again.profiles.size);assertEquals(active,again.activeProfileId)
        } finally {target.close();transfer.delete()}
    }
    @Test fun replacementFailureRollsBackProfileEveryTableAndThePreRestoreSnapshot()=runBlocking {
        insert(backupProfile())
        val before=repo.profileBackup(DEFAULT_PROFILE_ID)
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_e05_note BEFORE INSERT ON notes WHEN NEW.text='FAIL_RESTORE' BEGIN SELECT RAISE(ABORT,'simulated restore failure'); END")
        val source=backupProfile().let { it.copy(profile=it.profile.copy(level=30),notes=it.notes.map { n->n.copy(text="FAIL_RESTORE") }) }
        assertThrows(Exception::class.java){runBlocking {repo.replace(DEFAULT_PROFILE_ID,source)}}
        assertEquals(before,repo.profileBackup(DEFAULT_PROFILE_ID));assertTrue(dao.snapshots(DEFAULT_PROFILE_ID).isEmpty())
    }
    @Test fun replacementCreatesReversibleSnapshotKeepsOtherProfileAndTrimsToThree()=runBlocking {
        insert(backupProfile());insert(backupProfile("profile:other").copy(notes=emptyList()))
        val before=repo.profileBackup(DEFAULT_PROFILE_ID);val other=repo.profileBackup("profile:other")
        val replacement=ProfileBackup(ProfileEntity(displayName="Obnovený",level=20))
        repo.replace(DEFAULT_PROFILE_ID,replacement)
        assertEquals(20,dao.profile()!!.level);assertTrue(dao.notes(DEFAULT_PROFILE_ID).isEmpty())
        val snapshot=dao.snapshots(DEFAULT_PROFILE_ID).single()
        repo.recover(DEFAULT_PROFILE_ID,snapshot.id)
        assertEquals(before,repo.profileBackup(DEFAULT_PROFILE_ID));assertEquals(other,repo.profileBackup("profile:other"))
        repeat(4){repo.replace(DEFAULT_PROFILE_ID,replacement.copy(profile=replacement.profile.copy(level=21+it)))}
        val snapshots=dao.snapshots(DEFAULT_PROFILE_ID)
        assertEquals(3,snapshots.size);assertTrue(snapshots.zipWithNext().all{it.first.createdAt>it.second.createdAt})
        assertEquals(23,BackupJson.decodeFromString<ProfileBackup>(snapshots.first().payloadJson).profile.level)
    }
    @Test fun invalidBackupAndProfileLimitLeaveExistingDataIntact()=runBlocking {
        insert(backupProfile());val before=repo.profileBackup(DEFAULT_PROFILE_ID)
        val foreign=backupFile().copy(profiles=listOf(backupProfile().copy(items=listOf(backupProfile().items.single().copy(profileId="other")))))
        assertThrows(IllegalArgumentException::class.java){runBlocking {repo.importNew(foreign)}}
        for(i in 1..99)dao.createProfile(ProfileEntity(id="profile:$i"))
        assertThrows(IllegalArgumentException::class.java){runBlocking {repo.importNew(backupFile())}}
        assertEquals(100,dao.profiles().size);assertEquals(before,repo.profileBackup(DEFAULT_PROFILE_ID))
    }
    @Test fun actualVersionOneFileMigratesWithoutDeletingAnyOldTable()=runBlocking {
        val name="e05-migration-${System.nanoTime()}.db";val path=context.getDatabasePath(name);path.parentFile!!.mkdirs()
        val schema=JSONObject(File("schemas/cz.jenda.wowforever.data.db.PersonalDatabase/1.json").readText()).getJSONObject("database")
        val old=SQLiteDatabase.openOrCreateDatabase(path,null)
        try {
            val tables=schema.getJSONArray("entities")
            for(i in 0 until tables.length()) {
                val table=tables.getJSONObject(i);val tableName=table.getString("tableName")
                old.execSQL(table.getString("createSql").replace("\${TABLE_NAME}",tableName))
                val indices=table.optJSONArray("indices") ?: org.json.JSONArray()
                for(j in 0 until indices.length())old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}",tableName))
            }
            val queries=schema.getJSONArray("setupQueries");for(i in 0 until queries.length())old.execSQL(queries.getString(i))
            old.execSQL("INSERT INTO profiles VALUES ('profile:default','Ze starší verze','HORDE','WARLOCK','ORC',9,1411,NULL,'[]')")
            old.execSQL("INSERT INTO quest_progress VALUES ('profile:default','wow-forever:quest:4641','Starý úkol','DEFERRED',1)")
            old.execSQL("INSERT INTO collected_items VALUES ('profile:default','wow-forever:item:5778','Hůl',2)")
            old.execSQL("INSERT INTO bookmarks VALUES ('profile:default','wow-forever:npc:5765','Ruzan','NPC',3)")
            old.execSQL("INSERT INTO notes VALUES ('note:old','profile:default','Původní poznámka',NULL,1411,0,42.7,69.0,4,NULL)")
            old.execSQL("INSERT INTO planned_stops VALUES ('profile:default',0,'wow-forever:npc:5765','Ruzan')")
            old.execSQL("INSERT INTO discovered_travel VALUES ('profile:default','wow-forever:travel:old','Cesta',5)")
            old.version=1
        } finally {old.close()}
        val upgraded=Room.databaseBuilder(context,PersonalDatabase::class.java,name).build()
        try {
            val p=PersonalRepository(upgraded).profileBackup(DEFAULT_PROFILE_ID)
            assertEquals(9,p.profile.level);assertEquals(6,p.rowCount)
            assertEquals("Původní poznámka",p.notes.single().text);assertEquals("DEFERRED",p.quests.single().state)
            assertTrue(p.rewards.isEmpty());assertNull(upgraded.personal().undo(DEFAULT_PROFILE_ID));assertTrue(upgraded.personal().snapshots(DEFAULT_PROFILE_ID).isEmpty())
            PersonalRepository(upgraded).chooseReward(DEFAULT_PROFILE_ID,"wow-forever:quest:792","Úkol","wow-forever:item:5778","Hůl")
            assertEquals(1,upgraded.personal().rewards(DEFAULT_PROFILE_ID).size)
            assertEquals(2,upgraded.openHelper.readableDatabase.version)
        } finally {upgraded.close();context.deleteDatabase(name)}
    }
}
