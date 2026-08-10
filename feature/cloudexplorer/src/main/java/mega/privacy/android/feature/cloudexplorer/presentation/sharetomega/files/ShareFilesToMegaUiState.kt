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
     * @property hasNoFilesToUpload True when the shared uris resolve to nothing that can be
     * uploaded (e.g. all rejected by the private-dir guard); the screen then shows an error
     * and closes.
     */
    data class Data(
        val rootNodeId: NodeId,
        val shareUris: List<UriPath>,
        val hasNoFilesToUpload: Boolean = false,
    ) : ShareFilesToMegaUiState
}
