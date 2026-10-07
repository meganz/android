package mega.privacy.android.feature.sync.domain.entity.progress

/**
 * What a sync transfer row shows. [isOwnPause] is the row's own pause, never the app-wide one;
 * [speed] is the display speed for [status], and [etaSeconds] is null when no estimate applies.
 */
internal data class SyncTransferRowState(
    val status: SyncTransferStatus,
    val isOwnPause: Boolean,
    val speed: Long,
    val progressPermille: Int,
    val etaSeconds: Long?,
)
