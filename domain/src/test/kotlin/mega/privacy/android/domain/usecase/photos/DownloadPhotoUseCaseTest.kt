package mega.privacy.android.domain.usecase.photos

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.photos.DownloadPhotoRequest
import mega.privacy.android.domain.entity.photos.DownloadPhotoResult
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.camerauploads.IsFolderPathExistingUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DownloadPreviewUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DownloadPublicNodePreviewUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DownloadPublicNodeThumbnailUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DownloadThumbnailUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DownloadPhotoUseCaseTest {

    private lateinit var underTest: DownloadPhotoUseCase

    private val downloadThumbnailUseCase: DownloadThumbnailUseCase = mock()
    private val downloadPreviewUseCase: DownloadPreviewUseCase = mock()
    private val downloadPublicNodeThumbnailUseCase: DownloadPublicNodeThumbnailUseCase = mock()
    private val downloadPublicNodePreviewUseCase: DownloadPublicNodePreviewUseCase = mock()
    private val isFolderPathExistingUseCase: IsFolderPathExistingUseCase = mock()

    @BeforeEach
    fun setup() {
        underTest = DownloadPhotoUseCase(
            downloadThumbnailUseCase = downloadThumbnailUseCase,
            downloadPreviewUseCase = downloadPreviewUseCase,
            downloadPublicNodeThumbnailUseCase = downloadPublicNodeThumbnailUseCase,
            downloadPublicNodePreviewUseCase = downloadPublicNodePreviewUseCase,
            isFolderPathExistingUseCase = isFolderPathExistingUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            downloadThumbnailUseCase,
            downloadPreviewUseCase,
            downloadPublicNodeThumbnailUseCase,
            downloadPublicNodePreviewUseCase,
            isFolderPathExistingUseCase
        )
    }

    @Test
    fun `test that empty file path result is returned when the request is for preview and the preview file path is null`() =
        runTest {
            val photo = mock<Photo.Image> {
                on { previewFilePath } doReturn null
            }
            val request = DownloadPhotoRequest(
                isPreview = true,
                photo = photo
            )

            val actual = underTest(request)

            assertThat(actual).isEqualTo(DownloadPhotoResult.EmptyFilePath)
        }

    @Test
    fun `test that empty file path result is returned when the request is for thumbnail and the thumbnail file path is null`() =
        runTest {
            val photo = mock<Photo.Image> {
                on { thumbnailFilePath } doReturn null
            }
            val request = DownloadPhotoRequest(
                isPreview = false,
                photo = photo
            )

            val actual = underTest(request)

            assertThat(actual).isEqualTo(DownloadPhotoResult.EmptyFilePath)
        }

    @Test
    fun `test that success result is returned when the file path already exists`() = runTest {
        val path = "path"
        val photo = mock<Photo.Image> {
            on { previewFilePath } doReturn path
            on { thumbnailFilePath } doReturn path
        }
        val request = DownloadPhotoRequest(
            isPreview = false,
            photo = photo
        )
        whenever(isFolderPathExistingUseCase(path = path)) doReturn true

        val actual = underTest(request)

        assertThat(actual).isEqualTo(
            DownloadPhotoResult.Success(
                previewFilePath = request.photo.previewFilePath,
                thumbnailFilePath = request.photo.thumbnailFilePath
            )
        )
    }

    @Test
    fun `test that success result is returned when the public node preview is successfully downloaded`() =
        runTest {
            val path = "path"
            val photo = mock<Photo.Image> {
                on { previewFilePath } doReturn path
                on { thumbnailFilePath } doReturn path
            }
            val request = DownloadPhotoRequest(
                isPreview = true,
                photo = photo,
                isPublicNode = true
            )
            whenever(isFolderPathExistingUseCase(path = path)) doReturn false
            whenever(downloadPublicNodePreviewUseCase(nodeId = request.photo.id)) doReturn true

            val actual = underTest(request)

            assertThat(actual).isEqualTo(
                DownloadPhotoResult.Success(
                    previewFilePath = request.photo.previewFilePath,
                    thumbnailFilePath = request.photo.thumbnailFilePath
                )
            )
        }

    @Test
    fun `test that error result is returned when the public node preview is not successfully downloaded`() =
        runTest {
            val path = "path"
            val photo = mock<Photo.Image> {
                on { previewFilePath } doReturn path
                on { thumbnailFilePath } doReturn path
            }
            val request = DownloadPhotoRequest(
                isPreview = true,
                photo = photo,
                isPublicNode = true
            )
            whenever(isFolderPathExistingUseCase(path = path)) doReturn false
            whenever(downloadPublicNodePreviewUseCase(nodeId = request.photo.id)) doReturn false

            val actual = underTest(request)

            assertThat(actual).isEqualTo(DownloadPhotoResult.Error)
        }

    @Test
    fun `test that success result is returned when the public node thumbnail is successfully downloaded`() =
        runTest {
            val path = "path"
            val photo = mock<Photo.Image> {
                on { previewFilePath } doReturn path
                on { thumbnailFilePath } doReturn path
            }
            val request = DownloadPhotoRequest(
                isPreview = false,
                photo = photo,
                isPublicNode = true
            )
            whenever(isFolderPathExistingUseCase(path = path)) doReturn false
            whenever(downloadPublicNodeThumbnailUseCase(nodeId = request.photo.id)) doReturn true

            val actual = underTest(request)

            assertThat(actual).isEqualTo(
                DownloadPhotoResult.Success(
                    previewFilePath = request.photo.previewFilePath,
                    thumbnailFilePath = request.photo.thumbnailFilePath
                )
            )
        }

    @Test
    fun `test that error result is returned when the public node thumbnail is not successfully downloaded`() =
        runTest {
            val path = "path"
            val photo = mock<Photo.Image> {
                on { previewFilePath } doReturn path
                on { thumbnailFilePath } doReturn path
            }
            val request = DownloadPhotoRequest(
                isPreview = false,
                photo = photo,
                isPublicNode = true
            )
            whenever(isFolderPathExistingUseCase(path = path)) doReturn false
            whenever(downloadPublicNodeThumbnailUseCase(nodeId = request.photo.id)) doReturn false

            val actual = underTest(request)

            assertThat(actual).isEqualTo(DownloadPhotoResult.Error)
        }

    @Test
    fun `test that success result is returned when the preview file is successfully downloaded`() =
        runTest {
            val path = "path"
            val photo = mock<Photo.Image> {
                on { previewFilePath } doReturn path
                on { thumbnailFilePath } doReturn path
            }
            val request = DownloadPhotoRequest(
                isPreview = true,
                photo = photo
            )
            whenever(isFolderPathExistingUseCase(path = path)) doReturn false

            val actual = underTest(request)

            assertThat(actual).isEqualTo(
                DownloadPhotoResult.Success(
                    previewFilePath = request.photo.previewFilePath,
                    thumbnailFilePath = request.photo.thumbnailFilePath
                )
            )
        }

    @Test
    fun `test that error result is returned when the preview file is not successfully downloaded`() =
        runTest {
            val path = "path"
            val photo = mock<Photo.Image> {
                on { previewFilePath } doReturn path
                on { thumbnailFilePath } doReturn path
            }
            val request = DownloadPhotoRequest(
                isPreview = true,
                photo = photo
            )
            whenever(isFolderPathExistingUseCase(path = path)) doReturn false
            whenever(
                downloadPreviewUseCase(nodeId = eq(request.photo.id))
            ) doAnswer {
                throw IllegalStateException("Preview node not found")
            }

            val actual = underTest(request)

            assertThat(actual).isEqualTo(DownloadPhotoResult.Error)
        }

    @Test
    fun `test that success result is returned when the thumbnail file is successfully downloaded`() =
        runTest {
            val path = "path"
            val photo = mock<Photo.Image> {
                on { previewFilePath } doReturn path
                on { thumbnailFilePath } doReturn path
            }
            val request = DownloadPhotoRequest(
                isPreview = false,
                photo = photo
            )
            whenever(isFolderPathExistingUseCase(path = path)) doReturn false

            val actual = underTest(request)

            assertThat(actual).isEqualTo(
                DownloadPhotoResult.Success(
                    previewFilePath = request.photo.previewFilePath,
                    thumbnailFilePath = request.photo.thumbnailFilePath
                )
            )
        }

    @Test
    fun `test that error result is returned when the thumbnail file is not successfully downloaded`() =
        runTest {
            val path = "path"
            val photo = mock<Photo.Image> {
                on { previewFilePath } doReturn path
                on { thumbnailFilePath } doReturn path
            }
            val request = DownloadPhotoRequest(
                isPreview = false,
                photo = photo
            )
            whenever(isFolderPathExistingUseCase(path = path)) doReturn false
            whenever(
                downloadThumbnailUseCase(nodeId = eq(request.photo.id))
            ) doAnswer {
                throw IllegalStateException("Thumbnail node not found")
            }

            val actual = underTest(request)

            assertThat(actual).isEqualTo(DownloadPhotoResult.Error)
        }
}
