package cz.jenda.wowforever

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CatalogFlowTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    @Before fun ready() {
        rule.waitUntil(20_000) { rule.onAllNodesWithTag("native_map").fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun addingBookmarkAndRecreatingActivityKeepsTheBookmark() {
        rule.onNodeWithTag("catalog").performClick()
        rule.onNodeWithTag("catalog_query").performTextInput("5778")
        rule.onNodeWithTag("catalog_wow-forever:item:5778").performClick()
        rule.onNodeWithText("Přidat záložku").performScrollTo().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Odebrat záložku").fetchSemanticsNodes().isNotEmpty() }
        lateinit var screenshot: android.graphics.Bitmap
        rule.runOnIdle { screenshot=captureView(org.robolectric.shadows.ShadowDialog.getLatestDialog().window!!.decorView) }
        val file=File("build/reports/e02-bookmark.png");file.parentFile!!.mkdirs()
        file.outputStream().use { screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
        rule.activityRule.scenario.recreate()
        rule.onNodeWithTag("bookmark_toggle").assertExists()
        rule.onNodeWithText("Zpět do katalogu").performClick()
        rule.onNodeWithTag("only_bookmarks").performClick()
        rule.onNodeWithTag("catalog_wow-forever:item:5778").assertExists()
        rule.onNodeWithTag("catalog_wow-forever:item:5778").performClick()
        rule.onNodeWithText("Odebrat záložku").performScrollTo().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Přidat záložku").fetchSemanticsNodes().isNotEmpty() }
    }
}
