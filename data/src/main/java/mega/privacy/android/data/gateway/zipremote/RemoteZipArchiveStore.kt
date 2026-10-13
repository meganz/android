package mega.privacy.android.data.gateway.zipremote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.domain.entity.zipbrowser.RemoteZipArchiveSource
import mega.privacy.android.domain.entity.zipbrowser.ZipArchiveException
import mega.privacy.android.domain.qualifier.IoDispatcher
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.SocketTimeoutException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps a sparse local copy of each cloud archive. The local copy has the same length as the cloud
 * version, but contains only the byte ranges that were read. This lets the archive code read a
 * cloud archive through a file path.
 *
 * After each write, the store sets the modification time of the local copy back to the time of the
 * cloud version. During a write, this time changes. The index cache, the unlocked passwords and the
 * extraction cache must therefore get the identity of a local copy from [identityOf], not from the
 * file.
 */
@Singleton
internal class RemoteZipArchiveStore @Inject constructor(
    private val cacheGateway: CacheGateway,
    private val rangeReaderGateway: RangeReaderGateway,
    private val remoteArchiveUrlGateway: RemoteArchiveUrlGateway,
    private val fileGateway: FileGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private class RemoteArchive(
        val source: RemoteZipArchiveSource,
        val file: File,
        val filled: ByteRangeSet = ByteRangeSet(),
        val lock: Mutex = Mutex(),
        @Volatile var lastUsed: Long = System.nanoTime(),
        @Volatile var directoryOffset: Long? = null,
    )

    private val registryLock = Mutex()
    private val archives = ConcurrentHashMap<String, RemoteArchive>()

    /**
     * Registers [source], downloads the central directory of the archive and returns the path of
     * the local copy. If this cloud version is already registered, the local copy keeps the data
     * that was already downloaded.
     *
     * The central directory is read here so that an archive that cannot be read fails before the
     * browser shows anything. If this fails, the store deletes the local copy.
     *
     * @throws ZipArchiveException.RemoteReadFailed when the archive cannot be read
     * @throws ZipArchiveException.InsufficientStorage when there is not sufficient space for the
     * central directory
     */
    suspend fun prepare(source: RemoteZipArchiveSource): String = onIo {
        val (path, created) = register(source)
        if (!created) return@onIo path
        try {
            ensureDirectory(path)
        } catch (e: Throwable) {
            archives.remove(path)
            File(path).parentFile?.deleteRecursively()
            throw e
        }
        path
    }

    private suspend fun register(source: RemoteZipArchiveSource): Pair<String, Boolean> =
        registryLock.withLock {
            val root = cacheGateway.getOrCreateCacheFolder(ROOT_FOLDER)
                ?: throw ZipArchiveException.RemoteReadFailed()
            // The person who uploaded or shared the file chooses the node name. The store therefore
            // never uses the name in a path. The browser gets the title from the node.
            val file = File(File(root, source.digest()), STAND_IN_NAME)
            if (!file.canonicalPath.startsWith(root.canonicalPath + File.separator)) {
                throw ZipArchiveException.RemoteReadFailed()
            }
            archives[file.absolutePath]?.let {
                if (file.isFile && file.length() == source.size) {
                    it.lastUsed = System.nanoTime()
                    return@withLock file.absolutePath to false
                }
                // Something outside the store deleted the local copy or made it shorter. The record
                // of the downloaded byte ranges is not correct now, so the store starts again.
                archives.remove(file.absolutePath)
            }
            file.parentFile?.mkdirs()
            RandomAccessFile(file, "rw").use { it.setLength(source.size) }
            archives[file.absolutePath] = RemoteArchive(source, file).apply {
                restoreModificationTime()
            }
            evictBeyondLimit(keep = file.absolutePath)
            file.absolutePath to true
        }

    fun isRemote(archivePath: String): Boolean = archives.containsKey(archivePath)

    /**
     * Returns the length and modification time of the cloud version of a local copy. The
     * modification time of the local copy changes during a write. Code that identifies archives by
     * modification time must therefore call this function first.
     *
     * @return the length and modification time, or null when [archivePath] is not a local copy
     */
    fun identityOf(archivePath: String): Pair<Long, Long>? =
        archives[archivePath]?.source?.let { it.size to it.modificationTime }

    /**
     * Returns true when [archivePath] is in the folder that contains the local copies, also when
     * the path is not registered. This function does not create the folder.
     */
    suspend fun isUnderRemoteRoot(archivePath: String): Boolean = withContext(ioDispatcher) {
        val root = cacheGateway.getCacheFolder(ROOT_FOLDER)
        File(archivePath).canonicalPath.startsWith(root.canonicalPath + File.separator)
    }

    /**
     * Downloads the end of the archive and its central directory. To list the entries, the archive
     * code needs only these parts.
     *
     * @throws ZipArchiveException.RemoteReadFailed when [archivePath] is no longer registered
     */
    suspend fun ensureDirectory(archivePath: String): Unit = onIo {
        val archive = registered(archivePath)
        val size = archive.source.size
        val tailStart = maxOf(0L, size - ZipCentralDirectoryLocator.TAIL_BYTES)
        ensure(archivePath, tailStart, size)
        val tail = readLocal(archive.file, tailStart, (size - tailStart).toInt())
        val location = ZipCentralDirectoryLocator.locate(tail)
            ?: ZipCentralDirectoryLocator.zip64RecordOffset(tail)?.let { recordAt ->
                if (recordAt < 0 || recordAt + ZipCentralDirectoryLocator.ZIP64_RECORD_BYTES > size) {
                    throw ZipArchiveException.Corrupted()
                }
                val recordEnd = recordAt + ZipCentralDirectoryLocator.ZIP64_RECORD_BYTES
                ensure(archivePath, recordAt, recordEnd)
                ZipCentralDirectoryLocator.locateZip64(
                    readLocal(archive.file, recordAt, ZipCentralDirectoryLocator.ZIP64_RECORD_BYTES)
                )
            }
            ?: throw ZipArchiveException.Corrupted()
        if (location.offset < 0 || location.size < 0 || location.offset + location.size > size) {
            throw ZipArchiveException.Corrupted()
        }
        ensure(archivePath, location.offset, location.offset + location.size)
        archive.directoryOffset = location.offset
    }

    /**
     * @return the offset where the central directory starts, or null before [ensureDirectory] runs
     */
    fun centralDirectoryOffset(archivePath: String): Long? = archives[archivePath]?.directoryOffset

    /**
     * Downloads the bytes from [start] to [endExclusive] of the archive, if they are not local yet.
     *
     * The store calls [onProgress] on the thread that reads the data. A caller that updates UI
     * state must send the value to the correct thread.
     *
     * @throws ZipArchiveException.RemoteReadFailed when the byte range cannot be read, or when
     * [archivePath] is no longer registered
     * @throws ZipArchiveException.InsufficientStorage when there is not sufficient space for the
     * byte range
     */
    suspend fun ensure(
        archivePath: String,
        start: Long,
        endExclusive: Long,
        onProgress: (Float) -> Unit = {},
    ): Unit = onIo {
        val archive = registered(archivePath)
        archive.lastUsed = System.nanoTime()
        val end = minOf(endExclusive, archive.source.size)
        // Another read often already downloaded the bytes that the visible page needs. This check
        // does not wait for the lock, so the page does not wait for the network read of the other
        // request.
        if (archive.missing(start, end).isEmpty()) return@onIo
        archive.lock.withLock {
            // The archive can be evicted between the lookup and the lock. Its file is then gone.
            if (archives[archivePath] !== archive) throw ZipArchiveException.RemoteReadFailed()
            val gaps = chunked(archive.missing(start, end), archive.source.size)
            val total = gaps.sumOf { it.last - it.first + 1 }
            if (total == 0L) return@onIo
            val available =
                fileGateway.allocateBytes(archive.file.parentFile ?: archive.file, total)
            if (available < total) {
                throw ZipArchiveException.InsufficientStorage(
                    requiredBytes = total,
                    availableBytes = available,
                )
            }
            var done = 0L
            try {
                gaps.forEach { gap ->
                    val length = gap.last - gap.first + 1
                    var gapDone = 0L
                    copyWithRetry(
                        archive = archive,
                        start = gap.first,
                        length = length,
                        onRetry = {
                            done -= gapDone
                            gapDone = 0L
                        },
                    ) { bytes ->
                        gapDone += bytes
                        done += bytes
                        onProgress((done.toFloat() / total).coerceAtMost(1f))
                    }
                    synchronized(archive.filled) { archive.filled.add(gap.first, gap.last + 1) }
                    archive.restoreModificationTime()
                }
            } finally {
                archive.restoreModificationTime()
            }
        }
    }

    /**
     * Does the same as [ensure], and emits the fraction of the byte range that is downloaded. The
     * flow completes when the byte range is local. It fails with the same exceptions as [ensure].
     */
    fun ensureWithProgress(archivePath: String, start: Long, endExclusive: Long): Flow<Float> =
        channelFlow {
            ensure(archivePath, start, endExclusive) { fraction -> trySend(fraction) }
            send(1f)
        }.conflate()

    suspend fun clear() = registryLock.withLock {
        archives.clear()
        withContext(ioDispatcher) {
            cacheGateway.getCacheFolder(ROOT_FOLDER).takeIf { it.exists() }?.deleteRecursively()
        }
        Unit
    }

    private suspend fun copyWithRetry(
        archive: RemoteArchive,
        start: Long,
        length: Long,
        onRetry: () -> Unit,
        onBytes: (Long) -> Unit,
    ) {
        try {
            copyOnce(archive, start, length, onBytes)
        } catch (timeout: SocketTimeoutException) {
            // A stopped server refuses the connection immediately. A timeout means that the server
            // is slow, and a restart does not fix that. A second timeout only makes the user wait
            // longer.
            throw ZipArchiveException.RemoteReadFailed(timeout)
        } catch (full: DiskFullException) {
            throw insufficientStorage(archive, length)
        } catch (first: IOException) {
            currentCoroutineContext().ensureActive()
            // Another screen can stop the shared streaming server while the user browses the
            // archive. The store starts the server again and tries one more time, so it does not
            // loop.
            onRetry()
            runCatching { remoteArchiveUrlGateway.ensureServerRunning() }
            currentCoroutineContext().ensureActive()
            try {
                copyOnce(archive, start, length, onBytes)
            } catch (full: DiskFullException) {
                throw insufficientStorage(archive, length)
            } catch (second: IOException) {
                throw ZipArchiveException.RemoteReadFailed(second)
            }
        }
    }

    private fun insufficientStorage(archive: RemoteArchive, requiredBytes: Long) =
        ZipArchiveException.InsufficientStorage(
            requiredBytes = requiredBytes,
            availableBytes = fileGateway.allocateBytes(
                archive.file.parentFile ?: archive.file,
                requiredBytes,
            ),
        )

    private suspend fun copyOnce(
        archive: RemoteArchive,
        start: Long,
        length: Long,
        onBytes: (Long) -> Unit,
    ) {
        val url = remoteArchiveUrlGateway.urlFor(archive.source)
            ?: throw IOException("No streaming link for the archive")
        rangeReaderGateway.copyRange(url, start, length, archive.file, onBytes)
    }

    /**
     * Extends each missing part to complete 64 KB chunks and joins missing parts that are near each
     * other. Many small entries next to each other then need a few requests, not one request each.
     */
    private fun chunked(gaps: List<LongRange>, size: Long): List<LongRange> {
        val rounded = gaps.map { gap ->
            val from = gap.first / CHUNK_BYTES * CHUNK_BYTES
            val to = minOf(size - 1, (gap.last / CHUNK_BYTES + 1) * CHUNK_BYTES - 1)
            from..to
        }
        return rounded.fold(mutableListOf()) { merged, gap ->
            val last = merged.lastOrNull()
            if (last != null && gap.first <= last.last + CHUNK_BYTES) {
                merged[merged.lastIndex] = last.first..maxOf(last.last, gap.last)
            } else {
                merged += gap
            }
            merged
        }
    }

    /**
     * Skips an archive that is being read, so the read does not lose its file. The next
     * registration evicts it when it is idle.
     */
    private fun evictBeyondLimit(keep: String) {
        val oldestFirst = archives.entries
            .filter { it.key != keep }
            .sortedBy { it.value.lastUsed }
        for ((path, archive) in oldestFirst) {
            if (archives.size <= MAX_ARCHIVES) return
            if (!archive.lock.tryLock()) continue
            try {
                archives.remove(path)
                archive.file.parentFile?.deleteRecursively()
            } finally {
                archive.lock.unlock()
            }
        }
    }

    private fun registered(archivePath: String): RemoteArchive =
        archives[archivePath] ?: throw ZipArchiveException.RemoteReadFailed()

    private fun RemoteArchive.missing(start: Long, endExclusive: Long): List<LongRange> =
        synchronized(filled) { filled.missing(start, endExclusive) }

    private fun RemoteArchive.restoreModificationTime() {
        if (!file.setLastModified(source.modificationTime)) {
            Timber.w("Could not restore the modification time of a remote archive stand-in")
        }
    }

    /**
     * Callers handle only [ZipArchiveException]. Other failures, for example a local copy that was
     * deleted during a read or an unexpected gateway error, would otherwise crash the app.
     */
    private suspend fun <T> onIo(block: suspend () -> T): T = withContext(ioDispatcher) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: ZipArchiveException) {
            throw e
        } catch (e: Throwable) {
            throw ZipArchiveException.RemoteReadFailed(e)
        }
    }

    private fun readLocal(file: File, start: Long, length: Int): ByteArray =
        ByteArray(length).also { bytes ->
            RandomAccessFile(file, "r").use {
                it.seek(start)
                it.readFully(bytes)
            }
        }

    private fun RemoteZipArchiveSource.digest(): String =
        MessageDigest.getInstance("SHA-256")
            .digest(
                "${nodeId.longValue}\u0000$isFolderLinkNode\u0000$size\u0000$modificationTime"
                    .toByteArray()
            )
            .joinToString("") { "%02x".format(it) }
            .take(16)

    private companion object {
        const val ROOT_FOLDER = "zip_browser_remote"
        const val STAND_IN_NAME = "archive.zip"
        const val CHUNK_BYTES = 64L * 1024
        const val MAX_ARCHIVES = 2
    }
}
