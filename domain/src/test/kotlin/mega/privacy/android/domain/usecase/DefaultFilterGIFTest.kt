package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.GifFileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.photos.Photo
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultFilterGIFTest {

    lateinit var underTest: FilterGIF

    @Before
    fun setUp() {
        underTest = DefaultFilterGIF()
    }

    @Test
    fun `test that if it is a gif image then return true`() =
        runTest {
            val gifImage = createImage(
                fileTypeInfo = GifFileTypeInfo("", "")
            )

            assertTrue { underTest().invoke(gifImage) }
        }

    @Test
    fun `test that if it is not a gif image then return false`() =
        runTest {
            val gifImage = createImage()

            assertFalse{ underTest().invoke(gifImage) }
        }

    private fun createImage(
        id: Long = 2L,
        parentId: Long = 0L,
        isFavourite: Boolean = false,
        modificationTime: LocalDateTime = LocalDateTime.now(),
        fileTypeInfo: FileTypeInfo = StaticImageFileTypeInfo("", ""),
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
            fileTypeInfo = fileTypeInfo
        )
    }
}