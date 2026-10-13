package mega.privacy.android.feature.sync.ui.model.progress

import androidx.compose.runtime.Immutable
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferFilterCounts

/**
 * The sync progress screen's state: typed values only; the UI builds every text, icon and format.
 *
 * @property filesLeft The episode's files left; 0 means none
 * @property activeBody What the selected pending tab shows
 * @property areTransfersPaused The app-wide transfer pause, never the own pause of a row
 * @property filterCounts Desktop's tab counters
 */
@Immutable
internal data class SyncProgressUiState(
    val selectedTab: SyncProgressTab = SyncProgressTab.Active,
    val filesLeft: Int = 0,
    val activeBody: SyncProgressBody = SyncProgressBody.Loading,
    val completedBody: SyncProgressCompletedBody = SyncProgressCompletedBody.Loading,
    val areTransfersPaused: Boolean = false,
    val filterCounts: SyncTransferFilterCounts = SyncTransferFilterCounts.Empty,
)
