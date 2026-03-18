package mega.privacy.android.navigation.destination

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey

@Serializable
data class ShareToMegaNavKey(val shareUris: List<UriPath>?) : NoSessionNavKey.Mandatory

@Serializable
data object ChatExplorerNavKey : NavKey

@Serializable
data class NodesExplorerNavKey(
    val nodeId: NodeId,
    val nodeSourceType: NodeSourceType,
) : NavKey