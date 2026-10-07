package mega.privacy.android.feature.sync.domain.entity.progress

import mega.privacy.android.feature.sync.domain.entity.SyncPauseReason

/**
 * Why the syncs are paused while the sync progress phase is Paused.
 */
internal sealed interface SyncPauseCause {

    /**
     * A suspended sync is over its storage quota
     */
    data object StorageOverQuota : SyncPauseCause

    /**
     * A device condition blocks the syncs
     *
     * @property reason The blocking condition
     */
    data class Condition(val reason: SyncPauseReason) : SyncPauseCause

    /**
     * The user paused the syncs, one by one or all at once, or a cross-device conflict paused them
     */
    data object User : SyncPauseCause
}
