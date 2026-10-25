package cz.jenda.wowforever

import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.data.CatalogSearchIndex
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.text.Normalizer
import java.util.Locale

class E11SearchTest {
    private fun oldNormalize(s:String)=Normalizer.normalize(s,Normalizer.Form.NFD).replace("\\p{M}+".toRegex(),"").lowercase(Locale.ROOT)
    // Exact E10 search algorithm retained only as the comparison baseline, never shipped.
    private fun oldSearch(entries:List<EntryRecord>,q:String):List<EntryRecord> {
        val words=oldNormalize(q.trim()).split(Regex("\\s+")).filter { it.isNotEmpty() }
        return entries.filter { e->words.all { it in oldNormalize((listOf(e.nameOriginal,e.descriptionCs)+e.aliases).joinToString(" ")) } }
            .sortedBy { e->if(words.all { it in oldNormalize(e.nameOriginal+" "+e.aliases.joinToString(" ")) })0 else 1 }
    }
    @Test fun indexedSearchKeepsDiacriticsMultiWordRankingAndStableOrderAcrossRealContent() {
        val entries=e10Package().entries;val index=CatalogSearchIndex(entries)
        for(q in listOf("", "  ","RUZAN","černokněžník","mapa","Hidden   Enemies","crossroads","kouzelnicke sily","duše","npc 3338","nenajdes"))
            assertEquals(q,oldSearch(entries,q).map { it.key },index.search(q).map { it.key })
    }
    @Test fun replacementIndexNeverKeepsNamesFromAnEarlierGeneration() {
        val old=e10Package().entries.first();val replaced=old.copy(nameOriginal="Nové jméno bez minulosti",descriptionCs="nový popis",aliases=emptyList())
        assertEquals(listOf(old),CatalogSearchIndex(listOf(old)).search(old.nameOriginal))
        assertTrue(CatalogSearchIndex(listOf(replaced)).search(old.nameOriginal).isEmpty())
        assertEquals(listOf(replaced),CatalogSearchIndex(listOf(replaced)).search("NOVE JMENO"))
    }
    @Test fun tenThousandSyntheticRowsMatchE10AndRecordComparableHostMeasurements() {
        val entries=(0 until 10_000).map { i->EntryRecord("synthetic:npc:$i",EntryKind.NPC,nameOriginal="Strážce $i",descriptionCs="Český průvodce k řece a zelené odměně. Pouze zátěžový údaj $i.",aliases=listOf("warlock","kamen $i"),verification=Verification.UNVERIFIED,sourceIds=listOf("synthetic"),payload="{}") }
        val queries=listOf("strazce 124","zelene odmene","warlock","nenajdes", "kamen 500", "český řeka", "Strážce 9999", "")
        val t=System.nanoTime();val index=CatalogSearchIndex(entries);val buildMs=(System.nanoTime()-t)/1e6
        repeat(2){queries.forEach { q->assertEquals(oldSearch(entries,q),index.search(q)) }}
        val old=mutableListOf<Double>();val newer=mutableListOf<Double>()
        repeat(3) { round->for((n,q) in queries.withIndex()) {
            lateinit var a:List<EntryRecord>;lateinit var b:List<EntryRecord>
            fun legacy(){val start=System.nanoTime();a=oldSearch(entries,q);old.add((System.nanoTime()-start)/1e6)}
            fun indexed(){val start=System.nanoTime();b=index.search(q);newer.add((System.nanoTime()-start)/1e6)}
            if((round+n)%2==0){legacy();indexed()}else{indexed();legacy()}
            assertEquals(a,b)
        }}
        fun stats(values:List<Double>)=buildJsonObject { val v=values.sorted();put("runs",v.size);put("p50_ms",v[v.size/2]);put("p95_ms",v[(v.size*0.95).toInt().coerceAtMost(v.lastIndex)]);put("max_ms",v.last()) }
        val report=buildJsonObject { put("environment","host JVM; not an Android device or UI latency");put("java",System.getProperty("java.version"));put("rows",entries.size);put("warmup_queries",16);put("index_build_ms",buildMs);put("legacy_e10",stats(old));put("indexed_e11",stats(newer));put("all_results_equal",true);put("android_300ms_goal_measured",false) }
        File("build/reports/e11-search.json").apply { parentFile.mkdirs();writeText(report.toString()) }
    }
}
