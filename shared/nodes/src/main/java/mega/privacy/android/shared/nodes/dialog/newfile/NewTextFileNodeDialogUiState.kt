package mega.privacy.android.shared.nodes.dialog.newfile

import androidx.compose.runtime.Stable
import de.palm.composestateevents.StateEventWithContent
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.NodeNameException

/**
 * UI state for the new text file dialog.
 */
@Stable
sealed interface NewTextFileNodeDialogUiState {

    /**
     * Initial loading state.
     */
    data object Loading : NewTextFileNodeDialogUiState

    /**
     * Data state.
     *
     * @property parentNodeId The parent node where the text file will be created. A value of
     * `NodeId(-1L)` means "use the user's root cloud node".
     * @property fileName Current value of the file name input.
     * @property fileNameException Validation exception from the last positive action, or null.
     * @property validationSuccessEvent Triggered with the trimmed file name when validation
     * succeeds, so the caller can run its positive action.
     */
    data class Data(
        val parentNodeId: NodeId,
        val fileName: String,
        val fileNameException: NodeNameException?,
        val validationSuccessEvent: StateEventWithContent<String>,
    ) : NewTextFileNodeDialogUiState

    companion object {
        internal const val DEFAULT_TEXT_FILE_EXTENSION = ".txt"
    }
}

