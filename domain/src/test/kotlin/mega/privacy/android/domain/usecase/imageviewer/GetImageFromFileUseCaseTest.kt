package mega.privacy.android.domain.usecase.imageviewer

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.repository.ImageRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File


@OptIn(ExperimentalCoroutinesApi::class)
class GetImageFromFileUseCaseTest {
    private lateinit var underTest: GetImageFromFileUseCase

    private val imageRepository = mock<ImageRepository>()

    @Before
    fun setUp() {
        underTest =
            GetImageFromFileUseCase(imageRepository)
    }


    @Test(expected = IllegalArgumentException::class)
    fun `test that exception is thrown when file does not exist`() {
        runTest {
            val file = mock<File> {
                on { exists() }.thenReturn(false)
                on { canRead() }.thenReturn(true)
            }

            underTest.invoke(
                file = file,
                highPriority = false,
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test that exception is thrown when file can not be read`() {
        runTest {
            val file = mock<File> {
                on { exists() }.thenReturn(true)
                on { canRead() }.thenReturn(false)
            }

            underTest.invoke(
                file = file,
                highPriority = false,
            )
        }
    }

    @Test
    fun `test that image repository function is invoked when file exists and can read`() {
        runTest {
            val file = mock<File> {
                on { exists() }.thenReturn(true)
                on { canRead() }.thenReturn(true)
            }
            val highPriority = false

            underTest.invoke(
                file = file,
                highPriority = highPriority,
            )
            verify(imageRepository, times(1)).getImageFromFile(
                file = file,
                highPriority = highPriority
            )
        }
    }

    @Test
    fun `test that result from image repository function is returned when file exists and can read`() {
        runTest {
            val file = mock<File> {
                on { exists() }.thenReturn(true)
                on { canRead() }.thenReturn(true)
            }
            val highPriority = false

            val expected = ImageResult(
                previewUri = "testPreviewUri",
                fullSizeUri = "testFullUri",
                isVideo = false,
                isFullyLoaded = true
            )

            whenever(
                imageRepository.getImageFromFile(
                    file = file,
                    highPriority = highPriority
                )
            ).thenReturn(expected)

            val actual = underTest.invoke(
                file = file,
                highPriority = highPriority,
            )
            assertThat(actual).isEqualTo(expected)
        }
    }
}