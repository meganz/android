package mega.privacy.android.domain.usecase.zipbrowser

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.entity.zipbrowser.ZipArchiveEntry
import mega.privacy.android.domain.entity.zipbrowser.ZipArchiveException
import mega.privacy.android.domain.entity.zipbrowser.ZipArchiveIndex
import mega.privacy.android.domain.entity.zipbrowser.ZipBatchExtractionState
import mega.privacy.android.domain.entity.zipbrowser.ZipExtractionState
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.ZipArchiveRepository
import java.io.File
import javax.inject.Inject

/**
 * Saves a selection out of an archive into a folder the user picked.
 *
 * The archive is already a local file, so this is a local copy rather than a transfer: the selected
 * entries are written out of the archive and then copied into the destination. A selected folder
 * keeps the structure it had inside the archive; a selected file lands directly in the destination.
 */
class SaveZipEntriesToFolderUseCase @Inject constructor(
    private val zipArchiveRepository: ZipArchiveRepository,
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * @param entryPaths the selected entries; a folder stands for everything below it
     * @param destination where to copy to: a folder path, a file uri or a document tree uri
     */
    operator fun invoke(
        archivePath: String,
        entryPaths: List<String>,
        destination: UriPath,
        password: CharArray? = null,
    ): Flow<ZipBatchExtractionState> = flow {
        val index = runCatching { zipArchiveRepository.getArchiveIndex(archivePath) }
            .getOrElse { throwable ->
                emit(ZipBatchExtractionState.Failed(throwable.asArchiveException()))
                return@flow
            }

        val files = entryPaths.flatMap { path -> index.filesUnder(path) }.distinctBy { it.path }
        if (files.isEmpty()) {
            emit(ZipBatchExtractionState.Finished(emptyList(), emptyMap()))
            return@flow
        }

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

        val staging = zipArchiveRepository.newStagingFolderPath()
        val failures = mutableMapOf<String, ZipArchiveException>()
        try {
            files.forEachIndexed { position, entry ->
                emit(ZipBatchExtractionState.InProgress(position, files.size, entry.path))
                // One entry failing must not lose the ones that worked, so the batch carries on and
                // reports what did not come out at the end.
                runCatching { extract(archivePath, entry.path, staging, password) }
                    .onFailure { throwable ->
                        failures[entry.path] = throwable.asArchiveException(entry.path)
                    }
            }

            val saved = entryPaths
                .map { File(staging, it) }
                .filter { it.exists() }
                .mapNotNull { source -> copyToDestination(source, destination) }

            emit(ZipBatchExtractionState.Finished(saved, failures))
        } catch (throwable: Throwable) {
            emit(ZipBatchExtractionState.Failed(throwable.asArchiveException()))
        } finally {
            fileSystemRepository.deleteFolderAndItsFiles(staging)
        }
    }

    private suspend fun extract(
        archivePath: String,
        entryPath: String,
        destinationFolderPath: String,
        password: CharArray?,
    ) {
        val terminal = zipArchiveRepository
            .extractEntryToFolder(archivePath, entryPath, destinationFolderPath, password)
            .last()
        if (terminal is ZipExtractionState.Failed) throw terminal.cause
    }

    /**
     * @return the name that was saved, or null when nothing could be written
     */
    private suspend fun copyToDestination(source: File, destination: UriPath): String? {
        val copied = when {
            fileSystemRepository.isContentUri(destination.value) ->
                fileSystemRepository.copyFilesToDocumentUri(source, destination)

            fileSystemRepository.isFileUri(destination.value) ->
                fileSystemRepository.copyFiles(
                    source,
                    fileSystemRepository.getFileFromFileUri(destination.value),
                )

            else -> fileSystemRepository.getFileByPath(destination.value)
                ?.let { folder -> fileSystemRepository.copyFiles(source, folder) }
                ?: throw IllegalArgumentException("Cannot save to $destination")
        }
        return source.name.takeIf { copied > 0 }
    }

    private fun Throwable.asArchiveException(entryPath: String? = null): ZipArchiveException =
        this as? ZipArchiveException
            ?: entryPath?.let { ZipArchiveException.EntryExtractionFailed(it, this) }
            ?: ZipArchiveException.Corrupted(this)

    private fun ZipArchiveIndex.filesUnder(path: String): List<ZipArchiveEntry> {
        val entry = entries[path] ?: return emptyList()
        if (!entry.isDirectory) return listOf(entry)
        return childrenOf(path).flatMap { filesUnder(it.path) }
    }
}
