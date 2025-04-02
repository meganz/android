package mega.privacy.android.domain.usecase.transfers.downloads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.NullFileException
import mega.privacy.android.domain.repository.CacheRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.file.IsExternalStorageContentUriUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetFileDestinationAndAppDataForDownloadUseCaseTest {
    private lateinit var underTest: GetFileDestinationAndAppDataForDownloadUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()
    private val isExternalStorageContentUriUseCase = mock<IsExternalStorageContentUriUseCase>()
    private val cacheRepository = mock<CacheRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetFileDestinationAndAppDataForDownloadUseCase(
            fileSystemRepository,
            cacheRepository,
            isExternalStorageContentUriUseCase,
        )
    }

    @BeforeEach
    fun cleanUp() = runTest {
        reset(
            fileSystemRepository,
            cacheRepository,
            isExternalStorageContentUriUseCase,
        )
        whenever(fileSystemRepository.isSDCardPathOrUri(any())) doReturn false
        whenever(fileSystemRepository.isContentUri(any())) doReturn false
        whenever(isExternalStorageContentUriUseCase(any())) doReturn false
    }

    @Test
    fun `test that same destination and null app data is returned when the destination is not in external storage or content uri`() =
        runTest {
            val expectedUri = UriPath(PATH_STRING)

            val (actualUri, actualAppData) = underTest(expectedUri)

            assertAll(
                { assertThat(actualUri).isEqualTo(expectedUri) },
                { assertThat(actualAppData).isNull() },
            )
        }

    @Test
    fun `test that external cache folder and SdCardDownload app data is NOT returned when the destination is a sd card path`() =
        runTest {
            val expectedUri = UriPath(PATH_STRING)

            whenever(fileSystemRepository.isSDCardPathOrUri(PATH_STRING)) doReturn true

            val (actualUri, actualAppData) = underTest(UriPath(PATH_STRING))

            assertAll(
                { assertThat(actualUri).isEqualTo(expectedUri) },
                { assertThat(actualAppData).isNull() },
            )
        }

    @Test
    fun `test that cache folder and SdCardDownload app data is returned when the destination is a content uri path`() =
        runTest {
            val cachePath = "Cache"
            val cacheFolder = File(cachePath)
            whenever(fileSystemRepository.isContentUri(PATH_STRING)) doReturn true
            whenever(cacheRepository.getCacheFolderNameForTransfer(false)) doReturn "temp"
            whenever(cacheRepository.getCacheFolder(any())) doReturn cacheFolder
            val expectedUri = UriPath(cachePath)
            val expectedAppData = TransferAppData.SdCardDownload(
                targetPathForSDK = cachePath,
                finalTargetUri = PATH_STRING
            )

            val (actualUri, actualAppData) = underTest(UriPath(PATH_STRING))

            assertAll(
                { assertThat(actualUri).isEqualTo(expectedUri) },
                { assertThat(actualAppData).isEqualTo(expectedAppData) },
            )
        }

    @Test
    fun `test that original uri and null app data is returned when the destination is an external content uri path and feature flag is true`() =
        runTest {
            whenever(isExternalStorageContentUriUseCase(PATH_STRING)) doReturn true
            val expectedUri = UriPath(PATH_STRING)

            val (actualUri, actualAppData) = underTest(expectedUri)

            assertAll(
                { assertThat(actualUri).isEqualTo(expectedUri) },
                { assertThat(actualAppData).isNull() },
            )
        }

    @Test
    fun `test that a NullFileException is thrown when the destination is a content uri path and getOrCreateSDCardTransfersCacheFolder returns null`() =
        runTest {
            whenever(fileSystemRepository.isContentUri(PATH_STRING)) doReturn true

            assertThrows<NullFileException> {
                underTest(UriPath(PATH_STRING))
            }
        }

    companion object {
        private const val PATH_STRING = "uriPath/"
    }
}