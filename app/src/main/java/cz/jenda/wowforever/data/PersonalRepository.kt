package cz.jenda.wowforever.data

import androidx.room.withTransaction
import cz.jenda.wowforever.data.db.*
import kotlinx.serialization.*
import java.util.UUID

@Serializable data class PersonalChange(val questKey:String?=null,val beforeQuest:QuestProgressEntity?=null,val afterQuest:QuestProgressEntity?=null,
    val itemKey:String?=null,val beforeItem:CollectedItemEntity?=null,val afterItem:CollectedItemEntity?=null,
    val rewardQuest:String?=null,val beforeReward:RewardChoiceEntity?=null,val afterReward:RewardChoiceEntity?=null)

class PersonalRepository(private val db:PersonalDatabase) {
    private val dao get()=db.personal()
    private suspend fun readProfile(id:String)=ProfileBackup(requireNotNull(dao.profile(id)),dao.allProgress(id),dao.collectedItems(id),
        dao.allBookmarks(id),dao.notes(id),dao.plannedStops(id),dao.discoveredTravel(id),dao.rewards(id))
    suspend fun profileBackup(id:String)=db.withTransaction { readProfile(id) }
    suspend fun addStop(profile:String,key:String,name:String)=db.withTransaction {
        require(key.length<=200 && isEntryKey(key) && name.isNotBlank() && name.length<=500)
        val rows=dao.plannedStops(profile);require(rows.size<200){"Výprava už má 200 cílů."}
        writeStops(profile,rows+PlannedStopEntity(profile,rows.size,key,name))
    }
    suspend fun moveStop(profile:String,position:Int,delta:Int)=db.withTransaction {
        require(delta==-1 || delta==1)
        val rows=dao.plannedStops(profile).toMutableList();val index=rows.indexOfFirst { it.position==position }
        if(index>=0 && index+delta in rows.indices) { val row=rows.removeAt(index);rows.add(index+delta,row);writeStops(profile,rows) }
    }
    suspend fun removeStop(profile:String,position:Int)=db.withTransaction { writeStops(profile,dao.plannedStops(profile).filter { it.position!=position }) }
    private suspend fun writeStops(profile:String,rows:List<PlannedStopEntity>) {
        dao.clearStops(profile);dao.insertStops(rows.mapIndexed { i,row -> row.copy(profileId=profile,position=i) })
    }
    private fun isEntryKey(key:String)=cz.jenda.wowforever.content.EntryKind.entries.any { key.startsWith("wow-forever:${it.name.lowercase()}:") && key.substringAfterLast(':').isNotBlank() }
    suspend fun setTravel(profile:String,key:String,name:String,discovered:Boolean)=db.withTransaction {
        require(key.startsWith("wow-forever:travel:") && key.length<=200 && name.isNotBlank() && name.length<=500)
        if(discovered)dao.saveDiscoveredTravel(DiscoveredTravelEntity(profile,key,name,System.currentTimeMillis())) else dao.deleteTravel(profile,key)
    }
    suspend fun export(active:String):ByteArray = db.withTransaction {
        BackupCodec.encode(BackupFile(exportedAt=System.currentTimeMillis(),activeProfileId=active,profiles=dao.profiles().map { readProfile(it.id) }))
    }
    suspend fun setQuest(profile:String,key:String,name:String,state:QuestState) = db.withTransaction {
        val before=dao.progress(profile,key)
        val after=QuestProgressEntity(profile,key,name,state.name,System.currentTimeMillis())
        dao.saveProgress(after)
        saveUndo(profile,"Stav úkolu: $name",PersonalChange(questKey=key,beforeQuest=before,afterQuest=after))
    }
    suspend fun setItem(profile:String,key:String,name:String,acquired:Boolean) = db.withTransaction {
        val before=dao.collectedItems(profile).firstOrNull { it.itemKey==key }
        val after=if(acquired) CollectedItemEntity(profile,key,name,System.currentTimeMillis()) else null
        if(after==null)dao.deleteItem(profile,key) else dao.saveCollectedItem(after)
        saveUndo(profile,"Získání předmětu: $name",PersonalChange(itemKey=key,beforeItem=before,afterItem=after))
    }
    suspend fun chooseReward(profile:String,quest:String,questName:String,item:String?,itemName:String?) = db.withTransaction {
        val before=dao.rewards(profile).firstOrNull { it.questKey==quest }
        val after=item?.let { RewardChoiceEntity(profile,quest,it,questName,requireNotNull(itemName),System.currentTimeMillis()) }
        if(after==null)dao.deleteReward(profile,quest) else dao.saveReward(after)
        saveUndo(profile,"Volba odměny: $questName",PersonalChange(rewardQuest=quest,beforeReward=before,afterReward=after))
    }
    private suspend fun saveUndo(profile:String,label:String,change:PersonalChange) {
        dao.saveUndo(UndoEntity(profile,label,BackupJson.encodeToString(change),System.currentTimeMillis()))
    }
    suspend fun undo(profile:String) = db.withTransaction {
        val row=dao.undo(profile) ?: return@withTransaction
        val change=BackupJson.decodeFromString<PersonalChange>(row.payloadJson)
        change.questKey?.let { key ->
            require(dao.progress(profile,key)==change.afterQuest){"Úkol se mezitím změnil. Starou změnu už nelze vrátit."}
            if(change.beforeQuest==null)dao.deleteProgress(profile,key) else dao.saveProgress(change.beforeQuest)
        }
        change.itemKey?.let { key ->
            require(dao.collectedItems(profile).firstOrNull { it.itemKey==key }==change.afterItem){"Předmět se mezitím změnil."}
            if(change.beforeItem==null)dao.deleteItem(profile,key) else dao.saveCollectedItem(change.beforeItem)
        }
        change.rewardQuest?.let { key ->
            require(dao.rewards(profile).firstOrNull { it.questKey==key }==change.afterReward){"Odměna se mezitím změnila."}
            if(change.beforeReward==null)dao.deleteReward(profile,key) else dao.saveReward(change.beforeReward)
        }
        dao.clearUndo(profile)
    }
    /** Imports are all-or-nothing; source profile and note IDs never overwrite another local profile. */
    suspend fun importNew(file:BackupFile):String {
        BackupCodec.validate(file)
        return db.withTransaction {
            require(dao.profiles().size+file.profiles.size<=100){"Celkem může být uložených nejvýše 100 profilů."}
            val ids=file.profiles.associate { it.profile.id to "profile:import:${UUID.randomUUID()}" }
            file.profiles.forEach { source ->
                val id=ids.getValue(source.profile.id)
                val copy=remap(source,id,true).copy(profile=source.profile.copy(id=id,displayName=(source.profile.displayName.take(45)+" · obnova")))
                check(dao.createProfile(copy.profile)>0)
                insertRows(copy)
            }
            ids.getValue(file.activeProfileId)
        }
    }
    suspend fun replace(profile:String,source:ProfileBackup) {
        BackupCodec.validateProfile(source)
        db.withTransaction {
            snapshot(profile,"Před nahrazením ze zálohy")
            replaceRows(profile,remap(source,profile,true))
        }
    }
    suspend fun recover(profile:String,snapshotId:String) = db.withTransaction {
        val source=dao.snapshots(profile).firstOrNull { it.id==snapshotId } ?: error("Snímek už není k dispozici.")
        val copy=BackupJson.decodeFromString<ProfileBackup>(source.payloadJson)
        BackupCodec.validateProfile(copy);require(copy.profile.id==profile)
        snapshot(profile,"Před vrácením místního snímku")
        replaceRows(profile,copy)
    }
    private suspend fun snapshot(profile:String,label:String) {
        val payload=BackupJson.encodeToString(readProfile(profile))
        val created=maxOf(System.currentTimeMillis(),(dao.snapshots(profile).firstOrNull()?.createdAt ?: 0)+1)
        dao.saveSnapshot(PersonalSnapshotEntity("snapshot:${UUID.randomUUID()}",profile,label,created,payload))
        dao.trimSnapshots(profile)
    }
    private suspend fun replaceRows(profile:String,copy:ProfileBackup) {
        require(dao.profile(profile)!=null)
        dao.clearProgress(profile);dao.clearItems(profile);dao.clearBookmarks(profile);dao.clearNotes(profile)
        dao.clearStops(profile);dao.clearTravel(profile);dao.clearRewards(profile);dao.clearUndo(profile)
        check(dao.updateProfile(copy.profile)==1)
        insertRows(copy)
    }
    private suspend fun insertRows(p:ProfileBackup) {
        dao.insertProgress(p.quests);dao.insertItems(p.items);dao.insertBookmarks(p.bookmarks);dao.insertNotes(p.notes)
        dao.insertStops(p.stops);dao.insertTravel(p.travel);dao.insertRewards(p.rewards)
    }
    private fun remap(p:ProfileBackup,id:String,newNoteIds:Boolean)=p.copy(profile=p.profile.copy(id=id),
        quests=p.quests.map { it.copy(profileId=id) },items=p.items.map { it.copy(profileId=id) },bookmarks=p.bookmarks.map { it.copy(profileId=id) },
        notes=p.notes.map { it.copy(profileId=id,id=if(newNoteIds) "note:${UUID.randomUUID()}" else it.id) },stops=p.stops.map { it.copy(profileId=id) },
        travel=p.travel.map { it.copy(profileId=id) },rewards=p.rewards.map { it.copy(profileId=id) })
}
