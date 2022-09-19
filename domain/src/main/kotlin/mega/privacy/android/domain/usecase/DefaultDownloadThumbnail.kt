package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.ImageRepository
import javax.inject.Inject

/**
 * The use case implementation class to download node thumbnail
 * @param repository GetImageRepository
 */
class DefaultDownloadThumbnail @Inject constructor(private val repository: ImageRepository) :
    DownloadThumbnail {

    override suspend fun invoke(nodeId: Long, callback: (success: Boolean) -> Unit) =
        repository.downloadThumbnail(nodeId, callback)
}