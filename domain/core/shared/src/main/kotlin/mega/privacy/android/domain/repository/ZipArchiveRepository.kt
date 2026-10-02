package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.zipbrowser.RemoteZipArchiveSource
import mega.privacy.android.domain.entity.zipbrowser.ZipArchiveIndex
import mega.privacy.android.domain.entity.zipbrowser.ZipExtractionState

/**
 * Reads archives for browsing, and extracts entries out of them one at a time.
 */
interface ZipArchiveRepository {

    /**
     * The folder structure of an archive, read from its central directory.
     *
     * The result is held for as long as the archive on disk is unchanged, so walking in and out of
     * folders does not re-read the directory.
     *
     * @throws mega.privacy.android.domain.entity.zipbrowser.ZipArchiveException.Corrupted when the
     * archive cannot be read
     */
    suspend fun getArchiveIndex(archivePath: String): ZipArchiveIndex

    /**
     * Extracts one entry into the bounded extraction cache, leaving every other entry alone.
     *
     * An entry already in the cache is returned without being extracted again. The flow emits
     * progress and terminates with [ZipExtractionState.Completed] or [ZipExtractionState.Failed];
     * it never throws. Cancelling the collector stops the write and removes the partial file.
     */
    fun extractEntry(
        archivePath: String,
        entryPath: String,
        password: CharArray? = null,
    ): Flow<ZipExtractionState>

    /**
     * Extracts one entry into [destinationFolderPath], at its own path inside the archive, so a
     * selection keeps the folder structure it had in the archive.
     *
     * Unlike [extractEntry] this touches neither the cache nor its eviction, so saving more than
     * the cache holds cannot delete what it has already written.
     */
    fun extractEntryToFolder(
        archivePath: String,
        entryPath: String,
        destinationFolderPath: String,
        password: CharArray? = null,
    ): Flow<ZipExtractionState>

    /**
     * A folder to extract into on the way to somewhere else, outside the bounded cache.
     */
    suspend fun newStagingFolderPath(): String

    /**
     * The path of an entry that is already in the extraction cache, or null when it is not.
     */
    suspend fun getCachedEntryPath(archivePath: String, entryPath: String): String?

    /**
     * Whether [password] decrypts the archive.
     */
    suspend fun isPasswordValid(archivePath: String, password: CharArray): Boolean

    /**
     * Removes every extracted entry from the cache.
     */
    suspend fun clearExtractionCache()

    /**
     * Removes the entries that were decrypted with a password, so their plaintext does not outlive
     * the process that was given the password.
     */
    suspend fun clearProtectedExtractions()

    /**
     * How many bytes are still free where extracted entries are written, so a batch can be refused
     * before anything is written rather than failing part way through.
     */
    suspend fun getExtractionFreeBytes(): Long

    /**
     * A local path standing for a cloud archive, read from the cloud only where it is needed.
     * The same source always gets the same path, so reopening it reuses what was already read.
     *
     * It reads the archive's directory, so a cloud archive that cannot be read is reported here
     * rather than after navigating to it.
     */
    suspend fun prepareRemoteArchive(source: RemoteZipArchiveSource): String

    /**
     * Removes the local stand-ins of cloud archives, which are only worth keeping while the app runs.
     */
    suspend fun clearRemoteArchives()
}
