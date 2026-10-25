package cz.jenda.wowforever.content

import kotlinx.serialization.Serializable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.InputStream
import java.text.Normalizer
import java.util.Locale

@Serializable enum class EntryKind { NPC, QUEST, ITEM, SPELL, TRAVEL, OBJECT }
@Serializable enum class Verification { BETA, COMMUNITY, CLASSIC, CONFLICT, UNVERIFIED, RELEASE }
@Serializable enum class RelationKind { PREREQUISITE, START_NPC, END_NPC, LEARN_SPELL, REWARD_CHOICE, EXCLUSIVE_QUESTS, START_OBJECT, END_OBJECT, START_ITEM, OBJECTIVE_NPC, OBJECTIVE_OBJECT, OBJECTIVE_ITEM, ITEM_SOURCE_NPC, ITEM_SOURCE_OBJECT, DUNGEON_QUEST, QUEST_REWARD }
@Serializable enum class MatchMode { ALL, ANY }

@Serializable data class ContentPackage(
    val schemaVersion: Int, val packageId: String, val generation: Int, val gameVariant: String,
    val checkedOn: String, val maps: List<MapRecord>, val sources: List<SourceRecord>,
    val entries: List<EntryRecord>, val locations: List<LocationRecord>, val relations: List<RelationRecord>, val guideRules: List<GuideRule> = emptyList(),
    val transportGraph: TransportGraph = TransportGraph(),
)
@Serializable data class MapRecord(
    val key: String, val uiMapId: Int, val floorId: Int, val name: String,
    val width: Int, val height: Int, val levels: Int, val build: String, val coordinateVersion: String,
    val sourceId: String, val sourceUrl: String, val transform: TransformRecord = TransformRecord(),
    val scopeCs:String? = null,
)
@Serializable data class TransformRecord(
    val scaleX: Double = 1.0, val scaleY: Double = 1.0, val offsetX: Double = 0.0, val offsetY: Double = 0.0,
) {
    fun normalized(x: Double, y: Double) = x/100.0*scaleX+offsetX to y/100.0*scaleY+offsetY
    fun percent(x: Double, y: Double) = (x-offsetX)/scaleX*100.0 to (y-offsetY)/scaleY*100.0
}
@Serializable data class SourceRecord(
    val id: String, val url: String, val kind: String, val checkedOn: String,
    val build: String? = null, val payload: String,
)
@Serializable data class EntryRecord(
    val key: String, val kind: EntryKind, val gameId: Int? = null, val nameOriginal: String,
    val descriptionCs: String, val aliases: List<String> = emptyList(), val verification: Verification,
    val releaseConfirmed: Boolean = false, val minimumLevel: Int? = null, val reportedLevel: Int? = null,
    val sourceIds: List<String>, val payload: String, val referencePosition: Boolean = false,
) {
    val searchText get() = normalizeSearch((listOf(nameOriginal, descriptionCs)+aliases).joinToString(" "))
}
@Serializable data class LocationRecord(
    val key: String, val entryKey: String, val mapKey: String, val coordinateVersion: String,
    val floorId: Int, val xPercent: Double, val yPercent: Double, val precisionPercent: Double,
    val sourceId: String,
)
@Serializable data class RelationRecord(
    val key: String, val kind: RelationKind, val ownerKey: String, val targetKeys: List<String>,
    val mode: MatchMode, val verification: Verification, val routingEnabled: Boolean = false,
    val sourceIds: List<String>,
)

val ContentJson = Json { ignoreUnknownKeys = false; encodeDefaults = true; isLenient = false }
const val CONTENT_PACKAGE_ID = "wow-forever:starter:durotar"
const val MAX_PACKAGE_BYTES = 32 * 1024 * 1024

fun decodePackage(bytes: ByteArray): ContentPackage {
    require(bytes.size <= MAX_PACKAGE_BYTES) { "Balíček je příliš velký." }
    return bytes.inputStream().use { decodePackage(it) }
}

/** Decode directly from bounded UTF-8 input instead of retaining a complete byte array and String. */
@OptIn(ExperimentalSerializationApi::class)
fun decodePackage(input:InputStream):ContentPackage = ContentJson.decodeFromStream(CatalogInput(input))

private class CatalogInput(private val source:InputStream):InputStream() {
    private var count=0L
    private var depth=0
    private var quoted=false
    private var escaped=false
    private fun inspect(value:Int) {
        count++;require(count<=MAX_PACKAGE_BYTES){"Balíček je příliš velký."}
        if(quoted) {
            if(escaped)escaped=false else if(value==92)escaped=true else if(value==34)quoted=false
        } else when(value) {
            34->quoted=true
            123,91->{depth++;require(depth<=32){"Příliš hluboká struktura balíčku"}}
            125,93->depth--
        }
    }
    override fun read():Int=source.read().also { if(it>=0)inspect(it) }
    override fun read(buffer:ByteArray,offset:Int,length:Int):Int {
        val size=source.read(buffer,offset,length)
        if(size>0)for(i in offset until offset+size)inspect(buffer[i].toInt() and 255)
        return size
    }
    override fun close()=source.close()
}
private val combiningMarks = Regex("\\p{M}+")
fun normalizeSearch(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace(combiningMarks, "").lowercase(Locale.ROOT)
