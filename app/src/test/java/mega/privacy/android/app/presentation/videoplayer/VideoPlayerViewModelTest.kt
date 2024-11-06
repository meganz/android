package mega.privacy.android.app.presentation.videoplayer

import android.content.Context
import android.content.Intent
import android.net.Uri
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.TimberJUnit5Extension
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemType
import mega.privacy.android.app.presentation.myaccount.InstantTaskExecutorExtension
import mega.privacy.android.app.presentation.videoplayer.mapper.VideoPlayerItemMapper
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerItem
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IS_PLAYLIST
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.VIDEO_BROWSE_ADAPTER
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.exception.BlockedMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.GetOfflineNodesByParentIdUseCase
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerStartUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerStopUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStopUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodeByHandleUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineNodeInformationByIdUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
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
    private val getThumbnailUseCase = mock<GetThumbnailUseCase>()
    private val hasCredentialsUseCase = mock<HasCredentialsUseCase>()
    private val megaApiFolderHttpServerIsRunningUseCase =
        mock<MegaApiFolderHttpServerIsRunningUseCase>()
    private val megaApiFolderHttpServerStartUseCase = mock<MegaApiFolderHttpServerStartUseCase>()
    private val megaApiFolderHttpServerStopUseCase = mock<MegaApiFolderHttpServerStopUseCase>()
    private val megaApiHttpServerIsRunningUseCase = mock<MegaApiHttpServerIsRunningUseCase>()
    private val megaApiHttpServerStartUseCase = mock<MegaApiHttpServerStartUseCase>()
    private val megaApiHttpServerStop = mock<MegaApiHttpServerStopUseCase>()
    private val getLocalFolderLinkFromMegaApiFolderUseCase =
        mock<GetLocalFolderLinkFromMegaApiFolderUseCase>()
    private val getLocalFolderLinkFromMegaApiUseCase = mock<GetLocalFolderLinkFromMegaApiUseCase>()
    private val getOfflineNodeInformationByIdUseCase = mock<GetOfflineNodeInformationByIdUseCase>()
    private val getOfflineNodesByParentIdUseCase = mock<GetOfflineNodesByParentIdUseCase>()
    private val monitorTransferEventsUseCase = mock<MonitorTransferEventsUseCase>()
    private val fakeMonitorTransferEventsFlow =
        MutableSharedFlow<TransferEvent.TransferTemporaryErrorEvent>()

    private val testHandle: Long = 123456
    private val testFileName = "test.mp4"
    private val testSize = 100L
    private val testDuration = 200.seconds
    private val testAbsolutePath = "https://www.example.com"

    private fun initViewModel() {
        underTest = VideoPlayerViewModel(
            context = context,
            mediaPlayerGateway = mediaPlayerGateway,
            applicationScope = CoroutineScope(UnconfinedTestDispatcher()),
            ioDispatcher = UnconfinedTestDispatcher(),
            videoPlayerItemMapper = videoPlayerItemMapper,
            getVideoNodeByHandleUseCase = getVideoNodeByHandleUseCase,
            getThumbnailUseCase = getThumbnailUseCase,
            hasCredentialsUseCase = hasCredentialsUseCase,
            megaApiFolderHttpServerIsRunningUseCase = megaApiFolderHttpServerIsRunningUseCase,
            megaApiFolderHttpServerStartUseCase = megaApiFolderHttpServerStartUseCase,
            megaApiFolderHttpServerStopUseCase = megaApiFolderHttpServerStopUseCase,
            megaApiHttpServerIsRunningUseCase = megaApiHttpServerIsRunningUseCase,
            megaApiHttpServerStartUseCase = megaApiHttpServerStartUseCase,
            megaApiHttpServerStop = megaApiHttpServerStop,
            getLocalFolderLinkFromMegaApiFolderUseCase = getLocalFolderLinkFromMegaApiFolderUseCase,
            getLocalFolderLinkFromMegaApiUseCase = getLocalFolderLinkFromMegaApiUseCase,
            getOfflineNodeInformationByIdUseCase = getOfflineNodeInformationByIdUseCase,
            getOfflineNodesByParentIdUseCase = getOfflineNodesByParentIdUseCase,
            monitorTransferEventsUseCase = monitorTransferEventsUseCase,
        )
    }

    @BeforeEach
    fun setUp() {
        whenever(monitorTransferEventsUseCase()).thenReturn(fakeMonitorTransferEventsFlow)
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
            getThumbnailUseCase,
            hasCredentialsUseCase,
            megaApiFolderHttpServerIsRunningUseCase,
            megaApiFolderHttpServerStartUseCase,
            megaApiFolderHttpServerStopUseCase,
            megaApiHttpServerIsRunningUseCase,
            megaApiHttpServerStartUseCase,
            megaApiHttpServerStop,
            getLocalFolderLinkFromMegaApiFolderUseCase,
            getLocalFolderLinkFromMegaApiUseCase,
            getOfflineNodeInformationByIdUseCase,
            getOfflineNodesByParentIdUseCase,
            monitorTransferEventsUseCase,
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
    fun `test that the isRetry is false when currentPlayingUri is null when hasCredentialsUseCase is true`() =
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
            whenever(hasCredentialsUseCase()).thenReturn(true)
            whenever(getLocalFolderLinkFromMegaApiUseCase(any())).thenReturn(null)
            underTest.initVideoPlaybackSources(intent)
            underTest.uiState.test {
                assertThat(awaitItem().isRetry).isFalse()
            }
        }

    @Test
    fun `test that the isRetry is false when currentPlayingUri is null when hasCredentialsUseCase is false`() =
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
            whenever(hasCredentialsUseCase()).thenReturn(false)
            whenever(getLocalFolderLinkFromMegaApiFolderUseCase(any())).thenReturn(null)
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
            on { id }.thenReturn(NodeId(testHandle))
            on { name }.thenReturn(testFileName)
            on { size }.thenReturn(testSize)
            on { duration }.thenReturn(testDuration)
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
}