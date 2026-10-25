package cz.jenda.wowforever
import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.packages.sequenceParts
import java.io.ByteArrayInputStream
import java.io.File
import org.junit.Test
import org.junit.Assert.*
class E10AuditDataTest {
    @Test fun correctedCatalogKeepsConflictsAndClassContent() {
        val p=java.util.zip.GZIPInputStream(File("../content/e10-v8.json.gz").inputStream()).use { decodePackage(it) }
        ContentValidator.validate(p);assertEquals(11912,p.entries.size);assertEquals(17330,p.locations.size)
        assertTrue(p.entries.map { it.key }.containsAll(WarlockQuestIndex.keys))
        for(id in listOf(1485,95328,96130))assertEquals(Verification.CONFLICT,p.entries.single { it.key=="wow-forever:quest:$id" }.verification)
        assertTrue(p.entries.none { it.releaseConfirmed });assertTrue(p.relations.none { it.routingEnabled })
    }
    @Test fun partsAreOpenedLazilyAndClosedWhenConsumerStops() {
        var active=0;var peak=0;var opened=0
        val input=sequenceParts(listOf("one","two","three")) {
            active++;opened++;peak=maxOf(peak,active)
            object:ByteArrayInputStream(byteArrayOf(1,2)) { override fun close(){active--;super.close()} }
        }
        assertEquals(1,opened)
        input.use { assertArrayEquals(byteArrayOf(1,2,1,2,1,2),it.readBytes()) }
        assertEquals(3,opened);assertEquals(1,peak);assertEquals(0,active)
        val partial=sequenceParts(listOf("one","two")) { object:ByteArrayInputStream(byteArrayOf(1)) { init{active++};override fun close(){active--;super.close()} } }
        partial.close();assertEquals(0,active)
    }
}
