package mega.privacy.android.domain.usecase.imagepreview.mapper

import kotlinx.coroutines.runBlocking
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.SvgFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.mock

class OfflineFileInformationToImageNodeMapperTest {

    private val mapper = OfflineFileInformationToImageNodeMapper()

    @Test
    fun `test that image is mapped correctly when invoked`() = runBlocking {
        val offlineFileInformation = mock<OfflineFileInformation> {
            on { isFolder }.thenReturn(false)
            on { fileTypeInfo }.thenReturn(mock<StaticImageFileTypeInfo>())
            on { handle }.thenReturn("123")
            on { name }.thenReturn("image.jpg")
            on { totalSize }.thenReturn(1024L)
            on { addedTime }.thenReturn(1000L)
            on { absolutePath }.thenReturn("/path/to/image.jpg")
            on { thumbnail }.thenReturn("/path/to/thumbnail.jpg")
        }

        val result = mapper(offlineFileInformation, filterSvg = false)

        assertNotNull(result)
        assertEquals("image.jpg", result?.name)
        assertEquals(1024L, result?.size)
        assertEquals("/path/to/image.jpg", result?.fullSizePath)
    }

    @Test
    fun `test that null is returned when filterSvg is true and image type is SVG`() = runBlocking {
        val offlineFileInformation = mock<OfflineFileInformation> {
            on { isFolder }.thenReturn(false)
            on { fileTypeInfo }.thenReturn(mock<SvgFileTypeInfo>())
            on { handle }.thenReturn("123")
            on { name }.thenReturn("image.svg")
            on { totalSize }.thenReturn(1024L)
            on { addedTime }.thenReturn(1000L)
            on { absolutePath }.thenReturn("/path/to/image.svg")
            on { thumbnail }.thenReturn("/path/to/thumbnail.jpg")
        }

        val result = mapper(offlineFileInformation, filterSvg = true)

        assertNull(result)
    }

    @Test
    fun `test that video file is mapped correctly when invoked`() = runBlocking {
        val offlineFileInformation = mock<OfflineFileInformation> {
            on { isFolder }.thenReturn(false)
            on { fileTypeInfo }.thenReturn(mock<VideoFileTypeInfo>())
            on { handle }.thenReturn("123")
            on { name }.thenReturn("video.mp4")
            on { totalSize }.thenReturn(2048L)
            on { addedTime }.thenReturn(2000L)
            on { absolutePath }.thenReturn("/path/to/video.mp4")
            on { thumbnail }.thenReturn("/path/to/thumbnail.jpg")
        }

        val result = mapper(offlineFileInformation, filterSvg = false)

        assertNotNull(result)
        assertEquals("video.mp4", result?.name)
        assertEquals(2048L, result?.size)
        assertEquals("/path/to/video.mp4", result?.fullSizePath)
    }

    @Test
    fun `test that null is returned when file type is folder`() = runBlocking {
        val offlineFileInformation = mock<OfflineFileInformation> {
            on { isFolder }.thenReturn(true)
            on { fileTypeInfo }.thenReturn(null)
            on { handle }.thenReturn("123")
            on { name }.thenReturn("folder")
            on { totalSize }.thenReturn(0)
            on { addedTime }.thenReturn(0L)
            on { absolutePath }.thenReturn("/path/to/folder")
            on { thumbnail }.thenReturn(null)
        }

        val result = mapper(offlineFileInformation, filterSvg = false)

        assertNull(result)
    }

    @Test
    fun `test that null is returned when file type is unavailable`() = runBlocking {
        val offlineFileInformation = mock<OfflineFileInformation> {
            on { isFolder }.thenReturn(false)
            on { fileTypeInfo }.thenReturn(null)
            on { handle }.thenReturn("123")
            on { name }.thenReturn("unknown")
            on { totalSize }.thenReturn(0L)
            on { addedTime }.thenReturn(0L)
            on { absolutePath }.thenReturn("/path/to/unknown")
            on { thumbnail }.thenReturn(null)
        }

        val result = mapper(offlineFileInformation, filterSvg = false)

        assertNull(result)
    }

    @Test
    fun `test that null is returned when file type not image or video`() = runBlocking {
        val offlineFileInformation = mock<OfflineFileInformation> {
            on { isFolder }.thenReturn(false)
            on { fileTypeInfo }.thenReturn(mock<PdfFileTypeInfo>())
            on { handle }.thenReturn("123")
            on { name }.thenReturn("filename.pdf")
            on { totalSize }.thenReturn(1024L)
            on { addedTime }.thenReturn(2000L)
            on { absolutePath }.thenReturn("/path/to/filename.pdf")
            on { thumbnail }.thenReturn(null)
        }

        val result = mapper(offlineFileInformation, filterSvg = false)

        assertNull(result)
    }
}