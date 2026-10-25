package cz.jenda.wowforever.data

import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.model.*

/** Explicit sourced positions only; Classic references remain visibly labelled. */
fun buildAtlas(maps: List<MapRecord>, entries: List<EntryRecord>, sources: Map<String,String>, locations: List<LocationRecord>): Atlas {
    val entryByKey = entries.associateBy { it.key }
    val mapByKey = maps.associateBy { it.key }
    val primary = locations.groupBy { it.entryKey }.mapValues { (_, list) -> list.minBy { it.key }.key }
    val points = locations.mapNotNull { l ->
        val e = entryByKey[l.entryKey] ?: return@mapNotNull null
        val m = mapByKey[l.mapKey] ?: return@mapNotNull null
        if (e.verification !in setOf(Verification.BETA, Verification.RELEASE, Verification.CLASSIC) && !(e.verification==Verification.COMMUNITY && e.referencePosition)) return@mapNotNull null
        val source = sources[l.sourceId] ?: return@mapNotNull null
        val id = if (l.key == primary[e.key]) e.key else "${e.key}@${l.key}"
        val name = e.nameOriginal + when(e.verification) { Verification.CLASSIC -> " · Classic"; Verification.COMMUNITY -> " · komunitní"; else -> "" }
        MapPoint(id, name, MapPosition(m.uiMapId,l.xPercent,l.yPercent,l.floorId), source,
            e.key,PointKind.valueOf(e.kind.name),l.precisionPercent,e.verification)
    }
    val order = listOf(947,1414,1411,1454,1413)
    return Atlas(maps.sortedWith(compareBy<MapRecord> { order.indexOf(it.uiMapId).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE }.thenBy { it.name }.thenBy { it.floorId }).map {
        MapDescriptor(it.uiMapId,it.name,it.width,it.height,it.levels,it.build,it.sourceUrl,it.transform,it.floorId,it.scopeCs)
    },points)
}
fun searchEntries(entries: List<EntryRecord>, query: String): List<EntryRecord> {
    return CatalogSearchIndex(entries).search(query)
}
fun filterPoints(points: List<MapPoint>, kinds: Set<String>, onlyBookmarks: Boolean, bookmarks: Set<String>) =
    points.filter { it.kind.name in kinds && (!onlyBookmarks || it.entryKey in bookmarks) }
