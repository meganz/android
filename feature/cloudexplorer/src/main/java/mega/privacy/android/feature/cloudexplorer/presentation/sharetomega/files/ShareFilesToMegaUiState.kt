package mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.files

import androidx.compose.runtime.Stable
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.uri.UriPath

/**
 * UI state for sharing files to mega.
 */
@Stable
sealed interface ShareFilesToMegaUiState {

    /**
     * Initial loading state.
     */
    data object Loading : ShareFilesToMegaUiState

    /**
     * Data state.
     *
     * @property rootNodeId Root node id.
     * @property shareUris Uris to share.
     */
    data class Data(
        val rootNodeId: NodeId,
        val shareUris: List<UriPath>,
    ) : ShareFilesToMegaUiState
}
