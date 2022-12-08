package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.SyncStatus

/**
 * SyncStatusIntMapper
 */
typealias SyncStatusIntMapper = (@JvmSuppressWildcards SyncStatus) -> @JvmSuppressWildcards Int

/**
 * Sync status to int
 *
 * @param syncStatus
 */
internal fun syncStatusToInt(syncStatus: SyncStatus) = when(syncStatus){
    SyncStatus.STATUS_PENDING -> 0
    SyncStatus.STATUS_TO_COMPRESS -> 3
}