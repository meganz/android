package mega.privacy.android.domain.usecase.media

import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

class ValidateAndUpdateUserAlbumUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val validateAlbumNameUseCase: ValidateAlbumNameUseCase
) {
    suspend operator fun invoke(albumId: AlbumId, name: String) {
        validateAlbumNameUseCase(name)
        albumRepository.updateAlbumName(albumId, name)
    }
}