package mega.privacy.android.app.presentation.filecontact.navigation

import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.node.NodeId

@Serializable
class FileContactInfo(
    val folderHandle: Long,
    val folderName: String,
) {
    val folderId: NodeId
        get() = NodeId(folderHandle)
}