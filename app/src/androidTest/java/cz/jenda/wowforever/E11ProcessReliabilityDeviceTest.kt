package cz.jenda.wowforever

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cz.jenda.wowforever.content.EntryKind
import cz.jenda.wowforever.data.*
import cz.jenda.wowforever.data.db.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Run each method in a separate instrumentation invocation; never clear data between phases. */
@RunWith(AndroidJUnit4::class)
class E11ProcessReliabilityDeviceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun evidence(name: String) = File(context.filesDir, "e11/$name").apply { parentFile.mkdirs() }
    private suspend fun open() = GuideStore(context, bundledAsset = BUNDLED_CONTENT_ASSET).also {
        it.initialize()
        assertEquals(8, it.packages.status().generation)
        assertEquals(11912, it.entries().size)
        assertEquals(61, it.atlas().maps.size)
    }
    private fun comparable(p: ProfileBackup): ProfileBackup {
        val id = "profile:comparison"
        return p.copy(
            profile = p.profile.copy(id = id, displayName = p.profile.displayName.removeSuffix(" · obnova")),
            quests = p.quests.map { it.copy(profileId = id) }.sortedBy { it.questKey },
            items = p.items.map { it.copy(profileId = id) }.sortedBy { it.itemKey },
            bookmarks = p.bookmarks.map { it.copy(profileId = id) }.sortedBy { it.entryKey },
            notes = p.notes.sortedBy { it.text }.mapIndexed { i, n -> n.copy(profileId = id, id = "note:comparison:$i") },
            stops = p.stops.map { it.copy(profileId = id) }.sortedBy { it.position },
            travel = p.travel.map { it.copy(profileId = id) }.sortedBy { it.travelKey },
            rewards = p.rewards.map { it.copy(profileId = id) }.sortedBy { it.questKey },
        )
    }
    @Test fun prepareState() = runBlocking {
        val store = open()
        try {
            val dao = store.personal.personal()
            assertEquals(1, dao.profiles().size)
            val entries = store.entries()
            val quest = entries.first { it.kind == EntryKind.QUEST }
            val item = entries.first { it.kind == EntryKind.ITEM }
            dao.updateProfile(ProfileEntity(displayName = "Jenda E11", level = 45, currentUiMapId = 1452,
                professionsJson = "[\"Alchymie\",\"Bylinkářství\"]"))
            dao.createProfile(ProfileEntity(id = "profile:e11:second", displayName = "Druhá cesta", race = "UNDEAD", level = 20))
            store.journal.setQuest(DEFAULT_PROFILE_ID, quest.key, quest.nameOriginal, QuestState.COMPLETED)
            store.journal.setQuest(DEFAULT_PROFILE_ID, "wow-forever:quest:1581", "Historický úkol mimo opravený výběr", QuestState.DEFERRED)
            store.toggleBookmark(quest)
            dao.saveNote(NoteEntity("note:e11:pinned", DEFAULT_PROFILE_ID, "Žluťoučký kůň – poznámka ve Winterspringu", quest.key,
                1452, 0, 43.25, 68.75, 1, quest.nameOriginal))
            store.journal.addStop(DEFAULT_PROFILE_ID, quest.key, quest.nameOriginal)
            store.journal.addStop(DEFAULT_PROFILE_ID, item.key, item.nameOriginal)
            store.journal.moveStop(DEFAULT_PROFILE_ID, 1, -1)
            store.journal.setTravel(DEFAULT_PROFILE_ID, "wow-forever:travel:e11", "Zapamatovaná doprava", true)
            store.journal.chooseReward(DEFAULT_PROFILE_ID, quest.key, quest.nameOriginal, item.key, item.nameOriginal)
            store.journal.setItem(DEFAULT_PROFILE_ID, item.key, item.nameOriginal, true)
            store.journal.setQuest("profile:e11:second", quest.key, quest.nameOriginal, QuestState.IN_PROGRESS)
            dao.saveNote(NoteEntity("note:e11:second", "profile:e11:second", "Jiný profil se nesmí přepsat", null, null, null, null, null, 2))
            val session = Session(mapId = 1452, activeProfileId = DEFAULT_PROFILE_ID,
                viewports = mapOf(1452 to SavedViewport(0.5, 0.5, 1.0, 0f)))
            store.session.save(session)
            withTimeout(15000) { while (store.session.read() != session) delay(10) }
            val backup = store.journal.export(DEFAULT_PROFILE_ID)
            assertEquals(2, BackupCodec.decode(backup).profiles.size)
            assertNotNull(dao.undo(DEFAULT_PROFILE_ID))
            evidence("before-restart.json").writeBytes(backup)
        } finally { store.close() }
    }
    @Test fun verifyAfterForceStopAndRestoreBackup() = runBlocking {
        val source = evidence("before-restart.json").inputStream().use(BackupCodec::read)
        val store = open()
        try {
            val dao = store.personal.personal()
            val observed = BackupCodec.decode(store.journal.export(DEFAULT_PROFILE_ID))
            assertEquals(source.profiles, observed.profiles)
            assertEquals(1452, store.session.read().mapId)
            assertEquals(DEFAULT_PROFILE_ID, store.session.read().activeProfileId)
            assertNotNull(dao.undo(DEFAULT_PROFILE_ID))
            val original = source.profiles.single { it.profile.id == DEFAULT_PROFILE_ID }
            assertEquals(1, original.items.size)
            store.journal.undo(DEFAULT_PROFILE_ID)
            assertTrue(store.journal.profileBackup(DEFAULT_PROFILE_ID).items.isEmpty())
            val localBeforeImport = BackupCodec.decode(store.journal.export(DEFAULT_PROFILE_ID)).profiles
            val importedActive = store.journal.importNew(source)
            assertTrue(importedActive != DEFAULT_PROFILE_ID)
            assertEquals(comparable(original), comparable(store.journal.profileBackup(importedActive)))
            localBeforeImport.forEach { assertEquals(it, store.journal.profileBackup(it.profile.id)) }
            assertEquals(4, dao.profiles().size)
            val allNotes = dao.profiles().flatMap { dao.notes(it.id) }
            assertEquals(allNotes.size, allNotes.map { it.id }.distinct().size)

            val beforeFailure = store.journal.profileBackup(DEFAULT_PROFILE_ID)
            val snapshotCount = dao.snapshots(DEFAULT_PROFILE_ID).size
            val rejected = original.copy(notes = original.notes.map { it.copy(text = "E11_REJECT_RESTORE") })
            store.personal.openHelper.writableDatabase.execSQL("CREATE TRIGGER e11_reject_note BEFORE INSERT ON notes WHEN NEW.text='E11_REJECT_RESTORE' BEGIN SELECT RAISE(ABORT,'E11 simulated disk write failure'); END")
            var failed = false
            try { store.journal.replace(DEFAULT_PROFILE_ID, rejected) } catch (_: android.database.sqlite.SQLiteException) { failed = true }
            finally { store.personal.openHelper.writableDatabase.execSQL("DROP TRIGGER e11_reject_note") }
            assertTrue("Injected restore failure must be observed", failed)
            assertEquals(beforeFailure, store.journal.profileBackup(DEFAULT_PROFILE_ID))
            assertEquals(snapshotCount, dao.snapshots(DEFAULT_PROFILE_ID).size)
            var malformedRejected = false
            try { BackupCodec.decode("{broken backup".toByteArray()) } catch (_: IllegalArgumentException) { malformedRejected = true }
            assertTrue(malformedRejected)
            assertEquals(beforeFailure, store.journal.profileBackup(DEFAULT_PROFILE_ID))

            store.journal.replace(DEFAULT_PROFILE_ID, original)
            assertEquals(comparable(original), comparable(store.journal.profileBackup(DEFAULT_PROFILE_ID)))
            val recovery = dao.snapshots(DEFAULT_PROFILE_ID).first()
            store.journal.recover(DEFAULT_PROFILE_ID, recovery.id)
            assertEquals(beforeFailure, store.journal.profileBackup(DEFAULT_PROFILE_ID))
            store.journal.replace(DEFAULT_PROFILE_ID, original)
            assertEquals(comparable(original), comparable(store.journal.profileBackup(DEFAULT_PROFILE_ID)))
            evidence("after-restore.json").writeBytes(store.journal.export(DEFAULT_PROFILE_ID))
        } finally { store.close() }
    }
    @Test fun verifyRestoredStateAfterSecondForceStop() = runBlocking {
        val expected = evidence("after-restore.json").inputStream().use(BackupCodec::read)
        val store = open()
        try {
            assertEquals(expected.profiles, BackupCodec.decode(store.journal.export(DEFAULT_PROFILE_ID)).profiles)
            assertEquals(4, store.personal.personal().profiles().size)
            assertTrue(store.personal.personal().snapshots(DEFAULT_PROFILE_ID).isNotEmpty())
            assertTrue(store.journal.profileBackup(DEFAULT_PROFILE_ID).quests.any { it.questKey == "wow-forever:quest:1581" })
            assertEquals(1452, store.session.read().mapId)
            evidence("process-reliability-passed.txt").writeText("Two force-stops; two profiles; all seven journal row types; import, replacement, local snapshot and failed transaction verified.\n")
        } finally { store.close() }
    }
}
