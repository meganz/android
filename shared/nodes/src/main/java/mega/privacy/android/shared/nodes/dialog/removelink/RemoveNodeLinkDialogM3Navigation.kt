package mega.privacy.android.shared.nodes.dialog.removelink

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.compose.ui.window.DialogProperties
import kotlinx.serialization.Serializable

@Serializable
data class RemoveNodeLinkDialogNavKey(val handles: List<Long>) : NavKey

fun EntryProviderScope<NavKey>.removeNodeLinkDialogM3(
    onDismiss: () -> Unit,
) {
    entry<RemoveNodeLinkDialogNavKey>(
        metadata = DialogSceneStrategy.dialog(
            DialogProperties(
                windowTitle = "Remove Node Link Dialog"
            )
        )
    ) { key ->
        RemoveNodeLinkDialogM3(
            nodes = key.handles,
            onDismiss = onDismiss,
        )
    }
}
