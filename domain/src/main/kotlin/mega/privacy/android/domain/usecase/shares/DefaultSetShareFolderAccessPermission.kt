package mega.privacy.android.domain.usecase.shares

import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.exception.ShareAccessNotSetException
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Default Implementation of [SetShareFolderAccessPermission]
 */
class DefaultSetShareFolderAccessPermission @Inject constructor(
    private val nodeRepository: NodeRepository,
) : SetShareFolderAccessPermission {
    override suspend fun invoke(
        folderNode: FolderNode,
        accessPermission: AccessPermission,
        vararg emails: String,
    ) {
        emails.mapNotNull { email ->
            runCatching {
                nodeRepository.setShareAccess(folderNode.id, accessPermission, email)
            }.exceptionOrNull()
        }.takeIf { it.isNotEmpty() }?.let { throw ShareAccessNotSetException(it.size) }
    }
}