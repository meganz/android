package mega.privacy.android.feature.sync.domain.entity.progress

/**
 * One input of the sync progress status.
 *
 * @property activity The latest sync activity
 * @property reason The latest pause reason, or Unknown until it arrives
 */
internal data class SyncProgressInput(
    val activity: SyncActivity,
    val reason: SyncPauseReasonInput,
)
