package mega.privacy.android.domain.usecase.media

import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

class CreateUserAlbumUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
) {
    suspend operator fun invoke(name: String) = albumRepository.createAlbum(name)
}