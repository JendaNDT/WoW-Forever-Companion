package cz.jenda.wowforever.data

import cz.jenda.wowforever.content.EntryKind
import cz.jenda.wowforever.data.db.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.InputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

const val MAX_BACKUP_BYTES=5*1024*1024
@Serializable data class ProfileBackup(val profile: ProfileEntity,val quests: List<QuestProgressEntity> = emptyList(),
    val items: List<CollectedItemEntity> = emptyList(),val bookmarks: List<BookmarkEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),val stops: List<PlannedStopEntity> = emptyList(),
    val travel: List<DiscoveredTravelEntity> = emptyList(),val rewards: List<RewardChoiceEntity> = emptyList()) {
    val rowCount get()=quests.size+items.size+bookmarks.size+notes.size+stops.size+travel.size+rewards.size
}
@Serializable data class BackupFile(val formatVersion: Int=1,val appId: String="cz.jenda.wowforever",val gameVariant: String="WOW_FOREVER",
    val exportedAt: Long,val activeProfileId: String,val profiles: List<ProfileBackup>)
val BackupJson=Json { encodeDefaults=true;ignoreUnknownKeys=false;isLenient=false }

class BackupFormatException(message:String):IllegalArgumentException(message)

object BackupCodec {
    fun read(input: InputStream): BackupFile {
        val output=ByteArrayOutputStream();val buffer=ByteArray(8192)
        while(true) { val n=input.read(buffer);if(n<0)break;if(n==0)continue;if(output.size()+n>MAX_BACKUP_BYTES)throw BackupFormatException("Soubor je větší než povolených 5 MB.");output.write(buffer,0,n) }
        return decode(output.toByteArray())
    }
    fun encode(value: BackupFile): ByteArray {
        validate(value)
        return BackupJson.encodeToString(value).encodeToByteArray().also { require(it.size<=MAX_BACKUP_BYTES){"Záloha je příliš velká."} }
    }
    fun decode(bytes: ByteArray): BackupFile {
        require(bytes.size<=MAX_BACKUP_BYTES){"Soubor je větší než povolených 5 MB."}
        try {
            val text=Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
            var depth=0;var quoted=false;var escaped=false
            text.forEach { c ->
                if(quoted) { if(escaped) escaped=false else if(c=='\\') escaped=true else if(c=='"') quoted=false }
                else when(c) { '"' -> quoted=true;'{','[' -> {depth++;require(depth<=32){"Záloha má neplatnou strukturu."}};'}',']' -> depth-- }
            }
            val root=BackupJson.parseToJsonElement(text).jsonObject
            val version=root["formatVersion"]?.jsonPrimitive?.intOrNull
            if(version!=1)throw BackupFormatException("Verze zálohy ${version ?: "bez čísla"} není podporovaná. Tato aplikace čte formát 1.")
            return BackupJson.decodeFromJsonElement<BackupFile>(root).also(::validate)
        } catch(e: BackupFormatException) { throw e }
        catch(e: IllegalArgumentException) { throw BackupFormatException("Záloha má neplatné údaje nebo patří jiné aplikaci či variantě hry.") }
        catch(e: Exception) { throw IllegalArgumentException("Soubor není platná záloha průvodce.",e) }
    }
    fun validate(file: BackupFile) {
        require(file.formatVersion==1 && file.appId=="cz.jenda.wowforever" && file.gameVariant=="WOW_FOREVER"){"Záloha patří jiné aplikaci nebo variantě hry."}
        require(file.exportedAt>=0 && file.profiles.size in 1..100){"Neplatný počet profilů."}
        unique(file.profiles.map { it.profile.id });require(file.activeProfileId in file.profiles.map { it.profile.id }){"Aktivní profil v záloze chybí."}
        require(file.profiles.sumOf { it.rowCount }<=50000){"Záloha obsahuje příliš mnoho záznamů."}
        unique(file.profiles.flatMap { it.notes }.map { it.id })
        file.profiles.forEach(::validateProfile)
    }
    fun validateProfile(p: ProfileBackup) {
        val profile=p.profile
        text(profile.id,150);text(profile.displayName,60)
        // A saved area remains valid for backup even when the active content was rolled back.
        require(profile.faction=="HORDE" && profile.playerClass=="WARLOCK" && profile.race in setOf("ORC","TROLL","UNDEAD") && profile.level in 1..60 && profile.currentUiMapId in cz.jenda.wowforever.content.SupportedMapFrames.ids){"Nepodporovaný profil postavy."}
        require(profile.ruleset==null || profile.ruleset.length<=60)
        val professions=BackupJson.decodeFromString<List<String>>(profile.professionsJson)
        require(professions.size<=20 && professions.all { it.isNotBlank() && it.length<=160 })
        fun owner(id: String) { require(id==profile.id){"Záznam odkazuje na jiný profil."} }
        fun time(n:Long) { require(n>=0){"Neplatný čas záznamu."} }
        unique(p.quests.map { it.questKey });unique(p.items.map { it.itemKey });unique(p.bookmarks.map { it.entryKey })
        unique(p.notes.map { it.id });unique(p.stops.map { it.position.toString() });unique(p.travel.map { it.travelKey });unique(p.rewards.map { it.questKey })
        p.quests.forEach { owner(it.profileId);key(it.questKey,"quest");text(it.nameSnapshot,500);require(it.state in QuestState.entries.map { s->s.name });time(it.updatedAt) }
        p.items.forEach { owner(it.profileId);key(it.itemKey,"item");text(it.nameSnapshot,500);time(it.acquiredAt) }
        p.bookmarks.forEach { owner(it.profileId);require(it.kind in EntryKind.entries.map { k->k.name });key(it.entryKey,it.kind.lowercase());text(it.nameSnapshot,500);time(it.savedAt) }
        p.notes.forEach {
            owner(it.profileId);text(it.id,200);text(it.text,2000);time(it.updatedAt)
            if(it.entryKey!=null) anyKey(it.entryKey)
            if(it.entryNameSnapshot!=null) text(it.entryNameSnapshot,500)
            // Entity validation also rejects incomplete locations and nonfinite coordinates.
            if(it.uiMapId!=null) require(it.uiMapId>0 && it.floorId!!>=0 && it.xPercent!! in 0.0..100.0 && it.yPercent!! in 0.0..100.0)
        }
        p.stops.forEach { owner(it.profileId);require(it.position>=0);anyKey(it.entryKey);text(it.nameSnapshot,500) }
        p.travel.forEach { owner(it.profileId);key(it.travelKey,"travel");text(it.nameSnapshot,500);time(it.discoveredAt) }
        p.rewards.forEach { owner(it.profileId);key(it.questKey,"quest");key(it.itemKey,"item");text(it.questNameSnapshot,500);text(it.itemNameSnapshot,500);time(it.updatedAt) }
    }
    private fun key(k:String,kind:String) { require(k.startsWith("wow-forever:$kind:") && k.length<=200 && k.substringAfterLast(':').isNotBlank()){"Neplatný identifikátor záznamu."} }
    private fun anyKey(k:String) { require(EntryKind.entries.any { k.startsWith("wow-forever:${it.name.lowercase()}:") });text(k,200) }
    private fun text(value:String,max:Int) { require(value.isNotBlank() && value.length<=max && '\u0000' !in value){"Záloha obsahuje neplatný text."} }
    private fun unique(keys:List<String>) { require(keys.distinct().size==keys.size){"Záloha obsahuje duplicitní záznamy."} }
}
