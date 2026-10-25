package cz.jenda.wowforever

import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.data.ProfileBackup
import cz.jenda.wowforever.data.db.ProfileEntity
import cz.jenda.wowforever.domain.RecommendationEngine
import kotlinx.serialization.encodeToString
import org.junit.Test
import org.junit.Assert.*
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.SequenceInputStream

class E10MemoryFixTest {
    @Test fun npcsSharingAnItemDropAreNotRelatedToEveryOtherDropSource() {
        val item=EntryRecord("wow-forever:item:1",EntryKind.ITEM,1,"Item","Předmět",verification=Verification.COMMUNITY,sourceIds=listOf("test"),payload="{}")
        val npcs=(1..1500).map { i -> EntryRecord("wow-forever:npc:$i",EntryKind.NPC,i,"NPC $i","Postava",verification=Verification.COMMUNITY,sourceIds=listOf("test"),payload="{}") }
        val relation=RelationRecord("wow-forever:relation:drop",RelationKind.ITEM_SOURCE_NPC,item.key,npcs.map { it.key },MatchMode.ANY,Verification.COMMUNITY,sourceIds=listOf("test"))
        val report=RecommendationEngine(npcs+item,listOf(relation),emptyList(),emptyMap()).evaluate(ProfileBackup(ProfileEntity()))
        assertEquals(npcs.size,report.all.single { it.entry.key==item.key }.relatedKeys.size)
        assertTrue(report.all.filter { it.entry.kind==EntryKind.NPC }.all { it.relatedKeys==setOf(item.key) })
        assertEquals(3000,report.all.sumOf { it.relatedKeys.size })
    }
    @Test fun streamingDecoderPreservesCzechAndQuotedPayloadAcrossTinyReads() {
        val p=samplePackage();val bytes=ContentJson.encodeToString(p).toByteArray()
        val small=object:ByteArrayInputStream(bytes) {
            override fun read(buffer:ByteArray,offset:Int,length:Int)=super.read(buffer,offset,minOf(length,7))
        }
        assertEquals(p,small.use { decodePackage(it) })
    }
    @Test fun streamingDecoderStillRejectsTooDeepDataAndTrailingDocuments() {
        val deep=("[".repeat(33)+"0"+"]".repeat(33)).byteInputStream()
        assertThrows(IllegalArgumentException::class.java){deep.use { decodePackage(it) }}
        val p=ContentJson.encodeToString(samplePackage())
        assertThrows(IllegalArgumentException::class.java){decodePackage((p+" {}").byteInputStream())}
    }
    @Test fun streamingDecoderCountsTrailingWhitespaceWithoutBufferingOversizedInput() {
        val json=ContentJson.encodeToString(samplePackage()).toByteArray()
        var remaining=MAX_PACKAGE_BYTES-json.size+1
        val padding=object:InputStream() {
            override fun read():Int=if(remaining-- >0)32 else -1
            override fun read(buffer:ByteArray,offset:Int,length:Int):Int {
                if(remaining<=0)return -1
                val n=minOf(remaining,length);buffer.fill(32.toByte(),offset,offset+n);remaining-=n;return n
            }
        }
        assertThrows(IllegalArgumentException::class.java){SequenceInputStream(ByteArrayInputStream(json),padding).use { decodePackage(it) }}
    }
}
