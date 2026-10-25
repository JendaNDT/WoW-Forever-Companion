package cz.jenda.wowforever

import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.data.*
import cz.jenda.wowforever.model.*
import org.junit.Test
import org.junit.Assert.*

class E04CatalogTest {
    private val p=samplePackage()
    @Test fun searchesCzechAliasesWithoutAccentsAndSeparatesIdenticallyNamedQuests() {
        assertEquals("wow-forever:spell:688",searchEntries(p.entries,"PRIVOLANI   IMPA").single().key)
        val quests=searchEntries(p.entries,"Vile Familiars").filter { it.kind == EntryKind.QUEST }
        assertEquals(setOf(792,1485,1499),quests.map { it.gameId }.toSet())
        assertTrue(searchEntries(p.entries,"neni v zadnem zaznamu").isEmpty())
    }
    @Test fun multiplePositionsFloorsAndTransformsStayDistinct() {
        val location=p.locations.first { it.entryKey == "wow-forever:npc:5765" }
        val base=p.maps.first { it.uiMapId == 1411 }
        val floor=base.copy(key="test-floor",floorId=1,transform=TransformRecord(0.8,0.7,0.1,0.2))
        val additional=location.copy(key="second-position",mapKey=floor.key,floorId=1,xPercent=20.0,yPercent=30.0,precisionPercent=4.0)
        val atlas=buildAtlas(p.maps+floor,p.entries,p.sources.associate { it.id to it.url },p.locations+additional)
        val points=atlas.points.filter { it.entryKey == location.entryKey }
        assertEquals(2,points.size);assertEquals(2,points.map { it.id }.distinct().size)
        assertEquals(1,atlas.pointsFor(1411,1).size)
        val point=points.single { it.position.floorId == 1 }
        assertTrue(point.approximate)
        val (x,y)=point.position.normalized(atlas.map(1411,1))
        assertEquals(0.26,x,0.00001);assertEquals(0.41,y,0.00001)
        val (px,py)=floor.transform.percent(x,y)
        assertEquals(20.0,px,0.00001);assertEquals(30.0,py,0.00001)
        assertThrows(IllegalArgumentException::class.java) { point.position.normalized(atlas.map(1411,0)) }
    }
    @Test fun absentCoordinatesStayAbsentAndBookmarkFilterUsesEntryIdentity() {
        val atlas=buildAtlas(p.maps,p.entries,p.sources.associate { it.id to it.url },p.locations)
        assertEquals(8,atlas.points.size)
        assertTrue(atlas.points.none { it.entryKey == "wow-forever:npc:3156" || it.entryKey == "wow-forever:item:5778" })
        val a=atlas.points.first(); val b=a.copy(id="alternate",position=a.position.copy(xPercent=20.0))
        assertEquals(setOf(a.id,b.id),filterPoints(atlas.points+b,setOf("NPC"),true,setOf(a.entryKey!!)).map { it.id }.toSet())
        assertTrue(filterPoints(atlas.points,setOf("NOTE"),false,emptySet()).isEmpty())
    }
}
