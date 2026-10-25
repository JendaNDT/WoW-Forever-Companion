package cz.jenda.wowforever

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cz.jenda.wowforever.data.*
import cz.jenda.wowforever.data.db.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class E04StorageTest {
    @Test fun noteRoundTripsAcrossFileCloseAndDeletionIsScopedToProfile() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val name="e04-note-${System.nanoTime()}.db"
        val original=Room.databaseBuilder(context,PersonalDatabase::class.java,name).build()
        original.personal().createProfile(ProfileEntity(level=7))
        original.personal().createProfile(ProfileEntity(id="other"))
        original.personal().saveBookmark(BookmarkEntity(DEFAULT_PROFILE_ID,"wow-forever:npc:5765","Ruzan","NPC",1))
        val note=NoteEntity("note:keep",DEFAULT_PROFILE_ID,"Vrátit se sem",null,1454,0,27.3,68.4,1)
        original.personal().saveNote(note);original.close()
        val reopened=Room.databaseBuilder(context,PersonalDatabase::class.java,name).build()
        try {
            assertEquals(note,reopened.personal().notes(DEFAULT_PROFILE_ID).single())
            reopened.personal().deleteNote(note.id,"other")
            assertEquals(1,reopened.personal().notes(DEFAULT_PROFILE_ID).size)
            reopened.personal().saveNote(note.copy(text="Změněný text",updatedAt=2))
            assertEquals("Změněný text",reopened.personal().notes(DEFAULT_PROFILE_ID).single().text)
            reopened.personal().deleteNote(note.id)
            assertTrue(reopened.personal().notes(DEFAULT_PROFILE_ID).isEmpty())
            assertEquals(7,reopened.personal().profile()!!.level)
            assertEquals(1,reopened.personal().bookmarkKeys(DEFAULT_PROFILE_ID).size)
        } finally { reopened.close();context.deleteDatabase(name) }
    }
    @Test fun oldE03SessionGetsSafeLayerDefaultsAndKeepsViewport() = runBlocking {
        val old="""{"mapId":1454,"selectedId":null,"history":["MAP"],"spoilers":"HINTS","viewports":{"1411":{"x":0.427,"y":0.69,"scale":2.5,"rotation":0.0}}}"""
        val restored=SessionSerializer.readFrom(old.byteInputStream())
        assertEquals(1454,restored.mapId);assertEquals(0,restored.floorId)
        assertEquals(DEFAULT_PROFILE_ID,restored.activeProfileId)
        assertEquals(setOf("NPC","QUEST","ITEM","SPELL","TRAVEL","NOTE"),restored.visibleKinds.toSet())
        assertFalse(restored.onlyBookmarks)
        assertEquals(2.5,restored.viewports[1411]!!.scale,0.0001)
    }
}
