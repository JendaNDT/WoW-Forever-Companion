package cz.jenda.wowforever.data

import androidx.room.withTransaction
import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.data.db.*
import kotlinx.serialization.encodeToString
import java.security.MessageDigest

enum class InstallResult { INSTALLED, UNCHANGED }

class CatalogInstaller(private val db: ContentDatabase) {
    suspend fun install(bytes: ByteArray): InstallResult {
        val pack = decodePackage(bytes)
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        return installDecoded(pack,hash)
    }
    /** Caller binds the already decoded catalog to a verified archive file digest. */
    internal suspend fun installDecoded(pack:ContentPackage,hash:String):InstallResult {
        require(hash.matches(Regex("[a-f0-9]{64}")))
        ContentValidator.validate(pack)
        return db.withTransaction {
            val dao = db.catalog()
            dao.metadata()?.let { old ->
                require(pack.packageId == old.packageId && pack.generation >= old.generation) { "Starší nebo odlišný balíček nelze instalovat." }
                if (pack.generation == old.generation) {
                    require(hash == old.sha256) { "Stejná generace má jiný obsah." }
                    return@withTransaction InstallResult.UNCHANGED
                }
            }
            dao.clearGuideRules(); dao.clearRelations(); dao.clearEntries(); dao.clearMaps(); dao.clearSources()
            pack.sources.asSequence().chunked(128).forEach { batch -> dao.insertSources(batch.map { SourceEntity(it.id, it.url, it.kind, ContentJson.encodeToString(it)) }) }
            dao.insertMaps(pack.maps.map { MapEntity(it.key, it.uiMapId, it.floorId, it.coordinateVersion, it.sourceId, ContentJson.encodeToString(it)) })
            pack.entries.asSequence().chunked(128).forEach { batch ->
                dao.insertEntries(batch.map { EntryEntity(it.key, it.kind.name, it.gameId, it.nameOriginal,it.minimumLevel, it.searchText, ContentJson.encodeToString(it)) })
                dao.insertEntrySources(batch.flatMap { e -> e.sourceIds.map { EntrySourceEntity(e.key, it) } })
            }
            pack.locations.asSequence().chunked(256).forEach { batch -> dao.insertLocations(batch.map { LocationEntity(it.key, it.entryKey, it.mapKey, it.sourceId,it.floorId, it.xPercent, it.yPercent, it.precisionPercent, it.coordinateVersion) }) }
            dao.insertRelations(pack.relations.map { RelationEntity(it.key, it.ownerKey, it.kind.name, it.mode.name, it.verification.name, it.routingEnabled) })
            pack.relations.asSequence().flatMap { r -> r.targetKeys.asSequence().map { RelationTargetEntity(r.key, it) } }.chunked(256).forEach { dao.insertTargets(it) }
            dao.insertRelationSources(pack.relations.flatMap { r -> r.sourceIds.map { RelationSourceEntity(r.key, it) } })
            dao.insertGuideRules(pack.guideRules.map { GuideRuleEntity(it.key,it.entryKey,ContentJson.encodeToString(it)) })
            dao.setMetadata(PackageEntity(packageId = pack.packageId, generation = pack.generation,
                schemaVersion = pack.schemaVersion, sha256 = hash, checkedOn = pack.checkedOn,
                transportJson = ContentJson.encodeToString(pack.transportGraph)))
            InstallResult.INSTALLED
        }
    }
}
