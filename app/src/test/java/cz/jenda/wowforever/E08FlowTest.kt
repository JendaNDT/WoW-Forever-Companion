package cz.jenda.wowforever

import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.*
import cz.jenda.wowforever.content.ContentJson
import cz.jenda.wowforever.data.*
import cz.jenda.wowforever.ui.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class E08FlowTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private var custom:GuideViewModel?=null
    private val extra=ViewModelStore()
    private val model get()=custom ?: ViewModelProvider(rule.activity)[GuideViewModel::class.java]
    @Before fun ready() {rule.finishWelcomeIfShown();
        rule.waitUntil(20_000){rule.onAllNodesWithTag("nav_MAP").fetchSemanticsNodes().isNotEmpty()}
        rule.runOnIdle { model.navigate(Destination.MAP);model.chooseMap(1411);model.resetLayers() }
    }
    @After fun close(){rule.runOnIdle{extra.clear();custom=null;model.navigate(Destination.MAP);model.chooseMap(1411)}}
    private fun settled(predicate:()->Boolean) {rule.waitUntil(15_000){rule.onAllNodesWithTag("nav_MAP").fetchSemanticsNodes();!model.isBusy && predicate()}}
    private fun scroll(tag:String){rule.onNodeWithTag("expedition_list").performScrollToNode(hasTestTag(tag))}
    private fun add(key:String) {rule.runOnIdle{model.addPlanStop(key)};settled{model.personalState.stops.lastOrNull()?.entryKey==key}}
    private fun shot(name:String,dialog:Boolean=true){
        lateinit var bitmap:android.graphics.Bitmap
        rule.runOnIdle{bitmap=captureView(if(dialog)org.robolectric.shadows.ShadowDialog.getShownDialogs().last { it.isShowing }.window!!.decorView else rule.activity.window.decorView)}
        File("build/reports/e08-$name.png").also { it.parentFile!!.mkdirs();it.outputStream().use { out->bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out) } }
    }
    @Test fun realCatalogAddsOrderedTargetsAndNumbersAppearOnTheActualMap() {
        rule.onNodeWithTag("expedition").performClick();rule.onNodeWithTag("plan_add").performClick()
        rule.onNodeWithTag("plan_query").performTextReplacement("12776")
        scroll("plan_pick_wow-forever:npc:12776");rule.onNodeWithTag("plan_pick_wow-forever:npc:12776").performClick();settled{model.personalState.stops.size==1}
        scroll("plan_query");rule.onNodeWithTag("plan_query").performTextReplacement("6928")
        scroll("plan_pick_wow-forever:npc:6928");rule.onNodeWithTag("plan_pick_wow-forever:npc:6928").performClick();settled{model.personalState.stops.size==2}
        scroll("plan_add");rule.onNodeWithTag("plan_add").performClick()
        scroll("plan_name_1");rule.onNodeWithTag("plan_name_1").assertTextContains("Hraug",substring=true);shot("plan")
        scroll("transport_coverage");rule.onNodeWithTag("transport_coverage").assertTextContains("0.",substring=true)
        scroll("plan_leg_status_1");rule.onNodeWithTag("plan_leg_status_1").assertTextContains("není doložené",substring=true)
        scroll("plan_map_1");rule.onNodeWithTag("plan_map_1").performClick()
        rule.onNodeWithTag("plan_map_legend").assertExists()
        rule.waitUntil(15_000){rule.onAllNodesWithContentDescription("Hraug · zastávky 1",useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
        rule.runOnIdle {assertEquals(listOf(2),model.filteredPoints.single { it.entryKey=="wow-forever:npc:6928" }.planNumbers)}
        shot("map",false)
    }
    @Test fun reorderRemoveDuplicateVisitsAndRecreationKeepTheVisiblePlanConsistent() {
        add("wow-forever:npc:12776");add("wow-forever:npc:6928");add("wow-forever:npc:12776")
        rule.onNodeWithTag("expedition").performClick();scroll("plan_up_3");rule.onNodeWithTag("plan_up_3").performClick()
        settled{model.expeditionStops[1].stored.entryKey=="wow-forever:npc:12776"}
        rule.runOnIdle{assertEquals(listOf(1,2),model.filteredPoints.single { it.entryKey=="wow-forever:npc:12776" }.planNumbers)}
        scroll("plan_remove_2");rule.onNodeWithTag("plan_remove_2").performClick();settled{model.expeditionStops.size==2}
        rule.activityRule.scenario.recreate()
        scroll("plan_name_1");rule.onNodeWithTag("plan_name_1").assertTextContains("Hraug",substring=true)
        scroll("plan_name_2");rule.onNodeWithTag("plan_name_2").assertTextContains("Grosk",substring=true);shot("reordered")
        rule.runOnIdle{assertTrue(model.questStates.isEmpty());assertTrue(model.acquiredItems.isEmpty())}
    }
    @Test fun personalDiscoveryIsSavedButDoesNotPromoteTheBetaZeppelinToVerifiedTransport() {
        val key="wow-forever:travel:durotar-undercity"
        rule.onNodeWithTag("expedition").performClick();rule.onNodeWithTag("plan_travel").performClick()
        scroll("discovery_$key");rule.onNodeWithTag("discovery_$key").performClick();settled{model.personalState.travel.any { it.travelKey==key }}
        rule.activityRule.scenario.recreate();scroll("discovery_$key");rule.onNodeWithTag("discovery_$key").assertIsSelected();shot("discovery")
        rule.runOnIdle{assertTrue(model.transportGraph.edges.isEmpty())}
        rule.onNodeWithTag("discovery_$key").performClick();settled{model.personalState.travel.isEmpty()}
    }
    @Test fun syntheticVerifiedFlightAppearsOnlyAfterBothDiscoveryChipsAreSet() {
        val store=(rule.activity.application as GuideApplication).store
        runBlocking{CatalogInstaller(store.content).install(ContentJson.encodeToString(t8Fixture()).encodeToByteArray())}
        rule.runOnIdle {
            custom=GuideViewModel(rule.activity.application,SavedStateHandle());extra.put("e08",model)
            rule.activity.setContent { GuideTheme { GuideScreen(model) } }
        }
        rule.waitUntil(20_000){rule.onAllNodesWithTag("nav_MAP").fetchSemanticsNodes();model.ready || model.dataError!=null}
        rule.runOnIdle{assertNull(model.dataError);model.saveProfile(model.profile.copy(level=10))};settled{model.profile.level==10}
        add("wow-forever:npc:12776");add("wow-forever:npc:6928")
        rule.onNodeWithTag("expedition").performClick();scroll("plan_leg_status_1");rule.onNodeWithTag("plan_leg_status_1").assertTextContains("Není doložená přístupná cesta",substring=true)
        scroll("plan_travel");rule.onNodeWithTag("plan_travel").performClick()
        listOf(T8_DA,T8_DB).forEach { key ->scroll("discovery_$key");rule.onNodeWithTag("discovery_$key").performClick();settled{model.personalState.travel.any { it.travelKey==key }} }
        scroll("plan_travel");rule.onNodeWithTag("plan_travel").performClick();scroll("plan_leg_status_1")
        rule.onNodeWithTag("plan_leg_status_1").assertTextContains("Ověřené spojení podle podkladů",substring=true);shot("synthetic-verified")
    }
}
