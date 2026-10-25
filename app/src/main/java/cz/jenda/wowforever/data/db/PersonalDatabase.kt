package cz.jenda.wowforever.data.db

import androidx.room.*
import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.Flow

const val DEFAULT_PROFILE_ID = "profile:default"
enum class QuestState { NOT_STARTED, IN_PROGRESS, COMPLETED, DEFERRED }

@Entity(tableName = "profiles")
@Serializable data class ProfileEntity(@PrimaryKey val id: String = DEFAULT_PROFILE_ID, val displayName: String = "Moje cesta",
    val faction: String = "HORDE", val playerClass: String = "WARLOCK", val race: String = "ORC",
    val level: Int = 1, val currentUiMapId: Int = 1411, val ruleset: String? = null,
    val professionsJson: String = "[]") {
    init { require(id.isNotBlank() && displayName.isNotBlank() && level in 1..60) }
}

@Entity(tableName = "quest_progress", primaryKeys = ["profileId", "questKey"], foreignKeys = [
    ForeignKey(entity = ProfileEntity::class, parentColumns = ["id"], childColumns = ["profileId"], onDelete = ForeignKey.CASCADE)])
@Serializable data class QuestProgressEntity(val profileId: String, val questKey: String, val nameSnapshot: String,
    val state: String, val updatedAt: Long) {
    init { require(questKey.startsWith("wow-forever:quest:") && state in QuestState.entries.map { it.name }) }
}

@Entity(tableName = "collected_items", primaryKeys = ["profileId", "itemKey"], foreignKeys = [
    ForeignKey(entity = ProfileEntity::class, parentColumns = ["id"], childColumns = ["profileId"], onDelete = ForeignKey.CASCADE)])
@Serializable data class CollectedItemEntity(val profileId: String, val itemKey: String, val nameSnapshot: String, val acquiredAt: Long)

@Entity(tableName = "bookmarks", primaryKeys = ["profileId", "entryKey"], foreignKeys = [
    ForeignKey(entity = ProfileEntity::class, parentColumns = ["id"], childColumns = ["profileId"], onDelete = ForeignKey.CASCADE)])
@Serializable data class BookmarkEntity(val profileId: String, val entryKey: String, val nameSnapshot: String, val kind: String, val savedAt: Long)

@Entity(tableName = "notes", foreignKeys = [ForeignKey(entity = ProfileEntity::class,
    parentColumns = ["id"], childColumns = ["profileId"], onDelete = ForeignKey.CASCADE)], indices = [Index("profileId")])
@Serializable data class NoteEntity(@PrimaryKey val id: String, val profileId: String, val text: String, val entryKey: String?,
    val uiMapId: Int?, val floorId: Int?, val xPercent: Double?, val yPercent: Double?, val updatedAt: Long,
    val entryNameSnapshot: String? = null) {
    init {
        require(text.isNotBlank())
        val unlocated = uiMapId == null && floorId == null && xPercent == null && yPercent == null
        require(unlocated || (uiMapId != null && uiMapId > 0 && floorId != null && floorId >= 0 &&
            xPercent != null && xPercent.isFinite() && xPercent in 0.0..100.0 &&
            yPercent != null && yPercent.isFinite() && yPercent in 0.0..100.0))
    }
}

@Entity(tableName = "planned_stops", primaryKeys = ["profileId", "position"], foreignKeys = [
    ForeignKey(entity = ProfileEntity::class, parentColumns = ["id"], childColumns = ["profileId"], onDelete = ForeignKey.CASCADE)])
@Serializable data class PlannedStopEntity(val profileId: String, val position: Int, val entryKey: String, val nameSnapshot: String)

@Entity(tableName = "discovered_travel", primaryKeys = ["profileId", "travelKey"], foreignKeys = [
    ForeignKey(entity = ProfileEntity::class, parentColumns = ["id"], childColumns = ["profileId"], onDelete = ForeignKey.CASCADE)])
@Serializable data class DiscoveredTravelEntity(val profileId: String, val travelKey: String, val nameSnapshot: String, val discoveredAt: Long)

@Entity(tableName = "reward_choices", primaryKeys = ["profileId","questKey"], foreignKeys = [
    ForeignKey(entity = ProfileEntity::class,parentColumns=["id"],childColumns=["profileId"],onDelete=ForeignKey.CASCADE)])
@Serializable data class RewardChoiceEntity(val profileId: String,val questKey: String,val itemKey: String,
    val questNameSnapshot: String,val itemNameSnapshot: String,val updatedAt: Long)

@Entity(tableName = "undo_actions", foreignKeys = [ForeignKey(entity=ProfileEntity::class,parentColumns=["id"],childColumns=["profileId"],onDelete=ForeignKey.CASCADE)])
data class UndoEntity(@PrimaryKey val profileId: String,val label: String,val payloadJson: String,val createdAt: Long)

@Entity(tableName = "personal_snapshots",foreignKeys=[ForeignKey(entity=ProfileEntity::class,parentColumns=["id"],childColumns=["profileId"],onDelete=ForeignKey.CASCADE)],indices=[Index("profileId")])
data class PersonalSnapshotEntity(@PrimaryKey val id: String,val profileId: String,val label: String,val createdAt: Long,val payloadJson: String)

@Dao interface PersonalDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun createProfile(profile: ProfileEntity): Long
    @Update suspend fun updateProfile(profile: ProfileEntity): Int
    @Query("SELECT * FROM profiles WHERE id=:id") suspend fun profile(id: String = DEFAULT_PROFILE_ID): ProfileEntity?
    @Query("SELECT * FROM bookmarks WHERE profileId=:id ORDER BY savedAt DESC") fun observeBookmarks(id: String = DEFAULT_PROFILE_ID): Flow<List<BookmarkEntity>>
    @Query("SELECT * FROM bookmarks WHERE profileId=:profile AND entryKey=:key") suspend fun bookmark(profile: String, key: String): BookmarkEntity?
    @Query("SELECT entryKey FROM bookmarks WHERE profileId=:profile") suspend fun bookmarkKeys(profile: String): List<String>
    @Upsert suspend fun saveBookmark(row: BookmarkEntity)
    @Query("DELETE FROM bookmarks WHERE profileId=:profile AND entryKey=:key") suspend fun deleteBookmark(profile: String, key: String)
    @Upsert suspend fun saveProgress(row: QuestProgressEntity)
    @Query("SELECT * FROM quest_progress WHERE profileId=:profile AND questKey=:key") suspend fun progress(profile: String, key: String): QuestProgressEntity?
    @Upsert suspend fun saveCollectedItem(row: CollectedItemEntity)
    @Query("SELECT * FROM collected_items WHERE profileId=:profile") suspend fun collectedItems(profile: String): List<CollectedItemEntity>
    @Upsert suspend fun saveNote(row: NoteEntity)
    @Query("SELECT * FROM notes WHERE profileId=:profile") suspend fun notes(profile: String): List<NoteEntity>
    @Query("SELECT * FROM notes WHERE profileId=:profile ORDER BY updatedAt DESC") fun observeNotes(profile: String = DEFAULT_PROFILE_ID): Flow<List<NoteEntity>>
    @Query("DELETE FROM notes WHERE id=:id AND profileId=:profile") suspend fun deleteNote(id: String, profile: String = DEFAULT_PROFILE_ID)
    @Upsert suspend fun savePlannedStop(row: PlannedStopEntity)
    @Upsert suspend fun saveDiscoveredTravel(row: DiscoveredTravelEntity)
    @Query("SELECT * FROM profiles ORDER BY displayName,id") suspend fun profiles(): List<ProfileEntity>
    @Query("SELECT * FROM profiles ORDER BY displayName,id") fun observeProfiles(): Flow<List<ProfileEntity>>
    @Query("DELETE FROM profiles WHERE id=:id") suspend fun deleteProfile(id: String)
    @Query("SELECT * FROM quest_progress WHERE profileId=:profile") suspend fun allProgress(profile: String): List<QuestProgressEntity>
    @Query("SELECT * FROM quest_progress WHERE profileId=:profile") fun observeProgress(profile: String): Flow<List<QuestProgressEntity>>
    @Query("DELETE FROM quest_progress WHERE profileId=:profile AND questKey=:key") suspend fun deleteProgress(profile: String,key: String)
    @Query("SELECT * FROM collected_items WHERE profileId=:profile") fun observeItems(profile: String): Flow<List<CollectedItemEntity>>
    @Query("DELETE FROM collected_items WHERE profileId=:profile AND itemKey=:key") suspend fun deleteItem(profile: String,key: String)
    @Query("SELECT * FROM bookmarks WHERE profileId=:profile ORDER BY savedAt DESC") suspend fun allBookmarks(profile: String): List<BookmarkEntity>
    @Query("SELECT * FROM planned_stops WHERE profileId=:profile ORDER BY position") suspend fun plannedStops(profile: String): List<PlannedStopEntity>
    @Query("SELECT * FROM discovered_travel WHERE profileId=:profile") suspend fun discoveredTravel(profile: String): List<DiscoveredTravelEntity>
    @Query("SELECT * FROM planned_stops WHERE profileId=:profile ORDER BY position") fun observeStops(profile:String):Flow<List<PlannedStopEntity>>
    @Query("SELECT * FROM discovered_travel WHERE profileId=:profile") fun observeTravel(profile:String):Flow<List<DiscoveredTravelEntity>>
    @Query("DELETE FROM discovered_travel WHERE profileId=:profile AND travelKey=:key") suspend fun deleteTravel(profile:String,key:String)
    @Query("SELECT * FROM reward_choices WHERE profileId=:profile") suspend fun rewards(profile: String): List<RewardChoiceEntity>
    @Query("SELECT * FROM reward_choices WHERE profileId=:profile") fun observeRewards(profile: String): Flow<List<RewardChoiceEntity>>
    @Upsert suspend fun saveReward(row: RewardChoiceEntity)
    @Query("DELETE FROM reward_choices WHERE profileId=:profile AND questKey=:quest") suspend fun deleteReward(profile: String,quest: String)
    @Upsert suspend fun saveUndo(row: UndoEntity)
    @Query("SELECT * FROM undo_actions WHERE profileId=:profile") suspend fun undo(profile: String): UndoEntity?
    @Query("SELECT * FROM undo_actions WHERE profileId=:profile") fun observeUndo(profile: String): Flow<UndoEntity?>
    @Query("DELETE FROM undo_actions WHERE profileId=:profile") suspend fun clearUndo(profile: String)
    @Insert suspend fun saveSnapshot(row: PersonalSnapshotEntity)
    @Query("SELECT * FROM personal_snapshots WHERE profileId=:profile ORDER BY createdAt DESC,id DESC") suspend fun snapshots(profile: String): List<PersonalSnapshotEntity>
    @Query("DELETE FROM personal_snapshots WHERE profileId=:profile AND id NOT IN (SELECT id FROM personal_snapshots WHERE profileId=:profile ORDER BY createdAt DESC,id DESC LIMIT 3)") suspend fun trimSnapshots(profile: String)
    @Insert suspend fun insertNotes(rows: List<NoteEntity>)
    @Insert suspend fun insertProgress(rows: List<QuestProgressEntity>)
    @Insert suspend fun insertItems(rows: List<CollectedItemEntity>)
    @Insert suspend fun insertBookmarks(rows: List<BookmarkEntity>)
    @Insert suspend fun insertStops(rows: List<PlannedStopEntity>)
    @Insert suspend fun insertTravel(rows: List<DiscoveredTravelEntity>)
    @Insert suspend fun insertRewards(rows: List<RewardChoiceEntity>)
    @Query("DELETE FROM quest_progress WHERE profileId=:profile") suspend fun clearProgress(profile: String)
    @Query("DELETE FROM collected_items WHERE profileId=:profile") suspend fun clearItems(profile: String)
    @Query("DELETE FROM bookmarks WHERE profileId=:profile") suspend fun clearBookmarks(profile: String)
    @Query("DELETE FROM notes WHERE profileId=:profile") suspend fun clearNotes(profile: String)
    @Query("DELETE FROM planned_stops WHERE profileId=:profile") suspend fun clearStops(profile: String)
    @Query("DELETE FROM discovered_travel WHERE profileId=:profile") suspend fun clearTravel(profile: String)
    @Query("DELETE FROM reward_choices WHERE profileId=:profile") suspend fun clearRewards(profile: String)

}

@Database(entities = [ProfileEntity::class, QuestProgressEntity::class, CollectedItemEntity::class,
    BookmarkEntity::class, NoteEntity::class, PlannedStopEntity::class, DiscoveredTravelEntity::class,RewardChoiceEntity::class,UndoEntity::class,PersonalSnapshotEntity::class], version = 2, exportSchema = true, autoMigrations = [AutoMigration(from = 1, to = 2)])
abstract class PersonalDatabase : RoomDatabase() { abstract fun personal(): PersonalDao }
