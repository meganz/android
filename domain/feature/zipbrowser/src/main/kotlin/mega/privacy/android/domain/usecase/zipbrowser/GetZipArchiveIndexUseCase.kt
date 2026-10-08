package mega.privacy.android.domain.usecase.zipbrowser

import mega.privacy.android.domain.entity.zipbrowser.ZipArchiveIndex
import mega.privacy.android.domain.repository.ZipArchiveRepository
import javax.inject.Inject

/**
 * Reads the folder structure of a local archive without decompressing it.
 */
class GetZipArchiveIndexUseCase @Inject constructor(
    private val zipArchiveRepository: ZipArchiveRepository,
) {

    /**
     * @param archivePath absolute path of the archive on disk
     */
    suspend operator fun invoke(archivePath: String): ZipArchiveIndex =
        zipArchiveRepository.getArchiveIndex(archivePath)
}
