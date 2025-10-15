package mega.privacy.android.core.nodecomponents.dialog.sharefolder

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.compose.ui.window.DialogProperties
import kotlinx.serialization.Serializable
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper

@Serializable
data class ShareFolderAccessDialogNavKey(
    val nodes: String,
    val contacts: String,
    val isFromBackups: Boolean
) : NavKey

fun EntryProviderScope<NavKey>.shareFolderAccessDialogM3(
    onDismiss: () -> Unit,
) {
    entry<ShareFolderAccessDialogNavKey>(
        metadata = DialogSceneStrategy.dialog(
            DialogProperties(
                windowTitle = "Share Folder Access Dialog"
            )
        )
    ) { key ->
        val mapper = NodeHandlesToJsonMapper()
        val handles = mapper(key.nodes)
        val contacts = key.contacts.split(",").filter { contact -> contact.isNotBlank() }

        ShareFolderAccessDialogM3(
            handles = handles,
            contactData = contacts,
            isFromBackups = key.isFromBackups,
            onDismiss = onDismiss,
        )
    }
}