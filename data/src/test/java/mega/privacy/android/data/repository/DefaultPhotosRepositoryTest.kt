package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.ImageMapper
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.SortOrderIntMapperImpl
import mega.privacy.android.data.mapper.VideoMapper
import mega.privacy.android.data.wrapper.DateUtilWrapper
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.GifFileTypeInfo
import mega.privacy.android.domain.entity.RawFileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.PhotosRepository
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultPhotosRepositoryTest {
    private lateinit var underTest: PhotosRepository

    private val nodeRepository = mock<NodeRepository>()
    private val megaApiGateway = mock<MegaApiGateway>()
    private val cacheFolderFacade = mock<CacheFolderGateway> {
        on { getCacheFolder(any()) }.thenReturn(null)
    }
    private val megaLocalStorageGateway = mock<MegaLocalStorageGateway>()
    private val dateUtilWrapper = mock<DateUtilWrapper> {
        on { fromEpoch(any()) }.thenReturn(LocalDateTime.now())
    }
    private val imageMapper: ImageMapper = ::createImage
    private val videoMapper: VideoMapper = ::createVideo
    private val fileTypeInfoMapper: FileTypeInfoMapper = ::mapFileTypeInfo
    private val sortOrderIntMapper: SortOrderIntMapper = mock<SortOrderIntMapperImpl>()

    @Before
    fun setUp() {
        whenever(nodeRepository.monitorNodeUpdates())
            .thenReturn(flowOf())
    }

    @Test
    fun `when file is image should return photo with static image file type`() = runTest {
        val nodeId = NodeId(1L)
        val megaNode = createMegaNode(handle = nodeId.longValue, name = "file.image")

        whenever(megaApiGateway.getMegaNodeByHandle(nodeHandle = nodeId.longValue))
            .thenReturn(megaNode)

        underTest = createUnderTest(this)
        val actualPhoto = underTest.getPhotoFromNodeID(nodeId)
        assertThat(actualPhoto?.fileTypeInfo)
            .isInstanceOf(StaticImageFileTypeInfo::class.java)
    }

    @Test
    fun `when file is gif should return photo with gif file type`() = runTest {
        val nodeId = NodeId(1L)
        val megaNode = createMegaNode(handle = nodeId.longValue, name = "file.gif")

        whenever(megaApiGateway.getMegaNodeByHandle(nodeHandle = nodeId.longValue))
            .thenReturn(megaNode)

        underTest = createUnderTest(this)
        val actualPhoto = underTest.getPhotoFromNodeID(nodeId)
        assertThat(actualPhoto?.fileTypeInfo).isInstanceOf(GifFileTypeInfo::class.java)
    }

    @Test
    fun `when file is raw should return photo with raw file type`() = runTest {
        val nodeId = NodeId(1L)
        val megaNode = createMegaNode(handle = nodeId.longValue, name = "file.raw")

        whenever(megaApiGateway.getMegaNodeByHandle(nodeHandle = nodeId.longValue))
            .thenReturn(megaNode)

        underTest = createUnderTest(this)
        val actualPhoto = underTest.getPhotoFromNodeID(nodeId)
        assertThat(actualPhoto?.fileTypeInfo).isInstanceOf(RawFileTypeInfo::class.java)
    }

    @Test
    fun `when file is video should return photo with video file type`() = runTest {
        val nodeId = NodeId(1L)
        val megaNode = createMegaNode(handle = nodeId.longValue, name = "file.video")

        whenever(megaApiGateway.getMegaNodeByHandle(nodeHandle = nodeId.longValue))
            .thenReturn(megaNode)

        underTest = createUnderTest(this)
        val actualPhoto = underTest.getPhotoFromNodeID(nodeId)
        assertThat(actualPhoto?.fileTypeInfo).isInstanceOf(VideoFileTypeInfo::class.java)
    }

    @Test
    fun `when file is neither photo extension should return null result`() = runTest {
        val nodeId = NodeId(1L)
        val megaNode = createMegaNode(handle = nodeId.longValue, name = "file.xxx")

        whenever(megaApiGateway.getMegaNodeByHandle(nodeHandle = nodeId.longValue))
            .thenReturn(megaNode)

        underTest = createUnderTest(this)
        val actualPhoto = underTest.getPhotoFromNodeID(nodeId)
        assertThat(actualPhoto?.fileTypeInfo == null)
    }

    private fun createUnderTest(coroutineScope: CoroutineScope) = DefaultPhotosRepository(
        nodeRepository = nodeRepository,
        megaApiFacade = megaApiGateway,
        appScope = coroutineScope,
        ioDispatcher = UnconfinedTestDispatcher(),
        cacheFolderFacade = cacheFolderFacade,
        megaLocalStorageFacade = megaLocalStorageGateway,
        dateUtilFacade = dateUtilWrapper,
        imageMapper = imageMapper,
        videoMapper = videoMapper,
        fileTypeInfoMapper = fileTypeInfoMapper,
        sortOrderIntMapper = sortOrderIntMapper,
    )

    private fun createMegaNode(
        handle: Long = 0L,
        parentHandle: Long = 0L,
        name: String = "",
        isFavourite: Boolean = false,
        size: Long = 0L,
        duration: Int = 0,
    ): MegaNode = mock {
        on { this.handle }.thenReturn(handle)
        on { this.parentHandle }.thenReturn(parentHandle)
        on { this.name }.thenReturn(name)
        on { this.isFavourite }.thenReturn(isFavourite)
        on { this.size }.thenReturn(size)
        on { this.duration }.thenReturn(duration)
    }

    private fun createImage(
        id: Long,
        albumPhotoId: Long? = null,
        parentId: Long,
        name: String,
        isFavourite: Boolean,
        creationTime: LocalDateTime,
        modificationTime: LocalDateTime,
        thumbnailFilePath: String?,
        previewFilePath: String?,
        fileTypeInfo: FileTypeInfo,
    ): Photo.Image = Photo.Image(
        id,
        albumPhotoId,
        parentId,
        name,
        isFavourite,
        creationTime,
        modificationTime,
        thumbnailFilePath,
        previewFilePath,
        fileTypeInfo,
    )

    private fun createVideo(
        id: Long,
        albumPhotoId: Long? = null,
        parentId: Long,
        name: String,
        isFavourite: Boolean,
        creationTime: LocalDateTime,
        modificationTime: LocalDateTime,
        thumbnailFilePath: String?,
        previewFilePath: String?,
        fileTypeInfo: FileTypeInfo,
    ): Photo.Video = Photo.Video(
        id,
        albumPhotoId,
        parentId,
        name,
        isFavourite,
        creationTime,
        modificationTime,
        thumbnailFilePath,
        previewFilePath,
        fileTypeInfo as VideoFileTypeInfo,
    )

    private fun mapFileTypeInfo(megaNode: MegaNode): FileTypeInfo {
        val name = megaNode.name
        return if (name.contains("image")) {
            StaticImageFileTypeInfo(mimeType = "", extension = "image")
        } else if (name.contains("gif")) {
            GifFileTypeInfo(mimeType = "", extension = "gif")
        } else if (name.contains("raw")) {
            return RawFileTypeInfo(mimeType = "", extension = "raw")
        } else if (name.contains("video")) {
            return VideoFileTypeInfo(mimeType = "", extension = "video", duration = 120)
        } else {
            return UnknownFileTypeInfo(mimeType = "", extension = "")
        }
    }
}
