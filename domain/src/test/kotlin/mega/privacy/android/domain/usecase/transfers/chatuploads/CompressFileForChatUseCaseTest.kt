package mega.privacy.android.domain.usecase.transfers.chatuploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
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

    @BeforeAll
    fun setup() {
        underTest = CompressFileForChatUseCase(
            isImageFileUseCase,
            isVideoFileUseCase,
            downscaleImageForChatUseCase,
            compressVideoForChatUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() =
        reset(
            isImageFileUseCase,
            isVideoFileUseCase,
            downscaleImageForChatUseCase,
            compressVideoForChatUseCase,
        )

    @Test
    fun `test that DownscaleImageForChatUseCase result is returned when file is an image`() =
        runTest {
            val path = "path"
            val original = mock<File> {
                on { it.absolutePath } doReturn path
            }
            val expected = mock<File>()
            whenever(isImageFileUseCase(original.absolutePath)) doReturn true
            whenever(downscaleImageForChatUseCase(original)) doReturn expected
            val actual = underTest(original)
            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test that CompressVideoForChatUseCase result is returned when file is a video`() =
        runTest {
            val path = "path"
            val original = mock<File> {
                on { it.absolutePath } doReturn path
            }
            val expected = mock<File>()
            whenever(isImageFileUseCase(original.absolutePath)) doReturn false
            whenever(isVideoFileUseCase(original.absolutePath)) doReturn true
            whenever(compressVideoForChatUseCase(original)) doReturn expected
            val actual = underTest(original)
            assertThat(actual).isEqualTo(expected)
        }
}