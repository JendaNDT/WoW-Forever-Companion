package cz.jenda.wowforever

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import cz.jenda.wowforever.data.*
import cz.jenda.wowforever.data.db.*
import kotlinx.coroutines.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class E11RecoveryTest {
    @Test fun corruptPreferencesRecoverThroughRealDataStoreWithoutChangingPersonalHistory()=runBlocking {
        val c=ApplicationProvider.getApplicationContext<Context>()
        File(c.filesDir,"datastore/guide-session.json").apply { parentFile.mkdirs();writeText("{broken settings") }
        val store=GuideStore(c)
        try {
            store.initialize();val dao=store.personal.personal()
            dao.updateProfile(ProfileEntity(level=15,currentUiMapId=1413))
            store.journal.setQuest(DEFAULT_PROFILE_ID,"wow-forever:quest:842","Crossroads Conscription",QuestState.COMPLETED)
            dao.saveNote(NoteEntity("note:recovery",DEFAULT_PROFILE_ID,"Zachovat při opravě nastavení",null,1413,0,51.7,30.3,1))
            val before=store.journal.profileBackup(DEFAULT_PROFILE_ID)
            val recovered=store.session.read()
            assertTrue(recovered.settingsRecovered);assertNotNull(store.session.error.value)
            assertEquals(1411,recovered.mapId);assertEquals(before,store.journal.profileBackup(DEFAULT_PROFILE_ID))
            store.session.save(recovered.copy(mapId=1454))
            withTimeout(10_000) { while(store.session.read().let { it.mapId!=1454 || it.settingsRecovered })delay(10) }
            assertEquals(1454,SessionStore(c).read().mapId)
            assertEquals(before,store.journal.profileBackup(DEFAULT_PROFILE_ID))
            assertEquals(before,BackupCodec.decode(store.journal.export(DEFAULT_PROFILE_ID)).profiles.single())
        } finally { store.close() }
    }
    @Test fun olderSettingsRemainReadableAndInvalidViewportsCannotEnterTheMap() {
        val old="""{"mapId":1454,"viewports":{"1454":{"x":0.4,"y":0.3,"scale":2.0,"rotation":90.0}}}"""
        val restored=runBlocking { SessionSerializer.readFrom(old.byteInputStream()) }
        assertFalse(restored.settingsRecovered);assertEquals(2.0,restored.viewports[1454]!!.viewport()!!.scale,0.0)
        assertNull(SavedViewport(0.5,0.5,Double.NaN,0f).viewport());assertNull(SavedViewport(0.5,0.5,-1.0,0f).viewport())
    }
}
