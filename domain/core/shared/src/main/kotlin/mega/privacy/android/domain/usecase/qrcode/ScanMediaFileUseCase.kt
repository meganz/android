package mega.privacy.android.domain.usecase.qrcode

import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * The use case to scan media files
 */
class ScanMediaFileUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * Update media store by scanning given media files
     * @param paths Array of paths to be scanned.
     * @param mimeTypes array of MIME types for each path.
     */
    operator fun invoke(paths: Array<String>, mimeTypes: Array<String>) =
        fileSystemRepository.scanMediaFile(paths, mimeTypes)
}