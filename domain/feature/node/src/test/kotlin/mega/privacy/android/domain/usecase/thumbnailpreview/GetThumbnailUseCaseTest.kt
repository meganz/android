package mega.privacy.android.domain.usecase.thumbnailpreview

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.thumbnailpreview.ThumbnailPreviewRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class GetThumbnailUseCaseTest {
    private lateinit var underTest: GetThumbnailUseCase
    private val thumbnailPreviewRepository = mock<ThumbnailPreviewRepository>()


    @Before
    fun setUp() {
        underTest = GetThumbnailUseCase(
            thumbnailPreviewRepository = thumbnailPreviewRepository,
        )
    }

    @Test
    fun `test that if local thumbnail exist then return local thumbnail`() = runTest {
        val expected = mock<File>()
        whenever(thumbnailPreviewRepository.getThumbnailFromLocal(any())).thenReturn(expected)

        assertThat(underTest.invoke(any())).isEqualTo(expected)
    }

    @Test
    fun `test that if local thumbnail does not exist then return thumbnail from server`() =
        runTest {
            val expected = mock<File>()
            whenever(thumbnailPreviewRepository.getThumbnailFromLocal(any())).thenReturn(null)
            whenever(thumbnailPreviewRepository.getThumbnailFromServer(any())).thenReturn(expected)

            assertThat(underTest.invoke(any())).isEqualTo(expected)
        }

    @Test
    fun `test that if local thumbnail does not exist and an error is thrown when retrieving from server then return null`() =
        runTest {
            whenever(thumbnailPreviewRepository.getThumbnailFromLocal(any())).thenReturn(null)
            whenever(thumbnailPreviewRepository.getThumbnailFromServer(any())).thenThrow(
                MegaException(
                    0,
                    null
                )
            )

            assertThat(underTest.invoke(any())).isEqualTo(null)
        }
}