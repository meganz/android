package mega.privacy.android.core.nodecomponents.dialog.removeshare

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper
import mega.privacy.android.domain.entity.node.NodeId

@Serializable
data class RemoveShareFolderDialogNavKey(val nodes: String) : NavKey

fun NavGraphBuilder.removeShareFolderDialogM3(
    onDismiss: () -> Unit,
) {
    dialog<RemoveShareFolderDialogNavKey> {
        val args = it.toRoute<RemoveShareFolderDialogNavKey>()
        val mapper = NodeHandlesToJsonMapper()
        val nodes = mapper(args.nodes)
            .map { handle -> NodeId(handle) }

        RemoveShareFolderDialogM3(
            nodes = nodes,
            onDismiss = onDismiss,
        )
    }
}