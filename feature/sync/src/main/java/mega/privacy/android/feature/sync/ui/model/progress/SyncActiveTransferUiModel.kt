package mega.privacy.android.feature.sync.ui.model.progress

import androidx.compose.runtime.Immutable
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStatus

/** One item of a pending tab's list: a transfer row, or the footer counting files that have no row. */
internal sealed interface SyncActiveListItem {

    /** One pending transfer. */
    data class Row(val model: SyncActiveTransferUiModel) : SyncActiveListItem

    /** The [count] of pending files that the list shows no row for. */
    data class MoreWaiting(val count: Int) : SyncActiveListItem
}

/**
 * One active sync transfer, keyed by [tag] because sync transfers have no unique id.
 *
 * @property isOwnPause The SDK pause or an optimistic pause of the row, never the app-wide pause
 * @property lastErrorCode Set only for the Retrying status and the two quota statuses
 * @property band The desktop priority band of the row; only the relative order is meaningful
 */
@Immutable
internal data class SyncActiveTransferUiModel(
    val tag: Int,
    val fileName: String,
    val direction: SyncTransferDirection,
    val status: SyncTransferStatus,
    val transferredBytes: Long,
    val totalBytes: Long,
    val progressPermille: Int,
    val speed: Long,
    val etaSeconds: Long?,
    val isOwnPause: Boolean,
    val isStorageOverQuota: Boolean,
    val isTransferOverQuota: Boolean,
    val hasIssues: Boolean,
    val lastErrorCode: Int?,
    val isDraggable: Boolean,
    val band: Int,
)

/** The moves a row can offer; the ViewModel maps them to the domain moves. */
internal enum class SyncProgressMoveAction { ToTop, Up, Down, ToBottom }
