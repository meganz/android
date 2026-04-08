package mega.privacy.android.feature.cloudexplorer.presentation.sharetomega

import androidx.compose.runtime.Stable
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.uri.UriPath

/**
 * UI state for share to mega.
 */
@Stable
sealed interface ShareToMegaUiState {

    /**
     * Initial loading state.
     */
    data object Loading : ShareToMegaUiState

    /**
     * Data state.
     *
     * @property rootNodeId Root node id.
     * @property shareUris Uris to share.
     */
    data class Data(
        val rootNodeId: NodeId,
        val shareUris: List<UriPath>,
    ) : ShareToMegaUiState
}
