package cz.jenda.wowforever

import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.data.*
import cz.jenda.wowforever.data.db.QuestState
import cz.jenda.wowforever.domain.*
import cz.jenda.wowforever.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class UiPhase3RulesTest {
    private val q="wow-forever:quest:1"
    private val other="wow-forever:quest:2"
    private val npc="wow-forever:npc:1"
    private fun link(owner:String,kind:RelationKind)=RelationRecord("$owner:$kind",kind,owner,listOf(npc),MatchMode.ALL,Verification.CLASSIC,false,emptyList())
    private val point=MapPoint(npc,"NPC",MapPosition(1411,50.0,50.0),"https://example.test",verification=Verification.CLASSIC)
    private fun projected(index:MapRoleIndex,states:Map<String,QuestState>,kinds:Set<String> = setOf("QUEST"),show:Boolean=false)=index.project(listOf(point),kinds,false,emptySet(),states,show,emptyMap())
    @Test fun mixedQuestRolesUseActiveTurnInAndKeepOtherUnfinishedQuests() {
        val index=MapRoleIndex(listOf(link(q,RelationKind.START_NPC),link(q,RelationKind.END_NPC),link(other,RelationKind.START_NPC)))
        assertEquals(MarkerRole.START,projected(index,emptyMap()).single().markerRole)
        assertEquals(MarkerRole.END,projected(index,mapOf(q to QuestState.IN_PROGRESS)).single().markerRole)
        assertEquals(MarkerRole.START,projected(index,mapOf(q to QuestState.COMPLETED)).single().markerRole)
        assertTrue(projected(index,mapOf(q to QuestState.COMPLETED,other to QuestState.COMPLETED)).isEmpty())
        assertEquals(MarkerRole.PERSON,projected(index,mapOf(q to QuestState.COMPLETED,other to QuestState.COMPLETED),setOf("NPC")).single().markerRole)
    }
    @Test fun completedQuestCanBeRevealedExplicitlyWithoutChangingTheDefaultFilterOrItsPosition() {
        val p=point.copy(id=q,entryKey=q,kind=PointKind.QUEST)
        val index=MapRoleIndex(emptyList());val states=mapOf(q to QuestState.COMPLETED)
        assertTrue(index.project(listOf(p),setOf("QUEST"),false,emptySet(),states,false,emptyMap()).isEmpty())
        val revealed=index.project(listOf(p),emptySet(),true,emptySet(),states,false,mapOf(q to listOf(1,3)),q).single()
        assertEquals(p.position,revealed.position);assertEquals(listOf(1,3),revealed.planNumbers)
        assertEquals(Verification.CLASSIC,revealed.verification)
        assertEquals(p,index.project(listOf(p),setOf("QUEST"),false,emptySet(),states,true,emptyMap()).single().copy(markerRole=p.markerRole))
    }
    @Test fun itemSourcesAndObjectivesUseDiamondsAndLayerChangesDoNotInventLocations() {
        val index=MapRoleIndex(listOf(link(q,RelationKind.OBJECTIVE_NPC),link("wow-forever:item:1",RelationKind.ITEM_SOURCE_NPC)))
        val result=projected(index,mapOf(q to QuestState.IN_PROGRESS)).single()
        assertEquals(MarkerRole.TARGET,result.markerRole);assertEquals(point.position,result.position)
        assertTrue(index.project(emptyList(),setOf("QUEST"),false,emptySet(),emptyMap(),false,emptyMap()).isEmpty())
        assertEquals(MarkerRole.TARGET,projected(index,emptyMap(),setOf("NPC")).single().markerRole)
    }
    @Test fun oldSessionSkipsWelcomeWhileNewInstallAndPreferencesHaveSafeDefaults()=runBlocking {
        val old=SessionSerializer.readFrom("{\"mapId\":1454,\"history\":[\"MAP\",\"JOURNEY\"]}".byteInputStream())
        assertTrue(old.welcomeComplete);assertEquals(1454,old.mapId)
        assertFalse(SessionSerializer.defaultValue.welcomeComplete)
        assertFalse(old.showCompletedQuests);assertFalse(old.keepMapScreenOn)
        val out=java.io.ByteArrayOutputStream();val expected=old.copy(lastBackupAt=123,personalRevision=3,backedUpRevision=2,keepMapScreenOn=true)
        SessionSerializer.writeTo(expected,out);assertEquals(expected,SessionSerializer.readFrom(out.toByteArray().inputStream()))
    }
    @Test fun reminderRequiresSevenDaysAndUnbackedChangesAndHandlesClockRollback() {
        val week=7*86_400_000L;val start=1000L
        val never=Session(personalRevision=1,unbackedSince=start)
        assertFalse(never.backupReminderDue(start+week-1));assertTrue(never.backupReminderDue(start+week));assertFalse(never.backupReminderDue(start-1))
        val backed=never.copy(lastBackupAt=start,backedUpRevision=1)
        assertFalse(backed.backupReminderDue(start+week*2))
        assertTrue(backed.copy(personalRevision=2).backupReminderDue(start+week))
        assertEquals(14L,backed.lastBackupDays(start+week*2))
    }
    @Test fun beforeLeavingOnlyUsesKnownAreaAndDoesNotTurnReferenceDataIntoAvailability() {
        fun advice(key:String,area:Set<Int>,progress:Boolean=false,wish:Boolean=false,importance:Importance=Importance.DISCOVERY,level:Int=10)=Advice(
            EntryRecord(key,EntryKind.QUEST,nameOriginal=key,descriptionCs="",verification=Verification.CLASSIC,sourceIds=emptyList(),payload="{}",reportedLevel=level),
            Availability.UNVERIFIED,emptyList(),null,importance,area,emptySet(),emptySet(),progress,wish,false,null,0)
        val a=advice(q,setOf(1411),progress=true,wish=true)
        val b=advice(other,setOf(1411),importance=Importance.CLASS)
        val c=advice("wow-forever:quest:3",setOf(1454),wish=true)
        val d=advice("wow-forever:quest:4",emptySet(),wish=true)
        val e=advice("wow-forever:quest:5",setOf(1411),importance=Importance.CLASS,level=60)
        val f=advice("wow-forever:quest:6",setOf(1411),wish=true)
        val result=AdviceReport(listOf(a,b,c,d,e,f),1411).beforeLeaving(10)
        assertEquals(listOf(a),result.inProgress);assertEquals(listOf(b),result.classQuests);assertEquals(listOf(f),result.wishes)
        assertEquals(Availability.UNVERIFIED,result.classQuests.single().availability)
    }
}
