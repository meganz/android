package mega.privacy.android.data.repository.photos

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.gateway.preferences.CameraUploadsSettingsPreferenceGateway
import mega.privacy.android.data.gateway.preferences.MediaTimelinePreferencesGateway
import mega.privacy.android.data.gateway.preferences.UIPreferencesGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.PhotoMapper
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.node.ImageNodeFileMapper
import mega.privacy.android.data.mapper.node.ImageNodeMapper
import mega.privacy.android.data.mapper.node.MegaNodeFromChatMessageMapper
import mega.privacy.android.data.mapper.node.MegaNodeMapper
import mega.privacy.android.data.mapper.node.TypedFileNodeToImageNodeMapper
import mega.privacy.android.data.mapper.node.TypedNodeMapper
import mega.privacy.android.data.mapper.photos.ContentConsumptionMegaStringMapMapper
import mega.privacy.android.data.mapper.photos.MediaTimelineFilterMapper
import mega.privacy.android.data.mapper.photos.MediaTimelineListFilterMapper
import mega.privacy.android.data.mapper.photos.MediaTimelineSectionMapper
import mega.privacy.android.data.mapper.photos.MegaStringMapSensitivesMapper
import mega.privacy.android.data.mapper.photos.MegaStringMapSensitivesRetriever
import mega.privacy.android.data.mapper.photos.TimelineFilterPreferencesJSONMapper
import mega.privacy.android.data.mapper.search.MegaSearchFilterMapper
import mega.privacy.android.data.mapper.search.MegaSearchPageMapper
import mega.privacy.android.data.repository.CancelTokenProvider
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.GifFileTypeInfo
import mega.privacy.android.domain.entity.RawFileTypeInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.media.MediaTimelineFilter
import mega.privacy.android.domain.entity.media.MediaTimelineSection
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchTarget
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.PhotosRepository
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinishUseCase
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaDateSectionList
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaGroupNodesByDateFilter
import nz.mega.sdk.MegaListAllNodesFilter
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaSearchFilter
import nz.mega.sdk.MegaStringMap
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultPhotosRepositoryTest {
    private lateinit var underTest: PhotosRepository

    private val nodeRepository = mock<NodeRepository>()
    private val megaApiGateway = mock<MegaApiGateway> {
        onBlocking { isSensitiveInherited(any()) }.thenReturn(false)
    }
    private val megaApiFolder = mock<MegaApiFolderGateway>()
    private val megaChatApiGateway = mock<MegaChatApiGateway>()
    private val fileGateway = mock<FileGateway>()
    private val fileTypeInfoMapper: FileTypeInfoMapper = mock()
    private val imageNodeFileMapper: ImageNodeFileMapper = mock()
    private val imageNodeMapper: ImageNodeMapper = mock()
    private val megaNodeFromChatMessageMapper: MegaNodeFromChatMessageMapper = mock()
    private val megaNodeMapper: MegaNodeMapper = mock()
    private val timelineFilterPreferencesJSONMapper: TimelineFilterPreferencesJSONMapper = mock()
    private val contentConsumptionMegaStringMapMapper: ContentConsumptionMegaStringMapMapper =
        mock()
    private val megaStringMapSensitivesMapper: MegaStringMapSensitivesMapper = mock()
    private val megaStringMapSensitivesRetriever: MegaStringMapSensitivesRetriever = mock()

    private val cameraUploadsSettingsPreferenceGateway =
        mock<CameraUploadsSettingsPreferenceGateway>()

    private val success = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }
    private val cancelTokenProvider = mock<CancelTokenProvider>()
    private val megaSearchFilterMapper = mock<MegaSearchFilterMapper>()
    private val megaSearchPageMapper = mock<MegaSearchPageMapper>()
    private val monitorFetchNodesFinishUseCase = mock<MonitorFetchNodesFinishUseCase> {
        onBlocking { invoke() }.thenReturn(emptyFlow())
    }
    private val uiPreferencesGateway = mock<UIPreferencesGateway>()
    private val mediaTimelinePreferencesGateway = mock<MediaTimelinePreferencesGateway>()
    private val photoMapper = mock<PhotoMapper>()
    private val typedNodeMapper = mock<TypedNodeMapper>()
    private val typedFileNodeToImageNodeMapper = mock<TypedFileNodeToImageNodeMapper>()
    private val sortOrderIntMapper = mock<SortOrderIntMapper>()
    private val mediaTimelineSectionMapper = mock<MediaTimelineSectionMapper>()
    private val mediaTimelineFilterMapper = mock<MediaTimelineFilterMapper>()
    private val mediaTimelineListFilterMapper = mock<MediaTimelineListFilterMapper>()
    private val ioDispatcher = UnconfinedTestDispatcher()
    private val appScope: CoroutineScope = CoroutineScope(UnconfinedTestDispatcher())

    private val mockBase64Id = "mockBase64Id"
    private val defaultVideoType =
        VideoFileTypeInfo(mimeType = "", extension = "video", duration = 120.seconds)
    private val defaultImageType = StaticImageFileTypeInfo(mimeType = "", extension = "image")

    private val mediaTimelineFilter = MediaTimelineFilter(
        granularity = MediaTimelineFilter.Granularity.Day,
        category = MediaTimelineFilter.Category.All,
        location = MediaTimelineFilter.Location.CloudDriveAndVault,
        sensitivity = MediaTimelineFilter.Sensitivity.ShowAll,
    )

    @Before
    fun setUp() {
        whenever(nodeRepository.monitorNodeUpdates())
            .thenReturn(flowOf())

        whenever(nodeRepository.monitorOfflineNodeUpdates())
            .thenReturn(flowOf())
    }

    @Test
    fun `when file is image should return photo with static image file type`() = runTest {
        val nodeId = NodeId(1L)
        val megaNode = createMegaNode(handle = nodeId.longValue, name = "file.image")

        whenever(megaApiGateway.getMegaNodeByHandle(nodeHandle = nodeId.longValue))
            .thenReturn(megaNode)

        whenever(megaApiGateway.isInRubbish(any()))
            .thenReturn(false)

        whenever(nodeRepository.isNodeInRubbishBin(NodeId(any())))
            .thenReturn(false)
        val fileType = StaticImageFileTypeInfo(mimeType = "", extension = "image")
        whenever(fileTypeInfoMapper(megaNode.name, megaNode.duration)).thenReturn(
            fileType
        )
        val image = mock<Photo.Image> {
            on { fileTypeInfo }.thenReturn(fileType)
        }
        whenever(
            photoMapper(
                node = megaNode,
                albumPhotoId = null,
                requireSerializedData = false,
                isAvailableOffline = false
            )
        ) doReturn image

        underTest = createUnderTest()
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

        whenever(megaApiGateway.isInRubbish(any()))
            .thenReturn(false)

        whenever(nodeRepository.isNodeInRubbishBin(NodeId(any())))
            .thenReturn(false)
        val fileType = GifFileTypeInfo(mimeType = "", extension = "gif")
        whenever(fileTypeInfoMapper(megaNode.name, megaNode.duration)).thenReturn(
            fileType
        )
        val image = mock<Photo.Image> {
            on { fileTypeInfo }.thenReturn(fileType)
        }
        whenever(
            photoMapper(
                node = megaNode,
                albumPhotoId = null,
                requireSerializedData = false,
                isAvailableOffline = false
            )
        ) doReturn image

        underTest = createUnderTest()
        val actualPhoto = underTest.getPhotoFromNodeID(nodeId)
        assertThat(actualPhoto?.fileTypeInfo).isInstanceOf(GifFileTypeInfo::class.java)
    }

    @Test
    fun `when file is raw should return photo with raw file type`() = runTest {
        val nodeId = NodeId(1L)
        val megaNode = createMegaNode(handle = nodeId.longValue, name = "file.raw")

        whenever(megaApiGateway.getMegaNodeByHandle(nodeHandle = nodeId.longValue))
            .thenReturn(megaNode)

        whenever(nodeRepository.isNodeInRubbishBin(NodeId(any())))
            .thenReturn(false)
        val fileType = RawFileTypeInfo(mimeType = "", extension = "raw")
        whenever(fileTypeInfoMapper(megaNode.name, megaNode.duration)).thenReturn(
            fileType
        )
        val image = mock<Photo.Image> {
            on { fileTypeInfo }.thenReturn(fileType)
        }
        whenever(
            photoMapper(
                node = megaNode,
                albumPhotoId = null,
                requireSerializedData = false,
                isAvailableOffline = false
            )
        ) doReturn image

        underTest = createUnderTest()
        val actualPhoto = underTest.getPhotoFromNodeID(nodeId)
        assertThat(actualPhoto?.fileTypeInfo).isInstanceOf(RawFileTypeInfo::class.java)
    }

    @Test
    fun `when file is video should return photo with video file type`() = runTest {
        val nodeId = NodeId(1L)
        val megaNode = createMegaNode(handle = nodeId.longValue, name = "file.video")

        whenever(megaApiGateway.getMegaNodeByHandle(nodeHandle = nodeId.longValue))
            .thenReturn(megaNode)

        whenever(megaApiGateway.isInRubbish(any()))
            .thenReturn(false)

        whenever(nodeRepository.isNodeInRubbishBin(NodeId(any())))
            .thenReturn(false)
        val fileType = VideoFileTypeInfo(mimeType = "", extension = "video", duration = 120.seconds)
        whenever(fileTypeInfoMapper(megaNode.name, megaNode.duration)).thenReturn(
            fileType
        )
        val image = mock<Photo.Image> {
            on { fileTypeInfo }.thenReturn(fileType)
        }
        whenever(
            photoMapper(
                node = megaNode,
                albumPhotoId = null,
                requireSerializedData = false,
                isAvailableOffline = false
            )
        ) doReturn image

        underTest = createUnderTest()
        val actualPhoto = underTest.getPhotoFromNodeID(nodeId)
        assertThat(actualPhoto?.fileTypeInfo).isInstanceOf(VideoFileTypeInfo::class.java)
    }

    @Test
    fun `when file is neither photo extension should return null result`() = runTest {
        val nodeId = NodeId(1L)
        val megaNode = createMegaNode(handle = nodeId.longValue, name = "file.xxx")

        whenever(megaApiGateway.getMegaNodeByHandle(nodeHandle = nodeId.longValue))
            .thenReturn(megaNode)

        underTest = createUnderTest()
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

        underTest = createUnderTest()
        assertThat(underTest.getTimelineFilterPreferences()).isNull()
    }

    @Test
    fun `test that getpreferences returns the right preferences`() = runTest {
        underTest = createUnderTest()

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
        underTest = createUnderTest()

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

    @Test
    fun `test that clearImageResult by node id evicts an uncompleted cached result`() = runTest {
        underTest = createUnderTest()
        val nodeId = NodeId(1L)
        underTest.saveImageResult(nodeId, ImageResult(isFullyLoaded = false))
        assertThat(underTest.monitorImageResult(nodeId)).isNotNull()

        underTest.clearImageResult(nodeId)

        assertThat(underTest.monitorImageResult(nodeId)).isNull()
    }

    @Test
    fun `test that clearImageResult by node id does nothing when the node is not cached`() =
        runTest {
            underTest = createUnderTest()

            // Should not throw for an unknown node.
            underTest.clearImageResult(NodeId(99L))
        }

    @Test
    fun `test that getMediaTimelineSections returns the sections mapped from the gateway result`() =
        runTest {
            val sdkFilter = mock<MegaGroupNodesByDateFilter>()
            val sectionList = mock<MegaDateSectionList>()
            val sections = listOf(
                MediaTimelineSection(
                    groupId = "May 2026",
                    startDate = 100L,
                    endDate = 200L,
                    count = 5L,
                ),
            )
            whenever(mediaTimelineFilterMapper(mediaTimelineFilter)).thenReturn(sdkFilter)
            whenever(
                megaApiGateway.groupAllNodesByDate(eq(sdkFilter), any(), anyOrNull())
            ).thenReturn(sectionList)
            whenever(mediaTimelineSectionMapper(sectionList)).thenReturn(sections)
            underTest = createUnderTest()

            val result = mockStatic(MegaCancelToken::class.java).use { mockedStatic ->
                mockedStatic.`when`<MegaCancelToken> { MegaCancelToken.createInstance() }
                    .thenReturn(mock())
                underTest.getMediaTimelineSections(mediaTimelineFilter)
            }

            assertThat(result).isEqualTo(sections)
        }

    @Test
    fun `test that getMediaTimelineSections returns an empty list when the gateway returns null`() =
        runTest {
            whenever(mediaTimelineFilterMapper(mediaTimelineFilter))
                .thenReturn(mock<MegaGroupNodesByDateFilter>())
            whenever(
                megaApiGateway.groupAllNodesByDate(any(), any(), anyOrNull())
            ).thenReturn(null)
            underTest = createUnderTest()

            val result = mockStatic(MegaCancelToken::class.java).use { mockedStatic ->
                mockedStatic.`when`<MegaCancelToken> { MegaCancelToken.createInstance() }
                    .thenReturn(mock())
                underTest.getMediaTimelineSections(mediaTimelineFilter)
            }

            assertThat(result).isEmpty()
            verify(mediaTimelineSectionMapper, never()).invoke(any())
        }

    @Test
    fun `test that listMediaNodesByPage returns the mapped file nodes and skips nodes that fail to map`() =
        runTest {
            val sdkFilter = mock<MegaListAllNodesFilter>()
            val nodeList = mock<MegaNodeList>()
            val mappedMegaNode = createMegaNode(handle = 1L)
            val unmappableMegaNode = createMegaNode(handle = 2L)
            val fileNode = mock<TypedFileNode>()
            val section = MediaTimelineSection(
                groupId = "May 2026",
                startDate = 10L,
                endDate = 99L,
                count = 5L,
            )
            whenever(mediaTimelineListFilterMapper(mediaTimelineFilter)).thenReturn(sdkFilter)
            whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(mock())
            whenever(
                megaApiGateway.listAllNodesByPageAtOffset(
                    filter = eq(sdkFilter),
                    order = any(),
                    cancelToken = anyOrNull(),
                    maxElements = any(),
                    offset = any(),
                )
            ).thenReturn(nodeList)
            whenever(megaApiGateway.getNodesFromMegaNodeList(nodeList))
                .thenReturn(listOf(mappedMegaNode, unmappableMegaNode))
            whenever(
                typedNodeMapper(eq(mappedMegaNode), anyOrNull(), anyOrNull(), any(), any())
            ).thenReturn(fileNode)
            whenever(
                typedNodeMapper(eq(unmappableMegaNode), anyOrNull(), anyOrNull(), any(), any())
            ).thenReturn(null)
            underTest = createUnderTest()

            val result = underTest.listMediaNodesByPage(
                filter = mediaTimelineFilter,
                section = section,
                order = SortOrder.ORDER_MODIFICATION_DESC,
                maxElements = 5,
                offset = 0L,
            )

            assertThat(result).containsExactly(fileNode)
        }

    @Test
    fun `test that listMediaNodesByPage returns an empty list when the gateway returns null`() =
        runTest {
            val section = MediaTimelineSection(
                groupId = "May 2026",
                startDate = 10L,
                endDate = 99L,
                count = 5L,
            )
            whenever(mediaTimelineListFilterMapper(mediaTimelineFilter))
                .thenReturn(mock<MegaListAllNodesFilter>())
            whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(mock())
            whenever(
                megaApiGateway.listAllNodesByPageAtOffset(any(), any(), anyOrNull(), any(), any())
            ).thenReturn(null)
            underTest = createUnderTest()

            val result = underTest.listMediaNodesByPage(
                filter = mediaTimelineFilter,
                section = section,
                order = SortOrder.ORDER_MODIFICATION_DESC,
                maxElements = 5,
                offset = 0L,
            )

            assertThat(result).isEmpty()
        }

    @Test
    fun `test that listMediaNodesByPage anchors the query to the section date range`() = runTest {
        val sdkFilter = mock<MegaListAllNodesFilter>()
        val section = MediaTimelineSection(
            groupId = "May 2026",
            startDate = 10L,
            endDate = 99L,
            count = 5L,
        )
        whenever(mediaTimelineListFilterMapper(mediaTimelineFilter)).thenReturn(sdkFilter)
        whenever(sortOrderIntMapper(SortOrder.ORDER_MODIFICATION_DESC)).thenReturn(8)
        whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(mock())
        whenever(
            megaApiGateway.listAllNodesByPageAtOffset(any(), any(), anyOrNull(), any(), any())
        ).thenReturn(null)
        underTest = createUnderTest()

        underTest.listMediaNodesByPage(
            filter = mediaTimelineFilter,
            section = section,
            order = SortOrder.ORDER_MODIFICATION_DESC,
            maxElements = 5,
            offset = 0L,
        )

        verify(sdkFilter).byTimestampAnchor(section.startDate, section.endDate, 8)
    }

    private fun createUnderTest() = DefaultPhotosRepository(
        nodeRepository = nodeRepository,
        megaApiFacade = megaApiGateway,
        megaApiFolder = megaApiFolder,
        appScope = appScope,
        ioDispatcher = ioDispatcher,
        fileGateway = fileGateway,
        fileTypeInfoMapper = fileTypeInfoMapper,
        imageNodeFileMapper = imageNodeFileMapper,
        megaChatApiGateway = megaChatApiGateway,
        timelineFilterPreferencesJSONMapper = timelineFilterPreferencesJSONMapper,
        contentConsumptionMegaStringMapMapper = contentConsumptionMegaStringMapMapper,
        imageNodeMapper = imageNodeMapper,
        megaNodeFromChatMessageMapper = megaNodeFromChatMessageMapper,
        cameraUploadsSettingsPreferenceGateway = cameraUploadsSettingsPreferenceGateway,
        sortOrderIntMapper = sortOrderIntMapper,
        megaNodeMapper = megaNodeMapper,
        sensitivesMapper = megaStringMapSensitivesMapper,
        sensitivesRetriever = megaStringMapSensitivesRetriever,
        cancelTokenProvider = cancelTokenProvider,
        megaSearchFilterMapper = megaSearchFilterMapper,
        megaSearchPageMapper = megaSearchPageMapper,
        monitorFetchNodesFinishUseCase = monitorFetchNodesFinishUseCase,
        uiPreferencesGateway = uiPreferencesGateway,
        mediaTimelinePreferencesGateway = mediaTimelinePreferencesGateway,
        photoMapper = photoMapper,
        typedNodeMapper = typedNodeMapper,
        typedFileNodeToImageNodeMapper = typedFileNodeToImageNodeMapper,
        mediaTimelineSectionMapper = mediaTimelineSectionMapper,
        mediaTimelineFilterMapper = mediaTimelineFilterMapper,
        mediaTimelineListFilterMapper = mediaTimelineListFilterMapper,
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
        on { this.isFile }.thenReturn(true)
        on { this.hasThumbnail() }.thenReturn(true)
        on { base64Handle }.thenReturn(mockBase64Id)
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
        isTakenDown: Boolean,
        isSensitive: Boolean,
        isSensitiveInherited: Boolean,
        base64Id: String,
    ): Photo.Image = Photo.Image(
        id = id,
        albumPhotoId = albumPhotoId,
        parentId = parentId,
        name = name,
        isFavourite = isFavourite,
        creationTime = creationTime,
        modificationTime = modificationTime,
        thumbnailFilePath = thumbnailFilePath,
        previewFilePath = previewFilePath,
        fileTypeInfo = fileTypeInfo,
        base64Id = base64Id,
        size = size,
        isTakenDown = isTakenDown,
        isSensitive = isSensitive,
        isSensitiveInherited = isSensitiveInherited,
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
        isTakenDown: Boolean,
        isSensitive: Boolean,
        isSensitiveInherited: Boolean,
        base64Id: String,
    ): Photo.Video = Photo.Video(
        id = id,
        albumPhotoId = albumPhotoId,
        parentId = parentId,
        name = name,
        isFavourite = isFavourite,
        creationTime = creationTime,
        modificationTime = modificationTime,
        thumbnailFilePath = thumbnailFilePath,
        previewFilePath = previewFilePath,
        fileTypeInfo = fileTypeInfo as VideoFileTypeInfo,
        base64Id = base64Id,
        size = size,
        isTakenDown = isTakenDown,
        isSensitive = isSensitive,
        isSensitiveInherited = isSensitiveInherited
    )

    @Test
    fun `test that getPhotosByFolderId returns list of photos when it is opened from cloud drive recursively`() =
        runTest {
            val token = mock<MegaCancelToken>()
            val imageNode = mock<MegaNode> {
                on { handle }.thenReturn(-1L)
                on { name }.thenReturn("image.jpg")
                on { hasThumbnail() }.thenReturn(true)
                on { base64Handle }.thenReturn(mockBase64Id)
            }
            val videoNode = mock<MegaNode> {
                on { handle }.thenReturn(-2L)
                on { name }.thenReturn("video.mp4")
                on { hasThumbnail() }.thenReturn(true)
                on { base64Handle }.thenReturn(mockBase64Id)
            }
            val imageFilter = mock<MegaSearchFilter>()
            val videoFilter = mock<MegaSearchFilter>()
            whenever(nodeRepository.isNodeInRubbishBin(NodeId(-1L))).thenReturn(false)
            whenever(nodeRepository.isNodeInRubbishBin(NodeId(-2L))).thenReturn(false)
            whenever(
                megaSearchFilterMapper(
                    parentHandle = NodeId(-1),
                    searchQuery = "",
                    searchTarget = SearchTarget.ROOT_NODES,
                    searchCategory = SearchCategory.IMAGES,
                )
            ).thenReturn(imageFilter)
            whenever(
                megaSearchFilterMapper(
                    parentHandle = NodeId(-1),
                    searchQuery = "",
                    searchTarget = SearchTarget.ROOT_NODES,
                    searchCategory = SearchCategory.VIDEO,
                )
            ).thenReturn(videoFilter)
            whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(token)
            whenever(
                megaApiGateway.searchWithFilter(
                    filter = imageFilter,
                    order = MegaApiJava.ORDER_MODIFICATION_DESC,
                    megaCancelToken = token
                ),
            ).thenReturn(listOf(imageNode))
            whenever(
                megaApiGateway.searchWithFilter(
                    filter = videoFilter,
                    order = MegaApiJava.ORDER_MODIFICATION_DESC,
                    megaCancelToken = token
                ),
            ).thenReturn(listOf(videoNode))
            initFileTypeInfoMapperReturnedValue(imageNode, videoNode)
            val image = mock<Photo.Image> {
                on { fileTypeInfo }.thenReturn(defaultImageType)
            }
            whenever(
                photoMapper(
                    node = imageNode,
                    albumPhotoId = null,
                    requireSerializedData = false,
                    isAvailableOffline = false
                )
            ) doReturn image
            val video = mock<Photo.Video> {
                on { fileTypeInfo }.thenReturn(defaultVideoType)
            }
            whenever(
                photoMapper(
                    node = videoNode,
                    albumPhotoId = null,
                    requireSerializedData = false,
                    isAvailableOffline = false
                )
            ) doReturn video

            underTest = createUnderTest()
            val actualPhotos = underTest.getPhotosByFolderId(NodeId(-1L), recursive = true)
            assertThat(actualPhotos).isNotEmpty()
        }

    private fun initFileTypeInfoMapperReturnedValue(imageNode: MegaNode, videoNode: MegaNode) {
        whenever(fileTypeInfoMapper(videoNode.name, videoNode.duration)).thenReturn(
            defaultVideoType
        )
        whenever(fileTypeInfoMapper(imageNode.name, imageNode.duration)).thenReturn(
            defaultImageType
        )
    }

    @Test
    fun `test that getPhotosByFolderId returns list of photos when it is opened from cloud drive non recursively`() =
        runTest {
            val token = mock<MegaCancelToken>()
            val imageNode = mock<MegaNode> {
                on { handle }.thenReturn(-1L)
                on { name }.thenReturn("image.jpg")
                on { hasThumbnail() }.thenReturn(true)
                on { base64Handle }.thenReturn(mockBase64Id)
            }
            val videoNode = mock<MegaNode> {
                on { handle }.thenReturn(-2L)
                on { name }.thenReturn("video.mp4")
                on { hasThumbnail() }.thenReturn(true)
                on { base64Handle }.thenReturn(mockBase64Id)
            }
            val imageFilter = mock<MegaSearchFilter>()
            val videoFilter = mock<MegaSearchFilter>()
            whenever(nodeRepository.isNodeInRubbishBin(NodeId(-1L))).thenReturn(false)
            whenever(nodeRepository.isNodeInRubbishBin(NodeId(-2L))).thenReturn(false)
            whenever(
                megaSearchFilterMapper(
                    parentHandle = NodeId(-1),
                    searchQuery = "",
                    searchTarget = SearchTarget.ROOT_NODES,
                    searchCategory = SearchCategory.IMAGES,
                )
            ).thenReturn(imageFilter)
            whenever(
                megaSearchFilterMapper(
                    parentHandle = NodeId(-1),
                    searchQuery = "",
                    searchTarget = SearchTarget.ROOT_NODES,
                    searchCategory = SearchCategory.VIDEO,
                )
            ).thenReturn(videoFilter)
            whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(token)
            whenever(
                megaApiGateway.getChildren(
                    filter = imageFilter,
                    order = MegaApiJava.ORDER_MODIFICATION_DESC,
                    megaCancelToken = token
                ),
            ).thenReturn(listOf(imageNode))
            whenever(
                megaApiGateway.getChildren(
                    filter = videoFilter,
                    order = MegaApiJava.ORDER_MODIFICATION_DESC,
                    megaCancelToken = token
                ),
            ).thenReturn(listOf(videoNode))
            initFileTypeInfoMapperReturnedValue(imageNode, videoNode)
            val image = mock<Photo.Image> {
                on { fileTypeInfo }.thenReturn(defaultImageType)
            }
            whenever(
                photoMapper(
                    node = imageNode,
                    albumPhotoId = null,
                    requireSerializedData = false,
                    isAvailableOffline = false
                )
            ) doReturn image
            val video = mock<Photo.Video> {
                on { fileTypeInfo }.thenReturn(defaultVideoType)
            }
            whenever(
                photoMapper(
                    node = videoNode,
                    albumPhotoId = null,
                    requireSerializedData = false,
                    isAvailableOffline = false
                )
            ) doReturn video

            underTest = createUnderTest()
            val actualPhotos = underTest.getPhotosByFolderId(NodeId(-1L), recursive = false)
            assertThat(actualPhotos).isNotEmpty()
        }


    @Test
    fun `test that getPhotosByFolderId returns list of photos when it is opened from folder link recursively`() =
        runTest {
            val token = mock<MegaCancelToken>()
            val imageNode = mock<MegaNode> {
                on { handle }.thenReturn(-1L)
                on { name }.thenReturn("image.jpg")
                on { hasThumbnail() }.thenReturn(true)
                on { base64Handle }.thenReturn(mockBase64Id)
            }
            val videoNode = mock<MegaNode> {
                on { handle }.thenReturn(-2L)
                on { name }.thenReturn("video.mp4")
                on { hasThumbnail() }.thenReturn(true)
                on { base64Handle }.thenReturn(mockBase64Id)
            }
            val imageFilter = mock<MegaSearchFilter>()
            val videoFilter = mock<MegaSearchFilter>()
            whenever(nodeRepository.isNodeInRubbishBin(NodeId(-1L))).thenReturn(false)
            whenever(nodeRepository.isNodeInRubbishBin(NodeId(-2L))).thenReturn(false)
            whenever(
                megaSearchFilterMapper(
                    parentHandle = NodeId(-1),
                    searchQuery = "",
                    searchTarget = SearchTarget.ROOT_NODES,
                    searchCategory = SearchCategory.IMAGES,
                )
            ).thenReturn(imageFilter)
            whenever(
                megaSearchFilterMapper(
                    parentHandle = NodeId(-1),
                    searchQuery = "",
                    searchTarget = SearchTarget.ROOT_NODES,
                    searchCategory = SearchCategory.VIDEO,
                )
            ).thenReturn(videoFilter)
            whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(token)
            whenever(
                megaApiFolder.search(
                    filter = imageFilter,
                    order = MegaApiJava.ORDER_MODIFICATION_DESC,
                    megaCancelToken = token
                ),
            ).thenReturn(listOf(imageNode))
            whenever(
                megaApiFolder.search(
                    filter = videoFilter,
                    order = MegaApiJava.ORDER_MODIFICATION_DESC,
                    megaCancelToken = token
                ),
            ).thenReturn(listOf(videoNode))
            initFileTypeInfoMapperReturnedValue(imageNode, videoNode)
            val image = mock<Photo.Image> {
                on { fileTypeInfo }.thenReturn(defaultImageType)
            }
            whenever(
                photoMapper(
                    node = imageNode,
                    albumPhotoId = null,
                    requireSerializedData = false,
                    isAvailableOffline = false
                )
            ) doReturn image
            val video = mock<Photo.Video> {
                on { fileTypeInfo }.thenReturn(defaultVideoType)
            }
            whenever(
                photoMapper(
                    node = videoNode,
                    albumPhotoId = null,
                    requireSerializedData = false,
                    isAvailableOffline = false
                )
            ) doReturn video

            underTest = createUnderTest()
            val actualPhotos = underTest.getPhotosByFolderId(
                NodeId(-1L),
                recursive = true,
                isFromFolderLink = true
            )
            assertThat(actualPhotos).isNotEmpty()
        }

    @Test
    fun `test that getPhotosByFolderId returns list of photos when it is opened from folder link non recursively`() =
        runTest {
            val token = mock<MegaCancelToken>()
            val imageNode = mock<MegaNode> {
                on { handle }.thenReturn(-1L)
                on { name }.thenReturn("image.jpg")
                on { hasThumbnail() }.thenReturn(true)
                on { base64Handle }.thenReturn(mockBase64Id)
            }
            val videoNode = mock<MegaNode> {
                on { handle }.thenReturn(-2L)
                on { name }.thenReturn("video.mp4")
                on { hasThumbnail() }.thenReturn(true)
                on { base64Handle }.thenReturn(mockBase64Id)
            }
            val imageFilter = mock<MegaSearchFilter>()
            val videoFilter = mock<MegaSearchFilter>()
            whenever(nodeRepository.isNodeInRubbishBin(NodeId(-1L))).thenReturn(false)
            whenever(nodeRepository.isNodeInRubbishBin(NodeId(-2L))).thenReturn(false)
            whenever(
                megaSearchFilterMapper(
                    parentHandle = NodeId(-1),
                    searchQuery = "",
                    searchTarget = SearchTarget.ROOT_NODES,
                    searchCategory = SearchCategory.IMAGES,
                )
            ).thenReturn(imageFilter)
            whenever(
                megaSearchFilterMapper(
                    parentHandle = NodeId(-1),
                    searchQuery = "",
                    searchTarget = SearchTarget.ROOT_NODES,
                    searchCategory = SearchCategory.VIDEO,
                )
            ).thenReturn(videoFilter)
            whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(token)
            whenever(
                megaApiFolder.getChildren(
                    filter = imageFilter,
                    order = MegaApiJava.ORDER_MODIFICATION_DESC,
                    megaCancelToken = token
                ),
            ).thenReturn(listOf(imageNode))
            whenever(
                megaApiFolder.getChildren(
                    filter = videoFilter,
                    order = MegaApiJava.ORDER_MODIFICATION_DESC,
                    megaCancelToken = token
                ),
            ).thenReturn(listOf(videoNode))
            initFileTypeInfoMapperReturnedValue(imageNode, videoNode)
            val image = mock<Photo.Image> {
                on { fileTypeInfo }.thenReturn(defaultImageType)
            }
            whenever(
                photoMapper(
                    node = imageNode,
                    albumPhotoId = null,
                    requireSerializedData = false,
                    isAvailableOffline = false
                )
            ) doReturn image
            val video = mock<Photo.Video> {
                on { fileTypeInfo }.thenReturn(defaultVideoType)
            }
            whenever(
                photoMapper(
                    node = videoNode,
                    albumPhotoId = null,
                    requireSerializedData = false,
                    isAvailableOffline = false
                )
            ) doReturn video

            underTest = createUnderTest()
            val actualPhotos = underTest.getPhotosByFolderId(
                NodeId(-1L),
                recursive = false,
                isFromFolderLink = true
            )
            assertThat(actualPhotos).isNotEmpty()
        }

    @Test
    fun `test that loadNextPageOfPhotos adds new photos to cache and emits them`() = runTest {
        val imageNode = mock<MegaNode> {
            on { handle }.thenReturn(-1L)
            on { name }.thenReturn("image.jpg")
            on { hasThumbnail() }.thenReturn(true)
            on { base64Handle }.thenReturn(mockBase64Id)
        }
        val videoNode = mock<MegaNode> {
            on { handle }.thenReturn(-2L)
            on { name }.thenReturn("video.mp4")
            on { hasThumbnail() }.thenReturn(true)
            on { base64Handle }.thenReturn(mockBase64Id)
        }
        val imageFilter = mock<MegaSearchFilter>()
        val videoFilter = mock<MegaSearchFilter>()
        val token = mock<MegaCancelToken>()

        whenever(
            megaSearchFilterMapper(
                parentHandle = null,
                searchQuery = "",
                searchTarget = SearchTarget.ROOT_NODES,
                searchCategory = SearchCategory.IMAGES,
            )
        ).thenReturn(imageFilter)
        whenever(
            megaSearchFilterMapper(
                parentHandle = null,
                searchQuery = "",
                searchTarget = SearchTarget.ROOT_NODES,
                searchCategory = SearchCategory.VIDEO,
            )
        ).thenReturn(videoFilter)
        whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(token)
        whenever(
            megaApiGateway.searchWithFilter(
                filter = eq(imageFilter),
                order = eq(MegaApiJava.ORDER_MODIFICATION_DESC),
                megaCancelToken = eq(token),
                megaSearchPage = anyOrNull()
            )
        ).thenReturn(listOf(imageNode))
        whenever(
            megaApiGateway.searchWithFilter(
                filter = eq(videoFilter),
                order = eq(MegaApiJava.ORDER_MODIFICATION_DESC),
                megaCancelToken = eq(token),
                megaSearchPage = anyOrNull()
            )
        ).thenReturn(listOf(videoNode))
        whenever(nodeRepository.isNodeInRubbishBin(any())).thenReturn(false)
        initFileTypeInfoMapperReturnedValue(imageNode, videoNode)
        val image = mock<Photo.Image> {
            on { id } doReturn -1L
            on { name } doReturn "image.jpg"
            on { fileTypeInfo }.thenReturn(defaultImageType)
        }
        whenever(
            photoMapper(
                node = imageNode,
                albumPhotoId = null,
                requireSerializedData = false,
                isAvailableOffline = false
            )
        ) doReturn image
        val video = mock<Photo.Video> {
            on { id } doReturn -2L
            on { name } doReturn "video.mp4"
            on { fileTypeInfo }.thenReturn(defaultVideoType)
        }
        whenever(
            photoMapper(
                node = videoNode,
                albumPhotoId = null,
                requireSerializedData = false,
                isAvailableOffline = false
            )
        ) doReturn video

        underTest = createUnderTest()

        underTest.loadNextPageOfPhotos()

        underTest.monitorPaginatedPhotos().test {
            val state = awaitItem()
            assertThat(state).hasSize(2)
            assertThat(state.map { it.name }).containsExactly("image.jpg", "video.mp4")
        }
    }

    @Test
    fun `test that loadNextPageOfPhotos returns empty list when no photos found`() = runTest {
        val imageFilter = mock<MegaSearchFilter>()
        val videoFilter = mock<MegaSearchFilter>()
        val token = mock<MegaCancelToken>()

        whenever(
            megaSearchFilterMapper(
                parentHandle = null,
                searchQuery = "",
                searchTarget = SearchTarget.ROOT_NODES,
                searchCategory = SearchCategory.IMAGES,
            )
        ).thenReturn(imageFilter)
        whenever(
            megaSearchFilterMapper(
                parentHandle = null,
                searchQuery = "",
                searchTarget = SearchTarget.ROOT_NODES,
                searchCategory = SearchCategory.VIDEO,
            )
        ).thenReturn(videoFilter)
        whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(token)
        whenever(
            megaApiGateway.searchWithFilter(
                filter = eq(imageFilter),
                order = eq(MegaApiJava.ORDER_MODIFICATION_DESC),
                megaCancelToken = eq(token),
                megaSearchPage = anyOrNull()
            )
        ).thenReturn(emptyList())
        whenever(
            megaApiGateway.searchWithFilter(
                filter = eq(videoFilter),
                order = eq(MegaApiJava.ORDER_MODIFICATION_DESC),
                megaCancelToken = eq(token),
                megaSearchPage = anyOrNull()
            )
        ).thenReturn(emptyList())

        underTest = createUnderTest()

        underTest.loadNextPageOfPhotos()

        underTest.monitorPaginatedPhotos().test {
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `test that getImageNodeFromChatMessage returns ImageNode successfully`() = runTest {
        val chatId = 123L
        val messageId = 456L
        val nodeHandle = 789L
        val megaNode = createMegaNode(handle = nodeHandle, name = "image.jpg")
        val expectedImageNode = mock<ImageNode>()

        whenever(megaNodeFromChatMessageMapper(chatId, messageId, 0)).thenReturn(megaNode)
        whenever(fileTypeInfoMapper(megaNode.name, megaNode.duration)).thenReturn(
            StaticImageFileTypeInfo(mimeType = "image/jpeg", extension = "jpg")
        )
        whenever(nodeRepository.isNodeInRubbishBin(NodeId(nodeHandle))).thenReturn(false)
        whenever(megaApiGateway.getNumVersions(any())).thenReturn(1)
        whenever(
            imageNodeMapper.invoke(
                megaNode = any(),
                numVersion = any(),
                requireSerializedData = eq(true),
                offline = anyOrNull(),
                chatId = eq(chatId),
                messageId = eq(messageId),
            )
        ).thenReturn(expectedImageNode)

        underTest = createUnderTest()

        val result = underTest.getImageNodeFromChatMessage(chatId, messageId)

        assertThat(result).isNotNull()
        assertThat(result).isEqualTo(expectedImageNode)
    }

    @Test
    fun `test that monitorMediaTypedNodes emits initial list on subscription`() = runTest {
        val filter = mock<MegaSearchFilter>()
        val token = mock<MegaCancelToken>()
        val node = createMegaNode(handle = 1L, name = "photo.jpg")
        whenever(
            megaSearchFilterMapper(
                parentHandle = null,
                searchQuery = "",
                searchTarget = SearchTarget.ROOT_NODES,
                searchCategory = SearchCategory.ALL_MEDIA,
            )
        ).thenReturn(filter)
        whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(token)
        whenever(
            megaApiGateway.searchWithFilter(
                filter,
                MegaApiJava.ORDER_MODIFICATION_DESC,
                token
            )
        ).thenReturn(listOf(node))
        whenever(megaApiGateway.isInRubbish(node)).thenReturn(false)
        val typedNode = mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(1L))
        }
        whenever(typedNodeMapper(megaNode = node, folderTypeData = null, offline = null))
            .thenReturn(typedNode)

        underTest = createUnderTest()

        underTest.monitorMediaTypedNodes.test {
            assertThat(awaitItem()).containsExactly(typedNode)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `test that monitorTimelineImageNodes maps media nodes from monitorMediaTypedNodes and excludes non-media`() =
        runTest {
            val filter = mock<MegaSearchFilter>()
            val token = mock<MegaCancelToken>()
            val imageMegaNode = createMegaNode(handle = 1L, name = "photo.jpg")
            val videoMegaNode = createMegaNode(handle = 2L, name = "video.mp4")
            val otherMegaNode = createMegaNode(handle = 3L, name = "doc.pdf")
            whenever(
                megaSearchFilterMapper(
                    parentHandle = null,
                    searchQuery = "",
                    searchTarget = SearchTarget.ROOT_NODES,
                    searchCategory = SearchCategory.ALL_MEDIA,
                )
            ).thenReturn(filter)
            whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(token)
            whenever(
                megaApiGateway.searchWithFilter(filter, MegaApiJava.ORDER_MODIFICATION_DESC, token)
            ).thenReturn(listOf(imageMegaNode, videoMegaNode, otherMegaNode))
            whenever(megaApiGateway.isInRubbish(any())).thenReturn(false)

            val otherType = UnknownFileTypeInfo(mimeType = "", extension = "pdf")
            val imageTypedNode = mock<TypedFileNode> {
                on { id }.thenReturn(NodeId(1L))
                on { type }.thenReturn(defaultImageType)
            }
            val videoTypedNode = mock<TypedFileNode> {
                on { id }.thenReturn(NodeId(2L))
                on { type }.thenReturn(defaultVideoType)
            }
            val otherTypedNode = mock<TypedFileNode> {
                on { id }.thenReturn(NodeId(3L))
                on { type }.thenReturn(otherType)
            }
            whenever(typedNodeMapper(megaNode = imageMegaNode, folderTypeData = null, offline = null))
                .thenReturn(imageTypedNode)
            whenever(typedNodeMapper(megaNode = videoMegaNode, folderTypeData = null, offline = null))
                .thenReturn(videoTypedNode)
            whenever(typedNodeMapper(megaNode = otherMegaNode, folderTypeData = null, offline = null))
                .thenReturn(otherTypedNode)

            val imageNode = mock<ImageNode>()
            val videoImageNode = mock<ImageNode>()
            whenever(typedFileNodeToImageNodeMapper(imageTypedNode)).thenReturn(imageNode)
            whenever(typedFileNodeToImageNodeMapper(videoTypedNode)).thenReturn(videoImageNode)

            underTest = createUnderTest()

            underTest.monitorTimelineImageNodes().test {
                assertThat(awaitItem()).containsExactly(imageNode, videoImageNode).inOrder()
                cancelAndConsumeRemainingEvents()
            }
            verify(typedFileNodeToImageNodeMapper, never()).invoke(otherTypedNode)
        }

    @Test
    fun `test that monitorMediaTypedNodes emits updated list when a valid node is added`() =
        runTest(ioDispatcher) {
            val filter = mock<MegaSearchFilter>()
            val token = mock<MegaCancelToken>()
            val existingNode = createMegaNode(handle = 1L, name = "photo.jpg")
            val newMegaNode = createMegaNode(handle = 2L, name = "photo2.jpg")
            val nodeUpdatesFlow = MutableSharedFlow<NodeUpdate>()
            whenever(nodeRepository.monitorNodeUpdates()).thenReturn(nodeUpdatesFlow)
            whenever(
                megaSearchFilterMapper(
                    parentHandle = null,
                    searchQuery = "",
                    searchTarget = SearchTarget.ROOT_NODES,
                    searchCategory = SearchCategory.ALL_MEDIA,
                )
            ).thenReturn(filter)
            whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(token)
            whenever(
                megaApiGateway.searchWithFilter(
                    filter,
                    MegaApiJava.ORDER_MODIFICATION_DESC,
                    token
                )
            ).thenReturn(listOf(existingNode))
            whenever(megaApiGateway.isInRubbish(existingNode)).thenReturn(false)
            val existingTypedNode = mock<TypedFileNode> {
                on { id }.thenReturn(NodeId(1L))
            }
            whenever(
                typedNodeMapper(
                    megaNode = existingNode,
                    folderTypeData = null,
                    offline = null
                )
            ).thenReturn(existingTypedNode)
            val newFileNode = mock<FileNode> {
                on { id }.thenReturn(NodeId(2L))
                on { type }.thenReturn(mock<StaticImageFileTypeInfo>())
            }
            whenever(nodeRepository.isNodeInRubbishBin(NodeId(2L))).thenReturn(false)
            whenever(nodeRepository.isNodeInCloudDrive(handle = 2L)).thenReturn(true)
            whenever(megaApiGateway.getMegaNodeByHandle(nodeHandle = 2L)).thenReturn(newMegaNode)
            val newTypedNode = mock<TypedFileNode> {
                on { id }.thenReturn(NodeId(2L))
            }
            whenever(typedNodeMapper(megaNode = newMegaNode, folderTypeData = null, offline = null))
                .thenReturn(newTypedNode)

            underTest = createUnderTest()

            underTest.monitorMediaTypedNodes.test {
                assertThat(awaitItem()).containsExactly(existingTypedNode)
                nodeUpdatesFlow.emit(NodeUpdate(mapOf(newFileNode to listOf(NodeChanges.New))))
                val updated = awaitItem()
                assertThat(updated).containsExactly(existingTypedNode, newTypedNode)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `test that monitorMediaTypedNodes removes node from list when it is moved to rubbish bin`() =
        runTest(ioDispatcher) {
            val filter = mock<MegaSearchFilter>()
            val token = mock<MegaCancelToken>()
            val node = createMegaNode(handle = 1L, name = "photo.jpg")
            val nodeUpdatesFlow = MutableSharedFlow<NodeUpdate>()
            whenever(nodeRepository.monitorNodeUpdates()).thenReturn(nodeUpdatesFlow)
            whenever(
                megaSearchFilterMapper(
                    parentHandle = null,
                    searchQuery = "",
                    searchTarget = SearchTarget.ROOT_NODES,
                    searchCategory = SearchCategory.ALL_MEDIA,
                )
            ).thenReturn(filter)
            whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(token)
            whenever(
                megaApiGateway.searchWithFilter(
                    filter,
                    MegaApiJava.ORDER_MODIFICATION_DESC,
                    token
                )
            ).thenReturn(listOf(node))
            whenever(megaApiGateway.isInRubbish(node)).thenReturn(false)
            val typedNode = mock<TypedFileNode> {
                on { id }.thenReturn(NodeId(1L))
            }
            whenever(typedNodeMapper(megaNode = node, folderTypeData = null, offline = null))
                .thenReturn(typedNode)
            val updatedFileNode = mock<FileNode> {
                on { id }.thenReturn(NodeId(1L))
            }
            whenever(nodeRepository.isNodeInRubbishBin(NodeId(1L))).thenReturn(true)

            underTest = createUnderTest()

            underTest.monitorMediaTypedNodes.test {
                assertThat(awaitItem()).containsExactly(typedNode)
                nodeUpdatesFlow.emit(
                    NodeUpdate(mapOf(updatedFileNode to listOf(NodeChanges.Remove)))
                )
                assertThat(awaitItem()).isEmpty()
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `test that monitorMediaTypedNodes does not emit when unchanged node update is received`() =
        runTest {
            val filter = mock<MegaSearchFilter>()
            val token = mock<MegaCancelToken>()
            val node = createMegaNode(handle = 1L, name = "photo.jpg")
            val nodeUpdatesFlow = MutableSharedFlow<NodeUpdate>()
            whenever(nodeRepository.monitorNodeUpdates()).thenReturn(nodeUpdatesFlow)
            whenever(
                megaSearchFilterMapper(
                    parentHandle = null,
                    searchQuery = "",
                    searchTarget = SearchTarget.ROOT_NODES,
                    searchCategory = SearchCategory.ALL_MEDIA,
                )
            ).thenReturn(filter)
            whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(token)
            whenever(
                megaApiGateway.searchWithFilter(
                    filter,
                    MegaApiJava.ORDER_MODIFICATION_DESC,
                    token
                )
            ).thenReturn(listOf(node))
            whenever(megaApiGateway.isInRubbish(node)).thenReturn(false)
            val typedNode = mock<TypedFileNode> {
                on { id }.thenReturn(NodeId(1L))
            }
            whenever(typedNodeMapper(megaNode = node, folderTypeData = null, offline = null))
                .thenReturn(typedNode)
            val unchangedFileNode = mock<FileNode> {
                on { id }.thenReturn(NodeId(1L))
                on { type }.thenReturn(mock<StaticImageFileTypeInfo>())
            }
            whenever(nodeRepository.isNodeInRubbishBin(NodeId(1L))).thenReturn(false)
            whenever(nodeRepository.isNodeInCloudDrive(handle = 1L)).thenReturn(true)
            whenever(megaApiGateway.getMegaNodeByHandle(nodeHandle = 1L)).thenReturn(node)
            whenever(typedNodeMapper(megaNode = node, folderTypeData = null, offline = null))
                .thenReturn(typedNode)

            underTest = createUnderTest()

            underTest.monitorMediaTypedNodes.test {
                assertThat(awaitItem()).containsExactly(typedNode)
                nodeUpdatesFlow.emit(
                    NodeUpdate(mapOf(unchangedFileNode to listOf(NodeChanges.Attributes)))
                )
                expectNoEvents()
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `test that monitorMediaTypedNodes shared flow replays last value to new subscriber`() =
        runTest(ioDispatcher) {
            val filter = mock<MegaSearchFilter>()
            val token = mock<MegaCancelToken>()
            val node = createMegaNode(handle = 1L, name = "photo.jpg")
            whenever(
                megaSearchFilterMapper(
                    parentHandle = null,
                    searchQuery = "",
                    searchTarget = SearchTarget.ROOT_NODES,
                    searchCategory = SearchCategory.ALL_MEDIA,
                )
            ).thenReturn(filter)
            whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(token)
            whenever(
                megaApiGateway.searchWithFilter(
                    filter,
                    MegaApiJava.ORDER_MODIFICATION_DESC,
                    token
                )
            ).thenReturn(listOf(node))
            whenever(megaApiGateway.isInRubbish(node)).thenReturn(false)
            val typedNode = mock<TypedFileNode> {
                on { id }.thenReturn(NodeId(1L))
            }
            whenever(typedNodeMapper(megaNode = node, folderTypeData = null, offline = null))
                .thenReturn(typedNode)

            underTest = createUnderTest()

            // First subscriber
            underTest.monitorMediaTypedNodes.test {
                assertThat(awaitItem()).containsExactly(typedNode)
                cancelAndConsumeRemainingEvents()
            }
            // Second subscriber
            underTest.monitorMediaTypedNodes.test {
                assertThat(awaitItem()).containsExactly(typedNode)
                cancelAndConsumeRemainingEvents()
            }
        }
}
