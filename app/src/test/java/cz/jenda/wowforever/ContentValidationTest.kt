package cz.jenda.wowforever

import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.model.*
import org.junit.Assert.*
import org.junit.Test

// Frozen E01–E06 fixture keeps migration/regression assertions tied to the actual old package.
fun samplePackage(): ContentPackage = decodePackage(ContentValidationTest::class.java.getResourceAsStream("/legacy-e06.json")!!.use { it.readBytes() })
fun currentPackage(): ContentPackage = decodePackage(ContentValidationTest::class.java.getResourceAsStream("/starter.json")!!.use { it.readBytes() })

class ContentValidationTest {
    private val p = samplePackage()
    private fun rejects(pack: ContentPackage) { assertThrows(InvalidContent::class.java) { ContentValidator.validate(pack) } }
    @Test fun sourceSamplePassesWithoutInventingUnknownLevels() {
        ContentValidator.validate(p)
        assertEquals(21, p.entries.size); assertEquals(8, p.locations.size)
        assertEquals(26, p.sources.size); assertEquals(25, p.relations.size)
        assertTrue(p.entries.filter { it.kind == EntryKind.QUEST }.all { it.minimumLevel == null })
        assertTrue(p.entries.none { it.releaseConfirmed })
        assertEquals(setOf(792,1485,1499), p.entries.filter { it.nameOriginal == "Vile Familiars" }.map { it.gameId }.toSet())
        assertFalse(p.locations.any { it.entryKey.endsWith(":3156") })
    }
    @Test fun duplicateIdentityIsRejected() { rejects(p.copy(entries = p.entries+p.entries.first())) }
    @Test fun wrongGameVariantIsRejected() { rejects(p.copy(gameVariant = "classic")) }
    @Test fun areaIdCannotReplaceTheMapFrame() { rejects(p.copy(maps=p.maps.map { if(it.uiMapId==1411) it.copy(uiMapId=14) else it })) }
    @Test fun unknownFormatIsRejected() { rejects(p.copy(schemaVersion = 99)) }
    @Test fun missingTargetIsRejected() { rejects(p.copy(relations = p.relations.mapIndexed { i,r -> if(i==0) r.copy(targetKeys=listOf("wow-forever:quest:99999")) else r })) }
    @Test fun wrongTargetKindIsRejected() { rejects(p.copy(relations = p.relations.mapIndexed { i,r -> if(i==0) r.copy(targetKeys=listOf("wow-forever:npc:5765")) else r })) }
    @Test fun missingSourceIsRejected() { rejects(p.copy(entries=p.entries.mapIndexed { i,e -> if(i==0) e.copy(sourceIds=listOf("missing")) else e })) }
    @Test fun outOfBoundsAndNonfiniteCoordinatesAreRejected() {
        listOf(-0.1, 100.1, Double.NaN).forEach { x ->
            rejects(p.copy(locations=p.locations.mapIndexed { i,l -> if(i==0) l.copy(xPercent=x) else l }))
        }
    }
    @Test fun mismatchedFrameAndFloorAreRejected() {
        rejects(p.copy(locations=p.locations.mapIndexed { i,l -> if(i==0) l.copy(floorId=1) else l }))
        rejects(p.copy(locations=p.locations.mapIndexed { i,l -> if(i==0) l.copy(coordinateVersion="other") else l }))
    }
    @Test fun cyclicAnyPrerequisitesAreRejected() {
        val relation = RelationRecord("wow-forever:relation:test-cycle", RelationKind.PREREQUISITE,
            "wow-forever:quest:788", listOf("wow-forever:quest:3090"), MatchMode.ANY, Verification.CLASSIC, sourceIds=listOf("eq_available"))
        rejects(p.copy(relations=p.relations+relation))
    }
    @Test fun betaReferenceCannotBecomeActiveRouting() {
        rejects(p.copy(relations=p.relations.mapIndexed { i,r -> if(i==0) r.copy(routingEnabled=true) else r }))
    }
    @Test fun falseReleaseLabelIsRejected() {
        rejects(p.copy(entries=p.entries.mapIndexed { i,e -> if(i==0) e.copy(releaseConfirmed=true) else e }))
    }
    @Test fun releaseClaimNeedsAnActualReleaseObservation() {
        rejects(p.copy(entries=p.entries.mapIndexed { i,e -> if(i==0) e.copy(releaseConfirmed=true,verification=Verification.RELEASE) else e }))
    }
    @Test fun anyGroupAndConflictingClaimsArePreserved() {
        assertEquals(MatchMode.ANY,p.relations.first { it.key.endsWith("pre:794") }.mode)
        assertEquals(2,p.relations.first { it.key.endsWith("pre:794") }.targetKeys.size)
        val quest = p.entries.first { it.gameId==1485 && it.kind==EntryKind.QUEST }
        assertEquals(Verification.CONFLICT,quest.verification)
        assertTrue(quest.payload.contains("\"value\":6")); assertTrue(quest.payload.contains("\"value\":8"))
    }
    @Test fun transformedCoordinatesRoundTripWithoutChangingIdentity() {
        val transform=TransformRecord(.8,.7,.05,.08)
        val map=MapDescriptor(1411,"Durotar",1002,668,3,"beta","https://example.test",transform,0)
        val (x,y)=MapPosition(1411,43.1,68.7).normalized(map)
        val (px,py)=transform.percent(x,y)
        assertEquals(43.1,px,.000001);assertEquals(68.7,py,.000001)
        assertThrows(IllegalArgumentException::class.java) { MapPosition(1411,43.1,68.7,1).normalized(map) }
        rejects(p.copy(maps=p.maps.mapIndexed { i,m -> if(i==0) m.copy(transform=TransformRecord(scaleX=0.0)) else m }))
    }
    @Test fun parserRejectsUnknownFieldsAndMalformedInput() {
        assertThrows(Exception::class.java) { decodePackage("{bad".toByteArray()) }
        assertThrows(Exception::class.java) { decodePackage("{\"schemaVersion\":1,\"unexpected\":true}".toByteArray()) }
        assertThrows(IllegalArgumentException::class.java) { decodePackage(ByteArray(MAX_PACKAGE_BYTES+1)) }
    }
}
