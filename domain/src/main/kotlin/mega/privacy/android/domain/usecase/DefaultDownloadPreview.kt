package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.ImageRepository
import javax.inject.Inject

/**
 * The use case implementation class to download node preview
 * @param repository GetImageRepository
 */
class DefaultDownloadPreview @Inject constructor(private val repository: ImageRepository) :
    DownloadPreview {

    override suspend fun invoke(nodeId: Long, callback: (success: Boolean) -> Unit) =
        repository.downloadPreview(nodeId, callback)
}