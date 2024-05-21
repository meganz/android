package mega.privacy.android.domain.usecase.transfers.chatuploads

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.chat.ChatUploadCompressionState
import mega.privacy.android.domain.usecase.chat.ChatUploadNotCompressedReason
import mega.privacy.android.domain.usecase.transfers.GetCacheFileForUploadUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.DownscaleImageForChatUseCase.Companion.DOWNSCALE_IMAGES_PX
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DownscaleImageForChatUseCaseTest {
    private lateinit var underTest: DownscaleImageForChatUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()
    private val getCacheFileForUploadUseCase =
        mock<GetCacheFileForUploadUseCase>()

    @BeforeAll
    fun setup() {
        underTest = DownscaleImageForChatUseCase(
            fileSystemRepository,
            getCacheFileForUploadUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() =
        reset(
            fileSystemRepository,
            getCacheFileForUploadUseCase,
        )

    @Test
    fun `test that scaled image from repository is returned when it needs to be scaled`() =
        runTest {
            val file = File("img.jpg")
            val expected = stubDestination()
            underTest(file).test {
                assertThat(awaitItem())
                    .isEqualTo(ChatUploadCompressionState.Compressed(expected))
                awaitComplete()
            }
            verify(fileSystemRepository).downscaleImage(file, expected, DOWNSCALE_IMAGES_PX)
        }

    @Test
    fun `test that FailedToCompress is returned when the file is not created`() =
        runTest {
            val file = File("img.jpg")
            val expected = stubDestination(exists = false)
            underTest(file).test {
                assertThat(awaitItem())
                    .isEqualTo(
                        ChatUploadCompressionState.NotCompressed(
                            ChatUploadNotCompressedReason.FailedToCompress
                        )
                    )
                awaitComplete()
            }
            verify(fileSystemRepository).downscaleImage(file, expected, DOWNSCALE_IMAGES_PX)
        }

    @Test
    fun `test that NoCacheFile is returned when the cache file is not created`() =
        runTest {
            val file = File("img.jpg")
            val expected = stubDestination()
            whenever(getCacheFileForUploadUseCase(any(), any())) doReturn null
            underTest(file).test {
                assertThat(awaitItem())
                    .isEqualTo(
                        ChatUploadCompressionState.NotCompressed(
                            ChatUploadNotCompressedReason.NoCacheFile
                        )
                    )
                awaitComplete()
            }
            verifyNoInteractions(fileSystemRepository)
        }

    private suspend fun stubDestination(exists: Boolean = true): File {
        val destination = mock<File> {
            on { it.name } doReturn "destination"
            on { it.exists() } doReturn exists
        }
        whenever(getCacheFileForUploadUseCase(any(), any())) doReturn destination
        return destination
    }
}