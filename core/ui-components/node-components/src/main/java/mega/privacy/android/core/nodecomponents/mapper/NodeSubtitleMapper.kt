package mega.privacy.android.core.nodecomponents.mapper

import mega.privacy.android.core.nodecomponents.model.NodeSubtitleText
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.shares.ShareFolderNode
import javax.inject.Inject

/**
 * Mapper to create NodeSubtitleText from TypedNode
 */
class NodeSubtitleMapper @Inject constructor() {

    /**
     * Invoke
     * @param node The node to get subtitle for
     * @param showPublicLinkCreationTime Whether to show public link creation time
     * @return NodeSubtitleText subtitle
     */
    operator fun invoke(
        node: TypedNode,
        showPublicLinkCreationTime: Boolean,
    ): NodeSubtitleText {
        return when (node) {
            is TypedFileNode -> {
                NodeSubtitleText.FileSubtitle(
                    fileSizeValue = node.size,
                    modificationTime = node.modificationTime,
                    showPublicLinkCreationTime = showPublicLinkCreationTime,
                    publicLinkCreationTime = node.exportedData?.publicLinkCreationTime
                )
            }

            is TypedFolderNode -> {
                // Check if it's a shared folder first
                (node as? ShareFolderNode)?.shareData?.let { shareData ->
                    return NodeSubtitleText.SharedSubtitle(
                        shareCount = shareData.count,
                        user = shareData.user,
                        userFullName = shareData.userFullName,
                        isVerified = shareData.isVerified
                    )
                }

                // Regular folder
                NodeSubtitleText.FolderSubtitle(
                    childFolderCount = node.childFolderCount,
                    childFileCount = node.childFileCount
                )
            }

            else -> NodeSubtitleText.Empty
        }
    }
} 