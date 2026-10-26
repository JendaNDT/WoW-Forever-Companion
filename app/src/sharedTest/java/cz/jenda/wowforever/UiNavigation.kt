package cz.jenda.wowforever

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.semantics.SemanticsActions

fun ComposeContentTestRule.finishWelcomeIfShown() {
    waitUntil(180_000) { onAllNodesWithTag("welcome_start").fetchSemanticsNodes().isNotEmpty() || onAllNodesWithTag("nav_MAP").fetchSemanticsNodes().isNotEmpty() }
    if(onAllNodesWithTag("welcome_start").fetchSemanticsNodes().isNotEmpty())onNodeWithTag("welcome_start").performScrollTo().performClick()
    waitUntil(30_000) { onAllNodesWithTag("nav_MAP").fetchSemanticsNodes().isNotEmpty() }
    if(onAllNodesWithTag("dismiss_map_hint").fetchSemanticsNodes().isNotEmpty())onNodeWithTag("dismiss_map_hint").performClick()
}
fun ComposeContentTestRule.chooseMap(id:Int) {
    onNodeWithTag("choose_map").performClick()
    onNodeWithTag("map_selector_list").performScrollToNode(hasTestTag("select_map_$id"))
    onNodeWithTag("select_map_$id").performClick()
}
fun ComposeContentTestRule.zoomMap() {
    val actions=onNodeWithTag("native_map").fetchSemanticsNode().config[SemanticsActions.CustomActions]
    runOnIdle { check(actions.first { it.label=="Přiblížit" }.action()) }
    waitForIdle()
}

fun ComposeContentTestRule.openSettings() { onNodeWithTag("settings").performClick() }
fun ComposeContentTestRule.openCharacter() { openSettings();onNodeWithTag("settings_character").performScrollTo().performClick() }
fun ComposeContentTestRule.openDisplay() { openSettings();onNodeWithTag("focus_settings").performScrollTo().performClick() }
fun ComposeContentTestRule.openSuggestions() { onNodeWithTag("nav_QUESTS").performClick();onNodeWithTag("quests_next").performClick() }
fun ComposeContentTestRule.openJournal() { onNodeWithTag("nav_QUESTS").performClick();onNodeWithTag("quests_journal").performClick() }
fun ComposeContentTestRule.openSearch() { onNodeWithTag("nav_SEARCH").performClick() }
