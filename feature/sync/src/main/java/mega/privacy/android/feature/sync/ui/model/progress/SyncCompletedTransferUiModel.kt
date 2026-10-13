package mega.privacy.android.feature.sync.ui.model.progress

import androidx.compose.runtime.Immutable
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection

/**
 * One completed sync transfer, keyed by [tag].
 *
 * @property location The name of the folder that holds the file; empty when it is not known
 * @property finishedAtMillis The app clock time in milliseconds
 */
@Immutable
internal data class SyncCompletedTransferUiModel(
    val tag: Int,
    val fileName: String,
    val direction: SyncTransferDirection,
    val location: String,
    val sizeBytes: Long,
    val finishedAtMillis: Long,
    val averageSpeed: Long,
)
