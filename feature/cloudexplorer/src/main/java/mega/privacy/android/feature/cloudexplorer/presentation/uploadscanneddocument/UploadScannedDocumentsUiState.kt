package mega.privacy.android.feature.cloudexplorer.presentation.uploadscanneddocument

import androidx.compose.runtime.Stable
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.uri.UriPath

/**
 * UI state for uploading a scanned document to MEGA.
 */
@Stable
sealed interface UploadScannedDocumentsUiState {

    /**
     * Initial loading state.
     */
    data object Loading : UploadScannedDocumentsUiState

    /**
     * Data state.
     *
     * @property rootNodeId Root node id.
     * @property uriPath Uri of the scanned document to upload.
     */
    data class Data(
        val rootNodeId: NodeId,
        val uriPath: UriPath,
    ) : UploadScannedDocumentsUiState
}
