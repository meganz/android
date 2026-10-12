package mega.privacy.android.feature.sync.domain.entity.progress

/**
 * Every known sync after one applied sync event, stamped with the run snapshot generation.
 * Only sent once the sync list has been read.
 */
internal data class SyncWorkStatesUpdate(
    val generation: Long,
    val syncs: List<SyncWorkState>,
)
