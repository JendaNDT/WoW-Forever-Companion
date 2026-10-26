package cz.jenda.wowforever.domain

import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.data.db.QuestState
import cz.jenda.wowforever.model.*

/** A role describes a sourced link, never a claim that the quest is available or ready. */
enum class MarkerRole(val symbol:String,val label:String) {
    START("!","Zadání úkolu"), END("?","Odevzdání úkolu"), TARGET("◆","Cíl úkolu nebo zdroj předmětu"),
    PERSON("•","Postava nebo místo"), NOTE("✎","Vlastní poznámka"), TRAVEL("↗","Doprava")
}
private data class PointRoleLink(val role:MarkerRole,val quest:String?)
class MapRoleIndex(relations:List<RelationRecord>) {
    private val links:Map<String,List<PointRoleLink>> = buildMap<String,MutableList<PointRoleLink>> {
        relations.forEach { r ->
            val role=when(r.kind) {
                RelationKind.START_NPC,RelationKind.START_OBJECT,RelationKind.START_ITEM->MarkerRole.START
                RelationKind.END_NPC,RelationKind.END_OBJECT->MarkerRole.END
                RelationKind.OBJECTIVE_NPC,RelationKind.OBJECTIVE_OBJECT,RelationKind.OBJECTIVE_ITEM,
                RelationKind.ITEM_SOURCE_NPC,RelationKind.ITEM_SOURCE_OBJECT->MarkerRole.TARGET
                else->null
            } ?: return@forEach
            val quest=r.ownerKey.takeIf { it.startsWith("wow-forever:quest:") }
            r.targetKeys.forEach { key->getOrPut(key){mutableListOf()}.add(PointRoleLink(role,quest)) }
        }
    }
    fun project(points:List<MapPoint>,kinds:Set<String>,bookmarksOnly:Boolean,bookmarks:Set<String>,
        states:Map<String,QuestState>,showCompleted:Boolean,planNumbers:Map<String,List<Int>>,revealed:String?=null):List<MapPoint> {
        val questsVisible=PointKind.QUEST.name in kinds
        return points.mapNotNull { p ->
            val explicit=p.id==revealed
            if(!explicit && bookmarksOnly && p.entryKey !in bookmarks)return@mapNotNull null
            if(!explicit && !showCompleted && p.kind==PointKind.QUEST && states[p.entryKey]==QuestState.COMPLETED)return@mapNotNull null
            val active=links[p.entryKey].orEmpty().filter { showCompleted || it.quest==null || states[it.quest]!=QuestState.COMPLETED }
            val questLinks=active.filter { it.quest!=null }
            if(!explicit && p.kind.name !in kinds && !(questsVisible && questLinks.isNotEmpty()))return@mapNotNull null
            val roles=if(questsVisible || explicit)active else active.filter { it.quest==null }
            val role=when {
                p.kind==PointKind.NOTE->MarkerRole.NOTE
                roles.any { it.role==MarkerRole.END && states[it.quest]==QuestState.IN_PROGRESS }->MarkerRole.END
                roles.any { it.role==MarkerRole.TARGET && it.quest!=null && states[it.quest]==QuestState.IN_PROGRESS }->MarkerRole.TARGET
                roles.any { it.role==MarkerRole.START && states[it.quest]!=QuestState.IN_PROGRESS }->MarkerRole.START
                roles.any { it.role==MarkerRole.END }->MarkerRole.END
                roles.any { it.role==MarkerRole.TARGET } || p.kind in setOf(PointKind.QUEST,PointKind.ITEM)->MarkerRole.TARGET
                roles.any { it.role==MarkerRole.START }->MarkerRole.START
                p.kind==PointKind.TRAVEL->MarkerRole.TRAVEL
                else->MarkerRole.PERSON
            }
            p.copy(markerRole=role,planNumbers=planNumbers[p.entryKey].orEmpty())
        }
    }
}

data class LeavingArea(val inProgress:List<Advice>,val classQuests:List<Advice>,val wishes:List<Advice>)
fun AdviceReport.beforeLeaving(level:Int):LeavingArea {
    val here=all.filter { currentArea in it.areaIds && !it.deferred }
    val progress=here.filter { it.inProgress }
    val classQuests=here.filter { !it.inProgress && it.entry.kind==EntryKind.QUEST && it.importance==Importance.CLASS &&
        it.referenceEligible && it.suggestedLevel?.let { n->n<=level+2 }==true }
    val used=(progress+classQuests).map { it.entry.key }.toSet()
    return LeavingArea(progress,classQuests,here.filter { it.wish && it.entry.key !in used })
}
