package cz.jenda.wowforever

import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.content.GuideCondition.*
import cz.jenda.wowforever.data.*
import cz.jenda.wowforever.data.db.*
import cz.jenda.wowforever.domain.*
import org.junit.Test
import org.junit.Assert.*

internal const val E6_A="wow-forever:quest:4641"
internal const val E6_B="wow-forever:quest:788"
internal const val E6_C="wow-forever:quest:97279"
internal const val E6_ITEM="wow-forever:item:5778"
internal const val E6_NPC="wow-forever:npc:5765"
internal const val E6_PROOF="test:e06-proof"
/** Complete access rules below are synthetic fixtures, not statements about the actual game. */
internal fun e06Rule(key:String,condition:GuideCondition=Always,importance:Importance=Importance.DISCOVERY)=GuideRule(
    "wow-forever:rule:test-${key.substringAfterLast(':')}",key,condition,importance,"Testovací důvod pro ${key.substringAfterLast(':')}",Verification.RELEASE,listOf(E6_PROOF),listOf(1411))
internal fun e06Fixture():ContentPackage {
    val p=samplePackage()
    val proof=SourceRecord(E6_PROOF,"https://example.org/synthetic-e06","in_game_release_observation","2026-10-23",payload="Synthetic test only")
    return p.copy(sources=p.sources+proof,entries=p.entries.map { it.copy(verification=Verification.RELEASE,releaseConfirmed=true,sourceIds=listOf(E6_PROOF)) },guideRules=listOf(
        e06Rule(E6_A,All(listOf(Faction(listOf("HORDE")),PlayerClass(listOf("WARLOCK")),Race(listOf("ORC","TROLL")),Level(5))),Importance.CLASS),
        e06Rule(E6_B,All(listOf(QuestCompleted(E6_A),Mode(listOf("Hardcore")),Profession(listOf("Tailoring"))))),
        e06Rule(E6_C,AnyOf(listOf(QuestCompleted(E6_A),ItemAcquired(E6_ITEM))),Importance.UNLOCK),
        e06Rule(E6_ITEM,importance=Importance.REWARD),e06Rule(E6_NPC)))
}
internal fun e06Personal(level:Int=5,race:String="ORC")=ProfileBackup(ProfileEntity(level=level,race=race))
internal fun e06Completed(key:String,state:QuestState=QuestState.COMPLETED)=QuestProgressEntity(DEFAULT_PROFILE_ID,key,"Historický název",state.name,1)
internal fun e06Engine(p:ContentPackage=e06Fixture())=RecommendationEngine(p.entries,p.relations,p.guideRules,p.locations.groupBy { it.entryKey }.mapValues { it.value.map { l->p.maps.first { m->m.key==l.mapKey }.uiMapId }.toSet() })

class E06RecommendationsTest {
    private fun advice(key:String,p:ProfileBackup=e06Personal(),pack:ContentPackage=e06Fixture())=e06Engine(pack).evaluate(p).all.first { it.entry.key==key }
    @Test fun shippedBetaAndClassicNeverBecomeAvailableEvenAtLevelSixty() {
        val p=samplePackage();val report=e06Engine(p).evaluate(e06Personal(60,"UNDEAD"))
        assertEquals(21,report.all.size);assertTrue(report.all.all { it.availability==Availability.UNVERIFIED && it.riskLabel==null && it.unlockCount==0 })
        assertTrue(report.now.isEmpty());assertTrue(report.leave.isEmpty());assertFalse(report.leaveUncertain.isEmpty())
        assertTrue(report.all.first { it.entry.key==E6_ITEM }.relatedKeys.containsAll(setOf("wow-forever:quest:792","wow-forever:quest:1499")))
    }
    @Test fun lowLevelAndRaceChangeRecomputeAvailabilityWithoutCallingItMissed() {
        assertEquals(Availability.LATER,advice(E6_A,e06Personal(1)).availability)
        assertEquals(Availability.AVAILABLE,advice(E6_A,e06Personal(5,"TROLL")).availability)
        assertEquals(Availability.EXCLUDED,advice(E6_A,e06Personal(5,"UNDEAD")).availability)
        assertEquals(Availability.AVAILABLE,advice(E6_A,e06Personal(60)).availability)
        assertNull(advice(E6_A,e06Personal(60)).riskLabel)
    }
    @Test fun factionClassAndAreaConditionsExplainAConcreteMismatch() {
        for(c in listOf(Faction(listOf("ALLIANCE")),PlayerClass(listOf("MAGE")))) {
            val p=e06Fixture().copy(guideRules=listOf(e06Rule(E6_A,c)))
            assertEquals(Availability.EXCLUDED,advice(E6_A,pack=p).availability)
        }
        val p=e06Fixture().copy(guideRules=listOf(e06Rule(E6_A,Area(listOf(1454)))))
        assertEquals(Availability.LATER,advice(E6_A,pack=p).availability)
    }
    @Test fun optionalModeAndProfessionMatterOnlyForRulesThatRequireThem() {
        val personal=e06Personal().copy(quests=listOf(e06Completed(E6_A)))
        assertEquals(Availability.UNVERIFIED,advice(E6_B,personal).availability)
        assertEquals(Availability.AVAILABLE,advice(E6_NPC,personal).availability)
        val selected=personal.copy(profile=personal.profile.copy(ruleset=" HARDCORE ",professionsJson="[\"tailoring\"]"))
        assertEquals(Availability.AVAILABLE,advice(E6_B,selected).availability)
        assertEquals(Availability.MISSING,advice(E6_B,selected.copy(profile=selected.profile.copy(professionsJson="[\"Mining\"]"))).availability)
    }
    @Test fun allAndAnyUseOnlyCompletedRecordsAndIgnoreDeferredAsACompletion() {
        val deferred=e06Personal().copy(quests=listOf(e06Completed(E6_A,QuestState.DEFERRED)))
        assertEquals(Availability.MISSING,advice(E6_C,deferred).availability)
        val acquired=deferred.copy(items=listOf(CollectedItemEntity(DEFAULT_PROFILE_ID,E6_ITEM,"Hůl",1)))
        assertEquals(Availability.AVAILABLE,advice(E6_C,acquired).availability)
        val all=e06Fixture().copy(guideRules=listOf(e06Rule(E6_C,All(listOf(QuestCompleted(E6_A),ItemAcquired(E6_ITEM))))))
        assertEquals(Availability.MISSING,advice(E6_C,acquired,all).availability)
        assertEquals(Availability.AVAILABLE,advice(E6_C,acquired.copy(quests=listOf(e06Completed(E6_A))),all).availability)
    }
    @Test fun nestedAnyWithAnUnknownAlternativeCannotProveMissingAccess() {
        val condition=All(listOf(Faction(listOf("HORDE")),AnyOf(listOf(QuestCompleted(E6_A),Unknown("Nedoložená jiná cesta")))))
        val pack=e06Fixture().copy(guideRules=listOf(e06Rule(E6_C,condition)))
        assertEquals(Availability.UNVERIFIED,advice(E6_C,pack=pack).availability)
        assertEquals(Availability.AVAILABLE,advice(E6_C,e06Personal().copy(quests=listOf(e06Completed(E6_A))),pack).availability)
    }
    @Test fun rewardSelectionDoesNotEqualAcquisitionAndOtherRewardExcludesTheVariant() {
        val base=e06Fixture()
        val pack=base.copy(guideRules=listOf(e06Rule(E6_A,RewardSelected("wow-forever:quest:792",E6_ITEM))),relations=base.relations.map { if(it.kind==RelationKind.REWARD_CHOICE)it.copy(verification=Verification.RELEASE,routingEnabled=true,sourceIds=listOf(E6_PROOF)) else it })
        ContentValidator.validate(pack)
        val p=e06Personal().copy(items=listOf(CollectedItemEntity(DEFAULT_PROFILE_ID,E6_ITEM,"Hůl",1)))
        assertEquals(Availability.UNVERIFIED,advice(E6_A,p,pack).availability)
        val reward=RewardChoiceEntity(DEFAULT_PROFILE_ID,"wow-forever:quest:792",E6_ITEM,"Úkol","Hůl",1)
        assertEquals(Availability.AVAILABLE,advice(E6_A,p.copy(items=emptyList(),rewards=listOf(reward)),pack).availability)
        assertEquals(Availability.EXCLUDED,advice(E6_A,p.copy(rewards=listOf(reward.copy(itemKey="wow-forever:item:999"))),pack).availability)
    }
    @Test fun completedAcquiredAndDiscoveredEntriesLeaveTheListsWhileDeferredRemainSeparate() {
        val p=e06Personal().copy(quests=listOf(e06Completed(E6_A),e06Completed(E6_C,QuestState.DEFERRED)),items=listOf(CollectedItemEntity(DEFAULT_PROFILE_ID,E6_ITEM,"Hůl",1)),
            bookmarks=listOf(BookmarkEntity(DEFAULT_PROFILE_ID,E6_ITEM,"Hůl","ITEM",1)),travel=listOf(DiscoveredTravelEntity(DEFAULT_PROFILE_ID,"wow-forever:travel:durotar-undercity","Cesta",1)))
        val r=e06Engine().evaluate(p)
        assertTrue(r.all.none { it.entry.key in setOf(E6_A,E6_ITEM,"wow-forever:travel:durotar-undercity") })
        assertTrue(r.now.none { it.entry.key==E6_C });assertTrue(r.leave.none { it.entry.key==E6_C });assertTrue(r.deferred.any { it.entry.key==E6_C })
    }
    @Test fun unverifiedRuleAndUnverifiedPrerequisiteCannotBecomeConfirmedWarnings() {
        val p=e06Fixture();val classic=p.copy(guideRules=p.guideRules.map { it.copy(verification=Verification.CLASSIC,window=GuideWindow(1,100,"Europe/Prague")) })
        val r=e06Engine(classic).evaluate(e06Personal(1),nowMillis=150)
        assertTrue(r.all.all { it.availability==Availability.UNVERIFIED && it.riskLabel==null })
        val prereq=RelationRecord("wow-forever:relation:test-pre",RelationKind.PREREQUISITE,E6_A,listOf(E6_B),MatchMode.ANY,Verification.CLASSIC,false,listOf(E6_PROOF))
        assertEquals(Availability.UNVERIFIED,advice(E6_A,pack=p.copy(relations=p.relations+prereq)).availability)
    }
    @Test fun exactDeadlineStopsUrgentRecommendationAndTimezoneIsExplicit() {
        val pack=e06Fixture().copy(guideRules=listOf(e06Rule(E6_A).copy(window=GuideWindow(50,100,"Europe/Prague"))))
        val engine=e06Engine(pack)
        assertEquals(Availability.LATER,engine.evaluate(e06Personal(),nowMillis=49).all.first{it.entry.key==E6_A}.availability)
        assertEquals(50L,engine.evaluate(e06Personal(),nowMillis=49).nextRefreshAtMillis)
        val active=engine.evaluate(e06Personal(),nowMillis=50);assertEquals("Časově omezené",active.all.first{it.entry.key==E6_A}.riskLabel);assertEquals(100L,active.nextRefreshAtMillis)
        val expired=engine.evaluate(e06Personal(),nowMillis=100);val a=expired.all.first{it.entry.key==E6_A}
        assertEquals(Availability.EXCLUDED,a.availability);assertNull(a.riskLabel);assertNull(expired.nextRefreshAtMillis);assertTrue(a.explanation.any{it.contains("Europe/Prague")})
    }
    @Test fun exclusiveGroupBlocksConfirmedOtherBranchAndDeferredDoesNot() {
        val relation=RelationRecord("wow-forever:relation:exclusive-e06",RelationKind.EXCLUSIVE_QUESTS,E6_A,listOf(E6_C),MatchMode.ANY,Verification.RELEASE,true,listOf(E6_PROOF))
        val base=e06Fixture();val pack=base.copy(relations=base.relations+relation,guideRules=base.guideRules.map { if(it.entryKey==E6_A)it.copy(exclusiveRelationKey=relation.key) else it })
        assertEquals("Výlučná volba",advice(E6_A,pack=pack).riskLabel)
        assertEquals(Availability.EXCLUDED,advice(E6_A,e06Personal().copy(quests=listOf(e06Completed(E6_C))),pack).availability)
        assertEquals(Availability.AVAILABLE,advice(E6_A,e06Personal().copy(quests=listOf(e06Completed(E6_C,QuestState.DEFERRED))),pack).availability)
    }
    @Test fun rankingKeepsClassAndRiskBeforeUserPreferenceAndPersistsStableTies() {
        val engine=e06Engine();val p=e06Personal()
        val explore=engine.evaluate(p,GuideFocus.EXPLORATION).now
        val equipment=engine.evaluate(p,GuideFocus.EQUIPMENT).now
        assertEquals(E6_A,explore.first().entry.key);assertEquals(E6_A,equipment.first().entry.key)
        assertTrue(explore.indexOfFirst { it.entry.key==E6_NPC }<explore.indexOfFirst { it.entry.key==E6_ITEM })
        assertTrue(equipment.indexOfFirst { it.entry.key==E6_ITEM }<equipment.indexOfFirst { it.entry.key==E6_NPC })
        assertEquals(equipment,engine.evaluate(p,GuideFocus.EQUIPMENT).now)
    }
    @Test fun missingAllAndAlreadySatisfiedAnyDoNotInflateSuccessorCount() {
        val p=e06Fixture();val rel=RelationRecord("wow-forever:relation:count-e06",RelationKind.PREREQUISITE,E6_C,listOf(E6_A,E6_B),MatchMode.ALL,Verification.RELEASE,true,listOf(E6_PROOF))
        val pack=p.copy(relations=p.relations+rel,guideRules=p.guideRules.map { if(it.entryKey==E6_C)it.copy(condition=Always) else it })
        assertEquals(0,advice(E6_A,pack=pack).unlockCount)
        val otherDone=e06Personal().copy(quests=listOf(e06Completed(E6_B)))
        assertEquals(1,advice(E6_A,otherDone,pack).unlockCount)
        assertEquals(0,advice(E6_A,otherDone,pack.copy(relations=p.relations+rel.copy(mode=MatchMode.ANY))).unlockCount)
    }
    @Test fun personalWishWithUnknownAreaStaysVisibleWithoutPretendingToBeLocated() {
        val key="wow-forever:travel:durotar-undercity";val p=e06Personal().copy(bookmarks=listOf(BookmarkEntity(DEFAULT_PROFILE_ID,key,"Doprava","TRAVEL",1)))
        val report=e06Engine(samplePackage()).evaluate(p)
        val a=report.personal.single();assertTrue(a.areaIds.isEmpty());assertEquals(Availability.UNVERIFIED,a.availability);assertTrue(report.leaveUncertain.contains(a))
    }
    @Test fun historyWishOutsideTheCatalogKeepsItsNameButNeverBecomesAvailable() {
        val key="wow-forever:item:990001"
        val p=e06Personal().copy(bookmarks=listOf(BookmarkEntity(DEFAULT_PROFILE_ID,key,"Staré přání","ITEM",1)))
        val a=e06Engine().evaluate(p).personal.single()
        assertEquals(key,a.entry.key);assertEquals("Staré přání",a.entry.nameOriginal);assertEquals(Availability.UNVERIFIED,a.availability);assertTrue(a.sourceIds.isEmpty())
    }

}
