package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * Disable export albums use case
 */
class DisableExportAlbumsUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
) {
    suspend operator fun invoke(albumIds: List<AlbumId>) =
        albumRepository.disableExportAlbums(albumIds)
}
