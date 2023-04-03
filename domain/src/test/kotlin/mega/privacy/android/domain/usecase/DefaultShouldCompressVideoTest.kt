package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.camerauploads.GetUploadVideoQualityUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

/**
 * Test class for [DefaultShouldCompressVideo]
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultShouldCompressVideoTest {
    private lateinit var underTest: ShouldCompressVideo

    private val cameraUploadRepository = mock<CameraUploadRepository>()
    private val getUploadVideoQualityUseCase = mock<GetUploadVideoQualityUseCase>()

    @Before
    fun setUp() {
        underTest = DefaultShouldCompressVideo(
            cameraUploadRepository = cameraUploadRepository,
            getUploadVideoQuality = getUploadVideoQualityUseCase,
        )
    }

    @Test
    fun `test that false is returned if no setting is returned`() = runTest {
        getUploadVideoQualityUseCase.stub {
            onBlocking { invoke() }.thenReturn(null)
        }

        assertThat(underTest()).isFalse()
    }

    @Test
    fun `test that false is returned if quality is original`() = runTest {
        getUploadVideoQualityUseCase.stub {
            onBlocking { invoke() }.thenReturn(VideoQuality.ORIGINAL)
        }

        assertThat(underTest()).isFalse()
    }

    @Test
    fun `test that true is returned for any non original values`() = runTest {
        VideoQuality.values()
            .filterNot { it == VideoQuality.ORIGINAL }
            .forEach { quality ->
                getUploadVideoQualityUseCase.stub {
                    onBlocking { invoke() }.thenReturn(quality)
                }

                assertThat(underTest()).isTrue()
            }
    }
}