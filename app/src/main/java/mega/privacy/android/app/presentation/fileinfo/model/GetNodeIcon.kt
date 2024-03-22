package mega.privacy.android.app.presentation.fileinfo.model

import mega.privacy.android.icon.pack.R as IconPackR
import androidx.annotation.DrawableRes
import mega.privacy.android.app.presentation.node.model.mapper.getDefaultFolderIcon
import mega.privacy.android.app.presentation.node.model.mapper.getFileIcon
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Get the icon resource associated to the [TypedNode]
 */
@DrawableRes
fun getNodeIcon(typedNode: TypedNode, originShares: Boolean) = when (typedNode) {
    is TypedFileNode -> getFileIcon(typedNode)
    is TypedFolderNode -> {
        //in SHARED_ITEMS drawer, outgoing share icon has priority over Camera uploads and Chat
        if (
        // the node is shown in shared drawer
            originShares
            // is a shared node
            && (typedNode.isShared || typedNode.isPendingShare)
            // node would be a chat or camera upload icon in another drawer
            && !typedNode.isInRubbishBin
            && !typedNode.isIncomingShare
        ) {
            IconPackR.drawable.ic_folder_outgoing_medium_solid
        } else {
            getDefaultFolderIcon(typedNode) //in other cases, default icon
        }
    }

    else -> IconPackR.drawable.ic_generic_medium_solid
}