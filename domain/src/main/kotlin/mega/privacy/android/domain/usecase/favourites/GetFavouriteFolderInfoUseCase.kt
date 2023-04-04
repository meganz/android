package mega.privacy.android.domain.usecase.favourites

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.domain.entity.FavouriteFolderInfo
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.ParentNotAFolderException
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.AddNodeType
import javax.inject.Inject

/**
 * The use case for getting children nodes by node
 */
class GetFavouriteFolderInfoUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val addNodeType: AddNodeType,
) {
    /**
     * Get children nodes by node
     * @param parentHandle parent node handle
     * @return Flow<FavouriteFolderInfo>
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(parentHandle: Long) =
        flow {
            emit(getFavouriteFolderInfo(parentHandle))
            emitAll(nodeRepository.monitorNodeUpdates().mapLatest {
                getFavouriteFolderInfo(parentHandle)
            })
        }

    private suspend fun getFavouriteFolderInfo(parentHandle: Long): FavouriteFolderInfo {
        val parent = nodeRepository.getNodeById(NodeId(parentHandle)) as? FolderNode
            ?: throw ParentNotAFolderException("Attempted to fetch favourite folder info for node: $parentHandle")
        val children = nodeRepository.getNodeChildren(parent)
            .map { addNodeType(it) }
        return FavouriteFolderInfo(
            children = children,
            name = parent.name,
            currentHandle = parentHandle,
            parentHandle = parent.parentId.longValue
        )
    }
}