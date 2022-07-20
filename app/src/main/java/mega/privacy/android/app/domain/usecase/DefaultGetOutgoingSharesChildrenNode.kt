package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.FilesRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Default Get children nodes of the outgoing shares parent handle or root list of outgoing shares node
 */
class DefaultGetOutgoingSharesChildrenNode @Inject constructor(
    private val getNodeByHandle: GetNodeByHandle,
    private val getChildrenNode: GetChildrenNode,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val getOthersSortOrder: GetOthersSortOrder,
    private val filesRepository: FilesRepository,
) : GetOutgoingSharesChildrenNode {

    override suspend fun invoke(parentHandle: Long): List<MegaNode>? {
        return if (parentHandle == -1L || parentHandle == MegaApiJava.INVALID_HANDLE) {
            filesRepository.getOutgoingSharesNode(getOthersSortOrder())
                .filter { it.user != null }
                .distinctBy { it.nodeHandle }
                .mapNotNull { getNodeByHandle(it.nodeHandle) }
        } else {
            getNodeByHandle(parentHandle)
                ?.let { getChildrenNode(it, getCloudSortOrder()) }
                ?: run { null }
        }
    }
}