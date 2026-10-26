package cz.jenda.wowforever.domain

import cz.jenda.wowforever.data.Session

const val BACKUP_REMINDER_DAYS=7
private const val DAY_MS=86_400_000L
fun Session.backupReminderDue(now:Long):Boolean {
    val since=if(lastBackupAt>0)lastBackupAt else unbackedSince
    return personalRevision!=backedUpRevision && since>0 && now>=since && now-since>=BACKUP_REMINDER_DAYS*DAY_MS
}
fun Session.lastBackupDays(now:Long):Long?=lastBackupAt.takeIf { it>0 }?.let { ((now-it).coerceAtLeast(0)/DAY_MS) }
