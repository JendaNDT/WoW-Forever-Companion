package cz.jenda.wowforever

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.data.*
import cz.jenda.wowforever.data.db.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class E07UpdateTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    @Test fun generationOneUpgradeReopensWithAllPersonalTablesAndPortableBackupIntact()=runBlocking {
        val name="e07-upgrade-${System.nanoTime()}";var c=Room.databaseBuilder(context,ContentDatabase::class.java,"$name-content").build()
        var d=Room.databaseBuilder(context,PersonalDatabase::class.java,"$name-personal").build()
        try {
            CatalogInstaller(c).install(ContentJson.encodeToString(samplePackage()).encodeToByteArray())
            d.personal().createProfile(ProfileEntity());PersonalRepository(d).replace(DEFAULT_PROFILE_ID,backupProfile())
            val before=PersonalRepository(d).profileBackup(DEFAULT_PROFILE_ID)
            val exported=BackupCodec.decode(PersonalRepository(d).export(DEFAULT_PROFILE_ID))
            CatalogInstaller(c).install(ContentJson.encodeToString(currentPackage()).encodeToByteArray())
            c.close();d.close()
            c=Room.databaseBuilder(context,ContentDatabase::class.java,"$name-content").build()
            d=Room.databaseBuilder(context,PersonalDatabase::class.java,"$name-personal").build()
            assertEquals(2,c.catalog().metadata()!!.generation);assertEquals(53,c.catalog().entries().size)
            assertEquals(27,c.catalog().locations().size);assertEquals(8,c.catalog().guideRules().size)
            assertEquals(before,PersonalRepository(d).profileBackup(DEFAULT_PROFILE_ID))
            assertEquals(exported.profiles,BackupCodec.decode(PersonalRepository(d).export(DEFAULT_PROFILE_ID)).profiles)
            assertNotNull(c.catalog().entry("wow-forever:npc:5765"));assertNotNull(c.catalog().entry("wow-forever:npc:12776"))
        } finally {c.close();d.close();context.deleteDatabase("$name-content");context.deleteDatabase("$name-personal")}
    }
    @Test fun failedNewContentTransactionCanBeRetriedWithoutPartialGeneration()=runBlocking {
        val c=Room.inMemoryDatabaseBuilder(context,ContentDatabase::class.java).build()
        try {
            val installer=CatalogInstaller(c);installer.install(ContentJson.encodeToString(samplePackage()).encodeToByteArray())
            val old=c.catalog().metadata()
            c.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_e07 BEFORE INSERT ON entries WHEN NEW.gameId=12776 BEGIN SELECT RAISE(ABORT,'simulated new NPC failure'); END")
            assertThrows(Exception::class.java){runBlocking{installer.install(ContentJson.encodeToString(currentPackage()).encodeToByteArray())}}
            assertEquals(old,c.catalog().metadata());assertEquals(21,c.catalog().entries().size);assertTrue(c.catalog().guideRules().isEmpty())
            c.openHelper.writableDatabase.execSQL("DROP TRIGGER reject_e07")
            assertEquals(InstallResult.INSTALLED,installer.install(ContentJson.encodeToString(currentPackage()).encodeToByteArray()))
            assertEquals(InstallResult.UNCHANGED,installer.install(ContentJson.encodeToString(currentPackage()).encodeToByteArray()))
            assertEquals(53,c.catalog().entries().size)
        } finally {c.close()}
    }
}
