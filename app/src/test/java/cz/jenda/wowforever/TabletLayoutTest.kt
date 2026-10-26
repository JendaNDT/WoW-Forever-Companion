package cz.jenda.wowforever

import androidx.compose.ui.test.*
import cz.jenda.wowforever.ui.supportsCompanionPane
import cz.jenda.wowforever.ui.companionPaneWidth
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import java.io.File

class TabletLayoutRulesTest {
    @Test fun panesDependOnUsableWindowAndTextSizeRatherThanTabletBrand() {
        assertTrue(supportsCompanionPane(1200f,700f,1f))
        assertFalse(supportsCompanionPane(720f,1100f,1f))
        assertFalse(supportsCompanionPane(1200f,360f,1f))
        assertFalse(supportsCompanionPane(1200f,700f,2f))
        assertTrue(supportsCompanionPane(1800f,1000f,2f))
        assertTrue(companionPaneWidth(1200f,1f) in 360f..480f)
    }
}
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],qualifiers="w1280dp-h800dp-land-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TabletLayoutTest:TabletLayoutChecks() {
    override fun outputDirectory()=File("build/reports/tablet")
    @Test fun detailAndCollapsedSectionsSurviveRecreationAndNarrowingTheWindow() {
        openRuzanFromArea()
        rule.onNodeWithTag("detail_description").performScrollTo().performClick()
        rule.onNodeWithTag("entry_description").assertDoesNotExist()
        rule.activityRule.scenario.recreate()
        rule.onNodeWithTag("entry_side_pane").assertIsDisplayed()
        rule.onNodeWithTag("entry_description").assertDoesNotExist()
        RuntimeEnvironment.setQualifiers("w800dp-h1280dp-port-mdpi")
        rule.activityRule.scenario.recreate()
        rule.onNodeWithTag("entry_side_pane").assertDoesNotExist()
        rule.onNodeWithTag("entry_sheet").assertExists()
        rule.onNodeWithTag("entry_description").assertDoesNotExist()
        rule.onNodeWithTag("detail_back").performClick()
        rule.onNodeWithTag("native_map").assertIsDisplayed()
        shot("portrait-map")
    }
    @Test fun expandingAfterADetailRestoresTheAreaListScrollPosition() {
        openRuzanFromArea()
        rule.onNodeWithTag("detail_back").performClick()
        rule.onNodeWithTag("area_detail_wow-forever:npc:5765").assertIsDisplayed()
        rule.runOnIdle { assertEquals(0,ShadowDialog.getShownDialogs().count { it.isShowing }) }
    }
}
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],qualifiers="w1280dp-h800dp-land-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TabletLargeTextTest:E11LargeTextChecks() {
    @Test fun tabletAtTwoHundredPercentUsesOneReadablePaneWithReachableActions() {
        rule.onNodeWithTag("companion_pane").assertDoesNotExist()
        rule.onNodeWithTag("points").assertIsDisplayed()
        rule.onNodeWithTag("fit").assertIsDisplayed()
        rule.openCharacter()
        rule.onNodeWithTag("save_profile").performScrollTo().assertIsDisplayed()
        shot("tablet-large-text")
    }
}
