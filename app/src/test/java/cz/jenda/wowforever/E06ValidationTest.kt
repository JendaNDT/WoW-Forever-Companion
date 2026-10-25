package cz.jenda.wowforever

import cz.jenda.wowforever.content.*
import kotlinx.serialization.encodeToString
import org.junit.Test
import org.junit.Assert.*

class E06ValidationTest {
    private val p=e06Fixture()
    private fun rejected(rule:GuideRule) { assertThrows(InvalidContent::class.java){ContentValidator.validate(p.copy(guideRules=listOf(rule)))} }
    @Test fun missingProofMissingTargetDuplicateRuleAndUnknownReferencedQuestAreRejected() {
        val rule=p.guideRules.first()
        rejected(rule.copy(sourceIds=listOf("missing")))
        rejected(rule.copy(entryKey="wow-forever:quest:99999"))
        rejected(rule.copy(condition=GuideCondition.QuestCompleted("wow-forever:quest:99999")))
        rejected(rule.copy(condition=GuideCondition.ItemAcquired(E6_A)))
        assertThrows(InvalidContent::class.java){ContentValidator.validate(p.copy(guideRules=p.guideRules+p.guideRules.first()))}
        val noProof=p.copy(sources=p.sources.map { if(it.id==E6_PROOF)it.copy(kind="classic_reference") else it })
        assertThrows(InvalidContent::class.java){ContentValidator.validate(noProof)}
    }
    @Test fun incompleteReferenceCannotEnterAnActiveConfirmedRule() {
        val rule=p.guideRules.first().copy(condition=GuideCondition.QuestCompleted(E6_B))
        val pack=p.copy(guideRules=listOf(rule),entries=p.entries.map { if(it.key==E6_B)it.copy(verification=Verification.CLASSIC,releaseConfirmed=false) else it })
        assertThrows(InvalidContent::class.java){ContentValidator.validate(pack)}
        rejected(rule.copy(condition=GuideCondition.RewardSelected("wow-forever:quest:792",E6_ITEM))) // original reward relation is still a beta reference
    }
    @Test fun emptyGroupInvalidRangeBadWindowAndUnprovedRiskAreRejected() {
        val rule=p.guideRules.first()
        for(condition in listOf(GuideCondition.All(emptyList()),GuideCondition.AnyOf(emptyList()),GuideCondition.Level(12,5),GuideCondition.Area(listOf(9999)),GuideCondition.Mode(listOf(" "))))rejected(rule.copy(condition=condition))
        rejected(rule.copy(window=GuideWindow(200,100,"Europe/Prague")))
        rejected(rule.copy(window=GuideWindow(1,100,"Unknown/Zone")))
        rejected(rule.copy(exclusiveRelationKey=p.relations.first().key))
    }
    @Test fun recursiveConditionSizeAndDepthAndSerializedUnknownTypeAreBounded() {
        val rule=p.guideRules.first()
        var condition:GuideCondition=GuideCondition.Always
        repeat(10){condition=GuideCondition.All(listOf(condition))};rejected(rule.copy(condition=condition))
        rejected(rule.copy(condition=GuideCondition.All(List(201){GuideCondition.Always})))
        val text=ContentJson.encodeToString(p).replace("\"type\":\"all\"","\"type\":\"unexpected\"")
        assertThrows(Exception::class.java){decodePackage(text.encodeToByteArray())}
        assertThrows(IllegalArgumentException::class.java){decodePackage(("[".repeat(40)+"]".repeat(40)).encodeToByteArray())}
    }
}
