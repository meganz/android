package mega.privacy.android.domain.usecase.zipbrowser

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.zipbrowser.ZipExtractionState
import mega.privacy.android.domain.repository.ZipArchiveRepository
import javax.inject.Inject

/**
 * Extracts one entry out of an archive, leaving every other entry alone.
 */
class ExtractZipEntryUseCase @Inject constructor(
    private val zipArchiveRepository: ZipArchiveRepository,
) {

    /**
     * @param password needed only for an encrypted entry
     * @return progress ending in [ZipExtractionState.Completed] or [ZipExtractionState.Failed]
     */
    operator fun invoke(
        archivePath: String,
        entryPath: String,
        password: CharArray? = null,
    ): Flow<ZipExtractionState> =
        zipArchiveRepository.extractEntry(archivePath, entryPath, password)
}
