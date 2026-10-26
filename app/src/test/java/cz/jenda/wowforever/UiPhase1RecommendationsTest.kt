package cz.jenda.wowforever

import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.data.db.QuestState
import cz.jenda.wowforever.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.zip.GZIPInputStream

class UiPhase1RecommendationsTest {
    @Test fun referenceQuestsBecomeUsefulWithoutBecomingConfirmed() {
        val p=samplePackage()
        val report=e06Engine(p).evaluate(e06Personal(5))
        val suggestions=report.questSuggestions(5)
        assertTrue(suggestions.current.isNotEmpty())
        assertTrue(suggestions.current.all { it.availability==Availability.UNVERIFIED })
        assertTrue(report.now.isEmpty())
        assertTrue(suggestions.current.all { it.entry.kind in setOf(EntryKind.QUEST,EntryKind.SPELL) && it.suggestedLevel!!<=7 })
        assertTrue(suggestions.current.none { it.areaIds.isEmpty() })
    }
    @Test fun shippedDataOneNeverSuggestsLevelSixtyAndKeepsAllVerification() {
        val p=GZIPInputStream(File("../content/e10-v8.json.gz").inputStream()).use { decodePackage(it) }
        val report=e06Engine(p).evaluate(e06Personal(1))
        val suggestions=report.questSuggestions(1)
        assertTrue("Data 8 must offer a starting quest",suggestions.current.isNotEmpty())
        assertTrue(suggestions.current.all { it.suggestedLevel!!<=3 && 1411 in it.areaIds })
        assertTrue(suggestions.current.none { it.entry.nameOriginal.startsWith("Doomcaller's") })
        assertTrue(suggestions.current.none { it.entry.kind in setOf(EntryKind.ITEM,EntryKind.NPC) })
        assertTrue(report.all.none { it.availability==Availability.AVAILABLE })
        assertTrue(report.all.all { !it.entry.releaseConfirmed })
        println("UI1 data8 level1: ${suggestions.current.size} current, ${suggestions.soon.size} soon, ${suggestions.unknown.size} unknown")
    }
    @Test fun futureAndUnknownReferencesStaySeparateAndCompletedOrDeferredStayOut() {
        val p=samplePackage()
        val base=p.entries.first { it.kind==EntryKind.QUEST }
        val entries=listOf(
            base.copy(key="wow-forever:quest:900001",reportedLevel=2,minimumLevel=1),
            base.copy(key="wow-forever:quest:900002",reportedLevel=5,minimumLevel=1),
            base.copy(key="wow-forever:quest:900003",reportedLevel=60,minimumLevel=1),
            base.copy(key="wow-forever:quest:900004",reportedLevel=null,minimumLevel=null),
            base.copy(key="wow-forever:quest:900005",reportedLevel=1,minimumLevel=1))
        val engine=RecommendationEngine(entries,emptyList(),emptyList(),entries.associate { it.key to setOf(1411) })
        val report=engine.evaluate(e06Personal(1))
        assertEquals(listOf(entries[4].key,entries[0].key),report.questSuggestions(1).current.map { it.entry.key })
        assertEquals(entries[1].key,report.questSuggestions(1).soon.single().entry.key)
        assertEquals(entries[3].key,report.questSuggestions(1).unknown.single().entry.key)
        val personal=e06Personal(1).copy(quests=listOf(e06Completed(entries[0].key),e06Completed(entries[4].key,QuestState.DEFERRED)))
        assertTrue(engine.evaluate(personal).questSuggestions(1).current.isEmpty())
        assertTrue(engine.evaluate(e06Personal(60)).questSuggestions(60).soon.isEmpty())
    }
    @Test fun incompatibleRaceDoesNotEnterReferencesAndExplicitPersonalTargetsRemain() {
        val fixture=e06Fixture().copy(entries=e06Fixture().entries.map { it.copy(verification=Verification.CLASSIC,releaseConfirmed=false) },
            guideRules=e06Fixture().guideRules.map { it.copy(verification=Verification.CLASSIC) })
        val wrongRace=e06Engine(fixture).evaluate(e06Personal(5,"UNDEAD"))
        assertEquals(Availability.UNVERIFIED,wrongRace.all.first { it.entry.key==E6_A }.availability)
        assertTrue(wrongRace.questSuggestions(5).current.none { it.entry.key==E6_A })
        val personal=e06Personal(1).copy(quests=listOf(e06Completed(E6_A,QuestState.IN_PROGRESS)))
        assertEquals(E6_A,e06Engine(fixture).evaluate(personal).questSuggestions(1).personal.single().entry.key)
    }
}
