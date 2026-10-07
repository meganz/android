package mega.privacy.android.feature.sync.domain.entity.progress

/**
 * One pass of the sync progress status over a timeline.
 *
 * @property status The status to emit, or null to keep the last one
 * @property timeline The timeline for the next pass
 * @property wakeAtMillis When to pass again without a new input, or null to wait for one
 */
internal data class SyncProgressStep(
    val status: SyncProgressStatus?,
    val timeline: SyncProgressTimeline,
    val wakeAtMillis: Long?,
)
