package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Default implementation of [GetFolderTreeInfo]
 *
 * @property nodeRepository [NodeRepository]
 */
class DefaultGetFolderTreeInfo @Inject constructor(
    private val nodeRepository: NodeRepository,
) : GetFolderTreeInfo {

    override suspend fun invoke(folderNode: TypedFolderNode): FolderTreeInfo =
        nodeRepository.getFolderTreeInfo(folderNode)
}
