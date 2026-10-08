package mega.privacy.android.domain.usecase.zipbrowser

import mega.privacy.android.domain.repository.ZipArchiveRepository
import javax.inject.Inject

/**
 * The local path of an entry that has already been extracted, so a caller can skip extracting it
 * again.
 */
class GetCachedZipEntryUseCase @Inject constructor(
    private val zipArchiveRepository: ZipArchiveRepository,
) {

    suspend operator fun invoke(archivePath: String, entryPath: String): String? =
        zipArchiveRepository.getCachedEntryPath(archivePath, entryPath)
}
