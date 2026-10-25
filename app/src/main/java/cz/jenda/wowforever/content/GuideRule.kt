package cz.jenda.wowforever.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.ZoneId

@Serializable enum class Importance { CLASS, UNLOCK, REWARD, DUNGEON, TRAVEL, DISCOVERY }
@Serializable sealed class GuideCondition {
    @Serializable @SerialName("always") data object Always:GuideCondition()
    @Serializable @SerialName("unknown") data class Unknown(val reasonCs:String):GuideCondition()
    @Serializable @SerialName("all") data class All(val conditions:List<GuideCondition>):GuideCondition()
    @Serializable @SerialName("any") data class AnyOf(val conditions:List<GuideCondition>):GuideCondition()
    @Serializable @SerialName("faction") data class Faction(val values:List<String>):GuideCondition()
    @Serializable @SerialName("class") data class PlayerClass(val values:List<String>):GuideCondition()
    @Serializable @SerialName("race") data class Race(val values:List<String>):GuideCondition()
    @Serializable @SerialName("level") data class Level(val minimum:Int,val maximum:Int?=null):GuideCondition()
    @Serializable @SerialName("mode") data class Mode(val values:List<String>):GuideCondition()
    @Serializable @SerialName("profession") data class Profession(val values:List<String>):GuideCondition()
    @Serializable @SerialName("area") data class Area(val uiMapIds:List<Int>):GuideCondition()
    @Serializable @SerialName("quest_completed") data class QuestCompleted(val questKey:String):GuideCondition()
    @Serializable @SerialName("item_acquired") data class ItemAcquired(val itemKey:String):GuideCondition()
    @Serializable @SerialName("reward_selected") data class RewardSelected(val questKey:String,val itemKey:String):GuideCondition()
}
@Serializable data class GuideWindow(val fromEpochMillis:Long,val untilEpochMillis:Long,val timeZone:String)
/** Conditions explicitly describe the complete access rule; Always must be backed by evidence too. */
@Serializable data class GuideRule(val key:String,val entryKey:String,val condition:GuideCondition,
    val importance:Importance,val reasonCs:String,val verification:Verification,val sourceIds:List<String>,
    val areaIds:List<Int> = emptyList(),val window:GuideWindow?=null,val exclusiveRelationKey:String?=null)

object GuideRuleValidator {
    fun validate(p:ContentPackage) {
        fun check(ok:Boolean) { require(ok){"Neplatné pravidlo doporučení."} }
        val entries=p.entries.associateBy { it.key };val sources=p.sources.associateBy { it.id };val maps=p.maps.map { it.uiMapId }.toSet()
        check(p.guideRules.size<=6000 && p.guideRules.map { it.key }.distinct().size==p.guideRules.size && p.guideRules.map { it.entryKey }.distinct().size==p.guideRules.size)
        p.guideRules.forEach { r ->
            val owner=entries[r.entryKey];check(r.key.matches(Regex("wow-forever:rule:[A-Za-z0-9][A-Za-z0-9._:-]*")) && r.key.length<=200 && owner!=null)
            check(r.reasonCs.isNotBlank() && r.reasonCs.length<=1000 && r.sourceIds.isNotEmpty() && r.sourceIds.distinct().size==r.sourceIds.size && r.sourceIds.all { it in sources })
            check(r.areaIds.distinct().size==r.areaIds.size && r.areaIds.all { it in maps })
            if(r.verification==Verification.RELEASE)check(owner?.releaseConfirmed==true && owner.verification==Verification.RELEASE && r.sourceIds.any { sources[it]?.kind=="in_game_release_observation" })
            r.window?.let { check(it.fromEpochMillis>=0 && it.untilEpochMillis>it.fromEpochMillis && runCatching{ZoneId.of(it.timeZone)}.isSuccess) }
            r.exclusiveRelationKey?.let { key ->
                val relation=p.relations.firstOrNull { it.key==key };check(relation?.kind==RelationKind.EXCLUSIVE_QUESTS && (r.entryKey==relation.ownerKey || r.entryKey in relation.targetKeys))
                if(r.verification==Verification.RELEASE)check(relation?.routingEnabled==true && relation.verification==Verification.RELEASE)
            }
            var nodes=0
            fun values(v:List<String>) { check(v.isNotEmpty() && v.size<=50 && v.distinct().size==v.size && v.all { it.isNotBlank() && it.length<=160 }) }
            fun target(key:String,kind:EntryKind) {
                val entry=entries[key];check(entry?.kind==kind)
                if(r.verification==Verification.RELEASE)check(entry?.verification==Verification.RELEASE && entry.releaseConfirmed)
            }
            fun condition(c:GuideCondition,depth:Int) {
                nodes++;check(nodes<=200 && depth<=8)
                when(c) {
                    GuideCondition.Always -> Unit
                    is GuideCondition.Unknown -> check(c.reasonCs.isNotBlank() && c.reasonCs.length<=500)
                    is GuideCondition.All -> { check(c.conditions.isNotEmpty());c.conditions.forEach { condition(it,depth+1) } }
                    is GuideCondition.AnyOf -> { check(c.conditions.isNotEmpty());c.conditions.forEach { condition(it,depth+1) } }
                    is GuideCondition.Faction -> { values(c.values);check(c.values.all { it in setOf("HORDE","ALLIANCE") }) }
                    is GuideCondition.PlayerClass -> { values(c.values);check(c.values.all { it.matches(Regex("[A-Z][A-Z_]*")) }) }
                    is GuideCondition.Race -> { values(c.values);check(c.values.all { it.matches(Regex("[A-Z][A-Z_]*")) }) }
                    is GuideCondition.Level -> check(c.minimum in 1..60 && (c.maximum==null || c.maximum in c.minimum..60))
                    is GuideCondition.Mode -> values(c.values)
                    is GuideCondition.Profession -> values(c.values)
                    is GuideCondition.Area -> check(c.uiMapIds.isNotEmpty() && c.uiMapIds.distinct().size==c.uiMapIds.size && c.uiMapIds.all { it in maps })
                    is GuideCondition.QuestCompleted -> target(c.questKey,EntryKind.QUEST)
                    is GuideCondition.ItemAcquired -> target(c.itemKey,EntryKind.ITEM)
                    is GuideCondition.RewardSelected -> {
                        target(c.questKey,EntryKind.QUEST);target(c.itemKey,EntryKind.ITEM)
                        check(p.relations.any { it.kind==RelationKind.REWARD_CHOICE && it.ownerKey==c.itemKey && c.questKey in it.targetKeys && (r.verification!=Verification.RELEASE || (it.verification==Verification.RELEASE && it.routingEnabled)) })
                    }
                }
            }
            condition(r.condition,0)
        }
    }
}
