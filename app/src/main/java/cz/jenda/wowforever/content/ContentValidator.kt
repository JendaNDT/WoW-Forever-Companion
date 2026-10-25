package cz.jenda.wowforever.content

import java.net.URI
import java.time.LocalDate

class InvalidContent(val problems: List<String>) : IllegalArgumentException(problems.joinToString("; "))

object ContentValidator {
    fun validate(p: ContentPackage) {
        val errors = mutableListOf<String>()
        fun check(ok: Boolean, message: String) { if (!ok) errors += message }
        fun date(value: String) = runCatching { LocalDate.parse(value) }.isSuccess
        fun url(value: String) = runCatching { URI(value).let { it.scheme == "https" && !it.host.isNullOrBlank() } }.getOrDefault(false)
        fun key(value: String, type: String) = value.matches(Regex("wow-forever:$type:[A-Za-z0-9][A-Za-z0-9._:-]*"))
        fun unique(values: List<String>, label: String) = check(values.distinct().size == values.size, "Duplicitní $label")
        check(p.schemaVersion == 1, "Nepodporovaná verze formátu")
        check(p.packageId == CONTENT_PACKAGE_ID && p.gameVariant == "wow-forever", "Nesprávná varianta hry nebo balíček")
        check(p.generation > 0, "Neplatná generace")
        check(date(p.checkedOn), "Neplatné datum kontroly")
        check(p.maps.isNotEmpty() && p.entries.isNotEmpty() && p.sources.isNotEmpty(), "Prázdný katalog")
        unique(p.maps.map { it.key }, "mapy"); unique(p.entries.map { it.key }, "záznamy")
        unique(p.sources.map { it.id }, "zdroje"); unique(p.locations.map { it.key }, "polohy")
        unique(p.relations.map { it.key }, "vazby")
        unique(p.maps.map { "${it.uiMapId}:${it.floorId}:${it.coordinateVersion}" }, "souřadnicové rámce")
        val maps = p.maps.associateBy { it.key }; val entries = p.entries.associateBy { it.key }
        val sources = p.sources.map { it.id }.toSet()
        val sourceRecords = p.sources.associateBy { it.id }
        val mapIds=p.maps.map{it.uiMapId}.toSet()
        check(mapIds.containsAll(setOf(947,1414,1411,1454)) && SupportedMapFrames.ids.containsAll(mapIds) && p.maps.size==mapIds.size,
            "Balíček neodpovídá podporovaným mapám")
        fun references(ids: List<String>, owner: String) {
            check(ids.isNotEmpty() && ids.distinct().size == ids.size && ids.all { it in sources }, "Neplatné zdroje: $owner")
        }
        p.sources.forEach { check(it.id.isNotBlank() && url(it.url) && date(it.checkedOn), "Neplatný zdroj ${it.id}") }
        p.maps.forEach { m ->
            check(key(m.key, "map") && m.uiMapId > 0 && m.floorId >= 0, "Neplatná mapa ${m.key}")
            check(m.key == "wow-forever:map:${m.uiMapId}" && m.floorId == 0 && Triple(m.width,m.height,m.levels) == SupportedMapFrames.frames[m.uiMapId] && m.build == "1.60.1.69893",
                "Nepodporovaný podklad ${m.key}")
            check(m.width > 0 && m.height > 0 && m.levels in 1..12 && m.coordinateVersion.isNotBlank(), "Neplatný rámec ${m.key}")
            check(m.scopeCs==null || (m.scopeCs.isNotBlank() && m.scopeCs.length<=1000),"Neplatný popis pokrytí ${m.key}")
            check(m.sourceId in sources && url(m.sourceUrl), "Neplatný zdroj mapy ${m.key}")
            with(m.transform) { check(listOf(scaleX, scaleY, offsetX, offsetY).all { it.isFinite() } && scaleX > 0 && scaleY > 0, "Neplatná transformace ${m.key}") }
        }
        p.entries.forEach { e ->
            check(key(e.key, e.kind.name.lowercase()) && (e.gameId == null || e.gameId > 0), "Neplatná identita ${e.key}")
            if (e.gameId != null) check(e.key == "wow-forever:${e.kind.name.lowercase()}:${e.gameId}", "Neshoda herního ID ${e.key}")
            check(e.nameOriginal.isNotBlank() && e.descriptionCs.isNotBlank() && e.aliases.all { it.isNotBlank() }, "Prázdný název nebo popis ${e.key}")
            check(e.minimumLevel == null || e.minimumLevel in 1..60, "Neplatná minimální úroveň ${e.key}")
            check(e.reportedLevel == null || e.reportedLevel in 1..60, "Neplatná udaná úroveň ${e.key}")
            check(!e.releaseConfirmed || e.verification == Verification.RELEASE, "Neoprávněné potvrzení vydané hry ${e.key}")
            check(e.verification != Verification.RELEASE || (e.releaseConfirmed && e.sourceIds.any { sourceRecords[it]?.kind == "in_game_release_observation" }),
                "Chybí herní důkaz z vydané hry ${e.key}")
            references(e.sourceIds, e.key)
            check(!e.referencePosition || (e.verification in setOf(Verification.CLASSIC,Verification.COMMUNITY) && e.kind in setOf(EntryKind.NPC,EntryKind.OBJECT,EntryKind.TRAVEL) && e.sourceIds.any { sourceRecords[it]?.kind=="questie_pinned_reference" }), "Nedoložená referenční poloha ${e.key}")
        }
        p.locations.forEach { l ->
            val m = maps[l.mapKey]
            check(l.entryKey in entries && m != null, "Chybějící cíl polohy ${l.key}")
            check(l.xPercent.isFinite() && l.yPercent.isFinite() && l.xPercent in 0.0..100.0 && l.yPercent in 0.0..100.0, "Neplatné souřadnice ${l.key}")
            check(l.precisionPercent.isFinite() && l.precisionPercent in 0.0..100.0, "Neplatná přesnost ${l.key}")
            check(l.sourceId in sources, "Chybějící zdroj polohy ${l.key}")
            if (m != null) {
                check(m.floorId == l.floorId && m.coordinateVersion == l.coordinateVersion, "Neshodné patro nebo souřadnicová verze ${l.key}")
                val (x, y) = m.transform.normalized(l.xPercent, l.yPercent)
                check(x in 0.0..1.0 && y in 0.0..1.0, "Poloha mimo podklad ${l.key}")
            }
        }
        p.relations.forEach { r ->
            val owner = entries[r.ownerKey]
            val targets = r.targetKeys.mapNotNull { entries[it] }
            check(key(r.key, "relation") && owner != null && targets.size == r.targetKeys.size && targets.isNotEmpty(), "Chybějící cíl vazby ${r.key}")
            unique(r.targetKeys, "cíle ${r.key}")
            if(r.kind==RelationKind.EXCLUSIVE_QUESTS) {
                check(r.ownerKey !in r.targetKeys && r.mode==MatchMode.ANY,"Neplatná výlučná skupina ${r.key}")
                check(!r.routingEnabled || r.sourceIds.any { sourceRecords[it]?.kind=="in_game_release_observation" },"Chybí herní důkaz výlučné skupiny ${r.key}")
            }
            val kinds = when(r.kind) {
                RelationKind.PREREQUISITE, RelationKind.EXCLUSIVE_QUESTS -> EntryKind.QUEST to EntryKind.QUEST
                RelationKind.START_NPC, RelationKind.END_NPC -> EntryKind.QUEST to EntryKind.NPC
                RelationKind.START_OBJECT, RelationKind.END_OBJECT, RelationKind.OBJECTIVE_OBJECT -> EntryKind.QUEST to EntryKind.OBJECT
                RelationKind.OBJECTIVE_NPC -> EntryKind.QUEST to EntryKind.NPC
                RelationKind.OBJECTIVE_ITEM, RelationKind.START_ITEM, RelationKind.QUEST_REWARD -> EntryKind.QUEST to EntryKind.ITEM
                RelationKind.ITEM_SOURCE_NPC -> EntryKind.ITEM to EntryKind.NPC
                RelationKind.ITEM_SOURCE_OBJECT -> EntryKind.ITEM to EntryKind.OBJECT
                RelationKind.DUNGEON_QUEST -> EntryKind.TRAVEL to EntryKind.QUEST
                RelationKind.LEARN_SPELL -> EntryKind.QUEST to EntryKind.SPELL
                RelationKind.REWARD_CHOICE -> EntryKind.ITEM to EntryKind.QUEST
            }
            check(owner?.kind == kinds.first && targets.all { it.kind == kinds.second }, "Nesprávné typy vazby ${r.key}")
            check(!r.routingEnabled || (r.verification == Verification.RELEASE && owner?.releaseConfirmed == true && targets.all { it.releaseConfirmed }), "Neověřená aktivní návaznost ${r.key}")
            references(r.sourceIds, r.key)
        }
        // A prerequisite graph must be acyclic even when its groups use ANY.
        val graph = p.relations.filter { it.kind == RelationKind.PREREQUISITE }
            .groupBy { it.ownerKey }.mapValues { (_, rs) -> rs.flatMap { it.targetKeys }.toSet() }
        val nodes = graph.keys + graph.values.flatten()
        val degree = nodes.associateWith { 0 }.toMutableMap()
        graph.values.flatten().forEach { degree[it] = degree.getValue(it)+1 }
        val queue = ArrayDeque(degree.filterValues { it == 0 }.keys)
        var visited = 0
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst(); visited++
            graph[id].orEmpty().forEach { target ->
                degree[target] = degree.getValue(target)-1
                if (degree[target] == 0) queue.addLast(target)
            }
        }
        check(visited == degree.size, "Cyklus návazností")
        runCatching { GuideRuleValidator.validate(p) }.onFailure { errors += "Neplatná pravidla osobních doporučení" }
        runCatching { TransportValidator.validate(p) }.onFailure { errors += "Neplatný dopravní graf" }
        if (errors.isNotEmpty()) throw InvalidContent(errors.distinct())
    }
}
