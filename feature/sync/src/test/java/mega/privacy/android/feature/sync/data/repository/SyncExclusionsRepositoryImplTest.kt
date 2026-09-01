package mega.privacy.android.feature.sync.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.sync.data.mapper.exclusion.MegaIgnoreFileMapper
import mega.privacy.android.feature.sync.data.mapper.exclusion.MegaIgnoreLineMapper
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreEntry
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.domain.exception.ExclusionsError
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException
import java.util.concurrent.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncExclusionsRepositoryImplTest {

    private lateinit var underTest: SyncExclusionsRepositoryImpl

    private val fileGateway: FileGateway = mock()
    private val megaIgnoreFileMapper: MegaIgnoreFileMapper = mock()

    private val localRoot =
        UriPath("content://com.android.externalstorage.documents/tree/primary%3ASync")
    private val exclusionsFile =
        UriPath("content://com.android.externalstorage.documents/document/primary%3ASync%2F.megaignore")
    private val strippedFile =
        UriPath("content://com.android.externalstorage.documents/document/primary%3ASync%2Fmegaignore")

    @BeforeEach
    fun setUp() {
        underTest = repositoryWith(megaIgnoreFileMapper)
    }

    @AfterEach
    fun tearDown() {
        reset(fileGateway, megaIgnoreFileMapper)
    }

    // region read

    @Test
    fun `test that getExclusions returns an empty model when the sync has no exclusions yet`() =
        runTest {
            whenever(fileGateway.getChildByName(localRoot, MEGA_IGNORE)) doReturn null
            whenever(fileGateway.canReadUri(localRoot)) doReturn true

            val actual = underTest.getExclusions(localRoot)

            assertThat(actual.getOrNull()).isEqualTo(ParsedMegaIgnore(entries = emptyList()))
        }

    @Test
    fun `test that getExclusions returns NoPermission when the root cannot be read`() = runTest {
        whenever(fileGateway.getChildByName(localRoot, MEGA_IGNORE)) doReturn null
        whenever(fileGateway.canReadUri(localRoot)) doReturn false

        val actual = underTest.getExclusions(localRoot)

        assertThat(actual.exceptionOrNull())
            .isInstanceOf(ExclusionsError.NoPermission::class.java)
    }

    @Test
    fun `test that getExclusions never writes`() = runTest {
        whenever(fileGateway.getChildByName(localRoot, MEGA_IGNORE)) doReturn null
        whenever(fileGateway.canReadUri(localRoot)) doReturn true

        underTest.getExclusions(localRoot)

        verify(fileGateway, never()).createChildFileSync(any(), any(), any())
        verify(fileGateway, never()).saveTextOnContentUri(any(), any())
    }

    @Test
    fun `test that getExclusions returns the parsed file when it exists`() = runTest {
        val parsed =
            ParsedMegaIgnore(entries = listOf(MegaIgnoreEntry.PreservedEntry("# a comment")))
        stubExistingFile("# a comment")
        whenever(megaIgnoreFileMapper("# a comment")) doReturn parsed

        val actual = underTest.getExclusions(localRoot)

        assertThat(actual.getOrNull()).isEqualTo(parsed)
    }

    @Test
    fun `test that getExclusions returns an empty model when the file exists but is empty`() =
        runTest {
            val underTestWithRealMapper = repositoryWith(realFileMapper())
            stubExistingFile("")

            val actual = underTestWithRealMapper.getExclusions(localRoot)

            assertThat(actual.getOrNull()?.entries).isEmpty()
        }

    @Test
    fun `test that getExclusions keeps the byte order mark so the mapper can see it`() = runTest {
        stubExistingFile("﻿-:*.tmp")
        whenever(megaIgnoreFileMapper("﻿-:*.tmp")) doReturn
                ParsedMegaIgnore(emptyList(), hadBom = true)

        val actual = underTest.getExclusions(localRoot)

        assertThat(actual.getOrNull()?.hadBom).isTrue()
    }

    @Test
    fun `test that getExclusions returns ReadFailed when the bytes are not valid UTF-8`() =
        runTest {
            whenever(fileGateway.getChildByName(localRoot, MEGA_IGNORE)) doReturn exclusionsFile
            whenever(fileGateway.getInputStream(exclusionsFile)) doReturn
                    byteArrayOf(0x2D, 0x3A, 0xFF.toByte(), 0xFE.toByte()).inputStream()

            val actual = underTest.getExclusions(localRoot)

            assertThat(actual.exceptionOrNull())
                .isInstanceOf(ExclusionsError.ReadFailed::class.java)
        }

    @Test
    fun `test that getExclusions returns NoPermission when the provider throws SecurityException`() =
        runTest {
            whenever(fileGateway.getChildByName(localRoot, MEGA_IGNORE)) doThrow
                    SecurityException("Permission denial")

            val actual = underTest.getExclusions(localRoot)

            assertThat(actual.exceptionOrNull())
                .isInstanceOf(ExclusionsError.NoPermission::class.java)
        }

    @Test
    fun `test that getExclusions returns ReadFailed when the input stream cannot be opened`() =
        runTest {
            whenever(fileGateway.getChildByName(localRoot, MEGA_IGNORE)) doReturn exclusionsFile
            whenever(fileGateway.getInputStream(exclusionsFile)) doReturn null

            val actual = underTest.getExclusions(localRoot)

            assertThat(actual.exceptionOrNull())
                .isInstanceOf(ExclusionsError.ReadFailed::class.java)
        }

    @Test
    fun `test that getExclusions returns ReadFailed when reading throws`() = runTest {
        whenever(fileGateway.getChildByName(localRoot, MEGA_IGNORE)) doReturn exclusionsFile
        whenever(fileGateway.getInputStream(exclusionsFile)) doThrow IOException("Provider died")

        val actual = underTest.getExclusions(localRoot)

        assertThat(actual.exceptionOrNull()).isInstanceOf(ExclusionsError.ReadFailed::class.java)
    }

    @Test
    fun `test that getExclusions lets cancellation through rather than reporting it as a failure`() {
        whenever(fileGateway.getChildByName(localRoot, MEGA_IGNORE)) doThrow
                CancellationException("Scope closed")

        assertThrows(CancellationException::class.java) {
            runBlocking { underTest.getExclusions(localRoot) }
        }
    }

    // endregion

    // region write

    @Test
    fun `test that setExclusions writes the serialized model to the existing file`() = runTest {
        val parsed = ParsedMegaIgnore(entries = listOf(MegaIgnoreEntry.PreservedEntry("-:*.tmp")))
        whenever(fileGateway.getChildByName(localRoot, MEGA_IGNORE)) doReturn exclusionsFile
        whenever(megaIgnoreFileMapper(parsed)) doReturn "-:*.tmp"
        whenever(fileGateway.saveTextOnContentUri(exclusionsFile.value, "-:*.tmp")) doReturn true

        val actual = underTest.setExclusions(localRoot, parsed)

        assertThat(actual.isSuccess).isTrue()
        verify(fileGateway).saveTextOnContentUri(exclusionsFile.value, "-:*.tmp")
    }

    @Test
    fun `test that setExclusions does not recreate a file that already exists`() = runTest {
        stubSuccessfulWriteTo(exclusionsFile)

        underTest.setExclusions(localRoot, EMPTY_MODEL)

        verify(fileGateway, never()).createChildFileSync(any(), any(), any())
    }

    @Test
    fun `test that setExclusions creates the file when the sync does not have one yet`() =
        runTest {
            whenever(fileGateway.getChildByName(localRoot, MEGA_IGNORE)) doReturn null
            whenever(fileGateway.createChildFileSync(localRoot, MEGA_IGNORE, false)) doReturn
                    exclusionsFile
            whenever(fileGateway.getFileNameFromUri(exclusionsFile.value)) doReturn MEGA_IGNORE
            whenever(megaIgnoreFileMapper(EMPTY_MODEL)) doReturn ""
            whenever(fileGateway.saveTextOnContentUri(exclusionsFile.value, "")) doReturn true

            val actual = underTest.setExclusions(localRoot, EMPTY_MODEL)

            assertThat(actual.isSuccess).isTrue()
            verify(fileGateway).createChildFileSync(localRoot, MEGA_IGNORE, false)
        }

    @Test
    fun `test that setExclusions returns ProviderRejectedDotfile when the file cannot be created`() =
        runTest {
            whenever(fileGateway.getChildByName(localRoot, MEGA_IGNORE)) doReturn null
            whenever(fileGateway.createChildFileSync(localRoot, MEGA_IGNORE, false)) doReturn null
            whenever(fileGateway.canReadUri(localRoot)) doReturn true

            val actual = underTest.setExclusions(localRoot, EMPTY_MODEL)

            assertThat(actual.exceptionOrNull())
                .isInstanceOf(ExclusionsError.ProviderRejectedDotfile::class.java)
        }

    @Test
    fun `test that setExclusions returns NoPermission rather than blaming the dotfile when the root is gone`() =
        runTest {
            whenever(fileGateway.getChildByName(localRoot, MEGA_IGNORE)) doReturn null
            whenever(fileGateway.createChildFileSync(localRoot, MEGA_IGNORE, false)) doReturn null
            whenever(fileGateway.canReadUri(localRoot)) doReturn false

            val actual = underTest.setExclusions(localRoot, EMPTY_MODEL)

            assertThat(actual.exceptionOrNull())
                .isInstanceOf(ExclusionsError.NoPermission::class.java)
        }

    @Test
    fun `test that setExclusions returns ProviderRejectedDotfile when the provider strips the leading dot`() =
        runTest {
            stubDotStrippingProvider()

            val actual = underTest.setExclusions(localRoot, EMPTY_MODEL)

            assertThat(actual.exceptionOrNull())
                .isInstanceOf(ExclusionsError.ProviderRejectedDotfile::class.java)
        }

    @Test
    fun `test that setExclusions removes the mangled child when the provider strips the leading dot`() =
        runTest {
            stubDotStrippingProvider()

            underTest.setExclusions(localRoot, EMPTY_MODEL)

            verify(fileGateway).deleteIfItIsAFileSync(strippedFile)
        }

    @Test
    fun `test that setExclusions never writes when the dotfile was rejected`() = runTest {
        stubDotStrippingProvider()

        underTest.setExclusions(localRoot, EMPTY_MODEL)

        verify(fileGateway, never()).saveTextOnContentUri(any(), any())
    }

    @Test
    fun `test that setExclusions returns WriteFailed when the created file cannot be identified`() =
        runTest {
            whenever(fileGateway.getChildByName(localRoot, MEGA_IGNORE)) doReturn null
            whenever(fileGateway.createChildFileSync(localRoot, MEGA_IGNORE, false)) doReturn
                    exclusionsFile
            whenever(fileGateway.getFileNameFromUri(exclusionsFile.value)) doReturn null
            whenever(megaIgnoreFileMapper(EMPTY_MODEL)) doReturn ""
            whenever(fileGateway.saveTextOnContentUri(exclusionsFile.value, "")) doReturn true

            val actual = underTest.setExclusions(localRoot, EMPTY_MODEL)

            assertThat(actual.exceptionOrNull())
                .isInstanceOf(ExclusionsError.WriteFailed::class.java)
            verify(fileGateway, never()).deleteIfItIsAFileSync(any())
        }

    @Test
    fun `test that setExclusions returns WriteFailed when the write fails`() = runTest {
        whenever(fileGateway.getChildByName(localRoot, MEGA_IGNORE)) doReturn exclusionsFile
        whenever(megaIgnoreFileMapper(EMPTY_MODEL)) doReturn ""
        whenever(fileGateway.saveTextOnContentUri(exclusionsFile.value, "")) doReturn false

        val actual = underTest.setExclusions(localRoot, EMPTY_MODEL)

        assertThat(actual.exceptionOrNull()).isInstanceOf(ExclusionsError.WriteFailed::class.java)
    }

    @Test
    fun `test that setExclusions returns NoPermission when the provider throws SecurityException`() =
        runTest {
            whenever(fileGateway.getChildByName(localRoot, MEGA_IGNORE)) doThrow
                    SecurityException("Permission denial")

            val actual = underTest.setExclusions(localRoot, EMPTY_MODEL)

            assertThat(actual.exceptionOrNull())
                .isInstanceOf(ExclusionsError.NoPermission::class.java)
        }

    @Test
    fun `test that setExclusions returns WriteFailed when the write throws`() = runTest {
        whenever(fileGateway.getChildByName(localRoot, MEGA_IGNORE)) doReturn exclusionsFile
        whenever(megaIgnoreFileMapper(EMPTY_MODEL)) doReturn ""
        whenever(fileGateway.saveTextOnContentUri(eq(exclusionsFile.value), eq(""))) doThrow
                IOException("Provider died")

        val actual = underTest.setExclusions(localRoot, EMPTY_MODEL)

        assertThat(actual.exceptionOrNull()).isInstanceOf(ExclusionsError.WriteFailed::class.java)
    }

    // endregion

    // region root shape

    /**
     * The probe has to take the [UriPath] itself rather than its raw string. A sync root is
     * legitimately either a bare path or a content URI, and the string-typed
     * [String] overload of `FileGateway.canReadUri` does not synthesise the missing `file://`
     * scheme, so it answers false for every bare-path root and turns an absent file into a
     * spurious error.
     */
    @Test
    fun `test that getExclusions probes the root through the uri path typed call`() = runTest {
        whenever(fileGateway.getChildByName(RAW_PATH_ROOT, MEGA_IGNORE)) doReturn null
        whenever(fileGateway.canReadUri(RAW_PATH_ROOT)) doReturn true

        underTest.getExclusions(RAW_PATH_ROOT)

        verify(fileGateway).canReadUri(RAW_PATH_ROOT)
        verify(fileGateway, never()).canReadUri(any<String>())
    }

    @Test
    fun `test that setExclusions probes the root through the uri path typed call`() = runTest {
        whenever(fileGateway.getChildByName(RAW_PATH_ROOT, MEGA_IGNORE)) doReturn null
        whenever(fileGateway.createChildFileSync(RAW_PATH_ROOT, MEGA_IGNORE, false)) doReturn null
        whenever(fileGateway.canReadUri(RAW_PATH_ROOT)) doReturn true

        underTest.setExclusions(RAW_PATH_ROOT, EMPTY_MODEL)

        verify(fileGateway).canReadUri(RAW_PATH_ROOT)
        verify(fileGateway, never()).canReadUri(any<String>())
    }

    // endregion

    /**
     * Guards the round trip at the repository boundary rather than only in the mapper's own tests:
     * the SDK evaluates filters last-match-wins, so a load-then-save that reordered lines, dropped
     * the ones this app does not model, or lost the byte order mark would change what is enforced.
     *
     * The size bound is spelled in bytes because [MegaIgnoreLineMapper] canonicalizes unit
     * shorthand, so `100M` would come back as its byte count and mask a real reordering here.
     */
    @Test
    fun `test that a load and save cycle writes the file back unchanged`() = runTest {
        val original =
            "﻿# keep me\n-:*.tmp\n+sync:.megaignore\nexclude-larger:104857600\n-:build"
        val underTestWithRealMapper = repositoryWith(realFileMapper())
        stubExistingFile(original)
        whenever(fileGateway.saveTextOnContentUri(eq(exclusionsFile.value), any())) doReturn true

        val loaded = underTestWithRealMapper.getExclusions(localRoot).getOrThrow()
        underTestWithRealMapper.setExclusions(localRoot, loaded)

        verify(fileGateway).saveTextOnContentUri(exclusionsFile.value, original)
    }

    private fun repositoryWith(mapper: MegaIgnoreFileMapper) = SyncExclusionsRepositoryImpl(
        fileGateway = fileGateway,
        megaIgnoreFileMapper = mapper,
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    private fun realFileMapper() = MegaIgnoreFileMapper(MegaIgnoreLineMapper())

    private suspend fun stubExistingFile(content: String) {
        whenever(fileGateway.getChildByName(localRoot, MEGA_IGNORE)) doReturn exclusionsFile
        whenever(fileGateway.getInputStream(exclusionsFile)) doReturn content.byteInputStream()
    }

    private suspend fun stubSuccessfulWriteTo(file: UriPath) {
        whenever(fileGateway.getChildByName(localRoot, MEGA_IGNORE)) doReturn file
        whenever(megaIgnoreFileMapper(EMPTY_MODEL)) doReturn ""
        whenever(fileGateway.saveTextOnContentUri(file.value, "")) doReturn true
    }

    private suspend fun stubDotStrippingProvider() {
        whenever(fileGateway.getChildByName(localRoot, MEGA_IGNORE)) doReturn null
        whenever(fileGateway.createChildFileSync(localRoot, MEGA_IGNORE, false)) doReturn
                strippedFile
        whenever(fileGateway.getFileNameFromUri(strippedFile.value)) doReturn "megaignore"
        whenever(megaIgnoreFileMapper(EMPTY_MODEL)) doReturn ""
        whenever(fileGateway.saveTextOnContentUri(strippedFile.value, "")) doReturn true
    }

    private companion object {
        const val MEGA_IGNORE = ".megaignore"
        const val RAW_PATH = "/storage/emulated/0/Sync"
        val RAW_PATH_ROOT = UriPath(RAW_PATH)
        val EMPTY_MODEL = ParsedMegaIgnore(entries = emptyList())
    }
}
