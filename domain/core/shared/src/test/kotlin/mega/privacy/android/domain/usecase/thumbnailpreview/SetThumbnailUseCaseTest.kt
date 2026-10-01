package mega.privacy.android.domain.usecase.thumbnailpreview

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.thumbnailpreview.ThumbnailPreviewRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetThumbnailUseCaseTest {

    private lateinit var underTest: SetThumbnailUseCase

    private lateinit var thumbnailPreviewRepository: ThumbnailPreviewRepository

    @BeforeAll
    fun setup() {
        thumbnailPreviewRepository = mock()
        underTest = SetThumbnailUseCase(thumbnailPreviewRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(thumbnailPreviewRepository)
    }


    @Test
    fun `test that set thumbnail invokes ThumbnailPreviewRepository`() = runTest {
        val handle = 1L
        val srcFilePath = "test/path"
        underTest(handle, srcFilePath)
        verify(thumbnailPreviewRepository).setThumbnail(handle, srcFilePath)
    }
}