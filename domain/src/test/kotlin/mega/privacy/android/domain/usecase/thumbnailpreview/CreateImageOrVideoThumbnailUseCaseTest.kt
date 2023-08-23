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
 * Test class for [CreateImageOrVideoThumbnailUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateImageOrVideoThumbnailUseCaseTest {

    private lateinit var underTest: CreateImageOrVideoThumbnailUseCase

    private val thumbnailPreviewRepository = mock<ThumbnailPreviewRepository>()

    @BeforeAll
    fun setUp() {
        underTest = CreateImageOrVideoThumbnailUseCase(
            thumbnailPreviewRepository = thumbnailPreviewRepository,
        )
    }

    @Test
    internal fun `test that thumbnail is generated when invoked`() =
        runTest {
            val handle = 1L
            val file = mock<File>()
            underTest(handle, file)
            verify(thumbnailPreviewRepository).createThumbnail(handle, file)
        }
}
