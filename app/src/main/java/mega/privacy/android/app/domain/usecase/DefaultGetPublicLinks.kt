package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.FilesRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetLinksSortOrder
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Default get a list with all public links
 */
class DefaultGetPublicLinks @Inject constructor(
    private val getNodeByHandle: GetNodeByHandle,
    private val getChildrenNode: GetChildrenNode,
    private val getLinksSortOrder: GetLinksSortOrder,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val filesRepository: FilesRepository,
) : GetPublicLinks {

    override suspend fun invoke(parentHandle: Long): List<MegaNode>? {
        return if (parentHandle == -1L || parentHandle == MegaApiJava.INVALID_HANDLE) {
            filesRepository.getPublicLinks(getLinksSortOrder())
        } else {
            getNodeByHandle(parentHandle)?.let {
                getChildrenNode(it, getCloudSortOrder())
            }
        }
    }
}