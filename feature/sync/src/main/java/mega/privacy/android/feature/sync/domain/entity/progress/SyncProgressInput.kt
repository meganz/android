package mega.privacy.android.feature.sync.domain.entity.progress

/**
 * One input of the sync progress status.
 *
 * @property runSnapshot The latest sync run snapshot
 * @property reason The latest pause reason, or Unknown until it arrives
 */
internal data class SyncProgressInput(
    val runSnapshot: SyncRunSnapshot,
    val reason: SyncPauseReasonInput,
)
