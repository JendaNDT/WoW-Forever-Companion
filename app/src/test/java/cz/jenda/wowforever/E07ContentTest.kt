package cz.jenda.wowforever

import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.data.*
import cz.jenda.wowforever.data.db.*
import cz.jenda.wowforever.domain.*
import org.junit.Assert.*
import org.junit.Test

class E07ContentTest {
    private val p=currentPackage()
    @Test fun pilotPreservesOldIdsAndOnlyAddsDatedSourceBackedRecords() {
        ContentValidator.validate(p)
        assertEquals(2,p.generation);assertEquals(53,p.entries.size);assertEquals(27,p.locations.size)
        assertEquals(49,p.relations.size);assertEquals(59,p.sources.size);assertEquals(8,p.guideRules.size)
        assertTrue(p.entries.map { it.key }.containsAll(samplePackage().entries.map { it.key }))
        assertEquals(samplePackage().maps,p.maps)
        assertTrue(p.entries.none { it.releaseConfirmed || it.verification==Verification.RELEASE })
        assertTrue(p.relations.none { it.routingEnabled });assertTrue(p.guideRules.none { it.verification==Verification.RELEASE })
        assertTrue(p.sources.all { it.checkedOn=="2026-10-23" })
    }
    @Test fun newDemonTrainerAndServicesUseBetaPositionsWithNoInventedQuestObjectives() {
        val hraug=p.locations.single { it.entryKey=="wow-forever:npc:12776" }
        assertEquals(40.6,hraug.xPercent,0.00001);assertEquals(68.3,hraug.yPercent,0.00001)
        assertTrue(p.locations.all { it.entryKey.startsWith("wow-forever:npc:") && it.mapKey=="wow-forever:map:1411" && it.floorId==0 })
        assertTrue(p.locations.none { it.entryKey=="wow-forever:npc:3156" })
        assertEquals(setOf("wow-forever:npc:12776"),searchEntries(p.entries,"trenér démonů").map { it.key }.toSet())
        assertTrue(p.relations.none { it.ownerKey=="wow-forever:quest:96652" && it.kind==RelationKind.START_NPC })
    }
    @Test fun sameNamesAndRewardChoicesRemainSeparateAndReferenceLinksStayInactive() {
        assertEquals(setOf(790,804),p.entries.filter { it.nameOriginal=="Sarkoth" }.map { it.gameId }.toSet())
        assertEquals(setOf(792,1485,1499),p.entries.filter { it.nameOriginal=="Vile Familiars" }.map { it.gameId }.toSet())
        val rewards=p.relations.filter { it.kind==RelationKind.REWARD_CHOICE && "wow-forever:quest:4402" in it.targetKeys }
        assertEquals(setOf(247840,247841,247846),rewards.map { it.ownerKey.substringAfterLast(':').toInt() }.toSet())
        listOf(789 to 788,804 to 790,6394 to 5441).forEach { (child,parent) ->
            val r=p.relations.single { it.kind==RelationKind.PREREQUISITE && it.ownerKey=="wow-forever:quest:$child" }
            assertEquals(listOf("wow-forever:quest:$parent"),r.targetKeys);assertEquals(Verification.CLASSIC,r.verification);assertFalse(r.routingEnabled)
        }
    }
    @Test fun completingClassicPredecessorDoesNotPromoteBetaSuggestionsToAvailable() {
        val personal=ProfileBackup(ProfileEntity(),quests=listOf(QuestProgressEntity(DEFAULT_PROFILE_ID,"wow-forever:quest:788","Cutting Teeth",QuestState.COMPLETED.name,1)))
        val report=RecommendationEngine(p.entries,p.relations,p.guideRules,mapOf()).evaluate(personal)
        assertTrue(report.now.isEmpty());assertEquals(52,report.all.size)
        assertTrue(report.all.all { it.availability==Availability.UNVERIFIED && it.riskLabel==null && it.unlockCount==0 })
        val imp=report.all.single { it.entry.key=="wow-forever:quest:1485" }
        assertTrue(imp.ruleReason!!.contains("Summon Imp"))
    }
    @Test fun newerSpellbookReferenceDoesNotRelabelOldMapsOrClaimCompleteAccess() {
        val source=p.sources.single { it.id=="e07_foreverchanges_spellbook" }
        assertEquals("1.60.1.70532",source.build)
        assertTrue(p.maps.all { it.build=="1.60.1.69893" })
        assertTrue(p.entries.single { it.key=="wow-forever:spell:688" }.sourceIds.contains(source.id))
        assertEquals(1,p.entries.single { it.key=="wow-forever:quest:1485" }.minimumLevel)
        assertTrue(p.entries.filter { it.kind==EntryKind.QUEST && it.gameId!=1485 }.all { it.minimumLevel==null })
        assertTrue(p.sources.none { it.kind=="in_game_release_observation" || it.url.contains("example.org") || it.id.startsWith("test:") })
    }
}
