package mega.privacy.android.domain.usecase.zipbrowser

import kotlinx.coroutines.flow.last
import mega.privacy.android.domain.entity.zipbrowser.ZipArchiveException
import mega.privacy.android.domain.entity.zipbrowser.ZipExtractionState
import mega.privacy.android.domain.repository.ZipArchiveRepository
import javax.inject.Inject

/**
 * Extracts one entry and waits for it, for the actions that need the bytes on disk before they can
 * run - saving, sharing or uploading a file that is still inside the archive.
 */
class ExtractZipEntryToFileUseCase @Inject constructor(
    private val zipArchiveRepository: ZipArchiveRepository,
) {

    /**
     * @return the local path of the extracted entry
     * @throws ZipArchiveException when the entry could not be extracted
     */
    suspend operator fun invoke(
        archivePath: String,
        entryPath: String,
        password: CharArray? = null,
    ): String =
        when (val terminal = zipArchiveRepository.extractEntry(archivePath, entryPath, password).last()) {
            is ZipExtractionState.Completed -> terminal.absolutePath
            is ZipExtractionState.Failed -> throw terminal.cause
            is ZipExtractionState.InProgress ->
                throw ZipArchiveException.EntryExtractionFailed(entryPath)
        }
}
