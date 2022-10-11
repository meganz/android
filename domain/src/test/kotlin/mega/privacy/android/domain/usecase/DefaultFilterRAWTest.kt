package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.RawFileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.photos.Photo
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultFilterRAWTest {

    lateinit var underTest: FilterRAW

    @Before
    fun setUp() {
        underTest = DefaultFilterRAW()
    }

    @Test
    fun `test that it is a raw image then return true`() =
        runTest {
            val image = createImage(fileTypeInfo = RawFileTypeInfo("", ""))

            assertTrue { underTest().invoke(image) }
        }

    @Test
    fun `test that it is not a raw image then return true`() =
        runTest {
            val image = createImage()

            assertFalse { underTest().invoke(image) }
        }

    private fun createImage(
        id: Long = 2L,
        parentId: Long = 0L,
        isFavourite: Boolean = false,
        modificationTime: LocalDateTime = LocalDateTime.now(),
        fileTypeInfo: FileTypeInfo = StaticImageFileTypeInfo("",
            ""),
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