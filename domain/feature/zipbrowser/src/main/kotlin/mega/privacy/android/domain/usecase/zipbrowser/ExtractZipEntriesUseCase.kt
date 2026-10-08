package mega.privacy.android.domain.usecase.zipbrowser

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.zipbrowser.ZipArchiveEntry
import mega.privacy.android.domain.entity.zipbrowser.ZipArchiveException
import mega.privacy.android.domain.entity.zipbrowser.ZipArchiveIndex
import mega.privacy.android.domain.entity.zipbrowser.ZipBatchExtractionState
import mega.privacy.android.domain.repository.ZipArchiveRepository
import javax.inject.Inject

/**
 * Extracts a selection of entries, expanding any selected folder to the files beneath it.
 *
 * The free space needed is checked once before anything is written, so a batch that cannot fit
 * fails immediately instead of part way through with files already on disk.
 */
class ExtractZipEntriesUseCase @Inject constructor(
    private val zipArchiveRepository: ZipArchiveRepository,
    private val extractZipEntryToFileUseCase: ExtractZipEntryToFileUseCase,
) {

    /**
     * @param entryPaths the selected entries; a folder stands for every file below it
     */
    operator fun invoke(
        archivePath: String,
        entryPaths: List<String>,
        password: CharArray? = null,
    ): Flow<ZipBatchExtractionState> = flow {
        val index = runCatching { zipArchiveRepository.getArchiveIndex(archivePath) }
            .getOrElse { throwable ->
                emit(
                    ZipBatchExtractionState.Failed(
                        throwable as? ZipArchiveException ?: ZipArchiveException.Corrupted(throwable)
                    )
                )
                return@flow
            }

        val files = entryPaths.flatMap { path -> index.filesUnder(path) }.distinctBy { it.path }
        val requiredBytes = files.sumOf { it.size }
        val availableBytes = zipArchiveRepository.getExtractionFreeBytes()
        if (availableBytes in 0 until requiredBytes) {
            emit(
                ZipBatchExtractionState.Failed(
                    ZipArchiveException.InsufficientStorage(requiredBytes, availableBytes)
                )
            )
            return@flow
        }

        val extracted = mutableListOf<String>()
        val failures = mutableMapOf<String, ZipArchiveException>()
        files.forEachIndexed { position, entry ->
            emit(ZipBatchExtractionState.InProgress(position, files.size, entry.path))
            runCatching {
                extractZipEntryToFileUseCase(archivePath, entry.path, password)
            }.onSuccess { path ->
                extracted += path
            }.onFailure { throwable ->
                // One entry failing must not lose the ones that worked, so the batch carries on
                // and reports what did not come out at the end.
                failures[entry.path] = throwable as? ZipArchiveException
                    ?: ZipArchiveException.EntryExtractionFailed(entry.path, throwable)
            }
        }

        emit(ZipBatchExtractionState.Finished(extracted, failures))
    }

    private fun ZipArchiveIndex.filesUnder(path: String): List<ZipArchiveEntry> {
        val entry = entries[path] ?: return emptyList()
        if (!entry.isDirectory) return listOf(entry)
        return childrenOf(path).flatMap { filesUnder(it.path) }
    }
}
