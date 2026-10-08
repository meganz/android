package mega.privacy.android.feature.sync.domain.entity.progress

/**
 * The desktop status chain's result; [busyWork] is the busy part, kept when Paused or IssuesFound wins.
 * @property issueCount The listed issues while the syncs are stalled, else 0
 * @property disabledSyncCount Syncs with an error that are Suspended or Disabled
 * @property reevaluateAtMillis The next instant a time-based rule changes, or null
 */
internal data class SyncEngineState(
    val status: SyncEngineStatus,
    val busyWork: SyncEngineWork?,
    val issueCount: Int,
    val disabledSyncCount: Int,
    val reevaluateAtMillis: Long?,
) {
    companion object {
        /** The state until every input is seeded. */
        val NotSeeded = SyncEngineState(
            status = SyncEngineStatus.NotSeeded,
            busyWork = null,
            issueCount = 0,
            disabledSyncCount = 0,
            reevaluateAtMillis = null,
        )
    }
}
