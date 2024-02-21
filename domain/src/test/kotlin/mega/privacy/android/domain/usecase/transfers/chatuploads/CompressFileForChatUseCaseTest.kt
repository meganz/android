package mega.privacy.android.domain.usecase.transfers.chatuploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.file.IsImageFileUseCase
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
    private val downscaleImageForChatUseCase = mock<DownscaleImageForChatUseCase>()

    @BeforeAll
    fun setup() {
        underTest = CompressFileForChatUseCase(
            isImageFileUseCase,
            downscaleImageForChatUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() =
        reset(
            isImageFileUseCase,
            downscaleImageForChatUseCase,
        )

    @Test
    fun `test that CompressFileForChatUseCase result is returned when file is an image`() =
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
}