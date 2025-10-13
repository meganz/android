package mega.privacy.android.core.nodecomponents.dialog.removelink

import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.compose.ui.window.DialogProperties
import kotlinx.serialization.Serializable
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper

@Serializable
data class RemoveNodeLinkDialogNavKey(val nodes: String) : NavKey

fun EntryProviderBuilder<NavKey>.removeNodeLinkDialogM3(
    onDismiss: () -> Unit,
) {
    entry<RemoveNodeLinkDialogNavKey>(
        metadata = DialogSceneStrategy.dialog(
            DialogProperties(
                windowTitle = "Remove Node Link Dialog"
            )
        )
    ) { key ->
        val mapper = NodeHandlesToJsonMapper()

        RemoveNodeLinkDialogM3(
            nodes = mapper(key.nodes),
            onDismiss = onDismiss,
        )
    }
}