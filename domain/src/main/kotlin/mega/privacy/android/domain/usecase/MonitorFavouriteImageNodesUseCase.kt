package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.repository.PhotosRepository
import mega.privacy.android.domain.usecase.favourites.GetAllFavoritesUseCase
import javax.inject.Inject

/**
 * Monitor the favourite image and video nodes, in the same order as the favourites list
 */
class MonitorFavouriteImageNodesUseCase @Inject constructor(
    private val getAllFavoritesUseCase: GetAllFavoritesUseCase,
    private val photosRepository: PhotosRepository,
) {

    /**
     * Monitor the favourite image and video nodes
     *
     * Sensitive nodes are not excluded here; the caller decides whether hidden nodes are shown.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<ImageNode>> = getAllFavoritesUseCase()
        .mapLatest { favourites ->
            favourites
                .filterIsInstance<TypedFileNode>()
                .filter { it.type is ImageFileTypeInfo || it.type is VideoFileTypeInfo }
                .mapNotNull { photosRepository.fetchImageNode(nodeId = it.id, filterSvg = false) }
        }
}
