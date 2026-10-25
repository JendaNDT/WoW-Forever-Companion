package cz.jenda.wowforever.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "package_metadata")
data class PackageEntity(@PrimaryKey val slot: Int = 1, val packageId: String, val generation: Int,
    val schemaVersion: Int, val sha256: String, val checkedOn: String,
    @ColumnInfo(defaultValue="'{}'") val transportJson:String = "{}")

@Entity(tableName = "sources")
data class SourceEntity(@PrimaryKey val id: String, val url: String, val kind: String, val recordJson: String)

@Entity(tableName = "maps", foreignKeys = [ForeignKey(entity = SourceEntity::class,
    parentColumns = ["id"], childColumns = ["sourceId"])], indices = [Index("sourceId"), Index(value = ["uiMapId", "floorId", "coordinateVersion"], unique = true)])
data class MapEntity(@PrimaryKey val key: String, val uiMapId: Int, val floorId: Int,
    val coordinateVersion: String, val sourceId: String, val recordJson: String)

@Entity(tableName = "entries", indices = [Index("kind"), Index("nameOriginal")])
data class EntryEntity(@PrimaryKey val key: String, val kind: String, val gameId: Int?,
    val nameOriginal: String, val minimumLevel: Int?, val searchText: String, val recordJson: String)

@Entity(tableName = "entry_sources", primaryKeys = ["entryKey", "sourceId"],
    foreignKeys = [ForeignKey(entity = EntryEntity::class, parentColumns = ["key"], childColumns = ["entryKey"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = SourceEntity::class, parentColumns = ["id"], childColumns = ["sourceId"])], indices = [Index("sourceId")])
data class EntrySourceEntity(val entryKey: String, val sourceId: String)

@Entity(tableName = "locations", foreignKeys = [
    ForeignKey(entity = EntryEntity::class, parentColumns = ["key"], childColumns = ["entryKey"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = MapEntity::class, parentColumns = ["key"], childColumns = ["mapKey"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = SourceEntity::class, parentColumns = ["id"], childColumns = ["sourceId"])],
    indices = [Index("entryKey"), Index("mapKey"), Index("sourceId")])
data class LocationEntity(@PrimaryKey val key: String, val entryKey: String, val mapKey: String,
    val sourceId: String, val floorId: Int, val xPercent: Double, val yPercent: Double,
    val precisionPercent: Double, val coordinateVersion: String)

@Entity(tableName = "relations", foreignKeys = [ForeignKey(entity = EntryEntity::class,
    parentColumns = ["key"], childColumns = ["ownerKey"], onDelete = ForeignKey.CASCADE)], indices = [Index("ownerKey")])
data class RelationEntity(@PrimaryKey val key: String, val ownerKey: String, val kind: String,
    val mode: String, val verification: String, val routingEnabled: Boolean)

@Entity(tableName = "relation_targets", primaryKeys = ["relationKey", "targetKey"], foreignKeys = [
    ForeignKey(entity = RelationEntity::class, parentColumns = ["key"], childColumns = ["relationKey"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = EntryEntity::class, parentColumns = ["key"], childColumns = ["targetKey"], onDelete = ForeignKey.CASCADE)], indices = [Index("targetKey")])
data class RelationTargetEntity(val relationKey: String, val targetKey: String)

@Entity(tableName = "relation_sources", primaryKeys = ["relationKey", "sourceId"], foreignKeys = [
    ForeignKey(entity = RelationEntity::class, parentColumns = ["key"], childColumns = ["relationKey"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = SourceEntity::class, parentColumns = ["id"], childColumns = ["sourceId"])], indices = [Index("sourceId")])
data class RelationSourceEntity(val relationKey: String, val sourceId: String)

@Entity(tableName="guide_rules",foreignKeys=[ForeignKey(entity=EntryEntity::class,parentColumns=["key"],childColumns=["entryKey"],onDelete=ForeignKey.CASCADE)],indices=[Index(value=["entryKey"],unique=true)])
data class GuideRuleEntity(@PrimaryKey val key:String,val entryKey:String,val recordJson:String)
data class SourceLink(val id:String,val url:String)

@Dao interface ContentDao {
    @Query("SELECT recordJson FROM entries ORDER BY kind, nameOriginal, gameId") fun entryRecordCursor():android.database.Cursor
    @Query("SELECT id,url FROM sources ORDER BY id") suspend fun sourceLinks():List<SourceLink>
    @Query("SELECT * FROM guide_rules ORDER BY `key`") suspend fun guideRules():List<GuideRuleEntity>
    @Insert suspend fun insertGuideRules(rows:List<GuideRuleEntity>)
    @Query("DELETE FROM guide_rules") suspend fun clearGuideRules()
    @Query("SELECT * FROM package_metadata WHERE slot=1") suspend fun metadata(): PackageEntity?
    @Query("SELECT * FROM entries ORDER BY kind, nameOriginal, gameId") suspend fun entries(): List<EntryEntity>
    @Query("SELECT * FROM entries WHERE `key`=:key") suspend fun entry(key: String): EntryEntity?
    @Query("SELECT * FROM entries WHERE instr(searchText, :query)>0 ORDER BY kind, nameOriginal") suspend fun search(query: String): List<EntryEntity>
    @Query("SELECT * FROM maps ORDER BY uiMapId") suspend fun maps(): List<MapEntity>
    @Query("SELECT * FROM locations ORDER BY `key`") suspend fun locations(): List<LocationEntity>
    @Query("SELECT * FROM sources ORDER BY id") suspend fun sources(): List<SourceEntity>
    @Query("SELECT * FROM relations ORDER BY `key`") suspend fun relations(): List<RelationEntity>
    @Query("SELECT * FROM relation_sources") suspend fun relationSources(): List<RelationSourceEntity>
    @Query("SELECT * FROM relation_targets") suspend fun targets(): List<RelationTargetEntity>
    @Insert suspend fun insertSources(rows: List<SourceEntity>)
    @Insert suspend fun insertMaps(rows: List<MapEntity>)
    @Insert suspend fun insertEntries(rows: List<EntryEntity>)
    @Insert suspend fun insertEntrySources(rows: List<EntrySourceEntity>)
    @Insert suspend fun insertLocations(rows: List<LocationEntity>)
    @Insert suspend fun insertRelations(rows: List<RelationEntity>)
    @Insert suspend fun insertTargets(rows: List<RelationTargetEntity>)
    @Insert suspend fun insertRelationSources(rows: List<RelationSourceEntity>)
    @Upsert suspend fun setMetadata(row: PackageEntity)
    @Query("DELETE FROM relations") suspend fun clearRelations()
    @Query("DELETE FROM entries") suspend fun clearEntries()
    @Query("DELETE FROM maps") suspend fun clearMaps()
    @Query("DELETE FROM sources") suspend fun clearSources()
}

@Database(entities = [PackageEntity::class, SourceEntity::class, MapEntity::class, EntryEntity::class,
    EntrySourceEntity::class, LocationEntity::class, RelationEntity::class, RelationTargetEntity::class,
    RelationSourceEntity::class, GuideRuleEntity::class], version = 3, exportSchema = true, autoMigrations=[AutoMigration(from=1,to=2),AutoMigration(from=2,to=3)])
abstract class ContentDatabase : RoomDatabase() { abstract fun catalog(): ContentDao }
