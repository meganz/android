package mega.privacy.android.domain.usecase.transfers.chatuploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
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
    private val getCacheFileForChatUploadUseCase =
        mock<GetCacheFileForChatUploadUseCase>()

    @BeforeAll
    fun setup() {
        underTest = DownscaleImageForChatUseCase(
            fileSystemRepository,
            getCacheFileForChatUploadUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() =
        reset(
            fileSystemRepository,
            getCacheFileForChatUploadUseCase,
        )

    @Test
    fun `test that gifs are not scaled`() = runTest {
        val file = File("img.gif")
        assertThat(underTest(file)).isNull()
        verifyNoInteractions(fileSystemRepository)
    }

    @Test
    fun `test that webp are not scaled`() = runTest {
        val file = File("img.webp")
        assertThat(underTest(file)).isNull()
        verifyNoInteractions(fileSystemRepository)
    }

    @Test
    fun `test that scaled image from repository is returned when needs to be scaled`() =
        runTest {
            val file = File("img.jpg")
            val expected = stubDestination()
            val actual = underTest(file)
            assertThat(actual).isEqualTo(expected)
            verify(fileSystemRepository).downscaleImage(file, expected, DOWNSCALE_IMAGES_PX)
        }

    private suspend fun stubDestination(): File {
        val destination = mock<File> {
            on { it.name } doReturn "destination"
            on { it.exists() } doReturn true
        }
        whenever(getCacheFileForChatUploadUseCase(any())) doReturn destination
        return destination
    }
}