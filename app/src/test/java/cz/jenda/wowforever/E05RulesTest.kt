package cz.jenda.wowforever

import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.data.db.QuestState.*
import cz.jenda.wowforever.domain.*
import org.junit.Test
import org.junit.Assert.*

class E05RulesTest {
    private fun r(owner:String="q",targets:List<String> = listOf("a","b"),mode:MatchMode=MatchMode.ALL,
        verified:Boolean=true)=RelationRecord("r:$owner",RelationKind.PREREQUISITE,owner,targets,mode,
        if(verified) Verification.RELEASE else Verification.CLASSIC,verified,emptyList())
    @Test fun allRequiresEveryCompletedStepAndDeferredNeverCounts() {
        val rules=QuestRules(listOf(r()))
        for(state in listOf(NOT_STARTED,IN_PROGRESS,DEFERRED)) assertEquals(PrerequisiteStatus.MISSING,rules.status("q",mapOf("a" to COMPLETED,"b" to state)))
        assertEquals(PrerequisiteStatus.SATISFIED,rules.status("q",mapOf("a" to COMPLETED,"b" to COMPLETED)))
        assertEquals(PrerequisiteStatus.MISSING,rules.status("q",emptyMap()))
    }
    @Test fun anyUnlocksWithOneCompletedAlternativeAndRecomputesAfterUndo() {
        val rules=QuestRules(listOf(r(mode=MatchMode.ANY)))
        assertEquals(PrerequisiteStatus.SATISFIED,rules.status("q",mapOf("a" to DEFERRED,"b" to COMPLETED)))
        assertEquals(PrerequisiteStatus.MISSING,rules.status("q",mapOf("a" to DEFERRED,"b" to IN_PROGRESS)))
    }
    @Test fun classicAndDisabledRulesNeverPromoteAvailabilityToConfirmed() {
        val completed=mapOf("a" to COMPLETED,"b" to COMPLETED)
        assertEquals(PrerequisiteStatus.UNVERIFIED,QuestRules(listOf(r(verified=false))).status("q",completed))
        assertEquals(PrerequisiteStatus.UNVERIFIED,QuestRules(listOf(r().copy(routingEnabled=false))).status("q",completed))
        assertEquals(PrerequisiteStatus.UNVERIFIED,QuestRules(emptyList()).status("q",completed))
    }
    @Test fun exclusiveChoiceIsScopedToVerifiedGroupAndCompletion() {
        val rules=QuestRules(emptyList(),listOf(ExclusiveGroup(setOf("a","b"),true),ExclusiveGroup(setOf("b","c"),false)))
        assertEquals(setOf("a"),rules.excludedBy("b",mapOf("a" to COMPLETED,"c" to COMPLETED)))
        assertTrue(rules.excludedBy("b",mapOf("a" to DEFERRED)).isEmpty())
        assertTrue(rules.excludedBy("c",mapOf("b" to COMPLETED)).isEmpty())
    }
    @Test fun impactWalkHandlesBranchesCyclesAndSeparateQuestIds() {
        val rules=QuestRules(listOf(r("b",listOf("a")),r("c",listOf("b")),r("a",listOf("c")),r("d",listOf("a"))))
        assertEquals(setOf("b","c","d"),rules.descendants("a"))
        val real=QuestRules(samplePackage().relations)
        assertEquals(0,real.prerequisites("wow-forever:quest:1499",mapOf("wow-forever:quest:792" to COMPLETED)).single().completed)
        assertEquals(1,real.prerequisites("wow-forever:quest:1499",mapOf("wow-forever:quest:1485" to COMPLETED)).single().completed)
    }
}
