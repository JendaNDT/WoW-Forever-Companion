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
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class E10StorageTest {
    private val c=ApplicationProvider.getApplicationContext<Context>()
    @Before fun clean(){File(c.filesDir,"content-generations").deleteRecursively();c.deleteDatabase("forever-content.db");c.deleteDatabase("forever-personal.db")}
    @Test fun realPublisherUpdateFromLegacyGenerationThreeReopensAndRetainsBothFrames()=runBlocking {
        var store=GuideStore(c)
        try{
            store.initialize();val m=store.packages
            m.activate(m.stage(e10Distribution(c,legacy3=true).inputStream()));store.selectActiveContent();assertEquals(3,store.content.catalog().metadata()!!.generation)
            val candidate=m.stage(e10Distribution(c).inputStream());assertEquals(3,m.status().generation)
            m.activate(candidate);store.selectActiveContent();assertEquals(4,m.status().generation);assertEquals(3,m.status().previousGeneration)
            assertEquals(139,store.entries().size);assertEquals(5,store.atlas().maps.size)
            val bytes=m.tileOpener()("maps/1413/2/1/2.png").use{it.readBytes()};assertArrayEquals(distributionTile(c,"maps/1413/2/1/2.png"),bytes)
            store.close();store=GuideStore(c);store.initialize();assertEquals(4,store.content.catalog().metadata()!!.generation)
            store.packages.rollback();store.selectActiveContent();assertEquals(3,store.content.catalog().metadata()!!.generation);assertEquals(4,store.atlas().maps.size)
        }finally{store.close()}
    }
    @Test fun notesPlansRewardsDiscoveriesAndBackupSurviveRegionalRemovalAndReactivation()=runBlocking {
        val store=GuideStore(c)
        try{
            store.initialize();store.packages.activate(store.packages.stage(e10Distribution(c).inputStream()));store.selectActiveContent()
            val dao=store.personal.personal();val journal=store.journal
            dao.updateProfile(ProfileEntity(level=15,currentUiMapId=1413));dao.createProfile(ProfileEntity(id="other"))
            journal.setQuest(DEFAULT_PROFILE_ID,"wow-forever:quest:842","Crossroads Conscription",QuestState.COMPLETED)
            journal.addStop(DEFAULT_PROFILE_ID,"wow-forever:npc:3338","Sergra Darkthorn")
            journal.setTravel(DEFAULT_PROFILE_ID,"wow-forever:travel:flight-point:3615","Devrak — letový bod",true)
            journal.setItem(DEFAULT_PROFILE_ID,"wow-forever:item:6477","Grassland Sash",true)
            journal.chooseReward(DEFAULT_PROFILE_ID,"wow-forever:quest:5730","Hidden Enemies","wow-forever:item:15444","Staff of Orgrimmar")
            dao.saveNote(NoteEntity("note:e10",DEFAULT_PROFILE_ID,"Vrátit se ke Crossroads",null,1413,0,51.7,30.3,1))
            val before=journal.profileBackup(DEFAULT_PROFILE_ID);val other=journal.profileBackup("other")
            store.packages.rollback();store.selectActiveContent();assertEquals(2,store.content.catalog().metadata()!!.generation)
            assertEquals(before,journal.profileBackup(DEFAULT_PROFILE_ID));assertEquals(other,journal.profileBackup("other"))
            assertTrue(store.atlas().maps.none{it.uiMapId==1413});assertNull(cz.jenda.wowforever.domain.resolveStops(before.stops,store.entries(),store.locations()).single().location)
            val file=BackupCodec.decode(journal.export(DEFAULT_PROFILE_ID));assertEquals(1,file.formatVersion)
            assertEquals(before,file.profiles.single{it.profile.id==DEFAULT_PROFILE_ID})
            store.packages.rollback();store.selectActiveContent();assertEquals(4,store.content.catalog().metadata()!!.generation)
            assertNotNull(cz.jenda.wowforever.domain.resolveStops(before.stops,store.entries(),store.locations()).single().location)
            assertEquals(before,journal.profileBackup(DEFAULT_PROFILE_ID))
        }finally{store.close()}
    }
    @Test fun missingNewTileCannotBorrowFallbackAssetsOrSwitchTheActiveCatalog()=runBlocking {
        val m=PackageManager(c);m.initialize()
        val bad=e10Distribution(c,change={it.remove("maps/1413/2/1/2.png")})
        assertThrows(IllegalArgumentException::class.java){runBlocking{m.stage(bad.inputStream())}}
        assertEquals(2,m.status().generation);assertEquals(BUILTIN,m.state.active)
    }
    @Test fun correctlySignedButIncompatibleNewCoordinateFrameIsRejected()=runBlocking {
        val m=PackageManager(c,E09Fixture.pair.public.encoded,{Long.MAX_VALUE});m.initialize()
        val p=e10Package().let{pack->pack.copy(maps=pack.maps.map{if(it.uiMapId==1413)it.copy(transform=it.transform.copy(offsetX=0.001)) else it})}
        val bytes=E09Fixture.zip(c,p,fileChange={files->
            File("../content/e10-maps").walkTopDown().filter{it.isFile}.forEach{f->files[f.relativeTo(File("../content/e10-maps")).invariantSeparatorsPath]=f.readBytes()}
        },manifestChange={it.copy(minimumAppCode=10)})
        assertThrows(IllegalArgumentException::class.java){runBlocking{m.stage(bytes.inputStream())}}
        assertEquals(BUILTIN,m.state.active)
    }
}
