package mega.privacy.android.domain.usecase.photos

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ImageRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.io.File

/**
 * Test class for [GeneratePreviewAndThumbnailUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GeneratePreviewAndThumbnailUseCaseTest {

    private lateinit var underTest: GeneratePreviewAndThumbnailUseCase

    private val imageRepository = mock<ImageRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GeneratePreviewAndThumbnailUseCase(
            imageRepository = imageRepository,
        )
    }

    @Test
    internal fun `test that preview and thumbnail is generated when invoked`() =
        runTest {
            val handle = 1L
            val file = mock<File>()
            underTest(handle, file)
            verify(imageRepository).generatePreview(handle, file)
            verify(imageRepository).generateThumbnail(handle, file)
        }
}
