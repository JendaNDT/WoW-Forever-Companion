package cz.jenda.wowforever

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.data.*
import cz.jenda.wowforever.data.db.*
import cz.jenda.wowforever.packages.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.*
import java.util.zip.*

const val E13_BUNDLE="content/bundled-data-6.wfp"

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=GuideApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class E13BundledContentTest {
    private val c=ApplicationProvider.getApplicationContext<Context>()
    private fun store(bundled:Boolean=true)=GuideStore(c,bundledAsset=if(bundled)E13_BUNDLE else null)
    @Before fun clean(){File(c.filesDir,"content-generations").deleteRecursively();c.deleteDatabase("forever-content.db");c.deleteDatabase("forever-personal.db")}
    @Test fun freshInstallHasAllPublishedContentWithoutAFilePickerOrNetwork()=runBlocking {
        val s=store()
        try{
            s.initialize();assertEquals(6,s.packages.status().generation);assertFalse(s.packages.status().downloaded)
            assertEquals(216,s.entries().size);assertEquals(84,s.locations().size);assertEquals(5,s.atlas().maps.size)
            assertArrayEquals(distributionTile(c,"maps/1413/2/1/2.png"),s.packages.tileOpener()("maps/1413/2/1/2.png").use{it.readBytes()})
            assertTrue(s.entries().any{it.key=="wow-forever:quest:96821"})
        }finally{s.close()}
    }
    private suspend fun upgrade(generation:Int) {
        var s=store(false)
        try{
            s.initialize()
            if(generation>2){s.packages.activate(s.packages.stage(e10Distribution(c,legacy3=generation==3).inputStream()));s.selectActiveContent()}
            assertEquals(generation,s.packages.status().generation)
            s.personal.personal().updateProfile(ProfileEntity(displayName="Jenda",level=8,currentUiMapId=1411))
            s.personal.personal().createProfile(ProfileEntity(id="other",displayName="Druhá postava"))
            s.journal.setQuest(DEFAULT_PROFILE_ID,"wow-forever:quest:788","Cutting Teeth",QuestState.COMPLETED)
            s.journal.setItem(DEFAULT_PROFILE_ID,"wow-forever:item:5778","Primitive Walking Stick",true)
            s.journal.addStop(DEFAULT_PROFILE_ID,"wow-forever:npc:5765","Ruzan")
            s.personal.personal().saveNote(NoteEntity("note:e13",DEFAULT_PROFILE_ID,"Vrátit se",null,1411,0,43.1,68.7,1))
            val before=s.journal.profileBackup(DEFAULT_PROFILE_ID);val other=s.journal.profileBackup("other")
            val export=s.journal.export(DEFAULT_PROFILE_ID)
            s.close();s=store();s.initialize()
            assertEquals(6,s.packages.status().generation);assertEquals(generation,s.packages.status().previousGeneration)
            assertEquals(before,s.journal.profileBackup(DEFAULT_PROFILE_ID));assertEquals(other,s.journal.profileBackup("other"))
            assertEquals(BackupCodec.decode(export).profiles,BackupCodec.decode(s.journal.export(DEFAULT_PROFILE_ID)).profiles)
            s.close();s=store();s.initialize();assertEquals(before,s.journal.profileBackup(DEFAULT_PROFILE_ID));assertEquals(6,s.packages.status().generation)
        }finally{s.close()}
    }
    @Test fun e12WithOnlyFallbackTwoUpgradesAndRetainsBothProfilesAndBackup()=runBlocking{upgrade(2)}
    @Test fun e12WithImportedFourUpgradesAndRetainsBothProfilesAndBackup()=runBlocking{upgrade(4)}
    @Test fun e12WithLegacyThreeUpgradesAndRetainsBothProfilesAndBackup()=runBlocking{upgrade(3)}
    @Test fun importedCurrentSixIsKeptInPlaceAndOnlyAcknowledged()=runBlocking {
        val old=store(false)
        try{old.initialize();old.packages.activate(old.packages.stage(e10Batch3Distribution(c).inputStream()));old.selectActiveContent()}finally{old.close()}
        val before=PackageManager(c).apply{initialize()}.state
        var activations=0
        val manager=PackageManager(c,bundledAsset=E13_BUNDLE,fault={if(it=="before-pointer")activations++})
        manager.initialize();assertEquals(before,manager.state);assertEquals(0,activations);assertFalse(manager.status().downloaded)
        assertTrue(File(manager.root,"bundled-receipt").isFile)
    }
    @Test fun explicitRollbackSurvivesRelaunchAndResetRestoresIntegratedSix()=runBlocking {
        var s=store(false)
        try{
            s.initialize();s.packages.activate(s.packages.stage(e10Distribution(c).inputStream()));s.selectActiveContent()
            s.close();s=store();s.initialize();assertEquals(6,s.packages.status().generation)
            s.journal.setQuest(DEFAULT_PROFILE_ID,"wow-forever:quest:96821","Legging It",QuestState.COMPLETED)
            val before=s.journal.profileBackup(DEFAULT_PROFILE_ID)
            s.packages.rollback();s.selectActiveContent();assertEquals(4,s.packages.status().generation)
            s.close();s=store();s.initialize();assertEquals(4,s.packages.status().generation)
            assertEquals(before,s.journal.profileBackup(DEFAULT_PROFILE_ID))
            s.packages.reset();s.selectActiveContent();assertEquals(6,s.packages.status().generation);assertFalse(s.packages.status().downloaded)
            assertNull(s.packages.status().previousGeneration);assertEquals(before,s.journal.profileBackup(DEFAULT_PROFILE_ID))
        }finally{s.close()}
    }
    private fun testArchive(generation:Int):ByteArray {
        val raw=E09Fixture.zip(c,e10Batch2Package().copy(generation=generation),
            fileChange={files->File("../content/e10-maps").walkTopDown().filter{it.isFile}.forEach{f->files[f.relativeTo(File("../content/e10-maps")).invariantSeparatorsPath]=f.readBytes()}},
            manifestChange={it.copy(minimumAppCode=10)})
        val files=linkedMapOf<String,ByteArray>()
        ZipInputStream(raw.inputStream()).use{z->while(true){val e=z.nextEntry?:break;files[e.name]=z.readBytes();z.closeEntry()}}
        val out=ByteArrayOutputStream()
        ZipOutputStream(out).use{z->(listOf("manifest.json","manifest.sig")+files.keys.filter{it!="manifest.json"&&it!="manifest.sig"}).forEach{n->z.putNextEntry(ZipEntry(n));z.write(files.getValue(n));z.closeEntry()}}
        return out.toByteArray()
    }
    @Test fun newerSignedContentIsNeverDowngradedButExplicitResetMaySelectBundledFive()=runBlocking {
        val pub=E09Fixture.pair.public.encoded
        val m=PackageManager(c,pub);m.initialize();m.activate(m.stage(testArchive(6).inputStream()))
        val original=m.state
        val bundle=testArchive(5)
        val upgraded=PackageManager(c,pub,bundledAsset="test-only",bundledOpener={bundle.inputStream()})
        upgraded.initialize();assertEquals(original,upgraded.state);assertEquals(6,upgraded.status().generation)
        upgraded.reset();assertEquals(5,upgraded.status().generation);assertFalse(upgraded.status().downloaded)
    }
    @Test fun failedActivationCanRetryFromApkWithoutChangingThePreviousSelection()=runBlocking {
        val old=PackageManager(c);old.initialize();old.activate(old.stage(e10Distribution(c).inputStream()));val before=old.state
        val failing=PackageManager(c,bundledAsset=E13_BUNDLE,fault={if(it=="before-pointer")throw IOException("simulated interruption")})
        failing.initialize();assertEquals(before,failing.state);assertEquals(4,failing.status().generation);assertNotNull(failing.recoveryMessage)
        val retry=PackageManager(c,bundledAsset=E13_BUNDLE);retry.initialize();assertEquals(6,retry.status().generation)
    }
    @Test fun damagedInstalledCatalogIsRecoveredFromApkDespiteAnExistingReceipt()=runBlocking {
        var s=store()
        try{
            s.initialize();s.journal.setQuest(DEFAULT_PROFILE_ID,"wow-forever:quest:788","Cutting Teeth",QuestState.COMPLETED)
            val before=s.journal.profileBackup(DEFAULT_PROFILE_ID)
            val db=s.packages.databaseFile()!!
            s.close();db.writeBytes(byteArrayOf(0))
            s=store();s.initialize();assertEquals(6,s.packages.status().generation);assertEquals(216,s.entries().size)
            assertEquals(before,s.journal.profileBackup(DEFAULT_PROFILE_ID))
        }finally{s.close()}
    }
    @Test fun insufficientSpaceOrDamagedBundleKeepFallbackUsable()=runBlocking {
        val noSpace=PackageManager(c,freeBytes={0},bundledAsset=E13_BUNDLE)
        noSpace.initialize();assertEquals(2,noSpace.status().generation);assertNotNull(noSpace.recoveryMessage)
        val bad=PackageManager(c,bundledAsset="test-only",bundledOpener={byteArrayOf(1,2,3).inputStream()})
        bad.initialize();assertEquals(2,bad.status().generation);assertNotNull(bad.recoveryMessage)
    }
}
