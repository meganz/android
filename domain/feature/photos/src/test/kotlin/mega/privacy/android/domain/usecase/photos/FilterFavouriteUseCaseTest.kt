package mega.privacy.android.domain.usecase.photos

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.photos.Photo
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds


@OptIn(ExperimentalCoroutinesApi::class)
class FilterFavouriteUseCaseTest {

    lateinit var underTest: FilterFavouriteUseCase


    @Before
    fun setUp() {
        underTest = FilterFavouriteUseCase()
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
            fileTypeInfo = VideoFileTypeInfo("", "", duration = 123.seconds)
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
            fileTypeInfo = StaticImageFileTypeInfo("", "")
        )
    }

}
