package mega.privacy.android.core.nodecomponents.dialog.sharefolder

import androidx.compose.runtime.remember
import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import kotlinx.serialization.Serializable
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode

@Serializable
data class ShareFolderDialogNavKey(val nodes: String) : NavKey {
    companion object {
        const val RESULT = "ShareFolderDialogNavKey:extra_result"
    }
}

/**
 * Result returned from [ShareFolderDialogM3] when user confirms the share action.
 * @param nodes List of nodes that were confirmed for sharing
 */
@Serializable
data class ShareFolderDialogResult(val nodes: List<TypedNode>)

fun EntryProviderScope<NavKey>.shareFolderDialogM3(
    onDismiss: () -> Unit,
    returnResult: (String, ShareFolderDialogResult) -> Unit,
) {
    entry<ShareFolderDialogNavKey>(
        metadata = DialogSceneStrategy.dialog(
            DialogProperties(windowTitle = "Share Folder Dialog")
        )
    ) { key ->
        val mapper = remember { NodeHandlesToJsonMapper() }
        val nodeIds = mapper(key.nodes).map { handle -> NodeId(handle) }

        ShareFolderDialogM3(
            nodeIds = nodeIds,
            onDismiss = onDismiss,
            onConfirm = { nodes ->
                returnResult(
                    ShareFolderDialogNavKey.RESULT,
                    ShareFolderDialogResult(nodes)
                )
                onDismiss()
            }
        )
    }
}


