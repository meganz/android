package mega.privacy.android.app.domain.usecase

import mega.privacy.android.domain.usecase.GetCloudSortOrder
import nz.mega.sdk.MegaNode
import javax.inject.Inject


/**
 * Default get children nodes of the browser parent handle
 *
 *  @property getNodeByHandle
 *  @property getChildrenNode
 *  @property getRootFolder
 *  @property getCloudSortOrder
 */
class DefaultGetBrowserChildrenNode @Inject constructor(
    private val getNodeByHandle: GetNodeByHandle,
    private val getChildrenNode: GetChildrenNode,
    private val getRootFolder: GetRootFolder,
    private val getCloudSortOrder: GetCloudSortOrder,
) : GetBrowserChildrenNode {

    override suspend fun invoke(parentHandle: Long): List<MegaNode>? {
        val node = (if (parentHandle != -1L) getNodeByHandle(parentHandle) else getRootFolder())
            ?: return null
        return getChildrenNode(parent = node, order = getCloudSortOrder())
    }

}