package mega.privacy.android.app.presentation.videoplayer

import android.content.Context
import android.content.Intent
import android.net.Uri
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.TimberJUnit5Extension
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemType
import mega.privacy.android.app.presentation.myaccount.InstantTaskExecutorExtension
import mega.privacy.android.app.presentation.videoplayer.mapper.VideoPlayerItemMapper
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerItem
import mega.privacy.android.app.utils.Constants.BACKUPS_ADAPTER
import mega.privacy.android.app.utils.Constants.CONTACT_FILE_ADAPTER
import mega.privacy.android.app.utils.Constants.FAVOURITES_ADAPTER
import mega.privacy.android.app.utils.Constants.FILE_BROWSER_ADAPTER
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FROM_ALBUM_SHARING
import mega.privacy.android.app.utils.Constants.FROM_IMAGE_VIEWER
import mega.privacy.android.app.utils.Constants.FROM_MEDIA_DISCOVERY
import mega.privacy.android.app.utils.Constants.INCOMING_SHARES_ADAPTER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CONTACT_EMAIL
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IS_PLAYLIST
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MEDIA_QUEUE_TITLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.LINKS_ADAPTER
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.OUTGOING_SHARES_ADAPTER
import mega.privacy.android.app.utils.Constants.RECENTS_ADAPTER
import mega.privacy.android.app.utils.Constants.RECENTS_BUCKET_ADAPTER
import mega.privacy.android.app.utils.Constants.RUBBISH_BIN_ADAPTER
import mega.privacy.android.app.utils.Constants.SEARCH_BY_ADAPTER
import mega.privacy.android.app.utils.Constants.VIDEO_BROWSE_ADAPTER
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.exception.BlockedMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.usecase.GetFileTypeInfoByNameUseCase
import mega.privacy.android.domain.usecase.GetLocalFilePathUseCase
import mega.privacy.android.domain.usecase.GetLocalLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.GetOfflineNodesByParentIdUseCase
import mega.privacy.android.domain.usecase.GetParentNodeFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.GetRootNodeFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.GetRubbishNodeUseCase
import mega.privacy.android.domain.usecase.GetUserNameByEmailUseCase
import mega.privacy.android.domain.usecase.file.GetFileByPathUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.mediaplayer.GetLocalFolderLinkUseCase
import mega.privacy.android.domain.usecase.mediaplayer.HttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.HttpServerStartUseCase
import mega.privacy.android.domain.usecase.mediaplayer.HttpServerStopUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodeByHandleUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesByEmailUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesByHandlesUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesByParentHandleUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesFromInSharesUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesFromOutSharesUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesFromPublicLinksUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideosByParentHandleFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideosBySearchTypeUseCase
import mega.privacy.android.domain.usecase.node.backup.GetBackupsNodeUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineNodeInformationByIdUseCase
import mega.privacy.android.domain.usecase.setting.MonitorSubFolderMediaDiscoverySettingsUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@ExtendWith(
    value = [
        CoroutineMainDispatcherExtension::class,
        InstantTaskExecutorExtension::class,
        TimberJUnit5Extension::class
    ]
)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideoPlayerViewModelTest {
    private lateinit var underTest: VideoPlayerViewModel

    private val context = mock<Context>()
    private val mediaPlayerGateway = mock<MediaPlayerGateway>()
    private val videoPlayerItemMapper = mock<VideoPlayerItemMapper>()
    private val getVideoNodeByHandleUseCase = mock<GetVideoNodeByHandleUseCase>()
    private val getVideoNodesUseCase = mock<GetVideoNodesUseCase>()
    private val getVideoNodesFromPublicLinksUseCase = mock<GetVideoNodesFromPublicLinksUseCase>()
    private val getVideoNodesFromInSharesUseCase = mock<GetVideoNodesFromInSharesUseCase>()
    private val getVideoNodesFromOutSharesUseCase = mock<GetVideoNodesFromOutSharesUseCase>()
    private val getVideoNodesByEmailUseCase = mock<GetVideoNodesByEmailUseCase>()
    private val getUserNameByEmailUseCase = mock<GetUserNameByEmailUseCase>()
    private val getRubbishNodeUseCase = mock<GetRubbishNodeUseCase>()
    private val getBackupsNodeUseCase = mock<GetBackupsNodeUseCase>()
    private val getRootNodeUseCase = mock<GetRootNodeUseCase>()
    private val getVideosBySearchTypeUseCase = mock<GetVideosBySearchTypeUseCase>()
    private val getVideoNodesByParentHandleUseCase = mock<GetVideoNodesByParentHandleUseCase>()
    private val getVideoNodesByHandlesUseCase = mock<GetVideoNodesByHandlesUseCase>()
    private val getRootNodeFromMegaApiFolderUseCase = mock<GetRootNodeFromMegaApiFolderUseCase>()
    private val getParentNodeFromMegaApiFolderUseCase =
        mock<GetParentNodeFromMegaApiFolderUseCase>()
    private val getVideosByParentHandleFromMegaApiFolderUseCase =
        mock<GetVideosByParentHandleFromMegaApiFolderUseCase>()
    private val monitorSubFolderMediaDiscoverySettingsUseCase =
        mock<MonitorSubFolderMediaDiscoverySettingsUseCase>()
    private val getThumbnailUseCase = mock<GetThumbnailUseCase>()
    private val httpServerIsRunningUseCase = mock<HttpServerIsRunningUseCase>()
    private val httpServerStartUseCase = mock<HttpServerStartUseCase>()
    private val httpServerStopUseCase = mock<HttpServerStopUseCase>()
    private val getLocalFolderLinkUseCase = mock<GetLocalFolderLinkUseCase>()
    private val getLocalLinkFromMegaApiUseCase = mock<GetLocalLinkFromMegaApiUseCase>()
    private val getFileTypeInfoByNameUseCase = mock<GetFileTypeInfoByNameUseCase>()
    private val getOfflineNodeInformationByIdUseCase = mock<GetOfflineNodeInformationByIdUseCase>()
    private val getOfflineNodesByParentIdUseCase = mock<GetOfflineNodesByParentIdUseCase>()
    private val getLocalFilePathUseCase = mock<GetLocalFilePathUseCase>()
    private val getFingerprintUseCase = mock<GetFingerprintUseCase>()
    private val monitorTransferEventsUseCase = mock<MonitorTransferEventsUseCase>()
    private val fakeMonitorTransferEventsFlow =
        MutableSharedFlow<TransferEvent.TransferTemporaryErrorEvent>()
    private val getFileByPathUseCase = mock<GetFileByPathUseCase>()

    private val testHandle: Long = 123456
    private val testFileName = "test.mp4"
    private val testSize = 100L
    private val testDuration = 200.seconds
    private val testAbsolutePath = "https://www.example.com"
    private val testTitle = "video queue title"

    private fun initViewModel() {
        underTest = VideoPlayerViewModel(
            context = context,
            mediaPlayerGateway = mediaPlayerGateway,
            applicationScope = CoroutineScope(UnconfinedTestDispatcher()),
            ioDispatcher = UnconfinedTestDispatcher(),
            videoPlayerItemMapper = videoPlayerItemMapper,
            getVideoNodeByHandleUseCase = getVideoNodeByHandleUseCase,
            getVideoNodesUseCase = getVideoNodesUseCase,
            getVideoNodesFromPublicLinksUseCase = getVideoNodesFromPublicLinksUseCase,
            getVideoNodesFromInSharesUseCase = getVideoNodesFromInSharesUseCase,
            getVideoNodesFromOutSharesUseCase = getVideoNodesFromOutSharesUseCase,
            getVideoNodesByEmailUseCase = getVideoNodesByEmailUseCase,
            getUserNameByEmailUseCase = getUserNameByEmailUseCase,
            getRubbishNodeUseCase = getRubbishNodeUseCase,
            getBackupsNodeUseCase = getBackupsNodeUseCase,
            getRootNodeUseCase = getRootNodeUseCase,
            getVideosBySearchTypeUseCase = getVideosBySearchTypeUseCase,
            getVideoNodesByParentHandleUseCase = getVideoNodesByParentHandleUseCase,
            getVideoNodesByHandlesUseCase = getVideoNodesByHandlesUseCase,
            getRootNodeFromMegaApiFolderUseCase = getRootNodeFromMegaApiFolderUseCase,
            getParentNodeFromMegaApiFolderUseCase = getParentNodeFromMegaApiFolderUseCase,
            getVideosByParentHandleFromMegaApiFolderUseCase = getVideosByParentHandleFromMegaApiFolderUseCase,
            monitorSubFolderMediaDiscoverySettingsUseCase = monitorSubFolderMediaDiscoverySettingsUseCase,
            getThumbnailUseCase = getThumbnailUseCase,
            httpServerIsRunningUseCase = httpServerIsRunningUseCase,
            httpServerStartUseCase = httpServerStartUseCase,
            httpServerStopUseCase = httpServerStopUseCase,
            getLocalFolderLinkUseCase = getLocalFolderLinkUseCase,
            getFileTypeInfoByNameUseCase = getFileTypeInfoByNameUseCase,
            getOfflineNodeInformationByIdUseCase = getOfflineNodeInformationByIdUseCase,
            getOfflineNodesByParentIdUseCase = getOfflineNodesByParentIdUseCase,
            getLocalLinkFromMegaApiUseCase = getLocalLinkFromMegaApiUseCase,
            getLocalFilePathUseCase = getLocalFilePathUseCase,
            getFingerprintUseCase = getFingerprintUseCase,
            monitorTransferEventsUseCase = monitorTransferEventsUseCase,
            getFileByPathUseCase = getFileByPathUseCase,
        )
    }

    @BeforeEach
    fun setUp() {
        whenever(monitorTransferEventsUseCase()).thenReturn(fakeMonitorTransferEventsFlow)
        whenever(monitorSubFolderMediaDiscoverySettingsUseCase()).thenReturn(flowOf(true))
        initViewModel()
    }

    @AfterEach
    fun resetMocks() {
        reset(
            context,
            mediaPlayerGateway,
            videoPlayerItemMapper,
            getVideoNodeByHandleUseCase,
            getVideoNodesUseCase,
            getVideoNodeByHandleUseCase,
            getVideoNodesUseCase,
            getVideoNodesFromPublicLinksUseCase,
            getVideoNodesFromInSharesUseCase,
            getVideoNodesFromOutSharesUseCase,
            getVideoNodesByEmailUseCase,
            getUserNameByEmailUseCase,
            getRubbishNodeUseCase,
            getBackupsNodeUseCase,
            getRootNodeUseCase,
            getVideosBySearchTypeUseCase,
            getVideoNodesByParentHandleUseCase,
            getVideoNodesByHandlesUseCase,
            getRootNodeFromMegaApiFolderUseCase,
            getParentNodeFromMegaApiFolderUseCase,
            getVideosByParentHandleFromMegaApiFolderUseCase,
            monitorSubFolderMediaDiscoverySettingsUseCase,
            getThumbnailUseCase,
            httpServerStopUseCase,
            httpServerStartUseCase,
            httpServerIsRunningUseCase,
            getLocalFilePathUseCase,
            getFileTypeInfoByNameUseCase,
            getOfflineNodeInformationByIdUseCase,
            getOfflineNodesByParentIdUseCase,
            getLocalLinkFromMegaApiUseCase,
            getLocalFilePathUseCase,
            getFingerprintUseCase,
            monitorTransferEventsUseCase,
            getFileByPathUseCase,
        )
    }

    @Test
    fun `test that the errorState is updated correctly when emit BlockedMegaException`() =
        runTest {
            mockBlockedMegaException()
            underTest.uiState.test {
                assertThat(awaitItem().error).isInstanceOf(BlockedMegaException::class.java)
            }
        }

    private suspend fun mockBlockedMegaException() {
        val expectedTransfer = mock<Transfer> {
            on { isForeignOverQuota }.thenReturn(true)
            on { nodeHandle }.thenReturn(INVALID_HANDLE)
        }
        val event = mock<TransferEvent.TransferTemporaryErrorEvent> {
            on { transfer }.thenReturn(expectedTransfer)
            on { error }.thenReturn(mock<BlockedMegaException>())
        }
        fakeMonitorTransferEventsFlow.emit(event)
    }

    @Test
    fun `test that the errorState is updated correctly when emit QuotaExceededMegaException`() =
        runTest {
            mockQuotaExceededMegaException()
            underTest.uiState.test {
                assertThat(awaitItem().error).isInstanceOf(QuotaExceededMegaException::class.java)
            }
        }

    private suspend fun mockQuotaExceededMegaException() {
        val expectedTransfer = mock<Transfer> {
            on { isForeignOverQuota }.thenReturn(false)
            on { nodeHandle }.thenReturn(INVALID_HANDLE)
        }
        val expectedError = mock<QuotaExceededMegaException> {
            on { value }.thenReturn(1)
        }
        val event = mock<TransferEvent.TransferTemporaryErrorEvent> {
            on { transfer }.thenReturn(expectedTransfer)
            on { error }.thenReturn(expectedError)
        }
        fakeMonitorTransferEventsFlow.emit(event)
    }

    @Test
    fun `test that the isRetry is false when INTENT_EXTRA_KEY_REBUILD_PLAYLIST is false`() =
        runTest {
            val intent = mock<Intent>()
            initTestDataForTestingInvalidParams(intent = intent, rebuildPlaylist = false)
            underTest.initVideoPlaybackSources(intent)
            underTest.uiState.test {
                assertThat(awaitItem().isRetry).isFalse()
            }
        }

    private fun initTestDataForTestingInvalidParams(
        intent: Intent,
        rebuildPlaylist: Boolean? = null,
        adapterType: Int? = null,
        data: Uri? = null,
        handle: Long? = null,
        fileName: String? = null,
    ) {
        rebuildPlaylist?.let {
            whenever(intent.getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true)).thenReturn(it)
        }
        adapterType?.let {
            whenever(
                intent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)
            ).thenReturn(it)
        }
        whenever(intent.data).thenReturn(data)
        handle?.let {
            whenever(intent.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)).thenReturn(it)
        }
        whenever(intent.getStringExtra(INTENT_EXTRA_KEY_FILE_NAME)).thenReturn(fileName)
    }

    @Test
    fun `test that the isRetry is false when INTENT_EXTRA_KEY_ADAPTER_TYPE is INVALID_VALUE`() =
        runTest {
            val intent = mock<Intent>()
            initTestDataForTestingInvalidParams(
                intent = intent,
                rebuildPlaylist = true,
                adapterType = INVALID_VALUE
            )
            underTest.initVideoPlaybackSources(intent)
            underTest.uiState.test {
                assertThat(awaitItem().isRetry).isFalse()
            }
        }

    @Test
    fun `test that the isRetry is false when data of Intent is null`() =
        runTest {
            val intent = mock<Intent>()
            initTestDataForTestingInvalidParams(
                intent = intent,
                rebuildPlaylist = true,
                adapterType = FOLDER_LINK_ADAPTER,
            )
            underTest.initVideoPlaybackSources(intent)
            underTest.uiState.test {
                assertThat(awaitItem().isRetry).isFalse()
            }
        }

    @Test
    fun `test that the isRetry is false when INTENT_EXTRA_KEY_HANDLE is INVALID_HANDLE`() =
        runTest {
            val intent = mock<Intent>()
            initTestDataForTestingInvalidParams(
                intent = intent,
                rebuildPlaylist = true,
                adapterType = FOLDER_LINK_ADAPTER,
                data = mock(),
                handle = INVALID_HANDLE
            )
            underTest.initVideoPlaybackSources(intent)
            underTest.uiState.test {
                assertThat(awaitItem().isRetry).isFalse()
            }
        }

    @Test
    fun `test that the isRetry is false when INTENT_EXTRA_KEY_FILE_NAME is null`() =
        runTest {
            val intent = mock<Intent>()
            initTestDataForTestingInvalidParams(
                intent = intent,
                rebuildPlaylist = true,
                adapterType = FOLDER_LINK_ADAPTER,
                data = mock(),
                handle = 123456
            )
            underTest.initVideoPlaybackSources(intent)
            underTest.uiState.test {
                assertThat(awaitItem().isRetry).isFalse()
            }
        }

    @Test
    fun `test that the isRetry is false when currentPlayingUri is null when getLocalFolderLink return null`() =
        runTest {
            val intent = mock<Intent>()
            initTestDataForTestingInvalidParams(
                intent = intent,
                rebuildPlaylist = true,
                adapterType = FOLDER_LINK_ADAPTER,
                data = mock(),
                handle = 123456,
                fileName = "test.mp4"
            )
            whenever(getLocalFolderLinkUseCase(any())).thenReturn(null)
            underTest.initVideoPlaybackSources(intent)
            underTest.uiState.test {
                assertThat(awaitItem().isRetry).isFalse()
            }
        }

    @Test
    fun `test that the mediaPlaySources is updated correctly when an intent is received`() =
        runTest {
            val intent = mock<Intent>()
            val testHandle: Long = 123456
            val testFileName = "test.mp4"
            val uri: Uri = mock()
            initTestDataForTestingInvalidParams(
                intent = intent,
                rebuildPlaylist = true,
                adapterType = VIDEO_BROWSE_ADAPTER,
                data = uri,
                handle = testHandle,
                fileName = testFileName
            )
            underTest.initVideoPlaybackSources(intent)
            underTest.uiState.test {
                val actual = awaitItem()
                actual.mediaPlaySources?.let { sources ->
                    assertThat(sources.mediaItems).isNotEmpty()
                    assertThat(sources.mediaItems.size).isEqualTo(1)
                    assertThat(sources.mediaItems[0].mediaId).isEqualTo(testHandle.toString())
                    assertThat(sources.newIndexForCurrentItem).isEqualTo(INVALID_VALUE)
                    assertThat(sources.nameToDisplay).isEqualTo(testFileName)
                }
                assertThat(actual.metadata?.nodeName).isEqualTo(testFileName)
            }
        }

    @Test
    fun `test that items is updated correctly when INTENT_EXTRA_KEY_IS_PLAYLIST is false`() =
        runTest {
            val intent = mock<Intent>()

            initTestDataForTestingInvalidParams(
                intent = intent,
                rebuildPlaylist = true,
                adapterType = VIDEO_BROWSE_ADAPTER,
                data = mock(),
                handle = testHandle,
                fileName = testFileName
            )
            val node = initTypedVideoNode()
            val videoPlayerItem = initVideoPlayerItem(testHandle, testFileName, testDuration)
            whenever(
                videoPlayerItemMapper(
                    nodeHandle = testHandle,
                    nodeName = testFileName,
                    thumbnail = null,
                    type = MediaQueueItemType.Playing,
                    size = 100,
                    duration = testDuration
                )
            ).thenReturn(videoPlayerItem)
            whenever(intent.getBooleanExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, true)).thenReturn(false)
            whenever(getVideoNodeByHandleUseCase(testHandle)).thenReturn(node)
            underTest.initVideoPlaybackSources(intent)
            underTest.uiState.test {
                val actual = awaitItem()
                actual.items.let { items ->
                    assertThat(items).isNotEmpty()
                    assertThat(items.size).isEqualTo(1)
                    assertThat(items[0]).isEqualTo(videoPlayerItem)
                }
                cancelAndConsumeRemainingEvents()
            }
        }

    private fun initTypedVideoNode() =
        mock<TypedVideoNode> {
            on { this.id }.thenReturn(NodeId(testHandle))
            on { this.name }.thenReturn(testFileName)
            on { this.size }.thenReturn(testSize)
            on { this.duration }.thenReturn(testDuration)
        }

    private fun initVideoPlayerItem(handle: Long, name: String, duration: Duration) =
        mock<VideoPlayerItem> {
            on { nodeHandle }.thenReturn(handle)
            on { nodeName }.thenReturn(name)
            on { this.duration }.thenReturn(duration)
        }

    @Test
    fun `test that stat is updated correctly when launch source is OFFLINE_ADAPTER`() =
        runTest {
            val intent = mock<Intent>()
            val testHandle: Long = 2
            val testFileName = "test.mp4"
            initTestDataForTestingInvalidParams(
                intent = intent,
                rebuildPlaylist = true,
                adapterType = OFFLINE_ADAPTER,
                data = mock(),
                handle = testHandle,
                fileName = testFileName
            )

            val testParentId = 654321
            val testTitle = "video queue title"
            val offlineNode = mock<OtherOfflineNodeInformation> {
                on { name }.thenReturn(testTitle)
            }
            whenever(
                intent.getIntExtra(
                    INTENT_EXTRA_KEY_PARENT_ID,
                    -1
                )
            ).thenReturn(testParentId)
            whenever(getOfflineNodeInformationByIdUseCase(testParentId)).thenReturn(offlineNode)

            val offlineFiles = (1..3).map {
                initOfflineFileInfo(it, it.toString())
            }
            val items = offlineFiles.map {
                initVideoPlayerItem(it.handle.toLong(), it.name, 200.seconds)
            }
            whenever(getOfflineNodesByParentIdUseCase(testParentId)).thenReturn(offlineFiles)
            offlineFiles.forEachIndexed { index, file ->
                whenever(
                    videoPlayerItemMapper(
                        file.handle.toLong(),
                        file.name,
                        null,
                        getMediaQueueItemType(index, 1),
                        file.totalSize,
                        200.seconds
                    )
                ).thenReturn(items[index])
            }

            whenever(
                intent.getBooleanExtra(
                    INTENT_EXTRA_KEY_IS_PLAYLIST,
                    true
                )
            ).thenReturn(true)

            Mockito.mockStatic(Uri::class.java).use {
                whenever(Uri.parse(testAbsolutePath)).thenReturn(mock())
                underTest.initVideoPlaybackSources(intent)
                underTest.uiState.test {
                    val actual = awaitItem()
                    actual.items.let { items ->
                        assertThat(items).isNotEmpty()
                        assertThat(items.size).isEqualTo(3)
                        items.forEachIndexed { index, item ->
                            assertThat(item).isEqualTo(items[index])
                        }
                    }
                    actual.mediaPlaySources?.let { sources ->
                        assertThat(sources.mediaItems).isNotEmpty()
                        assertThat(sources.mediaItems.size).isEqualTo(3)
                        assertThat(sources.newIndexForCurrentItem).isEqualTo(1)
                    }
                    assertThat(actual.playQueueTitle).isEqualTo(testTitle)
                    assertThat(actual.currentPlayingIndex).isEqualTo(1)
                    assertThat(actual.currentPlayingHandle).isEqualTo(2)
                    cancelAndConsumeRemainingEvents()
                }
            }
        }

    private fun initOfflineFileInfo(
        id: Int,
        handle: String,
    ): OfflineFileInformation {
        val fileTypedInfo = mock<VideoFileTypeInfo> {
            on { isSupported }.thenReturn(true)
            on { duration }.thenReturn(200.seconds)
        }
        return mock<OfflineFileInformation> {
            on { this.id }.thenReturn(id)
            on { name }.thenReturn("test.mp4")
            on { this.handle }.thenReturn(handle)
            on { totalSize }.thenReturn(100)
            on { fileTypeInfo }.thenReturn(fileTypedInfo)
            on { absolutePath }.thenReturn(testAbsolutePath)
        }
    }

    private fun getMediaQueueItemType(currentIndex: Int, playingIndex: Int) =
        when {
            currentIndex == playingIndex -> MediaQueueItemType.Playing
            playingIndex == -1 || currentIndex < playingIndex -> MediaQueueItemType.Previous
            else -> MediaQueueItemType.Next
        }

    @Test
    fun `test that stat is updated correctly when launch source is ZIP_ADAPTER`() =
        runTest {
            val intent = mock<Intent>()
            val testHandle: Long = 1.toString().hashCode().toLong()
            val testFileName = "test.mp4"
            initTestDataForTestingInvalidParams(
                intent = intent,
                rebuildPlaylist = true,
                adapterType = ZIP_ADAPTER,
                data = mock(),
                handle = testHandle,
                fileName = testFileName
            )

            val testZipPath = "test.zip"
            whenever(
                intent.getStringExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY)
            ).thenReturn(testZipPath)
            val testTitle = "video queue title"
            val testFiles: Array<File> = (1..3).map {
                val name = it.toString()
                whenever(getFileTypeInfoByNameUseCase(name)).thenReturn(mock<VideoFileTypeInfo>())
                initFile(name)
            }.toTypedArray()
            val testParentFile = mock<File> {
                on { name }.thenReturn(testTitle)
                on { listFiles() }.thenReturn(testFiles)
            }
            val testFile = mock<File> {
                on { parentFile }.thenReturn(testParentFile)
            }
            whenever(getFileByPathUseCase(testZipPath)).thenReturn(testFile)
            val items = testFiles.map {
                initVideoPlayerItem(it.name.hashCode().toLong(), it.name, 200.seconds)
            }
            testFiles.forEachIndexed { index, file ->
                whenever(
                    videoPlayerItemMapper(
                        file.name.hashCode().toLong(),
                        file.name,
                        null,
                        getMediaQueueItemType(index, 0),
                        file.length(),
                        0.seconds
                    )
                ).thenReturn(items[index])
            }
            whenever(
                intent.getBooleanExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, true)
            ).thenReturn(true)
            Mockito.mockStatic(FileUtil::class.java).use {
                testFiles.forEach { file ->
                    whenever(FileUtil.getUriForFile(context, file)).thenReturn(mock())
                }
                underTest.initVideoPlaybackSources(intent)
                underTest.uiState.test {
                    val actual = awaitItem()
                    actual.items.let { items ->
                        assertThat(items).isNotEmpty()
                        assertThat(items.size).isEqualTo(3)
                        items.forEachIndexed { index, item ->
                            assertThat(item).isEqualTo(items[index])
                        }
                    }
                    actual.mediaPlaySources?.let { sources ->
                        assertThat(sources.mediaItems).isNotEmpty()
                        assertThat(sources.mediaItems.size).isEqualTo(3)
                        assertThat(sources.newIndexForCurrentItem).isEqualTo(0)
                    }
                    assertThat(actual.playQueueTitle).isEqualTo(testTitle)
                    assertThat(actual.currentPlayingIndex).isEqualTo(0)
                    assertThat(actual.currentPlayingHandle).isEqualTo(
                        1.toString().hashCode().toLong()
                    )
                    cancelAndConsumeRemainingEvents()
                }
            }
        }

    private fun initFile(name: String) = mock<File> {
        on { this.name }.thenReturn(name)
        on { this.length() }.thenReturn(100L)
        on { isFile }.thenReturn(true)
    }

    @Test
    fun `test that state is updated correctly when launch source is VIDEO_BROWSE_ADAPTER`() =
        runTest {
            val intent = mock<Intent>()
            testStateIsUpdatedCorrectlyByLaunchSource(
                intent = intent,
                launchSource = VIDEO_BROWSE_ADAPTER
            ) {
                getVideoNodesUseCase(any())
            }
        }

    @ParameterizedTest(name = "when launch source is {0}")
    @ValueSource(ints = [RECENTS_ADAPTER, RECENTS_BUCKET_ADAPTER])
    fun `test that state is updated correctly with node handles`(launchSource: Int) =
        runTest {
            val intent = mock<Intent>()
            whenever(intent.getLongArrayExtra(any())).thenReturn(longArrayOf(1, 2, 3))
            testStateIsUpdatedCorrectlyByLaunchSource(
                intent = intent,
                launchSource = launchSource
            ) {
                getVideoNodesByHandlesUseCase(any())
            }
        }

    @ParameterizedTest(name = "parentHandle is {0}")
    @MethodSource("provideParametersForFolderLink")
    fun `test that state is updated correctly when launch source is FOLDER_LINK_ADAPTER`(
        parentHandle: Long,
        getParentNode: suspend () -> FileNode,
        initSourceData: suspend () -> String,
    ) =
        runTest {
            val intent = mock<Intent>()
            val testParentNode = mock<FileNode> {
                on { name }.thenReturn(testTitle)
            }
            whenever(
                intent.getLongExtra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, INVALID_HANDLE)
            ).thenReturn(parentHandle)
            whenever(getParentNode()).thenReturn(testParentNode)
            whenever(initSourceData()).thenReturn(testAbsolutePath)
            testStateIsUpdatedCorrectlyByLaunchSource(
                intent = intent,
                launchSource = FOLDER_LINK_ADAPTER
            ) {
                getVideosByParentHandleFromMegaApiFolderUseCase(any(), any())
            }
        }

    private fun provideParametersForFolderLink() = listOf(
        arrayOf(
            INVALID_VALUE,
            suspend { getRootNodeFromMegaApiFolderUseCase() },
            suspend { getLocalFolderLinkUseCase(any()) }
        ),
        arrayOf(
            testHandle,
            suspend { getParentNodeFromMegaApiFolderUseCase(any()) },
            suspend { getLocalFolderLinkUseCase(any()) }
        ),
    )

    private suspend fun testStateIsUpdatedCorrectlyByLaunchSource(
        intent: Intent,
        launchSource: Int,
        playingIndex: Int = 1,
        testArray: IntArray = intArrayOf(1, 2, 3),
        queueTitle: String = testTitle,
        initSourceData: suspend () -> List<TypedVideoNode>?,
    ) {
        val testHandle: Long = 2
        val testFileName = "test.mp4"
        initTestDataForTestingInvalidParams(
            intent = intent,
            rebuildPlaylist = true,
            adapterType = launchSource,
            data = mock(),
            handle = testHandle,
            fileName = testFileName
        )

        whenever(context.getString(any())).thenReturn(testTitle)

        val testVideoNodes = testArray.map {
            initVideoNode(it.toLong())
        }
        whenever(initSourceData()).thenReturn(testVideoNodes)
        whenever(getLocalLinkFromMegaApiUseCase(any())).thenReturn(testAbsolutePath)

        val entities = testVideoNodes.map {
            initVideoPlayerItem(it.id.longValue, it.name, it.duration)
        }
        testVideoNodes.forEachIndexed { index, node ->
            whenever(
                videoPlayerItemMapper(
                    node.id.longValue,
                    node.name,
                    null,
                    getMediaQueueItemType(index, playingIndex),
                    node.size,
                    node.duration
                )
            ).thenReturn(entities[index])
        }

        whenever(
            intent.getBooleanExtra(
                INTENT_EXTRA_KEY_IS_PLAYLIST,
                true
            )
        ).thenReturn(true)
        Mockito.mockStatic(Uri::class.java).use {
            whenever(Uri.parse(testAbsolutePath)).thenReturn(mock())
            underTest.initVideoPlaybackSources(intent)
            underTest.uiState.test {
                val actual = awaitItem()
                actual.items.let { items ->
                    assertThat(items).isNotEmpty()
                    assertThat(items.size).isEqualTo(testArray.size)
                    items.forEachIndexed { index, item ->
                        assertThat(item).isEqualTo(entities[index])
                    }
                }
                actual.mediaPlaySources?.let { sources ->
                    assertThat(sources.mediaItems).isNotEmpty()
                    assertThat(sources.mediaItems.size).isEqualTo(testArray.size)
                    assertThat(sources.newIndexForCurrentItem).isEqualTo(playingIndex)
                }
                assertThat(actual.playQueueTitle).isEqualTo(queueTitle)
                assertThat(actual.currentPlayingIndex).isEqualTo(playingIndex)
                assertThat(actual.currentPlayingHandle).isEqualTo(testArray[playingIndex])
                cancelAndConsumeRemainingEvents()
            }
        }
    }

    private fun initVideoNode(handle: Long) =
        mock<TypedVideoNode> {
            on { id }.thenReturn(NodeId(handle))
            on { name }.thenReturn(testFileName)
            on { size }.thenReturn(testSize)
            on { duration }.thenReturn(testDuration)
        }

    @Test
    fun `test that state is updated correctly when launch source is SEARCH_BY_ADAPTER`() =
        runTest {
            val intent = mock<Intent>()
            whenever(intent.getStringExtra(INTENT_EXTRA_KEY_MEDIA_QUEUE_TITLE)).thenReturn(testTitle)
            whenever(intent.getLongArrayExtra(INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH)).thenReturn(
                longArrayOf(1, 2, 3)
            )
            testStateIsUpdatedCorrectlyByLaunchSource(
                intent = intent,
                launchSource = SEARCH_BY_ADAPTER
            ) {
                getVideoNodesByHandlesUseCase(any())
            }
        }

    @Test
    fun `test that state is updated correctly when launch source is CONTACT_FILE_ADAPTER and parentHandle is INVALID_HANDLE`() =
        runTest {
            val intent = mock<Intent>()
            whenever(intent.getStringExtra(INTENT_EXTRA_KEY_CONTACT_EMAIL)).thenReturn("email")
            whenever(getUserNameByEmailUseCase(any())).thenReturn(testTitle)
            initTestDataByParentNode(intent, INVALID_HANDLE) {
                getRootNodeUseCase()
            }
            testStateIsUpdatedCorrectlyByLaunchSource(
                intent = intent,
                launchSource = CONTACT_FILE_ADAPTER,
                queueTitle = "$testTitle $testTitle"
            ) {
                getVideoNodesByEmailUseCase(any())
            }
        }

    @ParameterizedTest(name = "when launch source is {0}, and parentHandle is {1}")
    @MethodSource("provideParameters")
    fun `test that state is updated correctly`(
        launchSource: Int,
        parentHandle: Long,
        queueTitle: String,
        initParentNode: suspend () -> Node?,
        getVideoNodes: suspend () -> List<TypedVideoNode>?,
    ) = runTest {
        val intent = mock<Intent>()
        initTestDataByParentNode(intent, parentHandle) {
            initParentNode()
        }
        testStateIsUpdatedCorrectlyByLaunchSource(
            intent = intent,
            launchSource = launchSource,
            queueTitle = queueTitle
        ) {
            getVideoNodes()
        }
    }

    private fun provideParameters() = createInvalidTestParameters() + createValidTestParameters()

    private fun createInvalidTestParameters() = listOf(
        createTestParameterWithInvalidHandle(
            launchSource = FAVOURITES_ADAPTER,
            initParentNode = { getRootNodeUseCase() },
        ),
        createTestParameterWithInvalidHandle(
            launchSource = FROM_ALBUM_SHARING,
            initParentNode = { getRootNodeUseCase() },
        ),
        createTestParameterWithInvalidHandle(
            launchSource = FROM_IMAGE_VIEWER,
            initParentNode = { getRootNodeUseCase() },
        ),
        createTestParameterWithInvalidHandle(
            launchSource = FROM_MEDIA_DISCOVERY,
            initParentNode = { getRootNodeUseCase() },
            getVideoNodes = { getVideosBySearchTypeUseCase(any(), any(), any(), any()) },
        ),
        createTestParameterWithValidHandle(
            launchSource = FROM_MEDIA_DISCOVERY,
            getVideoNodes = { getVideosBySearchTypeUseCase(any(), any(), any(), any()) },
        ),
        createTestParameterWithInvalidHandle(
            launchSource = OUTGOING_SHARES_ADAPTER,
            initParentNode = { getRootNodeUseCase() },
            getVideoNodes = { getVideoNodesFromOutSharesUseCase(any(), any()) }
        ),
        createTestParameterWithInvalidHandle(
            launchSource = INCOMING_SHARES_ADAPTER,
            initParentNode = { getRootNodeUseCase() },
            getVideoNodes = { getVideoNodesFromInSharesUseCase(any()) }
        ),
        createTestParameterWithInvalidHandle(
            launchSource = LINKS_ADAPTER,
            initParentNode = { getRootNodeUseCase() },
            getVideoNodes = { getVideoNodesFromPublicLinksUseCase(any()) }
        ),
        createTestParameterWithInvalidHandle(
            launchSource = FILE_BROWSER_ADAPTER,
            initParentNode = { getRootNodeUseCase() },
        ),
        createTestParameterWithInvalidHandle(
            launchSource = BACKUPS_ADAPTER,
            initParentNode = { getBackupsNodeUseCase() },
        ),
        createTestParameterWithInvalidHandle(
            launchSource = RUBBISH_BIN_ADAPTER,
            initParentNode = { getRubbishNodeUseCase() },
        ),
    )

    private fun createValidTestParameters() = listOf(
        createTestParameterWithValidHandle(launchSource = FAVOURITES_ADAPTER),
        createTestParameterWithValidHandle(launchSource = FROM_ALBUM_SHARING),
        createTestParameterWithValidHandle(launchSource = FROM_IMAGE_VIEWER),
        createTestParameterWithValidHandle(launchSource = CONTACT_FILE_ADAPTER),
        createTestParameterWithValidHandle(launchSource = OUTGOING_SHARES_ADAPTER),
        createTestParameterWithValidHandle(launchSource = INCOMING_SHARES_ADAPTER),
        createTestParameterWithValidHandle(launchSource = LINKS_ADAPTER),
        createTestParameterWithValidHandle(launchSource = FILE_BROWSER_ADAPTER),
        createTestParameterWithValidHandle(launchSource = BACKUPS_ADAPTER),
        createTestParameterWithValidHandle(launchSource = RUBBISH_BIN_ADAPTER),
    )

    private fun createTestParameterWithInvalidHandle(
        launchSource: Int,
        initParentNode: suspend () -> Node? = { getVideoNodeByHandleUseCase(any(), any()) },
        getVideoNodes: suspend () -> List<TypedVideoNode>? =
            { getVideoNodesByParentHandleUseCase(any(), any()) },
    ) = arrayOf(launchSource, INVALID_HANDLE, testTitle, initParentNode, getVideoNodes)

    private fun createTestParameterWithValidHandle(
        launchSource: Int,
        initParentNode: suspend () -> Node? = { getVideoNodeByHandleUseCase(any(), any()) },
        getVideoNodes: suspend () -> List<TypedVideoNode>? =
            { getVideoNodesByParentHandleUseCase(any(), any()) },
    ) = arrayOf(launchSource, testHandle, testTitle, initParentNode, getVideoNodes)

    private suspend fun initTestDataByParentNode(
        intent: Intent,
        parentHandle: Long,
        initParentNode: suspend () -> Node? = { getVideoNodeByHandleUseCase(any(), any()) },
    ) {
        val testParentNode = mock<TypedVideoNode> {
            on { name }.thenReturn(testTitle)
        }
        whenever(
            intent.getLongExtra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, INVALID_HANDLE)
        ).thenReturn(parentHandle)
        whenever(initParentNode()).thenReturn(testParentNode)
    }
}