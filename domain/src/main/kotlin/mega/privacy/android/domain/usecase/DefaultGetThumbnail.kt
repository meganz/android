package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.ThumbnailRepository
import java.io.File
import javax.inject.Inject

/**
 * The use case implementation class to get node thumbnail
 * @param repository AlbumsRepository
 */
class DefaultGetThumbnail @Inject constructor(private val repository: ThumbnailRepository) :
    GetThumbnail {

    override suspend fun invoke(nodeId: Long): File? {
        return repository.getThumbnailFromLocal(nodeId) ?: repository.getThumbnailFromServer(
            nodeId
        )
    }
}