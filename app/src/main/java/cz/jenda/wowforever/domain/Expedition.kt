package cz.jenda.wowforever.domain

import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.data.ProfileBackup
import cz.jenda.wowforever.data.db.*

data class ExpeditionStop(val stored:PlannedStopEntity,val number:Int,val name:String,val location:LocationRecord?,val limitation:String?)
data class ExpeditionLeg(val from:Int,val to:Int,val status:LegStatus,val edges:List<TravelEdge> = emptyList())
enum class LegStatus { VERIFIED, SAME_PLACE, NO_POSITION, NO_NODE, NO_VERIFIED_PATH }

fun resolveStops(rows:List<PlannedStopEntity>,entries:List<EntryRecord>,locations:List<LocationRecord>):List<ExpeditionStop> {
    val catalog=entries.associateBy { it.key };val positions=locations.groupBy { it.entryKey }
    return rows.sortedBy { it.position }.mapIndexed { index,row ->
        val entry=catalog[row.entryKey];val places=positions[row.entryKey].orEmpty()
        val limitation=when {
            entry==null -> "Položka momentálně chybí v katalogu."
            places.isEmpty() -> "Přímá poloha není doložená. Vyber konkrétní NPC v souvisejících záznamech."
            places.size>1 -> "Více poloh; pro tento cíl zatím nelze zvolit jediný mapový bod."
            entry.verification==Verification.CLASSIC -> "Classic poloha — ve Forever čeká na ověření. Spojení se nepočítá jako ověřené."
            entry.verification==Verification.COMMUNITY && entry.referencePosition -> "Komunitní poloha — ověř ve hře. Spojení se nepočítá jako ověřené."
            entry.verification !in setOf(Verification.BETA,Verification.RELEASE) -> "Poloha čeká na ověření zdroje."
            else -> null
        }
        val location=places.singleOrNull()?.takeIf { entry?.verification in setOf(Verification.BETA,Verification.RELEASE,Verification.CLASSIC) || (entry?.verification==Verification.COMMUNITY && entry.referencePosition) }
        ExpeditionStop(row,index+1,entry?.nameOriginal ?: row.nameSnapshot,location,limitation)
    }
}

/** No geometric shortcuts. Breadth-first search minimizes the number of proved directed connections. */
class TransportRouter(private val graph:TransportGraph) {
    fun path(from:String,to:String,personal:ProfileBackup):List<TravelEdge>? {
        val nodes=graph.nodes.associateBy { it.key }
        if(from !in nodes || to !in nodes)return null
        if(from==to)return emptyList()
        val discovered=personal.travel.map { it.travelKey }.toSet()
        val adjacency=graph.edges.filter { e ->
            e.verification==Verification.RELEASE && allowed(e.condition,personal) &&
                (e.mode!=TravelMode.FLIGHT || listOf(e.from,e.to).all { nodes[it]?.discoveryKey?.let(discovered::contains)==true })
        }.sortedBy { it.key }.groupBy { it.from }
        val queue=ArrayDeque<String>();queue.add(from)
        val reached=mutableSetOf(from);val previous=mutableMapOf<String,TravelEdge>()
        while(queue.isNotEmpty()) {
            val current=queue.removeFirst()
            for(edge in adjacency[current].orEmpty()) {
                if(!reached.add(edge.to))continue
                previous[edge.to]=edge
                if(edge.to==to) {
                    val route=mutableListOf<TravelEdge>();var id=to
                    while(id!=from) { val step=previous.getValue(id);route.add(step);id=step.from }
                    return route.reversed()
                }
                queue.add(edge.to)
            }
        }
        return null
    }
    fun legs(stops:List<ExpeditionStop>,personal:ProfileBackup):List<ExpeditionLeg> = stops.zipWithNext().map { (a,b) ->
        if(a.location==null || b.location==null) ExpeditionLeg(a.number,b.number,LegStatus.NO_POSITION)
        else if(a.limitation!=null || b.limitation!=null) ExpeditionLeg(a.number,b.number,LegStatus.NO_VERIFIED_PATH)
        else if(a.location.key==b.location.key) ExpeditionLeg(a.number,b.number,LegStatus.SAME_PLACE)
        else {
            val from=graph.nodes.firstOrNull { it.locationKey==a.location.key };val to=graph.nodes.firstOrNull { it.locationKey==b.location.key }
            if(from==null || to==null)ExpeditionLeg(a.number,b.number,LegStatus.NO_NODE)
            else path(from.key,to.key,personal)?.let { ExpeditionLeg(a.number,b.number,LegStatus.VERIFIED,it) }
                ?: ExpeditionLeg(a.number,b.number,LegStatus.NO_VERIFIED_PATH)
        }
    }
    private fun allowed(c:GuideCondition,p:ProfileBackup):Boolean {
        fun match(value:String?,values:List<String>)=value!=null && values.any { normalizeSearch(it.trim())==normalizeSearch(value.trim()) }
        return when(c) {
            GuideCondition.Always -> true
            is GuideCondition.Unknown -> false
            is GuideCondition.All -> c.conditions.isNotEmpty() && c.conditions.all { allowed(it,p) }
            is GuideCondition.AnyOf -> c.conditions.any { allowed(it,p) }
            is GuideCondition.Faction -> match(p.profile.faction,c.values)
            is GuideCondition.PlayerClass -> match(p.profile.playerClass,c.values)
            is GuideCondition.Race -> match(p.profile.race,c.values)
            is GuideCondition.Level -> p.profile.level>=c.minimum && (c.maximum==null || p.profile.level<=c.maximum)
            is GuideCondition.Mode -> match(p.profile.ruleset,c.values)
            is GuideCondition.Profession -> runCatching { ContentJson.decodeFromString<List<String>>(p.profile.professionsJson) }.getOrDefault(emptyList()).any { match(it,c.values) }
            is GuideCondition.Area -> p.profile.currentUiMapId in c.uiMapIds
            is GuideCondition.QuestCompleted -> p.quests.any { it.questKey==c.questKey && it.state==QuestState.COMPLETED.name }
            is GuideCondition.ItemAcquired -> p.items.any { it.itemKey==c.itemKey }
            is GuideCondition.RewardSelected -> p.rewards.any { it.questKey==c.questKey && it.itemKey==c.itemKey }
        }
    }
}
