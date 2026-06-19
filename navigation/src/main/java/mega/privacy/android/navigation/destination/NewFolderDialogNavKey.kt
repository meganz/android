package mega.privacy.android.navigation.destination

import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.navigation.contract.dialog.DialogNavKey

@Serializable
data class NewFolderDialogNavKey(
    val parentNodeId: NodeId,
) : DialogNavKey {
    companion object {
        const val FOLDER_HANDLE_RESULT = "NewFolderDialogNavKey_folder_handle_result"
    }
}
