package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * Implementation of the CreateAlbum use case
 */
class DefaultCreateAlbum @Inject constructor(
    private val albumRepository: AlbumRepository,
) : CreateAlbum {

    override suspend fun invoke(name: String) {
        albumRepository.createAlbum(name)
    }
}