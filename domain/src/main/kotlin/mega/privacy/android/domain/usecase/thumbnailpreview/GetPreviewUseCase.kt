package mega.privacy.android.domain.usecase.thumbnailpreview

import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.thumbnailpreview.ThumbnailPreviewRepository
import javax.inject.Inject

/**
 * The use case implementation class to get node preview
 * @param thumbnailPreviewRepository [ThumbnailPreviewRepository]
 */
class GetPreviewUseCase @Inject constructor(
    private val thumbnailPreviewRepository: ThumbnailPreviewRepository,
) {

    /**
     * Invoke
     *
     * @param typedNode
     */
    suspend operator fun invoke(typedNode: TypedNode) =
        thumbnailPreviewRepository.getPreviewFromLocal(typedNode.id.longValue)
            ?: thumbnailPreviewRepository.getPreviewFromServer(typedNode)
}