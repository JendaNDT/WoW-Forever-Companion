package cz.jenda.wowforever

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import cz.jenda.wowforever.data.*
import cz.jenda.wowforever.data.db.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=GuideApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class E10FullUpgradeTest {
    @Test fun upgradeFromAcknowledgedSixPreservesProfilesAndRollbackAndNewAreaBackup()=runBlocking {
        val c=ApplicationProvider.getApplicationContext<Context>()
        File(c.filesDir,"content-generations").deleteRecursively();c.deleteDatabase("forever-content.db");c.deleteDatabase("forever-personal.db")
        var store=GuideStore(c,bundledAsset=E13_BUNDLE)
        try {
            store.initialize();assertEquals(6,store.packages.status().generation)
            store.personal.personal().updateProfile(ProfileEntity(displayName="Jenda",level=30))
            store.personal.personal().createProfile(ProfileEntity(id="other",displayName="Druhá cesta",race="UNDEAD"))
            store.journal.setQuest(DEFAULT_PROFILE_ID,"wow-forever:quest:1504","The Binding",QuestState.COMPLETED)
            store.personal.personal().saveNote(NoteEntity("note:full",DEFAULT_PROFILE_ID,"Původní poznámka",null,1411,0,43.0,68.0,1))
            val before=store.journal.profileBackup(DEFAULT_PROFILE_ID);val other=store.journal.profileBackup("other")
            store.close();store=GuideStore(c,bundledAsset=BUNDLED_CONTENT_ASSET);store.initialize()
            assertEquals(8,store.packages.status().generation);assertEquals(6,store.packages.status().previousGeneration)
            assertEquals(11912,store.entries().size);assertEquals(61,store.atlas().maps.size)
            assertEquals(before,store.journal.profileBackup(DEFAULT_PROFILE_ID));assertEquals(other,store.journal.profileBackup("other"))
            store.personal.personal().updateProfile(before.profile.copy(currentUiMapId=1452))
            val exported=store.journal.export(DEFAULT_PROFILE_ID);assertEquals(1452,BackupCodec.decode(exported).profiles.single { it.profile.id==DEFAULT_PROFILE_ID }.profile.currentUiMapId)
            store.packages.rollback();store.selectActiveContent();assertEquals(6,store.packages.status().generation)
            assertEquals(1452,store.journal.profileBackup(DEFAULT_PROFILE_ID).profile.currentUiMapId)
            store.close();store=GuideStore(c,bundledAsset=BUNDLED_CONTENT_ASSET);store.initialize()
            assertEquals(6,store.packages.status().generation) // A deliberate rollback survives startup.
            store.packages.reset();store.selectActiveContent();assertEquals(8,store.packages.status().generation)
            assertEquals(BackupCodec.decode(exported).profiles,BackupCodec.decode(store.journal.export(DEFAULT_PROFILE_ID)).profiles)
        } finally { store.close() }
    }
    @Test fun upgradeFromSevenKeepsEvenJournalKeysNoLongerSelectedByCorrectedCatalog()=runBlocking {
        val c=ApplicationProvider.getApplicationContext<Context>()
        File(c.filesDir,"content-generations").deleteRecursively();c.deleteDatabase("forever-content.db");c.deleteDatabase("forever-personal.db")
        var store=GuideStore(c)
        try {
            store.initialize()
            val files=File("../research/e10full").listFiles()!!.filter { it.name.startsWith("bundled-data-7.wfp.part") }.sortedBy { it.name }
            val archive=cz.jenda.wowforever.packages.sequenceParts(files.map { it.path }) { File(it).inputStream() }
            store.packages.activate(store.packages.stage(archive));store.selectActiveContent()
            assertEquals(7,store.packages.status().generation)
            store.journal.setQuest(DEFAULT_PROFILE_ID,"wow-forever:quest:1581","Elixirs for the Bladeleafs",QuestState.COMPLETED)
            val before=store.journal.profileBackup(DEFAULT_PROFILE_ID)
            store.close();store=GuideStore(c,bundledAsset=BUNDLED_CONTENT_ASSET);store.initialize()
            assertEquals(8,store.packages.status().generation);assertEquals(7,store.packages.status().previousGeneration)
            assertEquals(before,store.journal.profileBackup(DEFAULT_PROFILE_ID))
            store.packages.rollback();store.selectActiveContent();assertEquals(7,store.packages.status().generation)
            assertEquals(before,store.journal.profileBackup(DEFAULT_PROFILE_ID))
        } finally { store.close() }
    }
}
