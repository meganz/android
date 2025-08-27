package mega.privacy.android.core.nodecomponents.dialog.removelink

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper

@Serializable
data class RemoveNodeLinkDialogArgs(val nodes: String) : NavKey

fun NavGraphBuilder.removeNodeLinkDialogM3(
    onDismiss: () -> Unit,
) {
    dialog<RemoveNodeLinkDialogArgs> {
        val args = it.toRoute<RemoveNodeLinkDialogArgs>()
        val mapper = NodeHandlesToJsonMapper()

        RemoveNodeLinkDialogM3(
            nodes = mapper(args.nodes),
            onDismiss = onDismiss,
        )
    }
}