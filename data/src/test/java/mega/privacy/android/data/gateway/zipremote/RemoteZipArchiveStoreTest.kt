package mega.privacy.android.data.gateway.zipremote

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.zipbrowser.RemoteZipArchiveSource
import mega.privacy.android.domain.entity.zipbrowser.ZipArchiveException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.SocketTimeoutException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RemoteZipArchiveStoreTest {

    private val requests = mutableListOf<LongRange>()
    private var failuresLeft = 0
    private var failure: IOException = IOException("dropped")
    private var attempts = 0
    private var restarts = 0
    private var urlFailure: Throwable? = null
    private var gateAtStart: CompletableDeferred<Unit>? = null
    private var writableBytes: Long? = null
    private lateinit var cacheGateway: CacheGateway

    @BeforeEach
    fun resetReader() {
        requests.clear()
        failuresLeft = 0
        failure = IOException("dropped")
        attempts = 0
        restarts = 0
        urlFailure = null
        gateAtStart = null
        writableBytes = null
    }

    @Test
    fun `test that prepare creates a file as long as the remote archive`(@TempDir dir: File) =
        runTest {
            val (underTest, source) = store(dir, archive())

            val path = underTest.prepare(source)

            assertThat(File(path).length()).isEqualTo(source.size)
            assertThat(File(path).name).isEqualTo("archive.zip")
            assertThat(underTest.isRemote(path)).isTrue()
        }

    @Test
    fun `test that prepare gives a replaced version its own file`(@TempDir dir: File) = runTest {
        val (underTest, source) = store(dir, archive())

        val first = underTest.prepare(source)
        val second = underTest.prepare(source.copy(modificationTime = source.modificationTime + 1))

        assertThat(second).isNotEqualTo(first)
    }

    @Test
    fun `test that ensureDirectory reads only the end of the archive`(@TempDir dir: File) =
        runTest {
            val bytes = archive(fileCount = 200, fileBytes = 20_000)
            val (underTest, source) = store(dir, bytes)
            val path = underTest.prepare(source)

            underTest.ensureDirectory(path)

            assertThat(requests.sumOf { it.last - it.first + 1 }).isLessThan(bytes.size / 4L)
            assertThat(underTest.centralDirectoryOffset(path)).isGreaterThan(0L)
            assertThat(File(path).readBytes().takeLast(22)).isEqualTo(bytes.takeLast(22))
        }

    @Test
    fun `test that prepare throws corrupted when the directory lies outside the archive`(
        @TempDir dir: File,
    ) = runTest {
        val bytes = archive()
        val offsetIndex = bytes.size - 22 + 16
        val outside = bytes.size + 1_000
        repeat(4) { bytes[offsetIndex + it] = (outside shr (8 * it)).toByte() }
        val (underTest, source) = store(dir, bytes)

        assertThrows<ZipArchiveException.Corrupted> { underTest.prepare(source) }
    }

    @Test
    fun `test that ensure does not read a range twice`(@TempDir dir: File) = runTest {
        val (underTest, source) = store(dir, archive())
        val path = underTest.prepare(source)

        underTest.ensure(path, 0, 1_000)
        val afterFirst = requests.size
        underTest.ensure(path, 100, 900)

        assertThat(requests.size).isEqualTo(afterFirst)
    }

    @Test
    fun `test that ensure keeps the modification time of the remote version`(@TempDir dir: File) =
        runTest {
            val (underTest, source) = store(dir, archive())
            val path = underTest.prepare(source)

            underTest.ensure(path, 0, 1_000)

            assertThat(File(path).lastModified()).isEqualTo(source.modificationTime)
        }

    @Test
    fun `test that ensure starts the server and retries once when a read fails`(
        @TempDir dir: File,
    ) = runTest {
        val (underTest, source) = store(dir, archive(fileCount = 200, fileBytes = 20_000))
        val path = underTest.prepare(source)
        failuresLeft = 1

        underTest.ensure(path, 0, 1_000)

        assertThat(restarts).isEqualTo(1)
        assertThat(requests).isNotEmpty()
    }

    @Test
    fun `test that ensure progress never exceeds one when a read is retried`(@TempDir dir: File) =
        runTest {
            val (underTest, source) = store(dir, archive(fileCount = 200, fileBytes = 20_000))
            val path = underTest.prepare(source)
            failuresLeft = 1
            val progress = mutableListOf<Float>()

            underTest.ensure(path, 0, 30_000) { progress += it }

            assertThat(progress).isNotEmpty()
            assertThat(progress.max()).isAtMost(1f)
            assertThat(progress.last()).isEqualTo(1f)
        }

    @Test
    fun `test that ensureWithProgress emits a final one for a range already local`(
        @TempDir dir: File,
    ) = runTest {
        val (underTest, source) = store(dir, archive())
        val path = underTest.prepare(source)
        underTest.ensure(path, 0, 1_000)

        val progress = underTest.ensureWithProgress(path, 0, 1_000).toList()

        assertThat(progress).containsExactly(1f)
    }

    @Test
    fun `test that ensure reports a remote read failure and keeps the range missing`(
        @TempDir dir: File,
    ) = runTest {
        val (underTest, source) = store(dir, archive(fileCount = 200, fileBytes = 20_000))
        val path = underTest.prepare(source)
        failuresLeft = 2

        assertThrows<ZipArchiveException.RemoteReadFailed> { underTest.ensure(path, 0, 1_000) }

        requests.clear()
        underTest.ensure(path, 0, 1_000)
        assertThat(requests).isNotEmpty()
    }

    @Test
    fun `test that prepare reads the directory of the archive`(@TempDir dir: File) = runTest {
        val (underTest, source) = store(dir, archive(fileCount = 200, fileBytes = 20_000))

        val path = underTest.prepare(source)

        assertThat(requests).isNotEmpty()
        assertThat(underTest.centralDirectoryOffset(path)).isGreaterThan(0L)
    }

    @Test
    fun `test that prepare leaves nothing behind when the directory cannot be read`(
        @TempDir dir: File,
    ) = runTest {
        val (underTest, source) = store(dir, archive())
        failuresLeft = 2

        assertThrows<ZipArchiveException.RemoteReadFailed> { underTest.prepare(source) }

        val root = File(dir, "zip_browser_remote")
        assertThat(root.listFiles().orEmpty()).isEmpty()
    }

    @Test
    fun `test that prepare keeps at most two archives`(@TempDir dir: File) = runTest {
        val (underTest, source) = store(dir, archive())

        val first = underTest.prepare(source)
        underTest.prepare(source.copy(nodeId = NodeId(2)))
        underTest.prepare(source.copy(nodeId = NodeId(3)))

        assertThat(File(first).exists()).isFalse()
        assertThat(underTest.isRemote(first)).isFalse()
    }

    @Test
    fun `test that ensureWithProgress ends at the whole range`(@TempDir dir: File) = runTest {
        val (underTest, source) = store(dir, archive())
        val path = underTest.prepare(source)

        val progress = underTest.ensureWithProgress(path, 0, 30_000).toList()

        assertThat(progress.last()).isEqualTo(1f)
    }

    @Test
    fun `test that clear removes every remote archive`(@TempDir dir: File) = runTest {
        val (underTest, source) = store(dir, archive())
        val path = underTest.prepare(source)

        underTest.clear()

        assertThat(File(path).exists()).isFalse()
        assertThat(underTest.isRemote(path)).isFalse()
    }

    @Test
    fun `test that prepare keeps the stand-in inside its folder when the node name is a path`(
        @TempDir dir: File,
    ) = runTest {
        val (underTest, source) = store(dir, archive())
        val root = File(dir, "zip_browser_remote")

        val path = underTest.prepare(source.copy(name = "../../escape.zip"))

        assertThat(File(path).canonicalPath).startsWith(root.canonicalPath + File.separator)
        assertThat(File(path).name).isEqualTo("archive.zip")
    }

    @Test
    fun `test that prepare recreates a stand-in whose file was deleted`(@TempDir dir: File) =
        runTest {
            val (underTest, source) = store(dir, archive(fileCount = 200, fileBytes = 20_000))
            val path = underTest.prepare(source)
            File(path).delete()
            requests.clear()

            val again = underTest.prepare(source)

            assertThat(File(again).length()).isEqualTo(source.size)
            assertThat(requests).isNotEmpty()
            assertThat(underTest.centralDirectoryOffset(again)).isGreaterThan(0L)
        }

    @Test
    fun `test that ensure does not wait for another read when its range is already local`(
        @TempDir dir: File,
    ) = runTest {
        val (underTest, source) = store(dir, archive(fileCount = 200, fileBytes = 20_000))
        val path = underTest.prepare(source)
        val gate = CompletableDeferred<Unit>()
        gateAtStart = gate
        val other = launch { underTest.ensure(path, 0, 1_000) }
        advanceUntilIdle()

        withTimeout(1_000) { underTest.ensure(path, source.size - 22, source.size) }

        assertThat(other.isActive).isTrue()
        gate.complete(Unit)
        other.join()
    }

    @Test
    fun `test that ensureDirectory reports a remote read failure when the stand-in was deleted`(
        @TempDir dir: File,
    ) = runTest {
        val (underTest, source) = store(dir, archive())
        val path = underTest.prepare(source)
        File(path).delete()

        assertThrows<ZipArchiveException.RemoteReadFailed> { underTest.ensureDirectory(path) }
    }

    @Test
    fun `test that ensure reports a remote read failure when the link cannot be resolved by a runtime error`(
        @TempDir dir: File,
    ) = runTest {
        val (underTest, source) = store(dir, archive(fileCount = 200, fileBytes = 20_000))
        val path = underTest.prepare(source)
        urlFailure = IllegalStateException("no api")

        assertThrows<ZipArchiveException.RemoteReadFailed> { underTest.ensure(path, 0, 1_000) }
    }

    @Test
    fun `test that ensure does not retry a read that timed out`(@TempDir dir: File) = runTest {
        val (underTest, source) = store(dir, archive(fileCount = 200, fileBytes = 20_000))
        val path = underTest.prepare(source)
        attempts = 0
        failuresLeft = 1
        failure = SocketTimeoutException("stalled")

        assertThrows<ZipArchiveException.RemoteReadFailed> { underTest.ensure(path, 0, 1_000) }

        assertThat(attempts).isEqualTo(1)
        assertThat(restarts).isEqualTo(0)
    }

    @Test
    fun `test that identityOf returns the remote version of a registered archive`(
        @TempDir dir: File,
    ) = runTest {
        val (underTest, source) = store(dir, archive())
        val path = underTest.prepare(source)

        assertThat(underTest.identityOf(path)).isEqualTo(source.size to source.modificationTime)
        assertThat(underTest.identityOf(File(dir, "other.zip").absolutePath)).isNull()
    }

    @Test
    fun `test that isUnderRemoteRoot tells a stand-in path from any other`(@TempDir dir: File) =
        runTest {
            val (underTest, _) = store(dir, archive())

            assertThat(underTest.isUnderRemoteRoot(File(dir, "zip_browser_remote/x/archive.zip").path))
                .isTrue()
            assertThat(underTest.isUnderRemoteRoot(File(dir, "elsewhere/archive.zip").path)).isFalse()
        }

    @Test
    fun `test that clear does not create the remote folder`(@TempDir dir: File) = runTest {
        val (underTest, _) = store(dir, archive())
        File(dir, "zip_browser_remote").delete()

        underTest.clear()

        verify(cacheGateway, never()).getOrCreateCacheFolder(any())
        assertThat(File(dir, "zip_browser_remote").exists()).isFalse()
    }

    @Test
    fun `test that prepare does not evict an archive that is being read`(@TempDir dir: File) =
        runTest {
            val (underTest, source) = store(dir, archive(fileCount = 200, fileBytes = 20_000))
            val busy = underTest.prepare(source)
            val gate = CompletableDeferred<Unit>()
            gateAtStart = gate
            val read = launch { underTest.ensure(busy, 0, 1_000) }
            advanceUntilIdle()

            val idle = underTest.prepare(source.copy(nodeId = NodeId(2)))
            underTest.prepare(source.copy(nodeId = NodeId(3)))

            assertThat(File(busy).exists()).isTrue()
            assertThat(underTest.isRemote(busy)).isTrue()
            assertThat(underTest.isRemote(idle)).isFalse()
            gate.complete(Unit)
            read.join()
            assertThat(read.isCancelled).isFalse()
        }

    @Test
    fun `test that ensure throws remote read failed when the archive is not registered`(
        @TempDir dir: File,
    ) = runTest {
        val (underTest, source) = store(dir, archive())
        val path = underTest.prepare(source)
        underTest.clear()

        assertThrows<ZipArchiveException.RemoteReadFailed> { underTest.ensure(path, 0, 1_000) }
    }

    @Test
    fun `test that ensureDirectory throws remote read failed when the archive is not registered`(
        @TempDir dir: File,
    ) = runTest {
        val (underTest, _) = store(dir, archive())

        assertThrows<ZipArchiveException.RemoteReadFailed> {
            underTest.ensureDirectory(File(dir, "zip_browser_remote/x/archive.zip").path)
        }
    }

    @Test
    fun `test that prepare sets the modification time of the remote version`(@TempDir dir: File) =
        runTest {
            val (underTest, source) = store(dir, archive())

            val path = underTest.prepare(source)

            assertThat(File(path).lastModified()).isEqualTo(source.modificationTime)
        }

    @Test
    fun `test that ensure throws insufficient storage when the system cannot make room`(
        @TempDir dir: File,
    ) = runTest {
        val (underTest, source) = store(dir, archive(fileCount = 200, fileBytes = 20_000))
        val path = underTest.prepare(source)
        writableBytes = 10L
        requests.clear()

        val actual = assertThrows<ZipArchiveException.InsufficientStorage> {
            underTest.ensure(path, 0, 1_000)
        }

        assertThat(actual.availableBytes).isEqualTo(10L)
        assertThat(requests).isEmpty()
    }

    @Test
    fun `test that ensure throws insufficient storage without a retry when the disk is full`(
        @TempDir dir: File,
    ) = runTest {
        val (underTest, source) = store(dir, archive(fileCount = 200, fileBytes = 20_000))
        val path = underTest.prepare(source)
        failure = DiskFullException(IOException("ENOSPC"))
        failuresLeft = 1
        attempts = 0

        assertThrows<ZipArchiveException.InsufficientStorage> { underTest.ensure(path, 0, 1_000) }

        assertThat(restarts).isEqualTo(0)
        assertThat(attempts).isEqualTo(1)
    }

    private fun TestScope.store(
        dir: File,
        bytes: ByteArray,
    ): Pair<RemoteZipArchiveStore, RemoteZipArchiveSource> {
        val root = File(dir, "zip_browser_remote")
        cacheGateway = mock<CacheGateway>().stub {
            onBlocking { getOrCreateCacheFolder("zip_browser_remote") } doReturn root.apply { mkdirs() }
            onBlocking { getCacheFolder("zip_browser_remote") } doReturn root
        }
        val reader = object : RangeReaderGateway {
            override suspend fun copyRange(
                url: String,
                start: Long,
                length: Long,
                destination: File,
                onBytes: (Long) -> Unit,
            ) {
                attempts++
                if (start == 0L) gateAtStart?.await()
                if (failuresLeft-- > 0) throw failure
                requests += start until start + length
                RandomAccessFile(destination, "rw").use { file ->
                    file.seek(start)
                    file.write(bytes, start.toInt(), length.toInt())
                }
                onBytes(length)
            }
        }
        val urls = object : RemoteArchiveUrlGateway {
            override suspend fun urlFor(source: RemoteZipArchiveSource): String {
                urlFailure?.let { throw it }
                return "http://local/${source.nodeId.longValue}"
            }

            override suspend fun ensureServerRunning() {
                restarts++
            }
        }
        val source = RemoteZipArchiveSource(
            nodeId = NodeId(1),
            isFolderLinkNode = false,
            name = "Summer Trip.zip",
            size = bytes.size.toLong(),
            modificationTime = 1_700_000_000_000L,
        )
        val fileGateway = mock<FileGateway> {
            on { allocateBytes(any(), any()) } doAnswer {
                writableBytes ?: maxOf(it.getArgument<Long>(1), it.getArgument<File>(0).usableSpace)
            }
        }
        return RemoteZipArchiveStore(
            cacheGateway = cacheGateway,
            rangeReaderGateway = reader,
            remoteArchiveUrlGateway = urls,
            fileGateway = fileGateway,
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        ) to source
    }

    private fun archive(fileCount: Int = 20, fileBytes: Int = 2_000): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.setLevel(0)
            repeat(fileCount) { i ->
                zip.putNextEntry(ZipEntry("docs/file_$i.bin"))
                zip.write(ByteArray(fileBytes) { (it + i).toByte() })
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }
}
