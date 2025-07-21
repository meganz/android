package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.wrapper.DateUtilWrapper
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.thumbnailpreview.ThumbnailPreviewRepository
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PhotoMapperTest {
    private lateinit var underTest: PhotoMapper

    private val imageMapper: ImageMapper = ::toImage
    private val videoMapper: VideoMapper = ::toVideo
    private val fileTypeInfoMapper: FileTypeInfoMapper = mock()
    private val dateUtilWrapper: DateUtilWrapper = mock()
    private val thumbnailPreviewRepository: ThumbnailPreviewRepository = mock()
    private val megaApiGateway: MegaApiGateway = mock()

    private val testDateTime = LocalDateTime.of(2023, 1, 1, 12, 0, 0)
    private val testThumbnailPath = "/cache/thumbnails"
    private val testPreviewPath = "/cache/previews"
    private val testNodeHandle = 123456L
    private val testParentHandle = 789L
    private val testNodeName = "test_image.jpg"
    private val testBase64Handle = "test_base64_handle"
    private val testThumbnailFileName = testBase64Handle
    private val testPreviewFileName = "$testBase64Handle.jpg"

    @BeforeAll
    fun setUp() {
        underTest = PhotoMapper(
            imageMapper = imageMapper,
            videoMapper = videoMapper,
            fileTypeInfoMapper = fileTypeInfoMapper,
            dateUtilFacade = dateUtilWrapper,
            thumbnailPreviewRepository = thumbnailPreviewRepository,
            megaApiGateway = megaApiGateway
        )

        whenever(dateUtilWrapper.fromEpoch(any())).thenReturn(testDateTime)
        runBlocking {
            whenever(thumbnailPreviewRepository.getThumbnailCacheFolderPath()).thenReturn(
                testThumbnailPath
            )
            whenever(thumbnailPreviewRepository.getPreviewCacheFolderPath()).thenReturn(
                testPreviewPath
            )
            whenever(megaApiGateway.isSensitiveInherited(any())).thenReturn(false)
        }
    }

    @Test
    fun `test that image node is mapped correctly`() = runTest {
        val imageFileType = StaticImageFileTypeInfo(
            mimeType = "image/jpeg",
            extension = "jpg"
        )
        val testId = 999L
        val testNodeId = NodeId(testId)
        val testAlbumId = AlbumId(testId)
        val albumPhotoId = AlbumPhotoId(testId, testNodeId, testAlbumId)
        val megaNode = createMockMegaNode(
            handle = testNodeHandle,
            parentHandle = testParentHandle,
            name = testNodeName,
            isFavourite = true,
            isTakenDown = false,
            isMarkedSensitive = true,
            size = 1024L,
            creationTime = 1640995200L,
            modificationTime = 1640998800L,
            base64Handle = testBase64Handle,
        )

        whenever(fileTypeInfoMapper(testNodeName, 0)).thenReturn(imageFileType)
        val result = underTest(megaNode, albumPhotoId)

        assertThat(result).isInstanceOf(Photo.Image::class.java)
        val imagePhoto = result as Photo.Image

        assertPhotoProperties(
            photo = imagePhoto,
            expectedAlbumPhotoId = albumPhotoId.id,
            expectedName = testNodeName,
            expectedIsFavourite = true,
            expectedCreationTime = testDateTime,
            expectedModificationTime = testDateTime,
            expectedThumbnailPath = "$testThumbnailPath${File.separator}$testThumbnailFileName",
            expectedPreviewPath = "$testPreviewPath${File.separator}$testPreviewFileName",
            expectedFileTypeInfo = imageFileType,
            expectedSize = 1024L,
            expectedIsSensitive = true
        )

        verify(fileTypeInfoMapper).invoke(testNodeName, 0)
        verify(dateUtilWrapper, times(2)).fromEpoch(1640995200L)
        verify(megaApiGateway).isSensitiveInherited(megaNode)
    }

    @Test
    fun `test that video node is mapped correctly`() = runTest {
        val videoFileType = VideoFileTypeInfo(
            extension = "mp4",
            mimeType = "video/mp4",
            duration = 120.seconds
        )
        val megaNode = createMockMegaNode(
            handle = testNodeHandle,
            parentHandle = testParentHandle,
            name = "test_video.mp4",
            isFavourite = false,
            isTakenDown = false,
            isMarkedSensitive = false,
            size = 2048L,
            creationTime = 1640995200L,
            modificationTime = 1640998800L,
            base64Handle = testBase64Handle,
            duration = 120
        )

        whenever(fileTypeInfoMapper("test_video.mp4", 120)).thenReturn(videoFileType)

        val result = underTest(megaNode, null)

        assertThat(result).isInstanceOf(Photo.Video::class.java)
        val videoPhoto = result as Photo.Video

        assertPhotoProperties(
            photo = videoPhoto,
            expectedAlbumPhotoId = null,
            expectedName = "test_video.mp4",
            expectedIsFavourite = false,
            expectedCreationTime = testDateTime,
            expectedModificationTime = testDateTime,
            expectedThumbnailPath = "$testThumbnailPath${File.separator}$testThumbnailFileName",
            expectedPreviewPath = "$testPreviewPath${File.separator}$testPreviewFileName",
            expectedFileTypeInfo = videoFileType,
            expectedSize = 2048L,
            expectedIsSensitive = false,
        )

        verify(fileTypeInfoMapper).invoke("test_video.mp4", 120)
    }

    @Test
    fun `test that non-media node returns null`() = runTest {
        val megaNode = createMockMegaNode(
            handle = testNodeHandle,
            name = "document.pdf"
        )

        whenever(fileTypeInfoMapper("document.pdf", 0)).thenReturn(mock<PdfFileTypeInfo>())

        val result = underTest(megaNode, null)

        assertThat(result).isNull()
        verify(fileTypeInfoMapper).invoke("document.pdf", 0)
    }

    @Test
    fun `test that null thumbnail path returns null thumbnail file path`() = runTest {
        val imageFileType = StaticImageFileTypeInfo(
            mimeType = "image/jpeg",
            extension = "jpg"
        )
        val megaNode = createMockMegaNode(
            handle = testNodeHandle,
            name = testNodeName,
        )

        whenever(fileTypeInfoMapper(testNodeName, 0)).thenReturn(imageFileType)
        whenever(thumbnailPreviewRepository.getThumbnailCacheFolderPath()).thenReturn(null)
        whenever(thumbnailPreviewRepository.getPreviewCacheFolderPath()).thenReturn(null)

        val result = underTest(megaNode, null)

        assertThat(result).isInstanceOf(Photo.Image::class.java)
        val imagePhoto = result as Photo.Image

        assertAll(
            { assertThat(imagePhoto.thumbnailFilePath).isNull() },
            { assertThat(imagePhoto.previewFilePath).isNull() }
        )
    }

    @Test
    fun `test that sensitive inherited is mapped correctly`() = runTest {
        val imageFileType = StaticImageFileTypeInfo(
            mimeType = "image/jpeg",
            extension = "jpg"
        )
        val megaNode = createMockMegaNode(
            handle = testNodeHandle,
            name = testNodeName,
            isMarkedSensitive = false
        )

        whenever(fileTypeInfoMapper(testNodeName, 0)).thenReturn(imageFileType)
        whenever(megaApiGateway.isSensitiveInherited(megaNode)).thenReturn(true)

        val result = underTest(megaNode, null)

        assertThat(result).isInstanceOf(Photo.Image::class.java)
        val imagePhoto = result as Photo.Image

        assertAll(
            { assertThat(imagePhoto.isSensitive).isFalse() },
            { assertThat(imagePhoto.isSensitiveInherited).isTrue() }
        )
    }

    private fun createMockMegaNode(
        handle: Long = testNodeHandle,
        parentHandle: Long = testParentHandle,
        name: String = testNodeName,
        isFavourite: Boolean = false,
        isTakenDown: Boolean = false,
        isMarkedSensitive: Boolean = false,
        size: Long = 0L,
        creationTime: Long = 1640995200L,
        modificationTime: Long = 1640998800L,
        base64Handle: String = testBase64Handle,
        duration: Int = 0,
    ): MegaNode = mock {
        on { this.handle }.thenReturn(handle)
        on { this.parentHandle }.thenReturn(parentHandle)
        on { this.name }.thenReturn(name)
        on { this.isFavourite }.thenReturn(isFavourite)
        on { this.isTakenDown }.thenReturn(isTakenDown)
        on { this.isMarkedSensitive }.thenReturn(isMarkedSensitive)
        on { this.size }.thenReturn(size)
        on { this.creationTime }.thenReturn(creationTime)
        on { this.modificationTime }.thenReturn(modificationTime)
        on { this.base64Handle }.thenReturn(base64Handle)
        on { this.duration }.thenReturn(duration)
    }

    private fun assertPhotoProperties(
        photo: Photo,
        expectedAlbumPhotoId: Long?,
        expectedName: String,
        expectedIsFavourite: Boolean,
        expectedCreationTime: LocalDateTime,
        expectedModificationTime: LocalDateTime,
        expectedThumbnailPath: String?,
        expectedPreviewPath: String?,
        expectedFileTypeInfo: Any,
        expectedSize: Long,
        expectedIsSensitive: Boolean,
    ) {
        assertAll(
            { assertThat(photo.id).isEqualTo(testNodeHandle) },
            { assertThat(photo.albumPhotoId).isEqualTo(expectedAlbumPhotoId) },
            { assertThat(photo.parentId).isEqualTo(testParentHandle) },
            { assertThat(photo.name).isEqualTo(expectedName) },
            { assertThat(photo.isFavourite).isEqualTo(expectedIsFavourite) },
            { assertThat(photo.creationTime).isEqualTo(expectedCreationTime) },
            { assertThat(photo.modificationTime).isEqualTo(expectedModificationTime) },
            { assertThat(photo.thumbnailFilePath).isEqualTo(expectedThumbnailPath) },
            { assertThat(photo.previewFilePath).isEqualTo(expectedPreviewPath) },
            { assertThat(photo.fileTypeInfo).isEqualTo(expectedFileTypeInfo) },
            { assertThat(photo.size).isEqualTo(expectedSize) },
            { assertThat(photo.isTakenDown).isFalse() },
            { assertThat(photo.isSensitive).isEqualTo(expectedIsSensitive) },
            { assertThat(photo.isSensitiveInherited).isFalse() },
            { assertThat(photo.base64Id).isEqualTo(testBase64Handle) }
        )
    }
} 