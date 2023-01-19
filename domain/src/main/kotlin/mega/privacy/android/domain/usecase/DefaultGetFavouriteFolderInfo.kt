package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.domain.entity.FavouriteFolderInfo
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.ParentNotAFolderException
import mega.privacy.android.domain.repository.FileRepository
import javax.inject.Inject

/**
 * The use case implementation class to get children nodes by node
 * @param fileRepository FileRepository
 */
class DefaultGetFavouriteFolderInfo @Inject constructor(
    private val fileRepository: FileRepository,
    private val addNodeType: AddNodeType,
) : GetFavouriteFolderInfo {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(parentHandle: Long) =
        flow {
            emit(getFavouriteFolderInfo(parentHandle))
            emitAll(fileRepository.monitorNodeUpdates().mapLatest {
                getFavouriteFolderInfo(parentHandle)
            })
        }

    private suspend fun getFavouriteFolderInfo(parentHandle: Long): FavouriteFolderInfo {
        val parent = fileRepository.getNodeById(NodeId(parentHandle)) as? FolderNode
            ?: throw ParentNotAFolderException("Attempted to fetch favourite folder info for node: $parentHandle")
        val children = fileRepository.getNodeChildren(parent)
            .map { addNodeType(it) }
        return FavouriteFolderInfo(
            children = children,
            name = parent.name,
            currentHandle = parentHandle,
            parentHandle = parent.parentId.longValue
        )
    }
}