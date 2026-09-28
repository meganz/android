package mega.privacy.android.domain.usecase.shares

import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.exception.ShareAccessNotSetException
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Default Implementation of [SetOutgoingPermissions]
 */
class DefaultSetOutgoingPermissions @Inject constructor(
    private val nodeRepository: NodeRepository,
) : SetOutgoingPermissions {
    override suspend fun invoke(
        folderNode: TypedFolderNode,
        accessPermission: AccessPermission,
        vararg emails: String,
    ) {
        val result: (suspend (AccessPermission, String) -> Unit)? =
            nodeRepository.createShareKey(folderNode)
        emails.mapNotNull { email ->
            runCatching {
                result?.invoke(accessPermission, email)
            }.exceptionOrNull()?.also {
                it.printStackTrace()
            }
        }.takeIf { it.isNotEmpty() }?.let { throw ShareAccessNotSetException(it.size) }
    }
}