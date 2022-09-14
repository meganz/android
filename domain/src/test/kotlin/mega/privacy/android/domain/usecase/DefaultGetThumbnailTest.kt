package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
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
class DefaultGetThumbnailTest {
    private lateinit var underTest: DefaultGetThumbnail
    private val imageRepository = mock<ImageRepository>()


    @Before
    fun setUp() {
        underTest = DefaultGetThumbnail(
            repository = imageRepository,
        )
    }

    @Test
    fun `test that if local thumbnail exist then return local thumbnail`() = runTest {
        val expected = mock<File>()
        whenever(imageRepository.getThumbnailFromLocal(any())).thenReturn(expected)

        assertThat(underTest.invoke(any())).isEqualTo(expected)
    }

    @Test
    fun `test that if local thumbnail does not exist then return thumbnail from server`() =
        runTest {
            val expected = mock<File>()
            whenever(imageRepository.getThumbnailFromLocal(any())).thenReturn(null)
            whenever(imageRepository.getThumbnailFromServer(any())).thenReturn(expected)

            assertThat(underTest.invoke(any())).isEqualTo(expected)
        }

    @Test
    fun `test that if local thumbnail does not exist and an error is thrown when retrieving from server then return null`() =
        runTest {
            whenever(imageRepository.getThumbnailFromLocal(any())).thenReturn(null)
            whenever(imageRepository.getThumbnailFromServer(any())).thenThrow(MegaException(null,
                null))

            assertThat(underTest.invoke(any())).isEqualTo(null)
        }
}