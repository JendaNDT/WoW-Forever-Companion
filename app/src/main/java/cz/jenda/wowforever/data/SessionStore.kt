package cz.jenda.wowforever.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import cz.jenda.wowforever.model.Viewport
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/** Small immutable session, separate from the two Room databases. One DataStore per file. */
@Serializable data class SavedViewport(val x: Double, val y: Double, val scale: Double, val rotation: Float) {
    fun viewport(): Viewport? = if (x.isFinite() && y.isFinite() && scale.isFinite() && rotation.isFinite() && scale > 0)
        Viewport(x, y, scale, rotation) else null
}
enum class Destination(val label: String, val symbol: String) {
    MAP("Mapa", "◎"), QUESTS("Úkoly", "◇"), SEARCH("Hledat", "⌕"), SETTINGS("Nastavení", "⚙");
    companion object {
        val tabs=listOf(MAP,QUESTS,SEARCH)
        fun fromSaved(value:String):Destination?=when(value) {
            "NOW","JOURNEY"->QUESTS
            "CHARACTER"->SETTINGS
            else->entries.firstOrNull { it.name==value }
        }
    }
}
fun migrateDestinationHistory(values:List<String>):List<String> = listOf("MAP") + values.mapNotNull { Destination.fromSaved(it)?.name }
    .filter { it!="MAP" }.asReversed().distinct().asReversed()

enum class Spoilers(val label: String) { HINTS("Jen názvy a místa"), FULL("Podrobné popisy") }
@Serializable data class Session(
    val mapId: Int = 1411, val selectedId: String? = null,
    val history: List<String> = listOf("MAP"), val spoilers: String = "FULL",
    val viewports: Map<Int, SavedViewport> = emptyMap(),
    val floorId: Int = 0, val floorViewports: Map<String, SavedViewport> = emptyMap(),
    val visibleKinds: List<String> = listOf("NPC","QUEST","ITEM","SPELL","TRAVEL","NOTE"),
    val onlyBookmarks: Boolean = false, val activeProfileId: String = "profile:default",
    val guideFocus:String = "BALANCED",
    val questTab:String = "NEXT",
    val recentSearches:List<String> = emptyList(),
    val settingsRecovered:Boolean = false,
)
object SessionSerializer : Serializer<Session> {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    override val defaultValue = Session()
    override suspend fun readFrom(input: InputStream): Session = try {
        json.decodeFromString<Session>(input.readBytes().decodeToString())
    } catch (e: SerializationException) { throw CorruptionException("Neplatná uložená nastavení", e) }
    override suspend fun writeTo(t: Session, output: OutputStream) { output.write(json.encodeToString(Session.serializer(), t).encodeToByteArray()) }
}
private val Context.guideSession by dataStore(fileName = "guide-session.json", serializer = SessionSerializer,
    corruptionHandler = ReplaceFileCorruptionHandler { Session(settingsRecovered=true) })

class SessionStore(context: Context) {
    private val data = context.applicationContext.guideSession
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writes = Channel<Session>(Channel.CONFLATED)
    val error = MutableStateFlow<String?>(null)
    init {
        // Application lifetime: rotation / ViewModel destruction does not cancel an accepted write.
        scope.launch {
            for (value in writes) try { data.updateData { value }; error.value = null }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { error.value = "Nastavení se nepodařilo uložit. Zkus změnu zopakovat." }
        }
    }
    suspend fun read(): Session = data.data.first().also {
        if(it.settingsRecovered)error.value="Uložené nastavení mapy bylo poškozené a obnovilo se na výchozí. Postavy, úkoly a poznámky zůstaly zachované."
    }
    fun save(value: Session) { check(writes.trySend(value.copy(settingsRecovered=false)).isSuccess) }
}
