package cz.jenda.wowforever

import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.data.*
import cz.jenda.wowforever.data.db.*
import cz.jenda.wowforever.domain.*
import cz.jenda.wowforever.packages.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class E10DataTest {
    private val p=e10Package()
    private val atlas get()=buildAtlas(p.maps,p.entries,p.sources.associate{it.id to it.url},p.locations)
    @Test fun expandedDistributionHasRealScopedContentAndPreservesEveryExistingEntry(){
        ContentValidator.validate(p)
        assertEquals(4,p.generation);assertEquals(139,p.entries.size);assertEquals(62,p.locations.size)
        assertEquals(5,p.maps.size);assertEquals(140,p.sources.size);assertEquals(106,p.relations.size);assertEquals(36,p.guideRules.size)
        val old=currentPackage();val actual=p.entries.associateBy{it.key}
        old.entries.forEach{assertEquals(it,actual[it.key])}
        old.locations.forEach{assertTrue(it in p.locations)};old.relations.forEach{assertTrue(it in p.relations)}
        assertTrue(p.maps.single{it.uiMapId==1413}.scopeCs!!.contains("Crossroads"))
        assertTrue(p.maps.single{it.uiMapId==1454}.scopeCs!!.contains("Ragefire"))
        assertTrue(p.entries.none{it.releaseConfirmed || it.verification==Verification.RELEASE})
    }
    @Test fun newLocalCoordinatesStayInTheirOwnFramesAndEbruIsNotInventedOnBarrensMap(){
        val devrak=p.locations.single{it.entryKey=="wow-forever:npc:3615"}
        val doras=p.locations.single{it.entryKey=="wow-forever:npc:3310"}
        assertEquals("wow-forever:map:1413",devrak.mapKey);assertEquals(51.6,devrak.xPercent,0.001);assertEquals(30.4,devrak.yPercent,0.001)
        assertEquals("wow-forever:map:1454",doras.mapKey);assertEquals(45.3,doras.xPercent,0.001)
        assertTrue(p.locations.none{it.entryKey=="wow-forever:npc:5768"})
        val source=p.sources.single{it.id=="e10_npc_5768"};val facts=ContentJson.parseToJsonElement(source.payload).jsonObject["facts"]!!.jsonObject
        assertEquals(1414,facts["reportedMapId"]!!.jsonPrimitive.int);assertFalse(facts["usesPosition"]!!.jsonPrimitive.boolean)
        assertTrue(p.entries.none{it.gameId==3327});assertTrue(p.entries.none{it.gameId==2916})
    }
    @Test fun questRecorderZoneIsNotMistakenForGiverLocationAndNoQuestPinIsGuessed(){
        val areas=areaCoverage(atlas,p.entries,p.relations,p.guideRules)
        val quest="wow-forever:quest:865"
        assertEquals(listOf("wow-forever:npc:3446"),p.relations.single{it.kind==RelationKind.START_NPC && it.ownerKey==quest}.targetKeys)
        assertEquals("wow-forever:map:1413",p.locations.single{it.entryKey=="wow-forever:npc:3446"}.mapKey)
        assertTrue(p.locations.none{it.entryKey.startsWith("wow-forever:quest:")})
        assertEquals(24,atlas.pointsFor(1413).size);assertEquals(11,atlas.pointsFor(1454).size)
        assertEquals(22,areas.single{it.map.uiMapId==1413}.npcCount);assertEquals(10,areas.single{it.map.uiMapId==1454}.npcCount)
    }
    @Test fun reportedQuestLevelsNeverBecomeInventedMinimumsAndDuplicateChainNamesKeepDistinctIds(){
        val newer=p.entries.filter{it.kind==EntryKind.QUEST && it.sourceIds.any{s->s.startsWith("e10_")}}
        assertTrue(newer.all{it.minimumLevel==null})
        assertEquals(setOf(5726,5727,5728,5729,5730),newer.filter{it.nameOriginal=="Hidden Enemies"}.map{it.gameId}.toSet())
        val links=p.relations.filter{it.kind==RelationKind.PREREQUISITE && it.key.contains("e10:")}
        assertEquals(5,links.size);assertTrue(links.all{it.verification==Verification.CLASSIC && !it.routingEnabled})
    }
    @Test fun warlockMilestonesAndRewardChoicesKeepTheirEvidenceAndDoNotClaimBestGear(){
        val levels=p.entries.filter{it.key.startsWith("wow-forever:spell:book:warlock:")}.associate{it.nameOriginal to it.minimumLevel}
        assertEquals(mapOf("Summon Voidwalker" to 10,"Drain Soul" to 10,"Create Healthstone" to 10,"Create Soulstone" to 18,"Ritual of Summoning" to 20),levels)
        assertTrue(p.entries.filter{it.key.startsWith("wow-forever:spell:book:")}.all{it.gameId==null && it.verification==Verification.BETA})
        val choices=p.relations.filter{it.kind==RelationKind.REWARD_CHOICE && "wow-forever:quest:5730" in it.targetKeys}.map{it.ownerKey}.toSet()
        assertEquals(setOf("wow-forever:item:15443","wow-forever:item:15444"),choices)
        assertTrue(p.guideRules.filter{it.key.startsWith("wow-forever:rule:e10:")}.all{it.verification==Verification.BETA && it.window==null})
    }
    @Test fun betaAndClassicStillCannotProduceConfirmedAdviceOrUnlockedFlightRoutes(){
        val personal=ProfileBackup(ProfileEntity(level=60,currentUiMapId=1413),travel=p.entries.filter{it.kind==EntryKind.TRAVEL}.map{DiscoveredTravelEntity(DEFAULT_PROFILE_ID,it.key,it.nameOriginal,1)})
        val direct=p.locations.groupBy{it.entryKey}.mapValues{it.value.map{l->l.mapKey.substringAfterLast(':').toInt()}.toSet()}
        val report=RecommendationEngine(p.entries,p.relations,p.guideRules,direct).evaluate(personal)
        assertTrue(report.now.isEmpty());assertTrue(report.all.filter{it.entry.sourceIds.any{s->s.startsWith("e10_")}}.none{it.availability==Availability.AVAILABLE})
        assertEquals(3,p.transportGraph.nodes.size);assertTrue(p.transportGraph.edges.isEmpty())
        assertNull(TransportRouter(p.transportGraph).path("wow-forever:travel-node:flight:3615","wow-forever:travel-node:flight:3310",personal))
    }
    @Test fun actualPublisherManifestVerifiesOnE10ButRejectsE09BeforeCatalogDecoding(){
        val pub=javaClass.getResourceAsStream("/publisher.der")!!.use{it.readBytes()}
        val m=ManifestValidator.decode(e10ManifestBytes(),e10Signature(),pub,10)
        ManifestValidator.catalog(m,p);assertEquals(86,m.files.size);assertEquals(10,m.minimumAppCode)
        assertThrows(IllegalArgumentException::class.java){ManifestValidator.decode(e10ManifestBytes(),e10Signature(),pub,9)}
        assertThrows(IllegalArgumentException::class.java){ManifestValidator.catalog(m.copy(minimumAppCode=9),p)}
    }
}
