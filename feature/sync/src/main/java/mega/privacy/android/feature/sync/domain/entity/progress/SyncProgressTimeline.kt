package mega.privacy.android.feature.sync.domain.entity.progress

/**
 * Where one input of the sync progress status sits in the run.
 *
 * @property input The input
 * @property isBusy Whether the input's phase was busy (Transferring or Checking)
 * @property busyUntilMillis The earliest unknown-stats cap of a busy phase without work, else null
 * @property idleStartMillis When the busy phase before this idle stretch ended; null if none did
 */
internal data class SyncProgressTimeline(
    val input: SyncProgressInput,
    val isBusy: Boolean,
    val busyUntilMillis: Long?,
    val idleStartMillis: Long?,
)
