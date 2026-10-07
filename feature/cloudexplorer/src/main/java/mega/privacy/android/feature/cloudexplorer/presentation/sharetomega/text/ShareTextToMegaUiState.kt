package mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.text

import androidx.compose.runtime.Stable
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.node.NodeId

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
     * @property textFile The temporary file once it has been created after the user confirmed
     * the file name, ready to be uploaded.
     * @property isCreatingFile True while the temporary file is being written.
     * @property createFileFailedEvent Triggered when the temporary file could not be created.
     */
    data class Data(
        val rootNodeId: NodeId,
        val textFile: StateEventWithContent<DocumentEntity> = consumed(),
        val isCreatingFile: Boolean = false,
        val createFileFailedEvent: StateEvent = consumed,
    ) : ShareTextToMegaUiState
}
