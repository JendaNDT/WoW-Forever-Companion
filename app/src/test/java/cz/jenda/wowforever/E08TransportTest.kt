package cz.jenda.wowforever

import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.data.*
import cz.jenda.wowforever.data.db.*
import cz.jenda.wowforever.domain.*
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test
import java.io.File

internal const val T8_PROOF="test:e08-proof"
internal const val T8_A="wow-forever:travel-node:a"
internal const val T8_B="wow-forever:travel-node:b"
internal const val T8_C="wow-forever:travel-node:c"
internal const val T8_D="wow-forever:travel-node:d"
internal const val T8_DA="wow-forever:travel:synthetic-discovery-a"
internal const val T8_DB="wow-forever:travel:synthetic-discovery-b"
internal fun t8Personal(level:Int=10,discoveries:List<String> = emptyList(),complete:Boolean=false)=ProfileBackup(
    ProfileEntity(level=level),travel=discoveries.map { DiscoveredTravelEntity(DEFAULT_PROFILE_ID,it,"Cvičný bod",1) },
    quests=if(complete)listOf(QuestProgressEntity(DEFAULT_PROFILE_ID,"wow-forever:quest:788","Cutting Teeth",QuestState.COMPLETED.name,1)) else emptyList())
internal fun t8Fixture():ContentPackage {
    val real=currentPackage();val proof=SourceRecord(T8_PROOF,"https://example.org/synthetic-e08","in_game_release_observation","2026-10-23",payload="Synthetic tests only; no actual game verification")
    val entries=real.entries.map { it.copy(verification=Verification.RELEASE,releaseConfirmed=true,sourceIds=listOf(T8_PROOF)) }.toMutableList()
    fun travel(id:String)="wow-forever:travel:synthetic-$id".also { entries.add(EntryRecord(it,EntryKind.TRAVEL,nameOriginal="Cvičná doprava $id",descriptionCs="Pouze testovací záznam.",verification=Verification.RELEASE,releaseConfirmed=true,sourceIds=listOf(T8_PROOF),payload="{}")) }
    travel("discovery-a");travel("discovery-b")
    val locations=real.locations.map { it.copy(sourceId=T8_PROOF) }
    fun node(key:String,npc:Int,discovery:String?=null)=TravelNode(key,"Cvičný bod "+key.substringAfterLast(':'),locations.single { it.entryKey=="wow-forever:npc:$npc" }.key,listOf(T8_PROOF),discovery)
    val nodes=listOf(node(T8_A,12776,T8_DA),node(T8_B,6928,T8_DB),node(T8_C,9564),node(T8_D,5765))
    fun edge(id:String,from:String,to:String,mode:TravelMode=TravelMode.WALK,c:GuideCondition=GuideCondition.Always)=TravelEdge("wow-forever:travel-edge:$id",travel(id),from,to,mode,c,Verification.RELEASE,listOf(T8_PROOF),120)
    val edges=listOf(edge("flight",T8_A,T8_B,TravelMode.FLIGHT,GuideCondition.All(listOf(GuideCondition.PlayerClass(listOf("WARLOCK")),GuideCondition.Level(10)))) ,
        edge("boat",T8_B,T8_C,TravelMode.BOAT),edge("walk-a-d",T8_A,T8_D),edge("walk-d-b",T8_D,T8_B,c=GuideCondition.QuestCompleted("wow-forever:quest:788")),edge("portal",T8_C,T8_A,TravelMode.PORTAL))
    return real.copy(generation=real.generation+1,sources=real.sources+proof,entries=entries,locations=locations,transportGraph=TransportGraph(nodes,edges))
}

class E08TransportTest {
    private val p=t8Fixture()
    private fun rejects(graph:TransportGraph) { assertThrows(InvalidContent::class.java){ContentValidator.validate(p.copy(transportGraph=graph))} }
    @Test fun validatedGraphRoundTripsAndProductionContainsNoSyntheticConnections() {
        ContentValidator.validate(p)
        val bytes=ContentJson.encodeToString(p).encodeToByteArray();assertEquals(p,decodePackage(bytes))
        File("build/reports/e08-transport-fixture.json").also { it.parentFile!!.mkdirs();it.writeBytes(bytes) }
        val real=currentPackage();assertTrue(real.transportGraph.edges.isEmpty());assertTrue(real.transportGraph.nodes.isEmpty())
        assertTrue(real.entries.none { it.key.contains("synthetic") || it.sourceIds.contains(T8_PROOF) })
    }
    @Test fun flightNeedsBothDiscoveriesAndMatchingLevelEvenWhenShorterThanWalk() {
        val router=TransportRouter(p.transportGraph)
        assertNull(router.path(T8_A,T8_B,t8Personal()))
        assertNull(router.path(T8_A,T8_B,t8Personal(discoveries=listOf(T8_DA))))
        assertNull(router.path(T8_A,T8_B,t8Personal(1,listOf(T8_DA,T8_DB))))
        assertEquals(listOf(TravelMode.FLIGHT),router.path(T8_A,T8_B,t8Personal(discoveries=listOf(T8_DA,T8_DB)))!!.map { it.mode })
        val detour=router.path(T8_A,T8_B,t8Personal(complete=true))!!;assertEquals(2,detour.size);assertTrue(detour.all { it.mode==TravelMode.WALK })
        assertEquals(1,router.path(T8_A,T8_B,t8Personal(discoveries=listOf(T8_DA,T8_DB),complete=true))!!.size)
    }
    @Test fun directionCyclesBetaAndUnknownConditionsNeverInventAnAccessibleConnection() {
        val edge=p.transportGraph.edges.first();val graph=p.transportGraph.copy(edges=listOf(edge))
        val personal=t8Personal(discoveries=listOf(T8_DA,T8_DB))
        assertNull(TransportRouter(graph).path(T8_B,T8_A,personal))
        assertNull(TransportRouter(graph.copy(edges=listOf(edge.copy(verification=Verification.BETA)))).path(T8_A,T8_B,personal))
        assertNull(TransportRouter(graph.copy(edges=listOf(edge.copy(condition=GuideCondition.Unknown("Neověřeno"))))).path(T8_A,T8_B,personal))
        assertNotNull(TransportRouter(p.transportGraph).path(T8_B,T8_D,personal)) // B→C→A→D; directed cycle terminates
        assertNull(TransportRouter(graph).path("wow-forever:travel-node:missing",T8_A,personal))
    }
    @Test fun allAnyAndProfileRequirementsRespectCompletionInsteadOfDeferral() {
        val edge=p.transportGraph.edges[2]
        fun route(c:GuideCondition,personal:ProfileBackup)=TransportRouter(p.transportGraph.copy(edges=listOf(edge.copy(condition=c)))).path(T8_A,T8_D,personal)
        val cond=GuideCondition.All(listOf(GuideCondition.Faction(listOf("HORDE")),GuideCondition.Race(listOf("ORC")),GuideCondition.AnyOf(listOf(GuideCondition.Unknown("Nevíme"),GuideCondition.QuestCompleted("wow-forever:quest:788")))))
        assertNull(route(cond,t8Personal()));assertNotNull(route(cond,t8Personal(complete=true)))
        val deferred=t8Personal(complete=true).let { it.copy(quests=it.quests.map { q->q.copy(state=QuestState.DEFERRED.name) }) }
        assertNull(route(cond,deferred))
        val professions=GuideCondition.All(listOf(GuideCondition.Mode(listOf("Přežití")),GuideCondition.Profession(listOf("Těžba"))))
        assertNull(route(professions,t8Personal()))
        assertNotNull(route(professions,t8Personal().let { it.copy(profile=it.profile.copy(ruleset=" preziti ",professionsJson="[\"tezba\"]")) }))
    }
    @Test fun missingTargetsDuplicatesEvidenceAndFalseReleaseClaimsAreRejected() {
        val graph=p.transportGraph;val edge=graph.edges.first()
        rejects(graph.copy(nodes=graph.nodes+graph.nodes.first()))
        rejects(graph.copy(edges=graph.edges+edge))
        rejects(graph.copy(edges=listOf(edge.copy(to="wow-forever:travel-node:missing"))))
        rejects(graph.copy(nodes=graph.nodes.mapIndexed { i,n->if(i==0)n.copy(locationKey="missing") else n }))
        rejects(graph.copy(edges=listOf(edge.copy(sourceIds=listOf("missing")))))
        rejects(graph.copy(edges=listOf(edge.copy(estimatedSeconds=-1))))
        val noProof=p.copy(sources=p.sources.map { if(it.id==T8_PROOF)it.copy(kind="beta_reference") else it })
        assertThrows(InvalidContent::class.java){ContentValidator.validate(noProof)}
    }
    @Test fun flightWithoutEndpointDiscoveryAndWalkAcrossFramesAreRejected() {
        val graph=p.transportGraph
        rejects(graph.copy(nodes=graph.nodes.map { if(it.key==T8_B)it.copy(discoveryKey=null) else it }))
        val l=p.locations.first { it.entryKey=="wow-forever:npc:5765" };val map=p.maps.first { it.uiMapId==1454 }
        val changed=p.copy(locations=p.locations.map { if(it.key==l.key)it.copy(mapKey=map.key,coordinateVersion=map.coordinateVersion) else it })
        assertThrows(InvalidContent::class.java){ContentValidator.validate(changed)}
        // An explicitly recorded entrance can connect coordinate frames; no walk is inferred.
        ContentValidator.validate(changed.copy(transportGraph=graph.copy(edges=graph.edges.map { if(it.from==T8_D || it.to==T8_D)it.copy(mode=TravelMode.ENTRANCE) else it })))
    }
    @Test fun removedAndAmbiguousStopsKeepGlobalNumbersWithoutFakeCoordinates() {
        val rows=listOf(PlannedStopEntity(DEFAULT_PROFILE_ID,4,"wow-forever:npc:12776","Starý Hraug"),PlannedStopEntity(DEFAULT_PROFILE_ID,9,"wow-forever:npc:999999","Historie"),PlannedStopEntity(DEFAULT_PROFILE_ID,20,"wow-forever:quest:96652","Deník"))
        val stops=resolveStops(rows,currentPackage().entries,currentPackage().locations)
        assertEquals(listOf(1,2,3),stops.map { it.number });assertEquals("Hraug",stops.first().name)
        assertEquals("Historie",stops[1].name);assertNull(stops[1].location);assertNull(stops[2].location)
        val duplicate=currentPackage().locations.first { it.entryKey==rows.first().entryKey }.copy(key="wow-forever:npc:12776:location:second")
        assertNull(resolveStops(rows,currentPackage().entries,currentPackage().locations+duplicate).first().location)
        val legs=TransportRouter(TransportGraph()).legs(stops,t8Personal());assertTrue(legs.all { it.status==LegStatus.NO_POSITION })
    }
}
