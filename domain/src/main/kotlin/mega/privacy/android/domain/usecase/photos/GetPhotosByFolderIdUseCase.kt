package mega.privacy.android.domain.usecase.photos

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.PhotosRepository
import mega.privacy.android.domain.usecase.node.GetAncestorsIdsUseCase
import javax.inject.Inject

/**
 * Get Photos from a folder (support recursive sub folder)
 */
class GetPhotosByFolderIdUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
    private val nodeRepository: NodeRepository,
    private val getAncestorsIdsUseCase: GetAncestorsIdsUseCase,
) {

    /**
     * Get Photos from a folder
     *
     * @param folderId
     * @param recursive
     * @param isFromFolderLink
     *
     * @return photo
     */
    operator fun invoke(
        folderId: NodeId,
        recursive: Boolean,
        isFromFolderLink: Boolean = false,
    ): Flow<List<Photo>> = flow {
        val knownPhotoIds = MutableStateFlow(emptySet<Long>())

        suspend fun queryFolderPhotos(): List<Photo> =
            photosRepository.getPhotosByFolderId(
                folderId = folderId,
                recursive = recursive,
                isFromFolderLink = isFromFolderLink
            ).also { photos ->
                knownPhotoIds.value = photos.mapTo(mutableSetOf()) { it.id }
            }

        emit(queryFolderPhotos())
        emitAll(
            nodeRepository.monitorNodeUpdates()
                .filter { update ->
                    update.areChangesAffectingFolder(
                        folderId = folderId,
                        recursive = recursive,
                        isFromFolderLink = isFromFolderLink,
                        knownPhotoIds = knownPhotoIds.value,
                    )
                }
                .conflate()
                .map { queryFolderPhotos() }
        )
    }.cancellable()

    private suspend fun NodeUpdate.areChangesAffectingFolder(
        folderId: NodeId,
        recursive: Boolean,
        isFromFolderLink: Boolean,
        knownPhotoIds: Set<Long>,
    ): Boolean = changes.any { (node, nodeChanges) ->
        if (node !is FileNode || !node.isMediaNode()) return@any false
        if (nodeChanges.none { it in LIST_AFFECTING_CHANGES }) return@any false
        if (isFromFolderLink) return@any true

        node.id.longValue in knownPhotoIds || node.isInFolder(folderId, recursive)
    }

    private fun FileNode.isMediaNode(): Boolean =
        type is ImageFileTypeInfo || type is VideoFileTypeInfo

    private suspend fun Node.isInFolder(folderId: NodeId, recursive: Boolean): Boolean =
        if (recursive) folderId in getAncestorsIdsUseCase(this) else parentId == folderId

    private companion object {
        val LIST_AFFECTING_CHANGES = setOf(
            NodeChanges.New,
            NodeChanges.Remove,
            NodeChanges.Parent,
            NodeChanges.Timestamp,
            NodeChanges.Sensitive,
            NodeChanges.Favourite,
            NodeChanges.Name
        )
    }
}
