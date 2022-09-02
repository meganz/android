package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.ImageRepository
import java.io.File
import javax.inject.Inject

/**
 * The use case implementation class to get node preview
 * @param repository GetImageRepository
 */
class DefaultGetPreview @Inject constructor(private val repository: ImageRepository) :
    GetPreview {

    override suspend fun invoke(nodeId: Long): File? {
        return repository.getPreviewFromLocal(nodeId) ?: repository.getPreviewFromServer(
            nodeId
        )
    }
}