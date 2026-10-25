package cz.jenda.wowforever

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.data.*
import cz.jenda.wowforever.data.db.*
import cz.jenda.wowforever.domain.QuestRules
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Synthetic rule fixtures stay in tests; no invented exclusive group enters the shipped catalog. */
internal fun exclusiveFixture(enabled:Boolean=true):ContentPackage {
    val p=samplePackage();val keys=setOf("wow-forever:quest:1485","wow-forever:quest:1499")
    val proof=SourceRecord("test:exclusive-proof","https://example.org/observation","in_game_release_observation","2026-10-23",payload="Synthetic test only")
    return p.copy(sources=p.sources+proof,entries=p.entries.map { if(it.key in keys)it.copy(verification=Verification.RELEASE,releaseConfirmed=true,sourceIds=listOf(proof.id)) else it },
        relations=p.relations+RelationRecord("wow-forever:relation:test-exclusive",RelationKind.EXCLUSIVE_QUESTS,"wow-forever:quest:1485",listOf("wow-forever:quest:1499"),MatchMode.ANY,Verification.RELEASE,enabled,listOf(proof.id)))
}
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class E05ExclusiveTest {
    @Test fun installedExclusiveRelationSurvivesRoomAndIsEvaluatedSymmetrically()=runBlocking {
        val p=exclusiveFixture();val db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),ContentDatabase::class.java).build()
        try {
            CatalogInstaller(db).install(ContentJson.encodeToString(p).encodeToByteArray())
            val rows=db.relationRecords();val rule=rows.single { it.kind==RelationKind.EXCLUSIVE_QUESTS }
            assertEquals(listOf("test:exclusive-proof"),rule.sourceIds)
            val rules=QuestRules(rows);val a="wow-forever:quest:1485";val b="wow-forever:quest:1499"
            assertEquals(setOf(a),rules.excludedBy(b,mapOf(a to QuestState.COMPLETED)))
            assertEquals(setOf(b),rules.excludedBy(a,mapOf(b to QuestState.COMPLETED)))
            assertTrue(rules.excludedBy(a,mapOf(b to QuestState.DEFERRED)).isEmpty())
            assertTrue(QuestRules(exclusiveFixture(false).relations).excludedBy(a,mapOf(b to QuestState.COMPLETED)).isEmpty())
        } finally {db.close()}
    }
    @Test fun unsupportedOrUnprovedExclusiveGroupIsRejectedAtImportBoundary() {
        val p=exclusiveFixture();val r=p.relations.last()
        for(bad in listOf(r.copy(targetKeys=listOf("wow-forever:npc:5765")),r.copy(targetKeys=listOf(r.ownerKey)),r.copy(mode=MatchMode.ALL),r.copy(sourceIds=listOf(p.sources.first().id)),r.copy(verification=Verification.CLASSIC))) {
            assertThrows(InvalidContent::class.java){ContentValidator.validate(p.copy(relations=p.relations.dropLast(1)+bad))}
        }
    }
}
