package mega.privacy.android.feature.photos.downloader

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.photos.DownloadPhotoRequest
import mega.privacy.android.domain.entity.photos.DownloadPhotoResult
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.photos.DownloadPhotoUseCase
import mega.privacy.android.feature.photos.mapper.PhotoMapper
import mega.privacy.android.feature.photos.model.PhotoUiState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DownloadPhotoViewModelTest {

    private lateinit var underTest: DownloadPhotoViewModel

    private val downloadPhotoUseCase: DownloadPhotoUseCase = mock()
    private val photoMapper: PhotoMapper = mock()

    @BeforeEach
    fun setup() {
        underTest = DownloadPhotoViewModel(
            downloadPhotoUseCase = downloadPhotoUseCase,
            photoMapper = photoMapper
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            downloadPhotoUseCase,
            photoMapper
        )
    }

    @Test
    fun `test that the existing flow is returned if exists`() = runTest {
        val photoUiState = mock<PhotoUiState.Image>()
        val photo = mock<Photo.Image>()
        val isPreview = false
        val isPublicNode = false
        whenever(photoMapper(photoUiState = photoUiState)) doReturn photo
        whenever(
            downloadPhotoUseCase(
                request = DownloadPhotoRequest(
                    isPreview = isPreview,
                    photo = photo,
                    isPublicNode = isPublicNode
                )
            )
        ) doReturn DownloadPhotoResult.Success(
            previewFilePath = "preview",
            thumbnailFilePath = "thumbail"
        )

        val expected = underTest.getDownloadPhotoResult(
            photoUiState = photoUiState,
            isPreview = isPreview,
            isPublicNode = isPublicNode
        )
        val actual = underTest.getDownloadPhotoResult(
            photoUiState = photoUiState,
            isPreview = isPreview,
            isPublicNode = isPublicNode
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that the correct success result is returned when the download is successful`() =
        runTest {
            val photoUiState = mock<PhotoUiState.Image>()
            val photo = mock<Photo.Image>()
            val isPreview = false
            val isPublicNode = false
            val result = DownloadPhotoResult.Success(
                previewFilePath = "preview",
                thumbnailFilePath = "thumbail"
            )
            whenever(photoMapper(photoUiState = photoUiState)) doReturn photo
            whenever(
                downloadPhotoUseCase(
                    request = DownloadPhotoRequest(
                        isPreview = isPreview,
                        photo = photo,
                        isPublicNode = isPublicNode
                    )
                )
            ) doReturn result

            underTest.getDownloadPhotoResult(
                photoUiState = photoUiState,
                isPreview = isPreview,
                isPublicNode = isPublicNode
            ).test {
                assertThat(expectMostRecentItem()).isEqualTo(result)
            }
        }

    @Test
    fun `test that the error result is returned when the download fails`() =
        runTest {
            val photoUiState = mock<PhotoUiState.Image>()
            val photo = mock<Photo.Image>()
            val isPreview = false
            val isPublicNode = false
            whenever(photoMapper(photoUiState = photoUiState)) doReturn photo
            whenever(
                downloadPhotoUseCase(
                    request = DownloadPhotoRequest(
                        isPreview = isPreview,
                        photo = photo,
                        isPublicNode = isPublicNode
                    )
                )
            ) doThrow RuntimeException()

            underTest.getDownloadPhotoResult(
                photoUiState = photoUiState,
                isPreview = isPreview,
                isPublicNode = isPublicNode
            ).test {
                assertThat(expectMostRecentItem()).isEqualTo(DownloadPhotoResult.Error)
            }
        }
}
