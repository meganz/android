package mega.privacy.android.core.nodecomponents.dialog.rename

import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.node.NodeId

@Serializable
data class RenameNodeDialogNavKey(val nodeId: Long) : NavKey

fun EntryProviderScope<NavKey>.renameNodeDialogM3(
    onDismiss: () -> Unit,
) {
    entry<RenameNodeDialogNavKey>(
        metadata = DialogSceneStrategy.dialog(
            DialogProperties(
                windowTitle = "Rename Node Dialog"
            )
        )
    ) { key ->
        RenameNodeDialogM3(
            nodeId = NodeId(key.nodeId),
            onDismiss = onDismiss,
        )
    }
}