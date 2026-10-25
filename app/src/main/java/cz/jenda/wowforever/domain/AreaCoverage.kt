package cz.jenda.wowforever.domain

import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.model.*

data class AreaCoverage(val map:MapDescriptor,val npcCount:Int,val questCount:Int,val itemCount:Int,val travelCount:Int,val objectCount:Int=0)

/** Context through explicit NPC relations or an editorial guide area, never a guessed quest coordinate. */
fun areaCoverage(atlas:Atlas,entries:List<EntryRecord>,relations:List<RelationRecord>,rules:List<GuideRule>):List<AreaCoverage> {
    val catalog=entries.associateBy{it.key}
    val positions=atlas.points.groupBy { it.position.uiMapId to it.position.floorId }
    val questLocations=setOf(RelationKind.START_NPC,RelationKind.END_NPC,RelationKind.START_OBJECT,RelationKind.END_OBJECT)
    return atlas.maps.map { map ->
        val located=positions[map.uiMapId to map.floorId].orEmpty().mapNotNull{it.entryKey}.toSet()
        val questKeys=relations.filter { r -> r.kind in questLocations && r.targetKeys.any{it in located} }.map{it.ownerKey}.toSet()+
            rules.filter{map.uiMapId in it.areaIds && catalog[it.entryKey]?.kind==EntryKind.QUEST}.map{it.entryKey}
        val items=relations.filter{it.kind==RelationKind.REWARD_CHOICE && it.targetKeys.any{q->q in questKeys}}.map{it.ownerKey}.toSet()+
            relations.filter { it.ownerKey in questKeys && it.kind in setOf(RelationKind.QUEST_REWARD,RelationKind.OBJECTIVE_ITEM) }.flatMap { it.targetKeys }
        AreaCoverage(map,located.count{catalog[it]?.kind==EntryKind.NPC},questKeys.size,items.size,located.count{catalog[it]?.kind==EntryKind.TRAVEL},located.count { catalog[it]?.kind==EntryKind.OBJECT })
    }
}
