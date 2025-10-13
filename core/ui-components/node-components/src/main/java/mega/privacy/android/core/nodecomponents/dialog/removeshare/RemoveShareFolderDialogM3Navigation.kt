package mega.privacy.android.core.nodecomponents.dialog.removeshare

import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.compose.ui.window.DialogProperties
import kotlinx.serialization.Serializable
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper
import mega.privacy.android.domain.entity.node.NodeId

@Serializable
data class RemoveShareFolderDialogNavKey(val nodes: String) : NavKey

fun EntryProviderBuilder<NavKey>.removeShareFolderDialogM3(
    onDismiss: () -> Unit,
) {
    entry<RemoveShareFolderDialogNavKey>(
        metadata = DialogSceneStrategy.dialog(
            DialogProperties(
                windowTitle = "Remove Share Folder Dialog"
            )
        )
    ) { key ->
        val mapper = NodeHandlesToJsonMapper()
        val nodes = mapper(key.nodes)
            .map { handle -> NodeId(handle) }

        RemoveShareFolderDialogM3(
            nodes = nodes,
            onDismiss = onDismiss,
        )
    }
}