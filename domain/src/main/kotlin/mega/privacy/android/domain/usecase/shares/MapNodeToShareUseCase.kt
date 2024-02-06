package mega.privacy.android.domain.usecase.shares

import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.shares.ShareFileNode
import mega.privacy.android.domain.entity.node.shares.ShareFolderNode
import mega.privacy.android.domain.entity.node.shares.ShareNode
import javax.inject.Inject

/**
 * Map node to share node use case
 */
class MapNodeToShareUseCase @Inject constructor() {
    /**
     * Invoke
     *
     * @param node [TypedNode]
     * @param shareData [ShareData]
     * @return
     */
    operator fun invoke(node: TypedNode, shareData: ShareData? = null): ShareNode =
        when (node) {
            is TypedFileNode -> ShareFileNode(node, shareData)
            is TypedFolderNode -> ShareFolderNode(node, shareData)
            else -> throw IllegalStateException("Invalid type")
        }
}