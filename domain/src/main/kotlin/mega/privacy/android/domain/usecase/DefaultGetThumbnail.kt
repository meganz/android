package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.ImageRepository
import java.io.File
import javax.inject.Inject

/**
 * The use case implementation class to get node thumbnail
 * @param repository GetImageRepository
 */
class DefaultGetThumbnail @Inject constructor(private val repository: ImageRepository) :
    GetThumbnail {

    override suspend fun invoke(nodeId: Long): File? {
        runCatching {
            repository.getThumbnailFromLocal(nodeId) ?: repository.getThumbnailFromServer(nodeId)
        }.fold(
            onSuccess = { return it },
            onFailure = { return null }
        )
    }
}