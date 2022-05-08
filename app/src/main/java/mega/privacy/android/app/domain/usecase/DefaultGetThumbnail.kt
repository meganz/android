package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.repository.AlbumsRepository
import java.io.File
import javax.inject.Inject

/**
 * The use case implementation class to get node thumbnail
 * @param repository AlbumsRepository
 */
class DefaultGetThumbnail @Inject constructor(private val repository: AlbumsRepository) :
    GetThumbnail {

    override suspend fun invoke(nodeId: Long, base64Handle: String): File =
        repository.getThumbnail(nodeId, base64Handle)

}