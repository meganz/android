package mega.privacy.android.domain.usecase.photos

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.ImageRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class GetPublicNodeThumbnailUseCaseTest {
    private lateinit var underTest: GetPublicNodeThumbnailUseCase
    private val imageRepository = mock<ImageRepository>()


    @Before
    fun setUp() {
        underTest = GetPublicNodeThumbnailUseCase(
            imageRepository = imageRepository,
        )
    }

    @Test
    fun `test that if local thumbnail exist then return local thumbnail`() = runTest {
        val expected = mock<File>()
        whenever(imageRepository.getPublicNodeThumbnailFromLocal(any())).thenReturn(expected)

        Truth.assertThat(underTest.invoke(any())).isEqualTo(expected)
    }

    @Test
    fun `test that if local thumbnail does not exist then return thumbnail from server`() =
        runTest {
            val expected = mock<File>()
            whenever(imageRepository.getPublicNodeThumbnailFromLocal(any())).thenReturn(null)
            whenever(imageRepository.getPublicNodeThumbnailFromServer(any())).thenReturn(expected)

            Truth.assertThat(underTest.invoke(any())).isEqualTo(expected)
        }

    @Test
    fun `test that if local thumbnail does not exist and an error is thrown when retrieving from server then return null`() =
        runTest {
            whenever(imageRepository.getPublicNodeThumbnailFromLocal(any())).thenReturn(null)
            whenever(imageRepository.getPublicNodeThumbnailFromServer(any()))
                .thenThrow(MegaException(0, null))

            Truth.assertThat(underTest.invoke(any())).isEqualTo(null)
        }
}