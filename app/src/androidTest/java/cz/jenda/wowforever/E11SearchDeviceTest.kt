package cz.jenda.wowforever

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.data.CatalogSearchIndex
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class E11SearchDeviceTest {
    @Test fun recordIndexedSearchKernelOnTenThousandSyntheticRows() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val entries=(0 until 10_000).map { i->EntryRecord("synthetic:npc:$i",EntryKind.NPC,nameOriginal="Strážce $i",descriptionCs="Český průvodce k řece a zelené odměně.",aliases=listOf("warlock"),verification=Verification.UNVERIFIED,sourceIds=listOf("synthetic"),payload="{}") }
        val started=System.nanoTime();val index=CatalogSearchIndex(entries);val build=(System.nanoTime()-started)/1e6
        val queries=listOf("strazce 9999","zelene odmene","warlock","nenajdes")
        repeat(4){queries.forEach { index.search(it) }}
        val timings=mutableListOf<Double>()
        repeat(6){queries.forEach { q->val t=System.nanoTime();index.search(q);timings.add((System.nanoTime()-t)/1e6) }}
        assertEquals(listOf("synthetic:npc:9999"),index.search("STRÁŽCE 9999").map { it.key })
        val values=timings.sorted()
        val report=JSONObject().put("model",android.os.Build.MODEL).put("android",android.os.Build.VERSION.RELEASE)
            .put("debuggable",context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE!=0)
            .put("rows",10_000).put("index_build_ms",build).put("runs",values.size).put("p50_ms",values[12]).put("p95_ms",values[22]).put("max_ms",values.last())
            .put("scope","search kernel only; not end-to-end keyboard/UI latency").put("android_300ms_ui_goal_verified",false)
        File(context.getExternalFilesDir(null),"e11/e11-search-device.json").apply { parentFile.mkdirs();writeText(report.toString(2)) }
    }
}
