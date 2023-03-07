package mega.privacy.android.domain.usecase.shares

import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.contact.GetContactItem
import javax.inject.Inject

/**
 * Default implementation of [GetContactItemFromInShareFolder]
 */
class DefaultGetContactItemFromInShareFolder @Inject constructor(
    private val getContactItem: GetContactItem,
    private val nodeRepository: NodeRepository,
) : GetContactItemFromInShareFolder {
    override suspend fun invoke(folderNode: TypedFolderNode, skipCache: Boolean): ContactItem? =
        nodeRepository.getOwnerIdFromInShare(folderNode.id, false)?.let { userId ->
            getContactItem(userId, skipCache)
        }
}