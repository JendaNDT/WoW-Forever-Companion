package cz.jenda.wowforever.content

import kotlinx.serialization.Serializable

@Serializable enum class TravelMode { WALK, FLIGHT, BOAT, ZEPPELIN, PORTAL, ENTRANCE }
@Serializable data class TravelNode(val key:String,val nameCs:String,val locationKey:String,
    val sourceIds:List<String>,val discoveryKey:String?=null)
/** Every edge is directed. A reverse journey needs its own evidence and record. */
@Serializable data class TravelEdge(val key:String,val entryKey:String,val from:String,val to:String,
    val mode:TravelMode,val condition:GuideCondition,val verification:Verification,val sourceIds:List<String>,
    val estimatedSeconds:Int?=null)
@Serializable data class TransportGraph(val nodes:List<TravelNode> = emptyList(),val edges:List<TravelEdge> = emptyList())

object TransportValidator {
    fun validate(p:ContentPackage) {
        val graph=p.transportGraph;val nodes=graph.nodes.associateBy { it.key }
        val locations=p.locations.associateBy { it.key };val entries=p.entries.associateBy { it.key };val sources=p.sources.associateBy { it.id }
        fun check(value:Boolean) { require(value){"Neplatný dopravní graf."} }
        fun key(value:String,type:String)=value.matches(Regex("wow-forever:$type:[A-Za-z0-9][A-Za-z0-9._:-]*")) && value.length<=200
        fun evidence(ids:List<String>)=ids.size in 1..50 && ids.distinct().size==ids.size && ids.all { it in sources && it.length<=160 }
        fun release(ids:List<String>)=ids.any { sources[it]?.kind=="in_game_release_observation" }
        check(graph.nodes.size<=1000 && graph.edges.size<=1000)
        check(nodes.size==graph.nodes.size && graph.nodes.map { it.locationKey }.distinct().size==graph.nodes.size)
        check(graph.edges.map { it.key }.distinct().size==graph.edges.size)
        // Each directed connection has its own stable travel entry and access rule.
        check(graph.edges.map { it.entryKey }.distinct().size==graph.edges.size)
        graph.nodes.forEach { n ->
            check(key(n.key,"travel-node") && n.nameCs.isNotBlank() && n.nameCs.length<=200 && n.locationKey in locations && evidence(n.sourceIds))
            n.discoveryKey?.let { check(entries[it]?.kind==EntryKind.TRAVEL) }
        }
        graph.edges.forEach { e ->
            check(key(e.key,"travel-edge") && e.from in nodes && e.to in nodes && e.from!=e.to && entries[e.entryKey]?.kind==EntryKind.TRAVEL && evidence(e.sourceIds))
            check(e.estimatedSeconds==null || e.estimatedSeconds in 1..86400)
            val from=nodes.getValue(e.from);val to=nodes.getValue(e.to)
            if(e.mode==TravelMode.FLIGHT) check(from.discoveryKey!=null && to.discoveryKey!=null)
            val a=locations.getValue(from.locationKey);val b=locations.getValue(to.locationKey)
            // A walk never silently jumps into another coordinate frame or floor.
            if(e.mode==TravelMode.WALK) check(a.mapKey==b.mapKey && a.floorId==b.floorId)
            if(e.verification==Verification.RELEASE) {
                check(release(e.sourceIds))
                listOf(from,to).forEach { node ->
                    val l=locations.getValue(node.locationKey);val owner=entries.getValue(l.entryKey)
                    check(owner.releaseConfirmed && owner.verification==Verification.RELEASE && release(node.sourceIds) && release(listOf(l.sourceId)))
                    node.discoveryKey?.let { check(entries[it]?.releaseConfirmed==true && entries[it]?.verification==Verification.RELEASE) }
                }
            }
        }
        GuideRuleValidator.validate(p.copy(guideRules=graph.edges.mapIndexed { i,e ->
            GuideRule("wow-forever:rule:transport:$i",e.entryKey,e.condition,Importance.TRAVEL,"Podmínky dopravního úseku.",e.verification,e.sourceIds)
        }))
    }
}
