package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * Is album link valid use case
 */
class IsAlbumLinkValidUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
) {
    suspend operator fun invoke(albumLink: AlbumLink) = albumRepository.isAlbumLinkValid(albumLink)
}
