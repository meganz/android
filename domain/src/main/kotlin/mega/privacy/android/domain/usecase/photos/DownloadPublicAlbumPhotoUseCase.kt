package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * Download public album photo thumbnail or preview use case
 *
 * @param isPreview True to download preview, false to download thumbnail
 */
class DownloadPublicAlbumPhotoUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
) {
    suspend operator fun invoke(photoId: Long, path: String?, isPreview: Boolean): Boolean =
        if (isPreview) {
            albumRepository.downloadPublicPreview(photoId, path)
        } else {
            albumRepository.downloadPublicThumbnail(photoId, path)
        }
}
