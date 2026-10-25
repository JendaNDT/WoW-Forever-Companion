package cz.jenda.wowforever.domain

import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.data.db.QuestState

enum class PrerequisiteStatus { SATISFIED, MISSING, UNVERIFIED }
data class ExclusiveGroup(val questKeys: Set<String>, val verified: Boolean)
data class PrerequisiteCheck(val relation: RelationRecord,val satisfied: Boolean,val completed: Int)

/** Completion and availability are different facts. No unverified rule unlocks a quest. */
class QuestRules(val relations: List<RelationRecord>,private val exclusives: List<ExclusiveGroup> = emptyList()) {
    private val prerequisitesByOwner=relations.filter { it.kind==RelationKind.PREREQUISITE }.groupBy { it.ownerKey }
    private val descendantsByTarget=relations.filter { it.kind==RelationKind.PREREQUISITE }.flatMap { r -> r.targetKeys.map { it to r.ownerKey } }.groupBy({it.first},{it.second})
    private val rewardsByQuest=relations.filter { it.kind==RelationKind.REWARD_CHOICE }.flatMap { r -> r.targetKeys.map { it to r } }.groupBy({it.first},{it.second})
    private val groupsByQuest=(exclusives+relations.filter { it.kind==RelationKind.EXCLUSIVE_QUESTS }.map {
        ExclusiveGroup((listOf(it.ownerKey)+it.targetKeys).toSet(),it.routingEnabled && it.verification==Verification.RELEASE)
    }).flatMap { g -> g.questKeys.map { it to g } }.groupBy({it.first},{it.second})
    fun prerequisites(quest: String, states: Map<String,QuestState>): List<PrerequisiteCheck> = prerequisitesByOwner[quest].orEmpty().map { r ->
        val n=r.targetKeys.count { states[it] == QuestState.COMPLETED }
        PrerequisiteCheck(r,if(r.mode == MatchMode.ALL) n == r.targetKeys.size else n > 0,n)
    }
    fun status(quest: String, states: Map<String,QuestState>): PrerequisiteStatus {
        val checks=prerequisites(quest,states)
        if(checks.any { it.relation.routingEnabled && it.relation.verification == Verification.RELEASE && !it.satisfied }) return PrerequisiteStatus.MISSING
        if(checks.isEmpty() || checks.any { !it.relation.routingEnabled || it.relation.verification != Verification.RELEASE }) return PrerequisiteStatus.UNVERIFIED
        return PrerequisiteStatus.SATISFIED
    }
    fun exclusiveGroups(quest:String): List<ExclusiveGroup> = groupsByQuest[quest].orEmpty()
    fun excludedBy(quest: String, states: Map<String,QuestState>): Set<String> = exclusiveGroups(quest).filter { it.verified }
        .flatMap { it.questKeys }.filter { it != quest && states[it] == QuestState.COMPLETED }.toSet()
    fun descendants(key: String): Set<String> {
        val found=mutableSetOf<String>();val queue=ArrayDeque<String>();queue.add(key)
        while(queue.isNotEmpty()) {
            val parent=queue.removeFirst()
            descendantsByTarget[parent].orEmpty().forEach {
                if(it != key && found.add(it)) queue.add(it)
            }
        }
        return found
    }
    fun rewardCandidates(quest: String): List<RelationRecord> = rewardsByQuest[quest].orEmpty()
}
