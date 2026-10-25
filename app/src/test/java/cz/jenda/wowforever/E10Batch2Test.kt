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
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun e10Batch2Package() = decodePackage(File("../content/e10-v5.json").readBytes())
fun e10Batch2Distribution(c:Context, tamper:Boolean=false):ByteArray {
    val manifest=File("../content/e10-v5.manifest.json").readBytes()
    val m=ContentJson.decodeFromString<PackageManifest>(manifest.decodeToString())
    val files=linkedMapOf("manifest.json" to manifest,"manifest.sig" to File("../content/e10-v5.manifest.sig").readBytes())
    m.files.forEach{f->files[f.path]=if(f.path=="catalog.json")File("../content/e10-v5.json").readBytes() else distributionTile(c,f.path)}
    if(tamper)files["catalog.json"]=files.getValue("catalog.json")+byteArrayOf(32)
    val output=ByteArrayOutputStream()
    ZipOutputStream(output).use{z->files.forEach{(path,data)->z.putNextEntry(ZipEntry(path));z.write(data);z.closeEntry()}}
    return output.toByteArray()
}

class E10Batch2DataTest {
    @Test fun clothRewardsDistinctQuestIdsAndHandInNpcSurviveRealCatalogDecoding(){
        val p=e10Batch2Package();ContentValidator.validate(p)
        val old=e10Package();assertTrue(p.entries.containsAll(old.entries));assertTrue(p.locations.containsAll(old.locations))
        assertEquals(182,p.entries.size);assertEquals(74,p.locations.size)
        val search=CatalogSearchIndex(p.entries)
        assertEquals(setOf("wow-forever:quest:830","wow-forever:quest:831"),search.search("The Admiral's Orders").map{it.key}.toSet())
        assertEquals(listOf("wow-forever:npc:3139"),p.relations.single{it.ownerKey=="wow-forever:quest:96821" && it.kind==RelationKind.END_NPC}.targetKeys)
        assertEquals(listOf("wow-forever:quest:804"),p.relations.single{it.ownerKey=="wow-forever:item:4916" && it.kind==RelationKind.REWARD_CHOICE}.targetKeys)
        assertTrue(search.search("latkove kalhoty").any{it.key=="wow-forever:item:6713"})
        val actual=p.locations.single{it.entryKey=="wow-forever:npc:3230"}
        assertEquals("wow-forever:map:1454",actual.mapKey);assertEquals(33.2,actual.xPercent,0.001)
    }
    @Test fun betaMilestonesAndClassicChainCannotProduceConfirmedUnlocksOrMissableWarnings(){
        val p=e10Batch2Package()
        val person=ProfileBackup(ProfileEntity(level=10,currentUiMapId=1411),quests=listOf(QuestProgressEntity(DEFAULT_PROFILE_ID,"wow-forever:quest:830","The Admiral's Orders",QuestState.COMPLETED.name,1)))
        val direct=p.locations.groupBy{it.entryKey}.mapValues{it.value.map{l->l.mapKey.substringAfterLast(':').toInt()}.toSet()}
        val report=RecommendationEngine(p.entries,p.relations,p.guideRules,direct).evaluate(person)
        assertTrue(report.now.isEmpty());assertTrue(report.all.none{it.riskLabel!=null})
        val chain=p.relations.single{it.kind==RelationKind.PREREQUISITE && it.ownerKey=="wow-forever:quest:831"}
        assertFalse(chain.routingEnabled);assertEquals(Verification.CLASSIC,chain.verification)
        assertEquals(Availability.UNVERIFIED,report.all.single{it.entry.key=="wow-forever:quest:831"}.availability)
        val rules=p.guideRules.filter{":e10b:" in it.key}
        assertEquals(31,rules.size);assertTrue(rules.none{it.verification==Verification.RELEASE})
        assertTrue(report.uncertain.any{it.entry.nameOriginal=="Bane of Agony"})
        assertTrue(p.entries.none{it.releaseConfirmed})
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class E10Batch2StorageTest {
    private val c=ApplicationProvider.getApplicationContext<Context>()
    @Before fun clean(){File(c.filesDir,"content-generations").deleteRecursively();c.deleteDatabase("forever-content.db");c.deleteDatabase("forever-personal.db")}
    @Test fun signedFourToFiveUpdateAndRollbackKeepBothProfilesNewQuestRewardNoteAndExport()=runBlocking {
        var store=GuideStore(c)
        try{
            store.initialize();store.packages.activate(store.packages.stage(e10Distribution(c).inputStream()));store.selectActiveContent()
            val candidate=store.packages.stage(e10Batch2Distribution(c).inputStream())
            assertEquals(4,store.packages.status().generation)
            store.personal.personal().createProfile(ProfileEntity(id="other"))
            store.journal.setQuest(DEFAULT_PROFILE_ID,"wow-forever:quest:4641","Your Place in the World",QuestState.COMPLETED)
            val before=store.journal.profileBackup(DEFAULT_PROFILE_ID)
            val other=store.journal.profileBackup("other")
            store.packages.activate(candidate);store.selectActiveContent()
            assertEquals(before,store.journal.profileBackup(DEFAULT_PROFILE_ID));assertEquals(182,store.entries().size)
            store.journal.setQuest(DEFAULT_PROFILE_ID,"wow-forever:quest:96821","Legging It",QuestState.COMPLETED)
            store.journal.chooseReward(DEFAULT_PROFILE_ID,"wow-forever:quest:804","Sarkoth","wow-forever:item:4916","Soft Wool Vest")
            store.journal.addStop(DEFAULT_PROFILE_ID,"wow-forever:npc:275811","Pal'juh")
            store.personal.personal().saveNote(NoteEntity("note:e10b",DEFAULT_PROFILE_ID,"Doprovod",null,1411,0,51.2,79.7,1))
            val saved=store.journal.profileBackup(DEFAULT_PROFILE_ID)
            val backup=BackupCodec.decode(store.journal.export(DEFAULT_PROFILE_ID))
            assertEquals(saved,backup.profiles.single{it.profile.id==DEFAULT_PROFILE_ID})
            store.close();store=GuideStore(c);store.initialize()
            assertEquals(5,store.packages.status().generation)
            store.packages.rollback();store.selectActiveContent();assertEquals(4,store.packages.status().generation)
            assertEquals(saved,store.journal.profileBackup(DEFAULT_PROFILE_ID));assertEquals(other,store.journal.profileBackup("other"))
            assertNull(resolveStops(saved.stops,store.entries(),store.locations()).single().location)
            store.packages.rollback();store.selectActiveContent();assertEquals(5,store.packages.status().generation)
            assertNotNull(resolveStops(saved.stops,store.entries(),store.locations()).single().location)
            assertEquals(saved,store.journal.profileBackup(DEFAULT_PROFILE_ID))
        }finally{store.close()}
    }
    @Test fun modifiedSignedCatalogDoesNotSwitchActiveGenerationOrTouchPersonalData()=runBlocking {
        val store=GuideStore(c)
        try{
            store.initialize();store.packages.activate(store.packages.stage(e10Distribution(c).inputStream()));store.selectActiveContent()
            store.journal.setQuest(DEFAULT_PROFILE_ID,"wow-forever:quest:788","Cutting Teeth",QuestState.COMPLETED)
            val before=store.journal.profileBackup(DEFAULT_PROFILE_ID)
            assertThrows(IllegalArgumentException::class.java){runBlocking{store.packages.stage(e10Batch2Distribution(c,true).inputStream())}}
            assertEquals(4,store.packages.status().generation);assertEquals(before,store.journal.profileBackup(DEFAULT_PROFILE_ID))
        }finally{store.close()}
    }
}
