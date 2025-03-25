package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Get GPS coordinates use case
 *
 * @property fileSystemRepository
 * @constructor Create empty Get GPS coordinates use case
 */
class GetGPSCoordinatesUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
    private val isVideoFileUseCase: IsVideoFileUseCase,
    private val isImageFileUseCase: IsImageFileUseCase,
) {

    /**
     * Invoke
     *
     * @param uriPath Uri path of the file.
     * @param isVideo True if is a video, false if is an image, null if it's unknown.
     * @return GPS coordinates.
     */
    suspend operator fun invoke(uriPath: UriPath, isVideo: Boolean? = null): Pair<Double, Double>? {
        return when {
            isVideo == true -> getGPS(uriPath, true)
            isVideo == false -> getGPS(uriPath, false)
            isVideoFileUseCase(uriPath) -> getGPS(uriPath, true)
            isImageFileUseCase(uriPath) -> getGPS(uriPath, false)
            else -> null
        }
    }

    private suspend fun getGPS(uriPath: UriPath, isVideo: Boolean) =
        if (isVideo) {
            fileSystemRepository.getVideoGPSCoordinates(uriPath)
        } else {
            fileSystemRepository.getPhotoGPSCoordinates(uriPath)
        }
}
