package mega.privacy.android.core.nodecomponents.dialog.rename

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.node.NodeId

@Serializable
data class RenameNodeDialogNavKey(val nodeId: Long) : NavKey

fun NavGraphBuilder.renameNodeDialogM3(
    onDismiss: () -> Unit,
) {
    dialog<RenameNodeDialogNavKey> {
        val args = it.toRoute<RenameNodeDialogNavKey>()

        RenameNodeDialogM3(
            nodeId = NodeId(args.nodeId),
            onDismiss = onDismiss,
        )
    }
}