package mega.privacy.android.core.nodecomponents.dialog.sharefolder

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper

@Serializable
data class ShareFolderAccessDialogArgs(
    val nodes: String,
    val contacts: String,
    val isFromBackups: Boolean
) : NavKey

fun NavGraphBuilder.shareFolderAccessDialogM3(
    onDismiss: () -> Unit,
) {
    dialog<ShareFolderAccessDialogArgs> {
        val args = it.toRoute<ShareFolderAccessDialogArgs>()
        val mapper = NodeHandlesToJsonMapper()
        val handles = mapper(args.nodes)
        val contacts = args.contacts.split(",").filter { contact -> contact.isNotBlank() }

        ShareFolderAccessDialogM3(
            handles = handles,
            contactData = contacts,
            isFromBackups = args.isFromBackups,
            onDismiss = onDismiss,
        )
    }
}