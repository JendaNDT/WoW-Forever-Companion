package cz.jenda.wowforever

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import cz.jenda.wowforever.data.GuideStore
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

class E11CleanupFailureApplication:GuideApplication() {
    override val store by lazy { GuideStore(this,packageFault={ if(it=="before-prune")throw java.io.IOException("Test cleanup failure") }) }
}
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=E11CleanupFailureApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class E11CleanupFlowTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    @Test fun cleanupFailureAfterCommitCannotRollBackDiskWhileKeepingNewContentOnScreen() {
        rule.waitUntil(30_000){rule.onAllNodesWithTag("nav_MAP").fetchSemanticsNodes().isNotEmpty()}
        val model=ViewModelProvider(rule.activity)[GuideViewModel::class.java]
        val store=(rule.activity.application as GuideApplication).store
        val before=runBlocking { store.journal.profileBackup(model.profile.id) }
        val file=File(rule.activity.cacheDir,"e11-actual-data.wfp").apply { writeBytes(e10Distribution(rule.activity)) }
        rule.runOnIdle { model.readContentPackage(Uri.fromFile(file)) }
        rule.waitUntil(30_000){rule.waitForIdle();!model.isBusy && model.packagePreview!=null}
        rule.runOnIdle { model.activateContentPackage() }
        rule.waitUntil(30_000){rule.waitForIdle();!model.isBusy}
        rule.runOnIdle {
            assertEquals(4,model.contentMetadata!!.generation);assertEquals(4,store.packages.status().generation)
            assertEquals(5,model.atlas.maps.size);assertEquals(139,model.catalogEntries.size)
            assertTrue(model.searchCatalog("Sergra").any { it.key=="wow-forever:npc:3338" })
            assertTrue(model.packageMessage!!.contains("Úklid"))
        }
        assertEquals(before,runBlocking { store.journal.profileBackup(model.profile.id) })
        val reopened=GuideStore(rule.activity)
        try {runBlocking { reopened.initialize();assertEquals(4,reopened.content.catalog().metadata()!!.generation) }}finally{reopened.close()}
        file.delete()
    }
}
