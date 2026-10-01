package mega.privacy.android.domain.usecase.transfers.chatuploads

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.chat.ChatUploadCompressionState
import mega.privacy.android.domain.usecase.chat.ChatUploadNotCompressedReason
import mega.privacy.android.domain.usecase.file.IsImageFileUseCase
import mega.privacy.android.domain.usecase.file.IsVideoFileUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompressFileForChatUseCaseTest {
    private lateinit var underTest: CompressFileForChatUseCase

    private val isImageFileUseCase = mock<IsImageFileUseCase>()
    private val isVideoFileUseCase = mock<IsVideoFileUseCase>()
    private val downscaleImageForChatUseCase = mock<DownscaleImageForChatUseCase>()
    private val compressVideoForChatUseCase = mock<CompressVideoForChatUseCase>()
    private val chatAttachmentNeedsCompressionUseCase =
        mock<ChatAttachmentNeedsCompressionUseCase>()

    @BeforeAll
    fun setup() {
        underTest = CompressFileForChatUseCase(
            chatAttachmentNeedsCompressionUseCase,
            isImageFileUseCase,
            isVideoFileUseCase,
            downscaleImageForChatUseCase,
            compressVideoForChatUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() =
        reset(
            chatAttachmentNeedsCompressionUseCase,
            isImageFileUseCase,
            isVideoFileUseCase,
            downscaleImageForChatUseCase,
            compressVideoForChatUseCase,
        )

    @Test
    fun `test that NotCompressed is returned when chat attachment doesn't need compression`() =
        runTest {
            val path = "path"
            val uriPath = UriPath(path)
            whenever(chatAttachmentNeedsCompressionUseCase(uriPath)) doReturn false

            underTest(uriPath).test {
                assertThat(awaitItem()).isEqualTo(
                    ChatUploadCompressionState.NotCompressed(
                        ChatUploadNotCompressedReason.CompressionNotNeeded
                    )
                )
                awaitComplete()

            }
        }


    @Test
    fun `test that DownscaleImageForChatUseCase result is returned when file is an image`() =
        runTest {
            val path = "path"
            val original = UriPath(path)
            whenever(chatAttachmentNeedsCompressionUseCase(UriPath(path))) doReturn true
            val expected = ChatUploadCompressionState.Compressed(mock<File>())
            whenever(isImageFileUseCase(original)) doReturn true
            whenever(downscaleImageForChatUseCase(original)) doReturn flowOf(expected)

            underTest(original).test {
                assertThat(awaitItem()).isEqualTo(expected)
                awaitComplete()
            }
        }

    @Test
    fun `test that CompressVideoForChatUseCase result is returned when file is a video`() =
        runTest {
            val path = "path"
            val original = UriPath(path)
            val expected = ChatUploadCompressionState.Compressed(mock<File>())
            whenever(chatAttachmentNeedsCompressionUseCase(UriPath(path))) doReturn true
            whenever(isImageFileUseCase(original)) doReturn false
            whenever(isVideoFileUseCase(original)) doReturn true
            whenever(compressVideoForChatUseCase(original)) doReturn flowOf(expected)

            underTest(original).test {
                assertThat(awaitItem()).isEqualTo(expected)
                awaitComplete()
            }
        }
}