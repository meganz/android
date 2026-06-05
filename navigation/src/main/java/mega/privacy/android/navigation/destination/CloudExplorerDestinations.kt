package mega.privacy.android.navigation.destination

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.uri.UriPath

@Serializable
sealed interface ExplorerNavKey : NavKey

@Serializable
data class ShareFilesToMegaNavKey(val shareUris: List<UriPath>) : ExplorerNavKey

@Serializable
data class ShareTextToMegaNavKey(
    val text: String,
    val subject: String?,
    val email: String?,
) : ExplorerNavKey

@Serializable
data class UploadScannedDocumentNavKey(
    val uriPath: UriPath,
    val nodeSourceType: NodeSourceType,
    val hasMultipleScans: Boolean = false,
) : ExplorerNavKey

@Serializable
data object SelectCUFolderNavKey : ExplorerNavKey {
    const val RESULT = "SelectCUFolderNavKey::result"
}

@Serializable
data object ImportNavKey : ExplorerNavKey {
    const val RESULT = "ImportNavKey::result"
}

@Serializable
data class ShareFilesToChatNavKey(val chatId: Long) : ExplorerNavKey {
    companion object {
        const val RESULT = "ShareFilesToChatNavKey::result"
    }
}

@Serializable
data class CopyNavKey(val nodeIds: List<NodeId>) : ExplorerNavKey {
    companion object {
        const val RESULT = "CopyNavKey::result"
    }
}

@Serializable
data class MoveNavKey(val nodeIds: List<NodeId>, val disabledTargetId: NodeId) : ExplorerNavKey {
    companion object {
        const val RESULT = "MoveNavKey::result"
    }
}

@Serializable
data class CopyResult(val nodeIds: List<NodeId>, val target: NodeId)

@Serializable
data class MoveResult(val nodeIds: List<NodeId>, val target: NodeId)

@Serializable
data class NodesExplorerNavKey(
    val nodeId: NodeId,
    val nodeSourceType: NodeSourceType,
    val explorerMode: ExplorerMode,
    val startNavKey: ExplorerNavKey,
    val shareUris: List<UriPath>?,
) : NavKey