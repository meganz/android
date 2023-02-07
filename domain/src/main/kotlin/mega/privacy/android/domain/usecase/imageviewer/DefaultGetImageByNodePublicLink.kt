package mega.privacy.android.domain.usecase.imageviewer

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.repository.ImageRepository
import mega.privacy.android.domain.repository.NetworkRepository

/**
 * Default Implementation of [GetImageByNodePublicLink]
 */
class DefaultGetImageByNodePublicLink(
    private val networkRepository: NetworkRepository,
    private val imageRepository: ImageRepository,
) : GetImageByNodePublicLink {
    override suspend fun invoke(
        nodeFileLink: String,
        fullSize: Boolean,
        highPriority: Boolean,
    ): Flow<ImageResult> {
        return imageRepository.getImageByNodePublicLink(
            nodeFileLink,
            fullSize,
            highPriority,
            networkRepository.isMeteredConnection() ?: false
        )
    }
}