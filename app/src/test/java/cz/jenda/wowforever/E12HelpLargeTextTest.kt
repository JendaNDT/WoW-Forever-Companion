package cz.jenda.wowforever

import androidx.compose.ui.test.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class E12HelpLargeTextTest:E11LargeTextChecks() {
    @Test fun recoveryActionAndCloseRemainReachableWithTwoHundredPercentText(){
        rule.openSettings()
        rule.onNodeWithTag("open_help").performScrollTo().performClick()
        rule.onNodeWithTag("help_list").performScrollToNode(hasTestTag("help_backup"))
        rule.onNodeWithTag("help_backup").assertIsDisplayed();shot("help-large-text",true)
        rule.onNodeWithText("Zavřít návod").assertIsDisplayed().performClick()
        rule.onNodeWithTag("settings_back").performClick()
        rule.onNodeWithTag("native_map").assertIsDisplayed()
    }
}
