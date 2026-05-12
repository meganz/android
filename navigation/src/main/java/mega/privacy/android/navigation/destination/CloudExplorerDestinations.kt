package mega.privacy.android.navigation.destination

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.uri.UriPath

interface ExplorerNavKey : NavKey

@Serializable
data class ShareFilesToMegaNavKey(val shareUris: List<UriPath>) : ExplorerNavKey

@Serializable
data class ShareTextToMegaNavKey(
    val text: String,
    val subject: String?,
    val email: String?,
) : ExplorerNavKey

@Serializable
data class NodesExplorerNavKey(
    val nodeId: NodeId,
    val nodeSourceType: NodeSourceType,
    val explorerMode: ExplorerMode,
    val startNavKey: ExplorerNavKey,
    val shareUris: List<UriPath>?,
) : NavKey