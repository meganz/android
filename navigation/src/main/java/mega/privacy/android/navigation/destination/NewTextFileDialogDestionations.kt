package mega.privacy.android.navigation.destination

import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.navigation.contract.dialog.DialogNavKey

@Serializable
data class NewTextFileDialogNavKey(
    val parentNodeId: NodeId,
    val returnFileName: Boolean = false,
) : DialogNavKey {
    companion object {
        const val FILE_NAME_RESULT = "NewTextFileDialogNavKey_file_name_result"
    }
}

@Serializable
data class NewURLFileDialogNavKey(
    val parentNodeId: NodeId,
) : DialogNavKey {
    companion object {
        const val FILE_NAME_RESULT = "NewURLFileDialogNavKey_file_name_result"
    }
}
