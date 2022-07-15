package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.FilesRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Get children nodes of the incoming shares parent handle or root list of incoming shares node
 */
class DefaultGetIncomingSharesChildrenNode @Inject constructor(
    private val getNodeByHandle: GetNodeByHandle,
    private val getChildrenNode: GetChildrenNode,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val getOthersSortOrder: GetOthersSortOrder,
    private val filesRepository: FilesRepository,
) : GetIncomingSharesChildrenNode {

    /**
     * Get a list of all incoming shares or children nodes of the parent handle
     *
     * @param parentHandle
     * @return Children nodes of the parent handle, root list of incoming shares if parent handle is invalid
     */
    @Throws(Exception::class)
    override suspend fun invoke(parentHandle: Long): List<MegaNode> {
        return if (parentHandle == -1L || parentHandle == MegaApiJava.INVALID_HANDLE) {
            filesRepository.getIncomingSharesNode(getOthersSortOrder())
        } else {
            getNodeByHandle(parentHandle)
                ?.let { getChildrenNode(it, getCloudSortOrder()) }
                ?: run { throw Exception("Node cannot be retrieved") }
        }
    }
}