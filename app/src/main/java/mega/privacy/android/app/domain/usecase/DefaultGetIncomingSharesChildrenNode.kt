package mega.privacy.android.app.domain.usecase

import mega.privacy.android.data.repository.FilesRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Default Get children nodes of the incoming shares parent handle or root list of incoming shares node
 */
class DefaultGetIncomingSharesChildrenNode @Inject constructor(
    private val getNodeByHandle: GetNodeByHandle,
    private val getChildrenNode: GetChildrenNode,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val getOthersSortOrder: GetOthersSortOrder,
    private val filesRepository: FilesRepository,
) : GetIncomingSharesChildrenNode {

    override suspend fun invoke(parentHandle: Long): List<MegaNode>? {
        return if (parentHandle == -1L || parentHandle == MegaApiJava.INVALID_HANDLE) {
            filesRepository.getIncomingSharesNode(getOthersSortOrder())
        } else {
            getNodeByHandle(parentHandle)?.let {
                getChildrenNode(it, getCloudSortOrder())
            }
        }
    }
}