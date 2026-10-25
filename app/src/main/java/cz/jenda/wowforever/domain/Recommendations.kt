package cz.jenda.wowforever.domain

import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.data.ProfileBackup
import cz.jenda.wowforever.data.db.QuestState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// UNVERIFIED is deliberately not treated as AVAILABLE.
enum class Availability(val label:String) { AVAILABLE("Dostupné podle podkladů"), MISSING("Chybí podmínka"), LATER("Později nebo jinde"), EXCLUDED("Vyloučené podmínkou"), UNVERIFIED("K ověření") }
enum class GuideFocus(val label:String) { BALANCED("Vyváženě"), EXPLORATION("Objevování"), EQUIPMENT("Vybavení") }
data class Advice(val entry:EntryRecord,val availability:Availability,val explanation:List<String>,val ruleReason:String?,
    val importance:Importance,val areaIds:Set<Int>,val sourceIds:Set<String>,val relatedKeys:Set<String>,
    val inProgress:Boolean,val wish:Boolean,val deferred:Boolean,val riskLabel:String?,val unlockCount:Int,val ruleVerification:Verification?=null) {
    val personal get()=inProgress || wish
}
data class AdviceReport(val all:List<Advice> = emptyList(),val currentArea:Int=1411,val nextRefreshAtMillis:Long?=null) {
    private val here get()=all.filter { it.areaIds.isEmpty() || currentArea in it.areaIds }
    val now get()=here.filter { it.availability==Availability.AVAILABLE && !it.deferred }
    val personal get()=all.filter { it.personal && !it.deferred }
    val uncertain get()=here.filter { it.availability==Availability.UNVERIFIED && !it.deferred }
    val waiting get()=here.filter { it.availability in setOf(Availability.MISSING,Availability.LATER,Availability.EXCLUDED) && !it.deferred }
    val deferred get()=here.filter { it.deferred }
    val leave get()=here.filter { !it.deferred && it.availability==Availability.AVAILABLE && (it.personal || it.importance!=Importance.DISCOVERY || it.riskLabel!=null) }
    val leaveMissing get()=here.filter { !it.deferred && it.availability==Availability.MISSING }
    val leaveUncertain get()=uncertain.filter { it.personal || it.entry.kind in setOf(EntryKind.QUEST,EntryKind.ITEM,EntryKind.TRAVEL) || it.importance!=Importance.DISCOVERY }
}
private data class ConditionResult(val status:Availability,val lines:List<String>,val related:Set<String> = emptySet())

class RecommendationEngine(entries:List<EntryRecord>,private val relations:List<RelationRecord>,rules:List<GuideRule>,private val directAreas:Map<String,Set<Int>>) {
    private val entries=entries.associateBy { it.key }
    private val rules=rules.associateBy { it.entryKey }
    private val quests=QuestRules(relations)
    private val byOwner=relations.groupBy { it.ownerKey }
    private val byTarget=relations.flatMap { r -> r.targetKeys.map { it to r } }.groupBy({it.first},{it.second})
    private fun related(key:String)=(byOwner[key].orEmpty()+byTarget[key].orEmpty()).distinctBy { it.key }
    private fun name(key:String)=entries[key]?.nameOriginal ?: key
    private val areaCache=mutableMapOf<String,Set<Int>>()
    private fun areas(key:String,seen:Set<String> = emptySet()):Set<Int> {
        areaCache[key]?.let { return it }
        if(key in seen)return emptySet()
        val direct=directAreas[key].orEmpty()+rules[key]?.areaIds.orEmpty()
        if(direct.isNotEmpty())return direct.toSet().also { areaCache[key]=it }
        val next=related(key).flatMap { r -> when {
            r.ownerKey==key && r.kind in setOf(RelationKind.START_NPC,RelationKind.END_NPC,RelationKind.START_OBJECT,RelationKind.END_OBJECT,RelationKind.START_ITEM,RelationKind.ITEM_SOURCE_NPC,RelationKind.ITEM_SOURCE_OBJECT)->r.targetKeys
            r.ownerKey==key && r.kind==RelationKind.REWARD_CHOICE->r.targetKeys
            key in r.targetKeys && r.kind==RelationKind.LEARN_SPELL->listOf(r.ownerKey)
            else->emptyList()
        } }
        return next.flatMap { areas(it,seen+key) }.toSet().also { areaCache[key]=it }
    }
    private fun verified(r:RelationRecord)=r.routingEnabled && r.verification==Verification.RELEASE
    @Synchronized fun evaluate(personal:ProfileBackup,focus:GuideFocus=GuideFocus.BALANCED,nowMillis:Long=System.currentTimeMillis()):AdviceReport {
        val p=personal.profile
        val states=personal.quests.associate { it.questKey to QuestState.valueOf(it.state) }
        val items=personal.items.map { it.itemKey }.toSet();val rewards=personal.rewards.associateBy { it.questKey }
        val bookmarks=personal.bookmarks.map { it.entryKey }.toSet()
        val travel=personal.travel.map { it.travelKey }.toSet()
        val professions=runCatching { ContentJson.decodeFromString<List<String>>(p.professionsJson) }.getOrDefault(emptyList())
        fun normalize(s:String)=normalizeSearch(s.trim())
        fun eval(c:GuideCondition):ConditionResult {
            fun result(status:Availability,text:String,related:Set<String> = emptySet())=ConditionResult(status,listOf(text),related)
            fun membership(value:String?,values:List<String>,label:String,wrong:Availability=Availability.EXCLUDED):ConditionResult =
                if(value.isNullOrBlank())result(Availability.UNVERIFIED,"Doplň $label v části Postava; podmínku zatím nelze posoudit.")
                else if(values.any { normalize(it)==normalize(value) })result(Availability.AVAILABLE,"Podmínka pro $label je splněná.")
                else result(wrong,"Zvolená postava nesplňuje podmínku pro $label.")
            return when(c) {
                GuideCondition.Always->result(Availability.AVAILABLE,"Podklad výslovně neuvádí další podmínku přístupu.")
                is GuideCondition.Unknown->result(Availability.UNVERIFIED,c.reasonCs)
                is GuideCondition.All,is GuideCondition.AnyOf->{
                    val all=c is GuideCondition.All
                    val children=if(c is GuideCondition.All)c.conditions else (c as GuideCondition.AnyOf).conditions
                    val results=children.map(::eval)
                    val statuses=results.map { it.status }
                    val status=if(all) listOf(Availability.EXCLUDED,Availability.LATER,Availability.MISSING,Availability.UNVERIFIED).firstOrNull { it in statuses } ?: Availability.AVAILABLE
                        else when { Availability.AVAILABLE in statuses->Availability.AVAILABLE;Availability.UNVERIFIED in statuses->Availability.UNVERIFIED;Availability.MISSING in statuses->Availability.MISSING;Availability.LATER in statuses->Availability.LATER;else->Availability.EXCLUDED }
                    val relevant=if(!all && status==Availability.AVAILABLE)results.filter { it.status==Availability.AVAILABLE } else results
                    ConditionResult(status,listOf(if(all) "Musí platit všechny následující podmínky:" else "Stačí jedna z následujících možností:")+relevant.flatMap { it.lines },relevant.flatMap { it.related }.toSet())
                }
                is GuideCondition.Faction->membership(p.faction,c.values,"frakci")
                is GuideCondition.PlayerClass->membership(p.playerClass,c.values,"povolání")
                is GuideCondition.Race->membership(p.race,c.values,"rasu")
                is GuideCondition.Level->when { p.level<c.minimum->result(Availability.LATER,"Je potřeba úroveň ${c.minimum}; máš ${p.level}.");c.maximum!=null && p.level>c.maximum->result(Availability.EXCLUDED,"Podklad výslovně omezuje úroveň nejvýše na ${c.maximum}.");else->result(Availability.AVAILABLE,"Požadavek na úroveň je splněný.") }
                is GuideCondition.Mode->membership(p.ruleset,c.values,"režim")
                is GuideCondition.Profession->if(professions.isEmpty())result(Availability.UNVERIFIED,"Doplň profesi v části Postava, pokud ji máš; tento podklad ji vyžaduje.") else if(professions.any { v->c.values.any { normalize(v)==normalize(it) } })result(Availability.AVAILABLE,"Požadovaná profese je zaznamenaná.") else result(Availability.MISSING,"Chybí některá z požadovaných profesí: ${c.values.joinToString()}.")
                is GuideCondition.Area->if(p.currentUiMapId in c.uiMapIds)result(Availability.AVAILABLE,"Podmínka oblasti je splněná.") else result(Availability.LATER,"Tento krok je určený pro jinou oblast.")
                is GuideCondition.QuestCompleted->if(states[c.questKey]==QuestState.COMPLETED)result(Availability.AVAILABLE,"Hotový předchůdce: ${name(c.questKey)}.",setOf(c.questKey)) else result(Availability.MISSING,"Chybí dokončení: ${name(c.questKey)}. Odložení se nepočítá.",setOf(c.questKey))
                is GuideCondition.ItemAcquired->if(c.itemKey in items)result(Availability.AVAILABLE,"Získaný předmět je zaznamenaný: ${name(c.itemKey)}.",setOf(c.itemKey)) else result(Availability.MISSING,"Chybí záznam získání: ${name(c.itemKey)}.",setOf(c.itemKey))
                is GuideCondition.RewardSelected->{ val chosen=rewards[c.questKey]?.itemKey
                    when { chosen==c.itemKey->result(Availability.AVAILABLE,"Zaznamenaná volba odměny odpovídá podmínce.",setOf(c.questKey,c.itemKey));chosen==null->result(Availability.UNVERIFIED,"Volba odměny zatím není zaznamenaná.",setOf(c.questKey));else->result(Availability.EXCLUDED,"Zaznamenal jsi jinou odměnu; tato varianta podmínku nesplňuje.",setOf(c.questKey)) }
                }
            }
        }
        val history=(personal.bookmarks.map { Triple(it.entryKey,it.nameSnapshot,EntryKind.valueOf(it.kind)) }+
            personal.quests.map { Triple(it.questKey,it.nameSnapshot,EntryKind.QUEST) }).filter { it.first !in entries }.distinctBy { it.first }.map { (key,name,kind)->
                EntryRecord(key,kind,nameOriginal=name,descriptionCs="Osobní historie mimo aktuální katalog.",verification=Verification.UNVERIFIED,sourceIds=emptyList(),payload="{}")
            }
        val advice=(entries.values+history).mapNotNull { e ->
            if(states[e.key]==QuestState.COMPLETED || e.key in items || e.key in travel)return@mapNotNull null
            val r=rules[e.key];val known=r?.verification==Verification.RELEASE && e.verification==Verification.RELEASE && e.releaseConfirmed
            val checks=mutableListOf<ConditionResult>()
            checks+=r?.condition?.let(::eval) ?: ConditionResult(Availability.UNVERIFIED,listOf("Chybí zdrojované úplné podmínky dostupnosti pro Forever."))
            e.minimumLevel?.let { checks+=eval(GuideCondition.Level(it)) }
            if(e.kind==EntryKind.QUEST)quests.prerequisites(e.key,states).forEach { c ->
                checks+=ConditionResult(if(!verified(c.relation))Availability.UNVERIFIED else if(c.satisfied)Availability.AVAILABLE else Availability.MISSING,
                    listOf((if(verified(c.relation)) "Návaznost" else "Návaznost pouze k ověření")+": "+(if(c.relation.mode==MatchMode.ALL) "všechny kroky" else "stačí jeden krok")+" · hotovo ${c.completed}/${c.relation.targetKeys.size}."),c.relation.targetKeys.toSet())
            }
            val excluded=if(e.kind==EntryKind.QUEST)quests.excludedBy(e.key,states) else emptySet()
            if(excluded.isNotEmpty())checks+=ConditionResult(Availability.EXCLUDED,listOf("Jiná výlučná větev je dokončená: ${excluded.joinToString { name(it) }}."),excluded)
            r?.window?.let { window ->
                val zone=ZoneId.of(window.timeZone);val format=DateTimeFormatter.ofPattern("d. M. yyyy HH:mm").withZone(zone)
                checks+=when { nowMillis<window.fromEpochMillis->ConditionResult(Availability.LATER,listOf("Platí od ${format.format(Instant.ofEpochMilli(window.fromEpochMillis))} (${window.timeZone})."));nowMillis>=window.untilEpochMillis->ConditionResult(Availability.EXCLUDED,listOf("Platnost skončila ${format.format(Instant.ofEpochMilli(window.untilEpochMillis))} (${window.timeZone})."));else->ConditionResult(Availability.AVAILABLE,listOf("Platí do ${format.format(Instant.ofEpochMilli(window.untilEpochMillis))} (${window.timeZone}).")) }
            }
            val status=if(!known)Availability.UNVERIFIED else listOf(Availability.EXCLUDED,Availability.LATER,Availability.MISSING,Availability.UNVERIFIED).firstOrNull { s->checks.any { it.status==s } } ?: Availability.AVAILABLE
            val risk=if(!known || status!=Availability.AVAILABLE)null else when {
                r?.exclusiveRelationKey!=null->"Výlučná volba"
                r?.window!=null && nowMillis in r.window.fromEpochMillis until r.window.untilEpochMillis->"Časově omezené"
                else->null
            }
            val unlocks=if(!known)0 else byTarget[e.key].orEmpty().filter { it.kind==RelationKind.PREREQUISITE && verified(it) }.map { it.ownerKey }.distinct().count { child->
                if(states[child]==QuestState.COMPLETED || quests.excludedBy(child,states).isNotEmpty())false
                else { val before=quests.prerequisites(child,states);val after=quests.prerequisites(child,states+(e.key to QuestState.COMPLETED));before.any { !it.satisfied } && after.isNotEmpty() && after.all { verified(it.relation) && it.satisfied } }
            }
            val entryRelations=related(e.key)
            // An incoming relation links to its owner, not every other target in the group.
            // Shared item drops previously created millions of unrelated NPC-to-NPC links.
            val related=(checks.flatMap { it.related }+
                byOwner[e.key].orEmpty().flatMap { it.targetKeys }+
                byTarget[e.key].orEmpty().map { it.ownerKey }).filter { it!=e.key }.toSet()
            Advice(e,status,(if(!known)listOf("Úplné pravidlo doporučení není potvrzené pro vydanou Forever; podmínky níže jsou jen reference.") else emptyList())+checks.flatMap { it.lines }.distinct(),r?.reasonCs,r?.importance ?: if(e.kind==EntryKind.TRAVEL)Importance.TRAVEL else Importance.DISCOVERY,
                areas(e.key),e.sourceIds.toSet()+r?.sourceIds.orEmpty()+entryRelations.flatMap { it.sourceIds },related,states[e.key]==QuestState.IN_PROGRESS,e.key in bookmarks,states[e.key]==QuestState.DEFERRED,risk,unlocks,r?.verification)
        }
        fun preference(a:Advice)=when(focus) { GuideFocus.BALANCED->1;GuideFocus.EXPLORATION->if(a.importance in setOf(Importance.DISCOVERY,Importance.TRAVEL))0 else 1;GuideFocus.EQUIPMENT->if(a.importance==Importance.REWARD || a.entry.kind==EntryKind.ITEM)0 else 1 }
        val names=advice.associate { it.entry.key to normalizeSearch(it.entry.nameOriginal) }
        val sorted=advice.sortedWith(compareBy<Advice> { if(it.riskLabel!=null)0 else 1 }.thenBy { if(it.importance==Importance.CLASS)0 else 1 }.thenBy { if(it.inProgress)0 else 1 }.thenBy { preference(it) }.thenBy { it.importance.ordinal }.thenBy { if(p.currentUiMapId in it.areaIds)0 else 1 }.thenBy { names[it.entry.key] }.thenBy { it.entry.key })
        val next=rules.values.filter { it.verification==Verification.RELEASE }.mapNotNull { it.window }.flatMap { listOf(it.fromEpochMillis,it.untilEpochMillis) }.filter { it>nowMillis }.minOrNull()
        return AdviceReport(sorted,p.currentUiMapId,next)
    }
}
