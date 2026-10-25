package cz.jenda.wowforever

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import cz.jenda.wowforever.data.AtlasRepository
import cz.jenda.wowforever.map.MapComposeEngine
import cz.jenda.wowforever.model.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.json.JSONObject
import java.io.File

abstract class E11MapStressChecks {
    @get:Rule val rule=createComposeRule()
    private val c=ApplicationProvider.getApplicationContext<Context>()
    private val atlas=AtlasRepository(c.assets).load()
    private fun record(name:String,data:JSONObject) {
        val host=android.os.Build.FINGERPRINT.contains("robolectric")
        data.put("environment",if(host)"Robolectric native graphics; not Android device timing" else "Android ${android.os.Build.VERSION.RELEASE} ${android.os.Build.MODEL}")
        data.put("fps_measured",false);data.put("twenty_minute_memory_goal_measured",false)
        val folder=if(host)File("build/reports")else File(c.getExternalFilesDir(null),"e11")
        File(folder,"e11-$name.json").apply { parentFile.mkdirs();writeText(data.toString(2)) }
    }
    private fun action(block:suspend()->Unit) {
        val job=CoroutineScope(Dispatchers.Main).async { block() };rule.waitForIdle();rule.waitUntil(30_000){job.isCompleted};runBlocking { job.await() }
    }
    @Test fun fortyAreaChangesBoundTileStatesAndRestoreEvictedViewport() {
        val saved=mutableMapOf<String,Viewport>()
        val engine=MapComposeEngine(c.assets,atlas,{m,f->saved["$m:$f"]},{m,f,v->saved["$m:$f"]=v})
        var current by mutableStateOf(atlas.map(1411))
        rule.setContent { key(current.frameKey){engine.Render(current,Modifier.requiredSize(360.dp,240.dp).testTag("e11-map")) {}} }
        try {
            action {engine.fit(1411);engine.focus(atlas.points.first { it.position.uiMapId==1411 })}
            val expected=engine.viewport(1411)!!;var peak=0
            repeat(10) { for(id in listOf(1454,1414,947,1411)) {
                rule.runOnIdle {current=atlas.map(id)};rule.waitForIdle()
                rule.runOnIdle { peak=maxOf(peak,engine.retainedFrameCount);assertTrue(engine.retainedFrameCount<=2) }
            }}
            val returned=engine.viewport(1411)!!
            assertEquals(expected.x,returned.x,0.01);assertEquals(expected.y,returned.y,0.01);assertEquals(expected.scale,returned.scale,0.001)
            assertTrue(saved.size>=3)
            rule.runOnIdle { engine.trimMemory(1411,0);assertEquals(1,engine.retainedFrameCount) }
            assertEquals(returned.scale,engine.viewport(1411)!!.scale,0.001)
            record("map-retention",JSONObject().put("area_changes",40).put("peak_retained_frames",peak).put("evicted_viewport_restored",true))
        }finally {rule.runOnIdle {engine.close();engine.close();assertEquals(0,engine.retainedFrameCount)}}
    }
    @Test fun fiveThousandSyntheticCandidatesClusterAndSurviveRepeatedOperations() {
        val points=(0 until 5000).map { i->MapPoint("synthetic:$i","Zátěžový bod $i",MapPosition(1411,0.5+(i%100)*0.99,0.5+(i/100)*1.98),"https://example.test") }
        val engine=MapComposeEngine(c.assets,Atlas(atlas.maps,points),{_,_->null},{_,_,_->})
        rule.setContent { engine.Render(atlas.map(1411),Modifier.requiredSize(360.dp,240.dp).testTag("e11-stress")){} }
        try {
            action {engine.fit(1411)}
            var rendered=0
            rule.waitUntil(30_000){rendered=rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription),useUnmergedTree=true).fetchSemanticsNodes().size;rendered in 1..1000}
            repeat(12) { i->action {engine.focus(points[i*397]);engine.zoom(1411,1.1);engine.north(1411);engine.fit(1411)} }
            rule.onNodeWithTag("e11-stress").assertIsDisplayed();assertTrue(engine.viewport(1411)!!.scale.isFinite())
            record("map-5000",JSONObject().put("synthetic_candidates",5000).put("fit_semantic_nodes",rendered).put("operation_rounds",12).put("finite_viewport",true))
        }finally {rule.runOnIdle {engine.close()}}
    }
}
