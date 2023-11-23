package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.gateway.preferences.CameraUploadsSettingsPreferenceGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.ImageMapper
import mega.privacy.android.data.mapper.VideoMapper
import mega.privacy.android.data.mapper.photos.ContentConsumptionMegaStringMapMapper
import mega.privacy.android.data.mapper.photos.TimelineFilterPreferencesJSONMapper
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
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaStringMap
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultPhotosRepositoryTest {
    private lateinit var underTest: PhotosRepository

    private val nodeRepository = mock<NodeRepository>()
    private val megaApiGateway = mock<MegaApiGateway>()
    private val megaApiFolder = mock<MegaApiFolderGateway>()
    private val megaChatApiGateway = mock<MegaChatApiGateway>()
    private val cacheGateway = mock<CacheGateway> {
        onBlocking { getOrCreateCacheFolder(any()) }.thenReturn(null)
    }
    private val fileGateway = mock<FileGateway>()
    private val megaLocalStorageGateway = mock<MegaLocalStorageGateway>()
    private val dateUtilWrapper = mock<DateUtilWrapper> {
        on { fromEpoch(any()) }.thenReturn(LocalDateTime.now())
    }
    private val imageMapper: ImageMapper = ::createImage
    private val videoMapper: VideoMapper = ::createVideo
    private val fileTypeInfoMapper: FileTypeInfoMapper = ::mapFileTypeInfo
    private val timelineFilterPreferencesJSONMapper: TimelineFilterPreferencesJSONMapper = mock()
    private val contentConsumptionMegaStringMapMapper: ContentConsumptionMegaStringMapMapper =
        mock()

    private val cameraUploadsSettingsPreferenceGateway =
        mock<CameraUploadsSettingsPreferenceGateway>()

    private val success = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }

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

    @Test
    fun `test that getpreferences returns null if android settings doesnt exist`() = runTest {
        val nullRequest = mock<MegaRequest> {
            on { type }.thenReturn(MegaRequest.TYPE_GET_ATTR_USER)
            on { paramType }.thenReturn(MegaApiJava.USER_ATTR_CC_PREFS)
            on { megaStringMap }.thenReturn(null)
        }
        whenever(megaApiGateway.getUserAttribute(eq(MegaApiJava.USER_ATTR_CC_PREFS), any()))
            .thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), nullRequest, success
                )
            }

        underTest = createUnderTest(this)
        assertThat(underTest.getTimelineFilterPreferences()).isNull()
    }

    @Test
    fun `test that getpreferences returns the right preferences`() = runTest {
        underTest = createUnderTest(this)

        val expectedPrefStringMap = mock<MegaStringMap>()
        expectedPrefStringMap["cc"] = "abc"

        val expectedResult = timelineFilterPreferencesJSONMapper("abc")

        val request = mock<MegaRequest> {
            on { type }.thenReturn(MegaRequest.TYPE_GET_ATTR_USER)
            on { paramType }.thenReturn(MegaApiJava.USER_ATTR_CC_PREFS)
            on { megaStringMap }.thenReturn(expectedPrefStringMap)
        }
        whenever(megaApiGateway.getUserAttribute(eq(MegaApiJava.USER_ATTR_CC_PREFS), any()))
            .thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), request, success
                )
            }

        val actualPrefStringMap = underTest.getTimelineFilterPreferences()
        assertThat(actualPrefStringMap).isEqualTo(expectedResult)
    }

    @Test
    fun `test that setpreferences give the right value`() = runTest {
        underTest = createUnderTest(this)

        val expectedMegaStringMapValue = mapOf(Pair("abc", "def"))
        val expectedPrefStringMap = mock<MegaStringMap>()
        val valueToPut = mock<MegaStringMap>()
        whenever(expectedPrefStringMap.size()).thenReturn(1)
        whenever(expectedPrefStringMap.get("cc")).thenReturn(expectedMegaStringMapValue.toString())

        val getRequest = mock<MegaRequest> {
            on { type }.thenReturn(MegaRequest.TYPE_GET_ATTR_USER)
            on { paramType }.thenReturn(MegaApiJava.USER_ATTR_CC_PREFS)
            on { megaStringMap }.thenReturn(expectedPrefStringMap)
        }
        whenever(megaApiGateway.getUserAttribute(eq(MegaApiJava.USER_ATTR_CC_PREFS), any()))
            .thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), getRequest, success
                )
            }

        whenever(
            contentConsumptionMegaStringMapMapper(
                expectedPrefStringMap,
                expectedMegaStringMapValue
            )
        ).thenReturn(valueToPut)

        val request = mock<MegaRequest> {
            on { type }.thenReturn(MegaRequest.TYPE_GET_ATTR_USER)
            on { paramType }.thenReturn(MegaApiJava.USER_ATTR_CC_PREFS)
            on { megaStringMap }.thenReturn(expectedPrefStringMap)
        }
        whenever(
            megaApiGateway.setUserAttribute(
                eq(MegaApiJava.USER_ATTR_CC_PREFS),
                any<MegaStringMap>(),
                any()
            )
        ).thenAnswer {
            ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(), request, success
            )
        }

        assertThat(underTest.setTimelineFilterPreferences(expectedMegaStringMapValue))
            .isEqualTo(expectedMegaStringMapValue.toString())
    }

    private fun createUnderTest(coroutineScope: CoroutineScope) = DefaultPhotosRepository(
        nodeRepository = nodeRepository,
        megaApiFacade = megaApiGateway,
        megaApiFolder = megaApiFolder,
        appScope = coroutineScope,
        ioDispatcher = UnconfinedTestDispatcher(),
        cacheGateway = cacheGateway,
        fileGateway = fileGateway,
        megaLocalStorageFacade = megaLocalStorageGateway,
        dateUtilFacade = dateUtilWrapper,
        imageMapper = imageMapper,
        videoMapper = videoMapper,
        fileTypeInfoMapper = fileTypeInfoMapper,
        megaChatApiGateway = megaChatApiGateway,
        timelineFilterPreferencesJSONMapper = timelineFilterPreferencesJSONMapper,
        contentConsumptionMegaStringMapMapper = contentConsumptionMegaStringMapMapper,
        imageNodeMapper = mock(),
        megaLocalRoomGateway = mock(),
        cameraUploadsSettingsPreferenceGateway = cameraUploadsSettingsPreferenceGateway,
        sortOrderIntMapper = mock(),
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
        size: Long,
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
        size,
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
        size: Long,
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
        size,
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
