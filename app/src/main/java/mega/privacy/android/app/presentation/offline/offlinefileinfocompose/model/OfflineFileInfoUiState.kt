package mega.privacy.android.app.presentation.offline.offlinefileinfocompose.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.offline.OfflineFileInformation

/**
 * UI state for the OfflineFileInfoComposeViewModel
 *
 * @property offlineFileInformation OfflineFileInformation
 * @property isLoading true if the node information is loading
 * @property errorEvent event to show an error message
 */
data class OfflineFileInfoUiState(
    val offlineFileInformation: OfflineFileInformation? = null,
    val isLoading: Boolean = true,
    val errorEvent: StateEventWithContent<Boolean> = consumed(),
)