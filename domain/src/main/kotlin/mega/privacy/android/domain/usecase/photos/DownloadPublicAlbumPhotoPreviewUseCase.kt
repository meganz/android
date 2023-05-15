package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * Download public album photo preview use case
 */
class DownloadPublicAlbumPhotoPreviewUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
) {
    suspend operator fun invoke(photo: Photo, callback: (Boolean) -> Unit) =
        albumRepository.downloadPublicPreview(photo, callback)
}
