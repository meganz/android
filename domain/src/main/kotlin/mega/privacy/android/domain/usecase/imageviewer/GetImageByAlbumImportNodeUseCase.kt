package mega.privacy.android.domain.usecase.imageviewer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.usecase.node.AddImageTypeUseCase
import javax.inject.Inject

/**
 * The use case to get Image Result given Node Handle
 */
class GetImageByAlbumImportNodeUseCase @Inject constructor(
    private val addImageTypeUseCase: AddImageTypeUseCase,
    private val getImageUseCase: GetImageUseCase,
    private val albumRepository: AlbumRepository,
) {
    /**
     * Invoke
     *
     * @param nodeHandle            Image Node handle to request
     * @param fullSize              Flag to request full size image despite data/size requirements
     * @param highPriority          Flag to request image with high priority
     * @param resetDownloads        Callback to reset downloads
     *
     * @return Flow<ImageResult>
     */
    operator fun invoke(
        nodeHandle: Long,
        fullSize: Boolean,
        highPriority: Boolean,
        resetDownloads: () -> Unit,
    ): Flow<ImageResult> = flow {
        val imageNode = albumRepository.getPublicPhotoImageNode(nodeId = NodeId(nodeHandle))
        val node = addImageTypeUseCase(imageNode)

        emitAll(getImageUseCase(node, fullSize, highPriority, resetDownloads))
    }
}
