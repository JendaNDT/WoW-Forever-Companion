package cz.jenda.wowforever

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.data.*
import cz.jenda.wowforever.data.db.*
import cz.jenda.wowforever.domain.*
import cz.jenda.wowforever.packages.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.zip.*
import java.io.ByteArrayOutputStream

fun e10Batch3Package()=decodePackage(File("../content/e10-v6.json").readBytes())
fun e10Batch3Distribution(c:Context)=c.assets.open(E13_BUNDLE).use{it.readBytes()}

class E10Batch3DataTest {
    @Test fun classicPositionPackageRequiresTheAppThatCanDisplayItsProvenance(){
        val raw=File("../content/e10-v6.manifest.json").readBytes()
        val signature=File("../content/e10-v6.manifest.sig").readBytes()
        val public=File("src/main/assets/content/publisher.der").readBytes()
        assertThrows(IllegalArgumentException::class.java){ManifestValidator.decode(raw,signature,public,13)}
        val manifest=ManifestValidator.decode(raw,signature,public,14)
        assertEquals(14,manifest.minimumAppCode);ManifestValidator.catalog(manifest,e10Batch3Package())
        assertThrows(IllegalArgumentException::class.java){ManifestValidator.catalog(manifest.copy(minimumAppCode=13),e10Batch3Package())}
    }
    @Test fun onlyExplicitClassicReferencesCanJoinBetaAndReleaseMapPositions(){
        val p=e10Batch3Package();val id="wow-forever:npc:5875"
        for(status in listOf(Verification.UNVERIFIED,Verification.COMMUNITY,Verification.CONFLICT)){
            val entries=p.entries.map{if(it.key==id)it.copy(verification=status) else it}
            val atlas=buildAtlas(p.maps,entries,p.sources.associate{it.id to it.url},p.locations)
            assertEquals(83,atlas.points.size);assertTrue(atlas.points.none{it.entryKey==id})
            val stop=resolveStops(listOf(PlannedStopEntity(DEFAULT_PROFILE_ID,1,id,"Gan'rul Bloodeye")),entries,p.locations).single()
            assertNull(stop.location)
        }
    }
    @Test fun distinctQuestItemsReferencePositionsAndFixedRewardsRemainHonestAfterDecoding(){
        val p=e10Batch3Package();ContentValidator.validate(p)
        val previous=e10Batch2Package()
        assertTrue(p.entries.containsAll(previous.entries));assertTrue(p.locations.containsAll(previous.locations))
        assertTrue(p.relations.containsAll(previous.relations));assertEquals(216,p.entries.size);assertEquals(84,p.locations.size)
        val atlas=buildAtlas(p.maps,p.entries,p.sources.associate{it.id to it.url},p.locations)
        assertEquals(84,atlas.points.size);assertEquals(6,atlas.points.count{it.verification==Verification.CLASSIC})
        assertTrue(atlas.points.single{it.entryKey=="wow-forever:npc:5875"}.name.endsWith(" · Classic"))
        val search=CatalogSearchIndex(p.entries)
        assertTrue(search.search("šest míst").any{it.key=="wow-forever:item:11845"})
        assertTrue(search.search("Makeshift Stormcaller").any{it.key=="wow-forever:item:281274"})
        assertTrue(search.search("Dull Stormy Orb").any{it.key=="wow-forever:quest:97281"})
        assertEquals(Verification.CLASSIC,p.entries.single{it.gameId==5875 && it.kind==EntryKind.NPC}.verification)
        assertTrue(p.entries.single{it.key=="wow-forever:npc:5875"}.descriptionCs.startsWith("Classic reference:"))
        assertEquals(setOf("wow-forever:quest:1501"),p.relations.single{it.ownerKey=="wow-forever:quest:1504" && it.kind==RelationKind.PREREQUISITE}.targetKeys.toSet())
        assertTrue(p.relations.none{it.kind==RelationKind.START_NPC && it.ownerKey in setOf("wow-forever:quest:832","wow-forever:quest:97281")})
        assertTrue(p.relations.none{it.kind==RelationKind.REWARD_CHOICE && it.ownerKey in setOf("wow-forever:item:11845","wow-forever:item:281274")})
    }
    @Test fun newReferencesCannotClaimConfirmedAvailabilityOrCompleteDurotar(){
        val p=e10Batch3Package()
        val report=RecommendationEngine(p.entries,p.relations,p.guideRules,emptyMap()).evaluate(ProfileBackup(ProfileEntity(level=10,currentUiMapId=1411)))
        assertTrue(report.now.isEmpty());assertTrue(report.all.none{it.riskLabel!=null})
        for(key in listOf("wow-forever:quest:1506","wow-forever:quest:1501","wow-forever:quest:1504","wow-forever:quest:97282")){
            assertEquals(Availability.UNVERIFIED,report.all.single{it.entry.key==key}.availability)
        }
        assertTrue(p.relations.filter{":e10c:" in it.key}.none{it.routingEnabled})
        assertEquals(0,p.entries.count{it.releaseConfirmed});assertTrue(p.transportGraph.edges.isEmpty())
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class E10Batch3StorageTest {
    private val c=ApplicationProvider.getApplicationContext<Context>()
    @Before fun clean(){File(c.filesDir,"content-generations").deleteRecursively();c.deleteDatabase("forever-content.db");c.deleteDatabase("forever-personal.db")}
    @Test fun e13AcknowledgedFiveAutomaticallyUpgradesToSixAndRollbackKeepsBothProfilesAndNewHistory()=runBlocking {
        val oldBundle=e10Batch2Distribution(c)
        val old=PackageManager(c,bundledAsset="historical-e13",bundledOpener={oldBundle.inputStream()})
        old.initialize();assertEquals(5,old.status().generation)
        val receipt=File(old.root,"bundled-receipt").readText()
        var s=GuideStore(c)
        try{
            s.initialize();s.personal.personal().updateProfile(ProfileEntity(displayName="Jenda",level=10))
            s.personal.personal().createProfile(ProfileEntity(id="second",displayName="Druhá postava"))
            s.journal.setQuest(DEFAULT_PROFILE_ID,"wow-forever:quest:96821","Legging It",QuestState.COMPLETED)
            s.journal.chooseReward(DEFAULT_PROFILE_ID,"wow-forever:quest:804","Sarkoth","wow-forever:item:4916","Soft Wool Vest")
            s.personal.personal().saveNote(NoteEntity("old",DEFAULT_PROFILE_ID,"Moje poznámka",null,1411,0,43.0,68.0,1))
            val before=s.journal.profileBackup(DEFAULT_PROFILE_ID);val other=s.journal.profileBackup("second")
            s.close();s=GuideStore(c,bundledAsset=E13_BUNDLE);s.initialize()
            assertEquals(6,s.packages.status().generation);assertEquals(5,s.packages.status().previousGeneration)
            assertNotEquals(receipt,File(s.packages.root,"bundled-receipt").readText())
            assertEquals(before,s.journal.profileBackup(DEFAULT_PROFILE_ID));assertEquals(other,s.journal.profileBackup("second"))
            s.journal.addStop(DEFAULT_PROFILE_ID,"wow-forever:npc:5875","Gan'rul Bloodeye")
            s.journal.setQuest(DEFAULT_PROFILE_ID,"wow-forever:quest:1506","Gan'rul's Summons",QuestState.COMPLETED)
            val saved=s.journal.profileBackup(DEFAULT_PROFILE_ID)
            assertEquals(saved,BackupCodec.decode(s.journal.export(DEFAULT_PROFILE_ID)).profiles.single{it.profile.id==DEFAULT_PROFILE_ID})
            s.packages.rollback();s.selectActiveContent();assertEquals(5,s.packages.status().generation)
            assertNull(resolveStops(saved.stops,s.entries(),s.locations()).single().location)
            s.close();s=GuideStore(c,bundledAsset=E13_BUNDLE);s.initialize()
            assertEquals(5,s.packages.status().generation);assertEquals(saved,s.journal.profileBackup(DEFAULT_PROFILE_ID))
            s.packages.reset();s.selectActiveContent();assertEquals(6,s.packages.status().generation)
            val stop=resolveStops(saved.stops,s.entries(),s.locations()).single()
            assertNotNull(stop.location);assertTrue(stop.limitation!!.contains("Classic"))
            assertEquals(LegStatus.NO_VERIFIED_PATH,TransportRouter(s.transportGraph()).legs(listOf(stop,stop.copy(number=2)),saved).single().status)
            assertEquals(saved,s.journal.profileBackup(DEFAULT_PROFILE_ID));assertEquals(other,s.journal.profileBackup("second"))
        }finally{s.close()}
    }
    @Test fun modifiedGenerationSixCannotReplaceFiveOrChangePersonalData()=runBlocking {
        val s=GuideStore(c)
        try{
            s.initialize();s.packages.activate(s.packages.stage(e10Batch2Distribution(c).inputStream()));s.selectActiveContent()
            s.journal.setQuest(DEFAULT_PROFILE_ID,"wow-forever:quest:788","Cutting Teeth",QuestState.COMPLETED)
            val before=s.journal.profileBackup(DEFAULT_PROFILE_ID)
            val output=ByteArrayOutputStream()
            ZipInputStream(e10Batch3Distribution(c).inputStream()).use{input->ZipOutputStream(output).use{out->
                while(true){val e=input.nextEntry?:break;val bytes=input.readBytes();out.putNextEntry(ZipEntry(e.name));out.write(bytes);if(e.name=="catalog.json")out.write(32);out.closeEntry();input.closeEntry()}
            }}
            assertThrows(IllegalArgumentException::class.java){runBlocking{s.packages.stage(output.toByteArray().inputStream())}}
            assertEquals(5,s.packages.status().generation);assertEquals(before,s.journal.profileBackup(DEFAULT_PROFILE_ID))
        }finally{s.close()}
    }
}
