package cz.jenda.wowforever.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import cz.jenda.wowforever.GuideViewModel
import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.data.Destination
import cz.jenda.wowforever.data.db.QuestState
import cz.jenda.wowforever.domain.PrerequisiteStatus

fun stateLabel(value:QuestState)=when(value) {
    QuestState.NOT_STARTED->"Nezahájeno";QuestState.IN_PROGRESS->"Rozpracováno";QuestState.COMPLETED->"Hotovo";QuestState.DEFERRED->"Odloženo"
}
@OptIn(ExperimentalLayoutApi::class)
@Composable fun QuestStateActions(model:GuideViewModel,entry:EntryRecord) {
        val state=model.questStates[entry.key] ?: QuestState.NOT_STARTED
        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            QuestState.entries.forEach { choice -> FilterChip(selected=state==choice,onClick={model.requestQuest(entry.key,choice)},enabled=!model.isBusy,
                label={Text(when(choice) { QuestState.IN_PROGRESS->"Beru";QuestState.COMPLETED->"Hotovo";QuestState.DEFERRED->"Později";else->"Nezahájeno" })},modifier=Modifier.testTag("quest_state_${choice.name}")) }
        }
 }

@OptIn(ExperimentalLayoutApi::class)
@Composable fun ProgressPanel(model:GuideViewModel,entry:EntryRecord,onClose:()->Unit,onRelated:(String)->Unit) {
    if(entry.kind==EntryKind.QUEST) {
        when(model.questRules.status(entry.key,model.questStates)) {
            PrerequisiteStatus.SATISFIED -> Text("Doložené předchozí kroky jsou splněné. Další podmínky dostupnosti ověř ve hře.")
            PrerequisiteStatus.MISSING -> Text("Chybí dokončený předchozí krok.")
            PrerequisiteStatus.UNVERIFIED -> Text("Dostupnost ve Forever není ověřená.",style=MaterialTheme.typography.bodySmall)
        }
        model.questRules.prerequisites(entry.key,model.questStates).forEach { check ->
            Text((if(check.relation.routingEnabled && check.relation.verification==Verification.RELEASE) "Návaznost" else "Návaznost k ověření")+
                " · "+(if(check.relation.mode==MatchMode.ALL) "všechny kroky" else "stačí jeden krok")+" · ${check.completed}/${check.relation.targetKeys.size}",style=MaterialTheme.typography.labelMedium)
            check.relation.targetKeys.forEach { key ->
                TextButton(onClick={onRelated(key)}) { Text((model.historyEntry(key)?.nameOriginal ?: key)+" · "+stateLabel(model.questStates[key] ?: QuestState.NOT_STARTED)) }
            }
        }
        model.questRules.exclusiveGroups(entry.key).forEach { group ->
            Text(if(group.verified) "Vzájemně výlučná větev" else "Výlučnost k ověření",style=MaterialTheme.typography.titleSmall)
            group.questKeys.filter { it!=entry.key }.forEach { key ->
                TextButton(onClick={onRelated(key)}) { Text((model.historyEntry(key)?.nameOriginal ?: key)+" · "+stateLabel(model.questStates[key] ?: QuestState.NOT_STARTED)) }
            }
        }
        if(model.questRules.excludedBy(entry.key,model.questStates).isNotEmpty()) Text("Jiná větev už je zaznamenaná jako dokončená. Před změnou oprav její stav.",color=MaterialTheme.colorScheme.error)
        val reward=model.rewardChoices[entry.key]
        val candidates=model.questRules.rewardCandidates(entry.key)
        if(candidates.isNotEmpty() || reward!=null) {
            Text("Zaznamenaná volba odměny",style=MaterialTheme.typography.titleMedium)
            Text("Seznam odměn z podkladů může být neúplný. Volba neoznačí předmět za získaný.",style=MaterialTheme.typography.bodySmall)
            FilterChip(selected=reward==null,onClick={model.requestReward(entry.key,null)},enabled=!model.isBusy,label={Text("Nezaznamenáno")},modifier=Modifier.testTag("reward_none"))
            candidates.distinctBy { it.ownerKey }.forEach { relation ->
                val item=model.historyEntry(relation.ownerKey)
                FilterChip(selected=reward?.itemKey==relation.ownerKey,onClick={model.requestReward(entry.key,relation.ownerKey)},enabled=!model.isBusy,
                    label={Text(item?.nameOriginal ?: relation.ownerKey)},modifier=Modifier.testTag("reward_${relation.ownerKey}"))
                Text(verificationLabel(relation.verification),style=MaterialTheme.typography.labelSmall)
                if(item!=null) TextButton(onClick={onRelated(item.key)}) { Text("Otevřít předmět") }
            }
            if(reward!=null && candidates.none { it.ownerKey==reward.itemKey }) Text("${reward.itemNameSnapshot} · momentálně mimo katalog")
        }
        TextButton(onClick={model.navigate(Destination.JOURNEY);onClose()},modifier=Modifier.testTag("entry_journey")) { Text("Moje cesta") }
    }
    if(entry.kind==EntryKind.ITEM) {
        HorizontalDivider()
        Text("Evidence získání",style=MaterialTheme.typography.titleMedium)
        FilterChip(selected=entry.key in model.acquiredItems,onClick={model.setAcquired(entry.key,entry.key !in model.acquiredItems)},enabled=!model.isBusy,
            label={Text(if(entry.key in model.acquiredItems) "Předmět získaný" else "Označit jako získaný")},modifier=Modifier.testTag("item_acquired"))
        Text("Jde o získání předmětu, nikoli o aktuálně nasazenou výbavu. Záložka u předmětu slouží také jako přání.",style=MaterialTheme.typography.bodySmall)
        TextButton(onClick={model.navigate(Destination.JOURNEY);onClose()},modifier=Modifier.testTag("entry_journey")){Text("Moje cesta")}
    }
    if(model.undoAction!=null && entry.kind in setOf(EntryKind.ITEM,EntryKind.QUEST)) {
        TextButton(onClick=model::undoPersonal,enabled=!model.isBusy,modifier=Modifier.testTag("undo_personal")){Text("Vrátit poslední změnu")}
    }
}

@Composable fun RelatedEntryLinks(model:GuideViewModel,entry:EntryRecord,onRelated:(String)->Unit) {
    val shownKinds=setOf(RelationKind.START_NPC,RelationKind.END_NPC,RelationKind.START_OBJECT,RelationKind.END_OBJECT,RelationKind.START_ITEM,RelationKind.OBJECTIVE_NPC,RelationKind.OBJECTIVE_OBJECT,RelationKind.OBJECTIVE_ITEM,RelationKind.ITEM_SOURCE_NPC,RelationKind.ITEM_SOURCE_OBJECT,RelationKind.DUNGEON_QUEST,RelationKind.QUEST_REWARD)
    val links=model.relations.filter { it.kind in shownKinds && (it.ownerKey==entry.key || entry.key in it.targetKeys) }.flatMap { r ->
        if(r.ownerKey==entry.key)r.targetKeys.map { r to it } else listOf(r to r.ownerKey)
    }.distinctBy { it.first.kind to it.second }
    if(links.isNotEmpty()) {
        Text("Související záznamy z podkladů",style=MaterialTheme.typography.titleMedium)
        links.forEach { (relation,key) ->
            TextButton(onClick={onRelated(key)},modifier=Modifier.testTag("related_$key")) {
                Column { Text(model.historyEntry(key)?.nameOriginal ?: key)
                    Text(relationLabel(relation.kind)+" · "+verificationLabel(relation.verification),style=MaterialTheme.typography.labelSmall) }
            }
        }
    }
}

private fun relationLabel(kind:RelationKind)=when(kind) {
    RelationKind.START_NPC,RelationKind.START_OBJECT,RelationKind.START_ITEM -> "Zadání"
    RelationKind.END_NPC,RelationKind.END_OBJECT -> "Odevzdání"
    RelationKind.OBJECTIVE_NPC,RelationKind.OBJECTIVE_OBJECT,RelationKind.OBJECTIVE_ITEM -> "Cíl úkolu"
    RelationKind.ITEM_SOURCE_NPC,RelationKind.ITEM_SOURCE_OBJECT -> "Zdroj předmětu"
    RelationKind.DUNGEON_QUEST -> "Příprava na dungeon"
    RelationKind.QUEST_REWARD -> "Odměna: typ výběru neověřen"
    else -> "Návaznost"
}
