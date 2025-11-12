package mega.privacy.android.domain.usecase.media

import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

class ValidateAndCreateUserAlbumUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val validateAlbumNameUseCase: ValidateAlbumNameUseCase
) {
    suspend operator fun invoke(name: String) {
        validateAlbumNameUseCase(name)
        albumRepository.createAlbum(name)
    }
}