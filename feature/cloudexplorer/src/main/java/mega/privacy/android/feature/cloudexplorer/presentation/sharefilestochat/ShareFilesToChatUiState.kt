package mega.privacy.android.feature.cloudexplorer.presentation.sharefilestochat

import androidx.compose.runtime.Stable
import mega.privacy.android.domain.entity.node.NodeId

/**
 * UI state for selecting files to share to a chat.
 */
@Stable
sealed interface ShareFilesToChatUiState {

    /**
     * Initial loading state.
     */
    data object Loading : ShareFilesToChatUiState

    /**
     * Data state.
     *
     * @property rootNodeId Root node id.
     * @property chatId Target chat id to attach the selected files to.
     */
    data class Data(
        val rootNodeId: NodeId,
    ) : ShareFilesToChatUiState
}
