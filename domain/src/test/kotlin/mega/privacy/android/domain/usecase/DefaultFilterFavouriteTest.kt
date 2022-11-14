package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.PhotosRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import java.time.LocalDateTime
import kotlin.test.assertFalse
import kotlin.test.assertTrue


@OptIn(ExperimentalCoroutinesApi::class)
class DefaultFilterFavouriteTest {

    lateinit var underTest: FilterFavourite

    private val cameraUploadFolderId = 10L
    private val mediaUploadFolderId = 20L

    private val photosRepository = mock<PhotosRepository>() {
        onBlocking { getCameraUploadFolderId() }.thenReturn(cameraUploadFolderId)
        onBlocking { getMediaUploadFolderId() }.thenReturn(mediaUploadFolderId)
    }


    @Before
    fun setUp() {
        underTest = DefaultFilterFavourite(photosRepository)
    }

    @Test
    fun `test that a favourite video is in sync folder then return true`() =
        runTest {
            val video1 = createVideo(
                isFavourite = true,
                parentId = cameraUploadFolderId
            )

            val video2 = createVideo(
                isFavourite = true,
                parentId = mediaUploadFolderId
            )

            assertTrue { underTest().invoke(video1) }
            assertTrue { underTest().invoke(video2) }
        }

    @Test
    fun `test that a favourite video is not in sync folder then return false`() =
        runTest {

            val video = createVideo(
                isFavourite = true
            )

            assertFalse { underTest().invoke(video) }
        }

    @Test
    fun `test that a non-favourite video then return false`() =
        runTest {
            val video = createVideo()

            assertFalse { underTest().invoke(video) }
        }

    @Test
    fun `test that a non-favourite image then return false`() =
        runTest {
            val image = createImage()

            assertFalse { underTest().invoke(image) }
        }

    @Test
    fun `test that a favourite image then return true`() =
        runTest {
            val image = createImage(
                isFavourite = true
            )

            assertTrue { underTest().invoke(image) }
        }

    private fun createVideo(
        id: Long = 1L,
        parentId: Long = 0L,
        isFavourite: Boolean = false,
        modificationTime: LocalDateTime = LocalDateTime.now(),
    ): Photo {
        return Photo.Video(
            id = id,
            parentId = parentId,
            name = "",
            isFavourite = isFavourite,
            creationTime = LocalDateTime.now(),
            modificationTime = modificationTime,
            thumbnailFilePath = "thumbnailFilePath",
            previewFilePath = "previewFilePath",
            duration = 123,
            fileTypeInfo = VideoFileTypeInfo("","")
        )
    }


    private fun createImage(
        id: Long = 2L,
        parentId: Long = 0L,
        isFavourite: Boolean = false,
        modificationTime: LocalDateTime = LocalDateTime.now(),
    ): Photo {
        return Photo.Image(
            id = id,
            parentId = parentId,
            name = "",
            isFavourite = isFavourite,
            creationTime = LocalDateTime.now(),
            modificationTime = modificationTime,
            thumbnailFilePath = "thumbnailFilePath",
            previewFilePath = "previewFilePath",
            fileTypeInfo = StaticImageFileTypeInfo("","")
        )
    }

}