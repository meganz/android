package mega.privacy.android.feature.sync.domain.entity.progress

import mega.privacy.android.feature.sync.domain.entity.SyncPauseReason

/**
 * The pause reason as the sync progress status sees it. Its first value is sampled about a second
 * late, so it stays [Unknown] until then and a Paused status waits for it.
 */
internal sealed interface SyncPauseReasonInput {

    /**
     * The pause reason has not arrived yet
     */
    data object Unknown : SyncPauseReasonInput

    /**
     * The pause reason has arrived
     *
     * @property reason The condition blocking the syncs, or null if none
     */
    data class Known(val reason: SyncPauseReason?) : SyncPauseReasonInput
}
