package test.mega.privacy.android.app.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.data.repository.DefaultPhotosRepository
import mega.privacy.android.app.presentation.favourites.facade.DateUtilWrapper
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.ImageMapper
import mega.privacy.android.data.mapper.NodeUpdateMapper
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.VideoMapper
import mega.privacy.android.data.mapper.sortOrderToInt
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.GifFileTypeInfo
import mega.privacy.android.domain.entity.RawFileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Photo
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

    private val megaApiGateway = mock<MegaApiGateway>()
    private val cacheFolderFacade = mock<CacheFolderGateway> {
        on { getCacheFolder(any()) }.thenReturn(null)
    }
    private val megaLocalStorageGateway = mock<MegaLocalStorageGateway>()
    private val dateUtilWrapper = mock<DateUtilWrapper> {
        on { fromEpoch(any()) }.thenReturn(LocalDateTime.now())
    }
    private val nodeUpdateMapper = mock<NodeUpdateMapper>()
    private val imageMapper: ImageMapper = ::createImage
    private val videoMapper: VideoMapper = ::createVideo
    private val fileTypeInfoMapper: FileTypeInfoMapper = ::mapFileTypeInfo
    private val sortOrderIntMapper: SortOrderIntMapper = ::sortOrderToInt

    @Before
    fun setUp() {
        underTest = DefaultPhotosRepository(
            megaApiFacade = megaApiGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
            cacheFolderFacade = cacheFolderFacade,
            megaLocalStorageFacade = megaLocalStorageGateway,
            dateUtilFacade = dateUtilWrapper,
            imageMapper = imageMapper,
            videoMapper = videoMapper,
            nodeUpdateMapper = nodeUpdateMapper,
            fileTypeInfoMapper = fileTypeInfoMapper,
            sortOrderIntMapper = sortOrderIntMapper,
        )
    }

    @Test
    fun `when file is image should return photo with static image file type`() = runTest {
        val nodeId = NodeId(1L)
        val megaNode = createMegaNode(handle = nodeId.id, name = "file.image")

        whenever(megaApiGateway.getMegaNodeByHandle(nodeHandle = nodeId.id))
            .thenReturn(megaNode)

        val actualPhoto = underTest.getPhotoFromNodeID(nodeId)
        assertThat(actualPhoto?.fileTypeInfo).isInstanceOf(StaticImageFileTypeInfo::class.java)
    }

    @Test
    fun `when file is gif should return photo with gif file type`() = runTest {
        val nodeId = NodeId(1L)
        val megaNode = createMegaNode(handle = nodeId.id, name = "file.gif")

        whenever(megaApiGateway.getMegaNodeByHandle(nodeHandle = nodeId.id))
            .thenReturn(megaNode)

        val actualPhoto = underTest.getPhotoFromNodeID(nodeId)
        assertThat(actualPhoto?.fileTypeInfo).isInstanceOf(GifFileTypeInfo::class.java)
    }

    @Test
    fun `when file is raw should return photo with raw file type`() = runTest {
        val nodeId = NodeId(1L)
        val megaNode = createMegaNode(handle = nodeId.id, name = "file.raw")

        whenever(megaApiGateway.getMegaNodeByHandle(nodeHandle = nodeId.id))
            .thenReturn(megaNode)

        val actualPhoto = underTest.getPhotoFromNodeID(nodeId)
        assertThat(actualPhoto?.fileTypeInfo).isInstanceOf(RawFileTypeInfo::class.java)
    }

    @Test
    fun `when file is video should return photo with video file type`() = runTest {
        val nodeId = NodeId(1L)
        val megaNode = createMegaNode(handle = nodeId.id, name = "file.video")

        whenever(megaApiGateway.getMegaNodeByHandle(nodeHandle = nodeId.id))
            .thenReturn(megaNode)

        val actualPhoto = underTest.getPhotoFromNodeID(nodeId)
        assertThat(actualPhoto?.fileTypeInfo).isInstanceOf(VideoFileTypeInfo::class.java)
    }

    @Test
    fun `when file is neither photo extension should return null result`() = runTest {
        val nodeId = NodeId(1L)
        val megaNode = createMegaNode(handle = nodeId.id, name = "file.xxx")

        whenever(megaApiGateway.getMegaNodeByHandle(nodeHandle = nodeId.id))
            .thenReturn(megaNode)

        val actualPhoto = underTest.getPhotoFromNodeID(nodeId)
        assertThat(actualPhoto?.fileTypeInfo == null)
    }

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
        parentId: Long,
        name: String,
        isFavourite: Boolean,
        creationTime: LocalDateTime,
        modificationTime: LocalDateTime,
        thumbnailFilePath: String?,
        previewFilePath: String?,
        fileTypeInfo: FileTypeInfo,
    ): Photo = Photo.Image(
        id,
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
        parentId: Long,
        name: String,
        isFavourite: Boolean,
        creationTime: LocalDateTime,
        modificationTime: LocalDateTime,
        thumbnailFilePath: String?,
        previewFilePath: String?,
        duration: Int,
        fileTypeInfo: FileTypeInfo,
    ): Photo = Photo.Video(
        id,
        parentId,
        name,
        isFavourite,
        creationTime,
        modificationTime,
        thumbnailFilePath,
        previewFilePath,
        duration,
        fileTypeInfo,
    )

    private fun mapFileTypeInfo(megaNode: MegaNode): FileTypeInfo {
        val name = megaNode.name
        return if (name.contains("image")) {
            StaticImageFileTypeInfo(type = "", extension = "image")
        } else if (name.contains("gif")) {
            GifFileTypeInfo(type = "", extension = "gif")
        } else if (name.contains("raw")) {
            return RawFileTypeInfo(type = "", extension = "raw")
        } else if (name.contains("video")) {
            return VideoFileTypeInfo(type = "", extension = "video")
        } else {
            return UnknownFileTypeInfo(type = "", extension = "")
        }
    }
}
