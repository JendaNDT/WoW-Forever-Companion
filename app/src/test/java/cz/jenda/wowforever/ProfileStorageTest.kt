package cz.jenda.wowforever

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cz.jenda.wowforever.data.*
import cz.jenda.wowforever.data.db.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class ProfileStorageTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Test fun editingExistingProfileKeepsChildrenAndReopensWithOptionalFieldsEmpty() = runBlocking {
        val name = "e03-profile-${System.nanoTime()}.db"
        val db = Room.databaseBuilder(context, PersonalDatabase::class.java, name).build()
        val dao = db.personal()
        dao.createProfile(ProfileEntity())
        dao.saveBookmark(BookmarkEntity(DEFAULT_PROFILE_ID,"wow-forever:npc:5765","Ruzan","NPC",1))
        dao.saveProgress(QuestProgressEntity(DEFAULT_PROFILE_ID,"wow-forever:quest:1485","Vile Familiars","COMPLETED",1))
        dao.saveNote(NoteEntity("note",DEFAULT_PROFILE_ID,"Důležité",null,null,null,null,null,1))
        assertEquals(1,dao.updateProfile(dao.profile()!!.copy(displayName="Jenda",level=12,race="TROLL",currentUiMapId=1454)))
        db.close()
        val reopened = Room.databaseBuilder(context, PersonalDatabase::class.java, name).build()
        try {
            val p = reopened.personal().profile()!!
            assertEquals(12,p.level);assertEquals("TROLL",p.race);assertEquals(1454,p.currentUiMapId)
            assertNull(p.ruleset);assertEquals("[]",p.professionsJson)
            assertEquals(1,reopened.personal().observeBookmarks().first().size)
            assertEquals("COMPLETED",reopened.personal().progress(DEFAULT_PROFILE_ID,"wow-forever:quest:1485")!!.state)
            assertEquals("Důležité",reopened.personal().notes(DEFAULT_PROFILE_ID).single().text)
        } finally { reopened.close();context.deleteDatabase(name) }
    }
    @Test fun sessionSurvivesDataStoreDestructionAndReconstruction() = runBlocking {
        val file = File(context.cacheDir,"session-${System.nanoTime()}.json")
        val scope1 = CoroutineScope(SupervisorJob()+Dispatchers.IO)
        val data1 = DataStoreFactory.create(SessionSerializer,scope=scope1,produceFile={file})
        val expected = Session(mapId=1454,selectedId=null,history=listOf("MAP","JOURNEY","CHARACTER"),spoilers="HINTS",
            viewports=mapOf(1411 to SavedViewport(0.427,0.69,2.5,23f)))
        data1.updateData { expected }
        scope1.coroutineContext[Job]!!.cancelAndJoin()
        val scope2 = CoroutineScope(SupervisorJob()+Dispatchers.IO)
        val data2 = DataStoreFactory.create(SessionSerializer,scope=scope2,produceFile={file})
        try { assertEquals(expected,data2.data.first()); assertEquals(2.5,data2.data.first().viewports[1411]!!.viewport()!!.scale,0.0001) }
        finally { scope2.coroutineContext[Job]!!.cancelAndJoin();file.delete() }
    }
}
