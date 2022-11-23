package mega.privacy.android.app.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId
import nz.mega.sdk.MegaNode

/**
 * Use Case to copy a [MegaNode] and move it to a new [MegaNode] while updating its name
 */
fun interface CopyNode {

    /**
     * Copy a [MegaNode] and move it to a new [MegaNode] while updating its name
     *
     * @param nodeToCopy the [MegaNode] to copy
     * @param newNodeParent the [MegaNode] that [nodeToCopy] will be moved to
     * @param newNodeName the new name for [nodeToCopy] once it is moved to [newNodeParent]
     *
     * @return the handle of the new [MegaNode] that was copied
     */
    suspend operator fun invoke(
        nodeToCopy: MegaNode,
        newNodeParent: MegaNode,
        newNodeName: String,
    ): NodeId
}