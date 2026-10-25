package cz.jenda.wowforever

import cz.jenda.wowforever.data.*
import cz.jenda.wowforever.data.db.*
import org.junit.Test
import org.junit.Assert.*

internal fun backupProfile(id:String=DEFAULT_PROFILE_ID)=ProfileBackup(
    ProfileEntity(id=id,displayName="Jenda",level=12,currentUiMapId=1454,ruleset="K ověření",professionsJson="[\"Tailoring\"]"),
    listOf(QuestProgressEntity(id,"wow-forever:quest:990001","Historický úkol",QuestState.DEFERRED.name,1)),
    listOf(CollectedItemEntity(id,"wow-forever:item:5778","Primitive Walking Stick",2)),
    listOf(BookmarkEntity(id,"wow-forever:item:990002","Přání mimo katalog","ITEM",3)),
    listOf(NoteEntity("note:shared",id,"Vrátit se sem",null,1454,0,27.3,68.4,4)),
    listOf(PlannedStopEntity(id,0,"wow-forever:npc:5765","Ruzan")),
    listOf(DiscoveredTravelEntity(id,"wow-forever:travel:durotar-undercity","Doprava",5)),
    listOf(RewardChoiceEntity(id,"wow-forever:quest:792","wow-forever:item:5778","Vile Familiars","Primitive Walking Stick",6)))
internal fun backupFile(p:ProfileBackup=backupProfile())=BackupFile(exportedAt=123,activeProfileId=p.profile.id,profiles=listOf(p))

class E05BackupTest {
    @Test fun portableUtf8RoundTripKeepsEveryFieldAndUnknownContentHistory() {
        val file=backupFile()
        assertEquals(file,BackupCodec.read(BackupCodec.encode(file).inputStream()))
        assertEquals(7,file.profiles.single().rowCount)
    }
    @Test fun newerVersionVariantAndForeignOwnerAreRefused() {
        val bytes=BackupCodec.encode(backupFile()).decodeToString()
        listOf(bytes.replace("\"formatVersion\":1","\"formatVersion\":2"),bytes.replace("WOW_FOREVER","CLASSIC"),bytes.replace("\"profileId\":\"profile:default\"","\"profileId\":\"profile:other\""))
            .forEach { bad -> assertThrows(IllegalArgumentException::class.java){BackupCodec.decode(bad.encodeToByteArray())} }
    }
    @Test fun duplicatesTruncationInvalidUtf8UnknownFieldsAndIncompleteLocationAreRefused() {
        val p=backupProfile();val encoded=BackupCodec.encode(backupFile()).decodeToString()
        assertThrows(IllegalArgumentException::class.java){BackupCodec.encode(backupFile(p.copy(quests=p.quests+p.quests)))}
        listOf(encoded.dropLast(8),encoded.replace("\"floorId\":0","\"floorId\":null"),encoded.replace("\"exportedAt\":123","\"unrecognized\":true,\"exportedAt\":123"))
            .forEach { assertThrows(IllegalArgumentException::class.java){BackupCodec.decode(it.encodeToByteArray())} }
        assertThrows(IllegalArgumentException::class.java){BackupCodec.decode(byteArrayOf(0xc3.toByte(),0x28))}
    }
    @Test fun oversizedAndDeepInputAreBoundedBeforeDatabaseAccess() {
        assertThrows(IllegalArgumentException::class.java){BackupCodec.read(ByteArray(MAX_BACKUP_BYTES+1){32}.inputStream())}
        assertThrows(IllegalArgumentException::class.java){BackupCodec.decode(("[".repeat(40)+"]".repeat(40)).encodeToByteArray())}
        // Brackets inside quoted note text do not count as nesting.
        val p=backupProfile();val text="[".repeat(80)
        assertEquals(text,BackupCodec.decode(BackupCodec.encode(backupFile(p.copy(notes=p.notes.map { it.copy(text=text) })))).profiles.single().notes.single().text)
    }
}
