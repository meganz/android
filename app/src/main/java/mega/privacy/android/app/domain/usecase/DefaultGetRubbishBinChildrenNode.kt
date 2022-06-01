package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.globalmanagement.SortOrderManagementInterface
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Default get children nodes of the rubbish bin parent handle
 *
 *  @property getNodeByHandle
 *  @property getChildrenNode
 *  @property getRubbishBinFolder
 *  @property sortOrderManagement
 */
class DefaultGetRubbishBinChildrenNode @Inject constructor(
    private val getNodeByHandle: GetNodeByHandle,
    private val getChildrenNode: GetChildrenNode,
    private val getRubbishBinFolder: GetRubbishBinFolder,
    private val sortOrderManagement: SortOrderManagementInterface,
) : GetRubbishBinChildrenNode {

    override fun invoke(parentHandle: Long): List<MegaNode>? {
        val rubbishNode = (if (parentHandle != -1L) getNodeByHandle(parentHandle) else getRubbishBinFolder())
            ?: return null
        return getChildrenNode(parent = rubbishNode, order = sortOrderManagement.getOrderCloud())
    }
}