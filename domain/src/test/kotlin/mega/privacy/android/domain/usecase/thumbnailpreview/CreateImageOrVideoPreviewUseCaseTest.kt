package mega.privacy.android.domain.usecase.thumbnailpreview

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.thumbnailpreview.ThumbnailPreviewRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.io.File

/**
 * Test class for [CreateImageOrVideoPreviewUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateImageOrVideoPreviewUseCaseTest {

    private lateinit var underTest: CreateImageOrVideoPreviewUseCase

    private val thumbnailPreviewRepository = mock<ThumbnailPreviewRepository>()

    @BeforeAll
    fun setUp() {
        underTest = CreateImageOrVideoPreviewUseCase(
            thumbnailPreviewRepository = thumbnailPreviewRepository,
        )
    }

    @Test
    internal fun `test that preview is generated when invoked`() =
        runTest {
            val handle = 1L
            val file = mock<File>()
            underTest(handle, file)
            verify(thumbnailPreviewRepository).createPreview(handle, file)
        }
}
