package mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.text

import androidx.compose.runtime.Stable
import de.palm.composestateevents.StateEventWithContent
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.uri.UriPath

/**
 * UI state for share text to mega.
 */
@Stable
sealed interface ShareTextToMegaUiState {

    /**
     * Initial loading state.
     */
    data object Loading : ShareTextToMegaUiState

    /**
     * Data state.
     *
     * @property rootNodeId Root node id used as default upload destination.
     * @property fileUri Uri of the temporary file once it has been created
     * confirmation; null while the user has not confirmed the file name.
     */
    data class Data(
        val rootNodeId: NodeId,
        val fileUri: StateEventWithContent<UriPath>,
    ) : ShareTextToMegaUiState
}
