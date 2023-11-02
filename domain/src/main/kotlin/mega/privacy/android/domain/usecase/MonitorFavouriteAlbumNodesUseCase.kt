package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.domain.entity.GifFileTypeInfo
import mega.privacy.android.domain.entity.RawFileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Get favourite album nodes use case
 */
class MonitorFavouriteAlbumNodesUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {
    operator fun invoke(): Flow<List<ImageNode>> = photosRepository.monitorImageNodes()
        .mapLatest { imageNodes ->
            imageNodes.filter {
                it.isFavourite && (isImageFileType(it) || isVideoFileType(it))
            }
        }.flowOn(defaultDispatcher)

    private fun isImageFileType(imageNode: ImageNode): Boolean {
        return imageNode.type.let {
            it is StaticImageFileTypeInfo || it is GifFileTypeInfo || it is RawFileTypeInfo
        }
    }

    private suspend fun isVideoFileType(imageNode: ImageNode): Boolean {
        return imageNode.type is VideoFileTypeInfo && inSyncFolder(imageNode.parentId)
    }

    private suspend fun inSyncFolder(parentId: NodeId): Boolean {
        val id = parentId.longValue

        return photosRepository.run {
            id == getCameraUploadFolderId() || id == getMediaUploadFolderId()
        }
    }
}
