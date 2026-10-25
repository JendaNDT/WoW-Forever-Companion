package cz.jenda.wowforever

import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.data.*
import cz.jenda.wowforever.data.db.*
import cz.jenda.wowforever.domain.*
import cz.jenda.wowforever.packages.*
import org.junit.Test
import org.junit.Assert.*
import java.io.File

fun fullCatalog()=java.util.zip.GZIPInputStream(File("../content/e10-v7.json.gz").inputStream()).use { decodePackage(it) }
class E10FullDataTest {
    companion object { val p by lazy { fullCatalog() } }
    @Test fun fullContractTypedObjectsAndClassicRecordPreservation() {
        ContentValidator.validate(p);assertEquals(12352,p.entries.size);assertEquals(61,p.maps.size);assertEquals(18441,p.locations.size)
        assertTrue(p.entries.containsAll(e10Batch3Package().entries))
        assertEquals(115,WarlockQuestIndex.keys.size) // Current index; this catalog stays frozen at generation 7.
        val atlas=buildAtlas(p.maps,p.entries,p.sources.associate { it.id to it.url },p.locations)
        assertEquals(18441,atlas.points.size);assertTrue(atlas.points.any { it.kind.name=="OBJECT" })
        assertTrue(atlas.points.any { it.verification==Verification.COMMUNITY && it.name.endsWith(" · komunitní") })
        assertTrue(atlas.points.none { it.position.uiMapId>=900000 })
    }
    @Test fun communityPositionsNeedExplicitProvenanceAndCannotProduceVerifiedRoutes() {
        val single=p.locations.groupingBy { it.entryKey }.eachCount().filterValues { it==1 }.keys
        val entry=p.entries.first { it.referencePosition && it.verification==Verification.COMMUNITY && it.key in single }
        val limited=resolveStops(listOf(PlannedStopEntity(DEFAULT_PROFILE_ID,0,entry.key,entry.nameOriginal)),p.entries,p.locations).single()
        assertNotNull(limited.location);assertTrue(limited.limitation!!.contains("Komunitní"))
        val hidden=buildAtlas(p.maps,listOf(entry.copy(referencePosition=false)),p.sources.associate { it.id to it.url },p.locations.filter { it.entryKey==entry.key })
        assertTrue(hidden.points.isEmpty())
        assertThrows(InvalidContent::class.java) { ContentValidator.validate(p.copy(entries=p.entries.map { if(it.key==entry.key)it.copy(verification=Verification.BETA) else it })) }
    }
    @Test fun manifestRequiresNewAppAndNoUnknownFrameIsAccepted() {
        val raw=File("../content/e10-v7.manifest.json").readBytes();val sig=File("../content/e10-v7.manifest.sig").readBytes();val pub=File("src/main/assets/content/publisher.der").readBytes()
        assertThrows(IllegalArgumentException::class.java){ManifestValidator.decode(raw,sig,pub,14)}
        val m=ManifestValidator.decode(raw,sig,pub,15);ManifestValidator.catalog(m,p)
        assertThrows(IllegalArgumentException::class.java){ManifestValidator.catalog(m.copy(minimumAppCode=14),p)}
        assertThrows(InvalidContent::class.java){ContentValidator.validate(p.copy(maps=p.maps+ p.maps.last().copy(key="wow-forever:map:999999",uiMapId=999999)))}
    }
    @Test fun largeCatalogRecommendationsAreStillReferencesAndNewMapBackupsRoundTrip() {
        val direct=p.locations.groupBy { it.entryKey }.mapValues { it.value.map { l->l.mapKey.substringAfterLast(':').toInt() }.toSet() }
        val personal=ProfileBackup(ProfileEntity(level=60,currentUiMapId=1452))
        val report=RecommendationEngine(p.entries,p.relations,p.guideRules,direct).evaluate(personal)
        assertEquals(p.entries.size,report.all.size);assertTrue(report.all.all { it.availability==Availability.UNVERIFIED && it.unlockCount==0 })
        assertTrue(report.all.sumOf { it.relatedKeys.size } <= 2*p.relations.sumOf { it.targetKeys.size }+5*p.guideRules.size)
        val backup=BackupFile(exportedAt=1,activeProfileId=DEFAULT_PROFILE_ID,profiles=listOf(personal.copy(bookmarks=listOf(BookmarkEntity(DEFAULT_PROFILE_ID,"wow-forever:object:1","Objekt","OBJECT",1)))))
        assertEquals(backup,BackupCodec.decode(BackupCodec.encode(backup)))
    }
}
