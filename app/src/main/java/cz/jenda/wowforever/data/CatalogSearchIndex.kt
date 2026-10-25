package cz.jenda.wowforever.data

import cz.jenda.wowforever.content.EntryRecord
import cz.jenda.wowforever.content.normalizeSearch

/** Immutable, built once per content generation off the UI thread. Input order breaks ties. */
class CatalogSearchIndex(entries: List<EntryRecord>) {
    private data class Row(val entry: EntryRecord, val full: String, val title: String)
    private val entries = entries.toList()
    private val rows = entries.map { Row(it, it.searchText, normalizeSearch(it.nameOriginal+" "+it.aliases.joinToString(" "))) }
    fun search(query: String): List<EntryRecord> {
        val normalized = normalizeSearch(query.trim())
        if (normalized.isEmpty()) return entries
        val words = normalized.split(spaces)
        val titles = ArrayList<EntryRecord>()
        val descriptions = ArrayList<EntryRecord>()
        for (row in rows) if (words.all { it in row.full }) {
            if (words.all { it in row.title }) titles.add(row.entry) else descriptions.add(row.entry)
        }
        titles.addAll(descriptions)
        return titles
    }
    private companion object { val spaces = Regex("\\s+") }
}
