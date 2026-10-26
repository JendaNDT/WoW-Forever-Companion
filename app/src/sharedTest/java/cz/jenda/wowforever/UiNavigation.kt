package cz.jenda.wowforever

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule

fun ComposeContentTestRule.openSettings() { onNodeWithTag("settings").performClick() }
fun ComposeContentTestRule.openCharacter() { openSettings();onNodeWithTag("settings_character").performScrollTo().performClick() }
fun ComposeContentTestRule.openDisplay() { openSettings();onNodeWithTag("focus_settings").performScrollTo().performClick() }
fun ComposeContentTestRule.openSuggestions() { onNodeWithTag("nav_QUESTS").performClick();onNodeWithTag("quests_next").performClick() }
fun ComposeContentTestRule.openJournal() { onNodeWithTag("nav_QUESTS").performClick();onNodeWithTag("quests_journal").performClick() }
fun ComposeContentTestRule.openSearch() { onNodeWithTag("nav_SEARCH").performClick() }
