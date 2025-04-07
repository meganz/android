package mega.privacy.android.app.presentation.videoplayer

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.media3.common.MediaItem
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.jraska.livedata.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.AnalyticsTestExtension
import mega.privacy.android.app.R
import mega.privacy.android.app.TimberJUnit5Extension
import mega.privacy.android.app.data.extensions.observeOnce
import mega.privacy.android.app.featuretoggle.ApiFeatures
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemType
import mega.privacy.android.app.mediaplayer.service.Metadata
import mega.privacy.android.app.presentation.myaccount.InstantTaskExecutorExtension
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent.DownloadTriggerEvent
import mega.privacy.android.app.presentation.videoplayer.mapper.LaunchSourceMapper
import mega.privacy.android.app.presentation.videoplayer.mapper.VideoPlayerItemMapper
import mega.privacy.android.app.presentation.videoplayer.model.MediaPlaybackState
import mega.privacy.android.app.presentation.videoplayer.model.MenuOptionClickedContent
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerItem
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerAddToAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerChatImportAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerCopyAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerDownloadAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerFileInfoAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerGetLinkAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerHideAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerMoveAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerRemoveAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerRemoveLinkAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerRenameAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerRubbishBinAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerSaveForOfflineAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerSendToChatAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerShareAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerUnhideAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoSize
import mega.privacy.android.app.triggeredContent
import mega.privacy.android.app.utils.Constants.BACKUPS_ADAPTER
import mega.privacy.android.app.utils.Constants.CONTACT_FILE_ADAPTER
import mega.privacy.android.app.utils.Constants.EXTRA_SERIALIZE_STRING
import mega.privacy.android.app.utils.Constants.FAVOURITES_ADAPTER
import mega.privacy.android.app.utils.Constants.FILE_BROWSER_ADAPTER
import mega.privacy.android.app.utils.Constants.FILE_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FROM_ALBUM_SHARING
import mega.privacy.android.app.utils.Constants.FROM_CHAT
import mega.privacy.android.app.utils.Constants.FROM_IMAGE_VIEWER
import mega.privacy.android.app.utils.Constants.FROM_MEDIA_DISCOVERY
import mega.privacy.android.app.utils.Constants.INCOMING_SHARES_ADAPTER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CHAT_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CONTACT_EMAIL
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IS_PLAYLIST
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MEDIA_QUEUE_TITLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MSG_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_VIDEO_COLLECTION_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_VIDEO_COLLECTION_TITLE
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.LINKS_ADAPTER
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.OUTGOING_SHARES_ADAPTER
import mega.privacy.android.app.utils.Constants.RECENTS_ADAPTER
import mega.privacy.android.app.utils.Constants.RECENTS_BUCKET_ADAPTER
import mega.privacy.android.app.utils.Constants.RUBBISH_BIN_ADAPTER
import mega.privacy.android.app.utils.Constants.SEARCH_BY_ADAPTER
import mega.privacy.android.app.utils.Constants.URL_FILE_LINK
import mega.privacy.android.app.utils.Constants.VIDEO_BROWSE_ADAPTER
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeNameCollisionWithActionResult
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.exception.BlockedMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetFileTypeInfoByNameUseCase
import mega.privacy.android.domain.usecase.GetLocalFilePathUseCase
import mega.privacy.android.domain.usecase.GetLocalLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.GetOfflineNodesByParentIdUseCase
import mega.privacy.android.domain.usecase.GetParentNodeFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.GetRootNodeFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.GetRubbishNodeUseCase
import mega.privacy.android.domain.usecase.GetUserNameByEmailUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.call.IsParticipatingInChatCallUseCase
import mega.privacy.android.domain.usecase.chat.message.delete.DeleteNodeAttachmentMessageByIdsUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.GetFileByPathUseCase
import mega.privacy.android.domain.usecase.file.GetFileUriUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeFromSerializedDataUseCase
import mega.privacy.android.domain.usecase.folderlink.GetPublicChildNodeFromIdUseCase
import mega.privacy.android.domain.usecase.mediaplayer.GetLocalFolderLinkUseCase
import mega.privacy.android.domain.usecase.mediaplayer.HttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.HttpServerStartUseCase
import mega.privacy.android.domain.usecase.mediaplayer.HttpServerStopUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.CanRemoveFromChatUseCase
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
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.MonitorVideoRepeatModeUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.SetVideoRepeatModeUseCase
import mega.privacy.android.domain.usecase.node.CheckChatNodesNameCollisionAndCopyUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionWithActionUseCase
import mega.privacy.android.domain.usecase.node.DisableExportUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInCloudDriveUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.backup.GetBackupsNodeUseCase
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineNodeInformationByIdUseCase
import mega.privacy.android.domain.usecase.photos.GetPublicAlbumNodeDataUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.setting.MonitorSubFolderMediaDiscoverySettingsUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.videosection.SaveVideoRecentlyWatchedUseCase
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import mega.privacy.mobile.analytics.event.VideoPlayerFullScreenPressedEvent
import mega.privacy.mobile.analytics.event.VideoPlayerGetLinkMenuToolbarEvent
import mega.privacy.mobile.analytics.event.VideoPlayerOriginalPressedEvent
import mega.privacy.mobile.analytics.event.VideoPlayerRemoveLinkMenuToolbarEvent
import mega.privacy.mobile.analytics.event.VideoPlayerSaveToDeviceMenuToolbarEvent
import mega.privacy.mobile.analytics.event.VideoPlayerShareMenuToolbarEvent
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import java.io.File
import java.time.Instant
import kotlin.Boolean
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.use

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
    private val monitorVideoRepeatModeUseCase = mock<MonitorVideoRepeatModeUseCase>()
    private val saveVideoRecentlyWatchedUseCase = mock<SaveVideoRecentlyWatchedUseCase>()
    private val setVideoRepeatModeUseCase = mock<SetVideoRepeatModeUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val fakeMonitorAccountDetailFlow = MutableSharedFlow<AccountDetail>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    private val isHiddenNodesOnboardedUseCase = mock<IsHiddenNodesOnboardedUseCase>()
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase>()
    private val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase>()
    private val canRemoveFromChatUseCase = mock<CanRemoveFromChatUseCase>()
    private val isNodeInRubbishBinUseCase = mock<IsNodeInRubbishBinUseCase>()
    private val isNodeInBackupsNodeUseCase = mock<IsNodeInBackupsUseCase>()
    private val isNodeInCloudDriveUseCase = mock<IsNodeInCloudDriveUseCase>()
    private val checkChatNodesNameCollisionAndCopyUseCase =
        mock<CheckChatNodesNameCollisionAndCopyUseCase>()
    private val getPublicAlbumNodeDataUseCase = mock<GetPublicAlbumNodeDataUseCase>()
    private val getChatFileUseCase = mock<GetChatFileUseCase>()
    private val getPublicNodeFromSerializedDataUseCase =
        mock<GetPublicNodeFromSerializedDataUseCase>()
    private val getPublicChildNodeFromIdUseCase = mock<GetPublicChildNodeFromIdUseCase>()
    private val getFileUriUseCase = mock<GetFileUriUseCase>()
    private val disableExportUseCase = mock<DisableExportUseCase>()
    private val isAvailableOfflineUseCase = mock<IsAvailableOfflineUseCase>()
    private val updateNodeSensitiveUseCase = mock<UpdateNodeSensitiveUseCase>()
    private val checkNodesNameCollisionWithActionUseCase =
        mock<CheckNodesNameCollisionWithActionUseCase>()
    private val deleteNodeAttachmentMessageByIdsUseCase =
        mock<DeleteNodeAttachmentMessageByIdsUseCase>()
    private val fakeMonitorNodeUpdatesFlow = MutableSharedFlow<NodeUpdate>()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase>()
    private val durationInSecondsTextMapper = mock<DurationInSecondsTextMapper>()
    private val isParticipatingInChatCallUseCase = mock<IsParticipatingInChatCallUseCase>()
    private val launchSourceMapper = mock<LaunchSourceMapper>()
    private val savedStateHandle = SavedStateHandle(mapOf())

    private val testHandle: Long = 123456
    private val testFileName = "test.mp4"
    private val testSize = 100L
    private val testDuration = 200.seconds
    private val testDurationString = "3:20"
    private val testAbsolutePath = "https://www.example.com"
    private val testTitle = "video queue title"
    private val expectedCollectionId = 123456L
    private val expectedCollectionTitle = "collection title"
    private val expectedChatId = 1000L
    private val expectedMessageId = 2000L

    private fun initViewModel() {
        underTest = VideoPlayerViewModel(
            context = context,
            mediaPlayerGateway = mediaPlayerGateway,
            applicationScope = CoroutineScope(UnconfinedTestDispatcher()),
            mainDispatcher = UnconfinedTestDispatcher(),
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
            monitorVideoRepeatModeUseCase = monitorVideoRepeatModeUseCase,
            saveVideoRecentlyWatchedUseCase = saveVideoRecentlyWatchedUseCase,
            setVideoRepeatModeUseCase = setVideoRepeatModeUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            isHiddenNodesOnboardedUseCase = isHiddenNodesOnboardedUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            canRemoveFromChatUseCase = canRemoveFromChatUseCase,
            isNodeInRubbishBinUseCase = isNodeInRubbishBinUseCase,
            isNodeInBackupsNodeUseCase = isNodeInBackupsNodeUseCase,
            isNodeInCloudDriveUseCase = isNodeInCloudDriveUseCase,
            checkChatNodesNameCollisionAndCopyUseCase = checkChatNodesNameCollisionAndCopyUseCase,
            getPublicAlbumNodeDataUseCase = getPublicAlbumNodeDataUseCase,
            getChatFileUseCase = getChatFileUseCase,
            getPublicNodeFromSerializedDataUseCase = getPublicNodeFromSerializedDataUseCase,
            getPublicChildNodeFromIdUseCase = getPublicChildNodeFromIdUseCase,
            getFileUriUseCase = getFileUriUseCase,
            disableExportUseCase = disableExportUseCase,
            isAvailableOfflineUseCase = isAvailableOfflineUseCase,
            updateNodeSensitiveUseCase = updateNodeSensitiveUseCase,
            checkNodesNameCollisionWithActionUseCase = checkNodesNameCollisionWithActionUseCase,
            deleteNodeAttachmentMessageByIdsUseCase = deleteNodeAttachmentMessageByIdsUseCase,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            launchSourceMapper = launchSourceMapper,
            savedStateHandle = savedStateHandle,
            durationInSecondsTextMapper = durationInSecondsTextMapper,
            isParticipatingInChatCallUseCase = isParticipatingInChatCallUseCase
        )
        savedStateHandle[INTENT_EXTRA_KEY_VIDEO_COLLECTION_ID] = expectedCollectionId
        savedStateHandle[INTENT_EXTRA_KEY_VIDEO_COLLECTION_TITLE] = expectedCollectionTitle
        savedStateHandle[INTENT_EXTRA_KEY_CHAT_ID] = expectedChatId
        savedStateHandle[INTENT_EXTRA_KEY_MSG_ID] = expectedMessageId
    }

    @BeforeEach
    fun setUp() {
        whenever(monitorTransferEventsUseCase()).thenReturn(fakeMonitorTransferEventsFlow)
        whenever(monitorSubFolderMediaDiscoverySettingsUseCase()).thenReturn(flowOf(true))
        wheneverBlocking { monitorNodeUpdatesUseCase() }.thenReturn(fakeMonitorNodeUpdatesFlow)
        wheneverBlocking { monitorAccountDetailUseCase() }.thenReturn(fakeMonitorAccountDetailFlow)
        wheneverBlocking { monitorShowHiddenItemsUseCase() }.thenReturn(flowOf(true))
        wheneverBlocking { isHiddenNodesOnboardedUseCase() }.thenReturn(false)
        wheneverBlocking {
            monitorVideoRepeatModeUseCase()
        }.thenReturn(flowOf(RepeatToggleMode.REPEAT_NONE))
        wheneverBlocking {
            getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)
        }.thenReturn(true)
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
            monitorVideoRepeatModeUseCase,
            saveVideoRecentlyWatchedUseCase,
            setVideoRepeatModeUseCase,
            getFeatureFlagValueUseCase,
            monitorAccountDetailUseCase,
            isHiddenNodesOnboardedUseCase,
            monitorShowHiddenItemsUseCase,
            getBusinessStatusUseCase,
            canRemoveFromChatUseCase,
            isNodeInRubbishBinUseCase,
            isNodeInBackupsNodeUseCase,
            isNodeInCloudDriveUseCase,
            checkChatNodesNameCollisionAndCopyUseCase,
            getPublicAlbumNodeDataUseCase,
            getChatFileUseCase,
            getPublicNodeFromSerializedDataUseCase,
            getPublicChildNodeFromIdUseCase,
            getFileUriUseCase,
            disableExportUseCase,
            isAvailableOfflineUseCase,
            updateNodeSensitiveUseCase,
            checkNodesNameCollisionWithActionUseCase,
            deleteNodeAttachmentMessageByIdsUseCase,
            monitorNodeUpdatesUseCase,
            launchSourceMapper,
            durationInSecondsTextMapper,
            isParticipatingInChatCallUseCase
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
        launchSource: Int? = null,
        data: Uri? = null,
        handle: Long? = null,
        fileName: String? = null,
    ) {
        rebuildPlaylist?.let {
            whenever(intent.getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true)).thenReturn(it)
        }
        launchSource?.let {
            savedStateHandle[INTENT_EXTRA_KEY_ADAPTER_TYPE] = launchSource
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
                launchSource = INVALID_VALUE
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
                launchSource = FOLDER_LINK_ADAPTER,
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
                launchSource = FOLDER_LINK_ADAPTER,
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
                launchSource = FOLDER_LINK_ADAPTER,
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
                launchSource = FOLDER_LINK_ADAPTER,
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
                launchSource = VIDEO_BROWSE_ADAPTER,
                data = uri,
                handle = testHandle,
                fileName = testFileName
            )
            underTest.initVideoPlaybackSources(intent)
            advanceUntilIdle()
            underTest.uiState.test {
                val actual = awaitItem()
                actual.mediaPlaySources?.let { sources ->
                    assertThat(sources.mediaItems).isNotEmpty()
                    assertThat(sources.mediaItems.size).isEqualTo(1)
                    assertThat(sources.mediaItems[0].mediaId).isEqualTo(testHandle.toString())
                    assertThat(sources.newIndexForCurrentItem).isEqualTo(INVALID_VALUE)
                    assertThat(sources.nameToDisplay).isEqualTo(testFileName)
                }
                assertThat(actual.metadata.nodeName).isEqualTo(testFileName)
            }
        }

    @Test
    fun `test that items is updated correctly when INTENT_EXTRA_KEY_IS_PLAYLIST is false`() =
        runTest {
            val intent = mock<Intent>()

            initTestDataForTestingInvalidParams(
                intent = intent,
                rebuildPlaylist = true,
                launchSource = VIDEO_BROWSE_ADAPTER,
                data = mock(),
                handle = testHandle,
                fileName = testFileName
            )
            val node = initTypedVideoNode()
            val videoPlayerItem = initVideoPlayerItem(testHandle, testFileName)
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

    private fun initVideoPlayerItem(
        handle: Long,
        name: String,
        type: MediaQueueItemType? = null,
        isSelect: Boolean = false,
    ) =
        mock<VideoPlayerItem> {
            on { nodeHandle }.thenReturn(handle)
            on { nodeName }.thenReturn(name)
            on { this.duration }.thenReturn(testDurationString)
            on { this.type }.thenReturn(type)
            on { this.isSelected }.thenReturn(isSelect)
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
                launchSource = OFFLINE_ADAPTER,
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
                initVideoPlayerItem(it.handle.toLong(), it.name)
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

            mockStatic(Uri::class.java).use {
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
                launchSource = ZIP_ADAPTER,
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
                initVideoPlayerItem(it.name.hashCode().toLong(), it.name)
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
            mockStatic(FileUtil::class.java).use {
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
            launchSource = launchSource,
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
        whenever(httpServerIsRunningUseCase(any())).thenReturn(1)

        val entities = testVideoNodes.map {
            initVideoPlayerItem(it.id.longValue, it.name)
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
        mockStatic(Uri::class.java).use {
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

    @Test
    fun `test that metadata is updated correctly`() = runTest {
        initViewModel()
        val testTitle = "title"
        val testArist = "artist"
        val testAlbum = "album"
        val testNodeName = "nodeName"
        val testMetadata = Metadata(
            title = testTitle,
            artist = testArist,
            album = testAlbum,
            nodeName = testNodeName
        )
        underTest.updateMetadata(testMetadata)
        testScheduler.advanceUntilIdle()
        underTest.uiState.test {
            val actual = awaitItem()
            assertThat(actual.metadata.title).isEqualTo(testTitle)
            assertThat(actual.metadata.artist).isEqualTo(testArist)
            assertThat(actual.metadata.album).isEqualTo(testAlbum)
            assertThat(actual.metadata.nodeName).isEqualTo(testNodeName)
        }
    }

    @Test
    fun `test that currentPlayingVideoSize is updated correctly`() = runTest {
        initViewModel()
        val testWidth = 1920
        val testHeight = 1080
        val testVideoSize = VideoSize(width = testWidth, height = testHeight)
        underTest.updateCurrentPlayingVideoSize(testVideoSize)
        testScheduler.advanceUntilIdle()
        underTest.uiState.test {
            val actual = awaitItem()
            assertThat(actual.currentPlayingVideoSize?.width).isEqualTo(testWidth)
            assertThat(actual.currentPlayingVideoSize?.height).isEqualTo(testHeight)
        }
    }

    @Test
    fun `test that state is updated correctly after updateCurrentPlayingHandle is invoked`() =
        runTest {
            val testHandle = 2L
            val handleNotInItems = 4L
            val testItems = (1..3).map {
                initVideoPlayerItem(it.toLong(), it.toString())
            }
            whenever(launchSourceMapper(any(), any(), any(), any(), any(), any())).thenReturn(
                emptyList()
            )
            val testItem = initVideoPlayerItem(2.toLong(), "2", MediaQueueItemType.Playing)
            whenever(getVideoNodeByHandleUseCase(any(), any())).thenReturn(mock())
            whenever(testItems[1].copy(type = MediaQueueItemType.Playing)).thenReturn(testItem)
            initViewModel()
            underTest.updateCurrentPlayingHandle(testHandle, testItems)
            testScheduler.advanceUntilIdle()
            underTest.uiState.test {
                awaitItem().let {
                    assertThat(it.currentPlayingIndex).isEqualTo(
                        testItems.indexOfFirst { it.nodeHandle == testHandle }
                    )
                    assertThat(it.currentPlayingHandle).isEqualTo(testHandle)
                    assertThat(it.items[1].type).isEqualTo(MediaQueueItemType.Playing)
                }
                underTest.updateCurrentPlayingHandle(handleNotInItems, testItems)
                awaitItem().let {
                    assertThat(it.currentPlayingIndex).isEqualTo(0)
                    assertThat(it.currentPlayingHandle).isEqualTo(handleNotInItems)
                }
            }
        }

    @Test
    fun `test that correct functions are invoked after setRepeatToggleModeForPlayer is invoked`() =
        runTest {
            val testMode = RepeatToggleMode.REPEAT_ONE
            initViewModel()
            underTest.setRepeatToggleModeForPlayer(testMode)
            verify(setVideoRepeatModeUseCase).invoke(testMode.ordinal)
            verify(mediaPlayerGateway).setRepeatToggleMode(testMode)
        }

    @Test
    fun `test that updateRepeatToggleMode is updated correctly`() =
        runTest {
            val testRepeatOneMode = RepeatToggleMode.REPEAT_ONE
            val testRepeatNoneMode = RepeatToggleMode.REPEAT_NONE
            initViewModel()
            underTest.updateRepeatToggleMode(testRepeatOneMode)
            testScheduler.advanceUntilIdle()
            underTest.uiState.test {
                assertThat(awaitItem().repeatToggleMode).isEqualTo(testRepeatOneMode)
                underTest.updateRepeatToggleMode(testRepeatNoneMode)
                assertThat(awaitItem().repeatToggleMode).isEqualTo(testRepeatNoneMode)
            }
        }

    @Test
    fun `test that saveVideoRecentlyWatchedUseCase is invoked as expected when saveVideoWatchedTime is called`() =
        runTest {
            val expectedId = 1L
            val instant = Instant.ofEpochMilli(2000L)
            mockStatic(Instant::class.java).use {
                it.`when`<Instant> { Instant.now() }.thenReturn(instant)
                val testMediaItem = MediaItem.Builder()
                    .setMediaId(expectedId.toString())
                    .build()
                whenever(mediaPlayerGateway.getCurrentMediaItem()).thenReturn(testMediaItem)
                underTest.saveVideoWatchedTime()

                verify(saveVideoRecentlyWatchedUseCase).invoke(
                    expectedId,
                    2,
                    expectedCollectionId,
                    expectedCollectionTitle
                )
            }
        }

    @Test
    fun `test that mediaPlaybackState is updated correctly`() = runTest {
        val testPlayingState = MediaPlaybackState.Playing
        val testPausedState = MediaPlaybackState.Paused
        initViewModel()
        underTest.updatePlaybackState(testPlayingState)
        testScheduler.advanceUntilIdle()
        underTest.uiState.test {
            assertThat(awaitItem().mediaPlaybackState).isEqualTo(testPlayingState)
            underTest.updatePlaybackState(testPausedState)
            assertThat(awaitItem().mediaPlaybackState).isEqualTo(testPausedState)
        }
    }

    @Test
    fun `test that snackBarMessage is updated correctly`() = runTest {
        val testMessage = "test message"
        initViewModel()
        underTest.updateSnackBarMessage(testMessage)
        testScheduler.advanceUntilIdle()
        underTest.uiState.test {
            assertThat(awaitItem().snackBarMessage).isEqualTo(testMessage)
            underTest.updateSnackBarMessage(null)
            assertThat(awaitItem().snackBarMessage).isNull()
        }
    }

    @Test
    fun `test that isRetry is updated correctly after onPlayerError is invoked more than 6 times`() =
        runTest {
            initViewModel()
            underTest.uiState.drop(1).test {
                underTest.onPlayerError()
                assertThat(awaitItem().isRetry).isTrue()
                repeat(6) {
                    underTest.onPlayerError()
                }
                assertThat(awaitItem().isRetry).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `the state is updated correctly when monitorAccountDetailUseCase is triggered`() =
        runTest {
            val testAccountType = mock<AccountType> {
                on { isBusinessAccount }.thenReturn(true)
            }
            whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Expired)
            val testActions = listOf<VideoPlayerMenuAction>(
                VideoPlayerDownloadAction,
                VideoPlayerFileInfoAction,
            )
            initLaunchSourceMapperReturned(testActions)
            emitAccountDetail(testAccountType)
            advanceUntilIdle()
            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.accountType?.isBusinessAccount).isTrue()
                assertThat(actual.isBusinessAccountExpired).isTrue()
                assertThat(actual.hiddenNodeEnabled).isTrue()
                assertThat(actual.menuActions).isNotEmpty()
                assertThat(actual.menuActions.size).isEqualTo(testActions.size)
                testActions.onEachIndexed { index, item ->
                    assertThat(actual.menuActions[index]).isEqualTo(item)
                }
                cancelAndConsumeRemainingEvents()
            }
        }

    private suspend fun emitAccountDetail(accountType: AccountType) {
        val testLevelDetail = mock<AccountLevelDetail> {
            on { this.accountType }.thenReturn(accountType)
        }
        val testAccountDetail = mock<AccountDetail> {
            on { levelDetail }.thenReturn(testLevelDetail)
        }
        fakeMonitorAccountDetailFlow.emit(testAccountDetail)
    }

    private suspend fun initLaunchSourceMapperReturned(actions: List<VideoPlayerMenuAction>) {
        whenever(getVideoNodeByHandleUseCase(any(), any())).thenReturn(mock())
        whenever(launchSourceMapper(any(), any(), any(), any(), any(), any())).thenReturn(actions)
    }

    @Test
    fun `the state is updated correctly when updateCurrentPlayingHandle is invoked`() =
        runTest {
            val testHandle = 1L
            val testItems: List<VideoPlayerItem> = listOf(
                initVideoPlayerItem(2L, ""),
                initVideoPlayerItem(testHandle, testHandle.toString()),
            )
            val testActions = listOf<VideoPlayerMenuAction>(
                VideoPlayerDownloadAction,
                VideoPlayerFileInfoAction,
            )
            initLaunchSourceMapperReturned(testActions)
            underTest.updateCurrentPlayingHandle(testHandle, testItems)
            advanceUntilIdle()
            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.currentPlayingHandle).isEqualTo(testHandle)
                assertThat(actual.currentPlayingIndex).isEqualTo(
                    testItems.indexOfFirst { it.nodeHandle == testHandle }
                )
                assertThat(actual.menuActions).isNotEmpty()
                assertThat(actual.menuActions.size).isEqualTo(testActions.size)
                testActions.onEachIndexed { index, item ->
                    assertThat(actual.menuActions[index]).isEqualTo(item)
                }
                cancelAndConsumeRemainingEvents()
            }
        }

    fun `test that isVideoOptionPopupShown is updated correctly`() = runTest {
        initViewModel()
        underTest.updateIsVideoOptionPopupShown(true)
        testScheduler.advanceUntilIdle()
        underTest.uiState.test {
            assertThat(awaitItem().isVideoOptionPopupShown).isTrue()
            underTest.updateIsVideoOptionPopupShown(false)
            assertThat(awaitItem().isVideoOptionPopupShown).isFalse()
        }
    }

    @Test
    internal fun `test that copy complete snack bar message is shown when node is copied to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.COPY,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 0
                )
            )

            underTest.copyNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            testScheduler.advanceUntilIdle()

            underTest.onSnackbarMessage().test().assertValue(R.string.context_correctly_copied)
        }

    @Test
    internal fun `test that copy error snack bar message is shown when node is not copied to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.COPY,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 1
                )
            )

            underTest.copyNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            testScheduler.advanceUntilIdle()

            underTest.onSnackbarMessage().test().assertValue(R.string.context_no_copied)
        }

    @Test
    internal fun `test that onExceptionThrown is triggered when copy failed`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            val runtimeException = RuntimeException("Copy node failed")
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.COPY,
                )
            ).thenThrow(runtimeException)
            underTest.copyNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.onExceptionThrown().test().assertValue(runtimeException)
        }

    @Test
    internal fun `test move complete snack bar message is shown when node is moved to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.MOVE,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 0
                )
            )
            underTest.moveNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.onSnackbarMessage().observeOnce {
                assertThat(it).isEqualTo(R.string.context_correctly_moved)
            }
        }

    @Test
    internal fun `test move error snack bar message is shown when node is not moved to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.MOVE,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 1
                )
            )
            underTest.moveNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.onSnackbarMessage().observeOnce {
                assertThat(it).isEqualTo(R.string.context_no_moved)
            }
        }

    @Test
    internal fun `test that onExceptionThrown is triggered when move failed`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            val runtimeException = RuntimeException("Move node failed")
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.MOVE,
                )
            ).thenThrow(runtimeException)

            underTest.moveNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()

            underTest.onExceptionThrown().test().assertValue(runtimeException)
        }

    @Test
    internal fun `test that copy complete snack bar message is shown when chat node is imported to different directory`() =
        runTest {
            val newParentNode = NodeId(158401030174851)
            whenever(
                checkChatNodesNameCollisionAndCopyUseCase(
                    chatId = expectedChatId,
                    messageIds = listOf(expectedMessageId),
                    newNodeParent = newParentNode,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 0
                )
            )
            underTest.importChatNode(newParentNode)
            advanceUntilIdle()
            underTest.onSnackbarMessage().observeOnce {
                assertThat(it).isEqualTo(R.string.context_correctly_copied)
            }
        }

    @Test
    internal fun `test that copy error snack bar message is shown when chat node is not imported to different directory`() =
        runTest {
            val newParentNode = NodeId(158401030174851)
            whenever(
                checkChatNodesNameCollisionAndCopyUseCase(
                    chatId = expectedChatId,
                    messageIds = listOf(expectedMessageId),
                    newNodeParent = newParentNode,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 1
                )
            )
            underTest.importChatNode(newParentNode)
            advanceUntilIdle()
            underTest.onSnackbarMessage().observeOnce {
                assertThat(it).isEqualTo(R.string.context_no_copied)
            }
        }

    @Test
    internal fun `test that onExceptionThrown is triggered when import failed`() =
        runTest {
            val newParentNode = NodeId(158401030174851)

            val runtimeException = RuntimeException("Import node failed")
            whenever(
                checkChatNodesNameCollisionAndCopyUseCase(
                    chatId = expectedChatId,
                    messageIds = listOf(expectedMessageId),
                    newNodeParent = newParentNode,
                )
            ).thenThrow(runtimeException)

            underTest.importChatNode(newParentNode)
            advanceUntilIdle()

            underTest.onExceptionThrown().observeOnce {
                assertThat(it).isEqualTo(runtimeException)
            }
        }

    @Test
    internal fun `test that snackbar message is shown when chat file is already available offline`() =
        runTest {
            val chatFile = mock<ChatDefaultFile>()
            whenever(getChatFileUseCase(expectedChatId, expectedMessageId)).thenReturn(chatFile)
            whenever(isAvailableOfflineUseCase(chatFile)).thenReturn(true)

            underTest.saveChatNodeToOffline()
            advanceUntilIdle()

            underTest.onSnackbarMessage().test().assertValue(R.string.file_already_exists)
        }

    @Test
    internal fun `test that startChatFileOfflineDownload event is triggered when chat file is not available offline`() =
        runTest {
            val chatFile = mock<ChatDefaultFile>()
            whenever(getChatFileUseCase(expectedChatId, expectedMessageId)).thenReturn(chatFile)
            whenever(isAvailableOfflineUseCase(chatFile)).thenReturn(false)

            underTest.saveChatNodeToOffline()
            advanceUntilIdle()

            underTest.onStartChatFileOfflineDownload().test().assertValue(chatFile)
        }

    @Test
    internal fun `test that exception is handled correctly when chat file is not found`() =
        runTest {
            whenever(getChatFileUseCase(expectedChatId, expectedMessageId)).thenReturn(null)

            underTest.saveChatNodeToOffline()
            advanceUntilIdle()

            underTest.onExceptionThrown().test().assertValue {
                it is IllegalStateException
            }
        }

    @ParameterizedTest(name = "action is {0}")
    @MethodSource("provideMenuActions")
    fun `test clickedMenuAction is updated correctly when action is not VideoPlayerDownloadAction`(
        action: VideoPlayerMenuAction,
    ) = runTest {
        underTest.updateClickedMenuAction(action)
        underTest.uiState.test {
            val actual = awaitItem()
            assertThat(actual.clickedMenuAction).isEqualTo(action)
            cancelAndConsumeRemainingEvents()
        }
    }

    private fun provideMenuActions() = listOf(
        VideoPlayerFileInfoAction,
        VideoPlayerChatImportAction,
        VideoPlayerSendToChatAction,
        VideoPlayerSaveForOfflineAction,
        VideoPlayerHideAction,
        VideoPlayerUnhideAction,
        VideoPlayerMoveAction,
        VideoPlayerCopyAction,
        VideoPlayerRemoveAction,
        VideoPlayerRubbishBinAction,
        VideoPlayerAddToAction,
    )

    @Test
    fun `test disableExportUseCase is invoked correctly`() = runTest {
        underTest.removeLink()
        verify(disableExportUseCase).invoke(any())
    }

    @Test
    fun `test isNodeComesFromIncoming is return true`() = runTest {
        whenever(isNodeInRubbishBinUseCase(any())).thenReturn(true)
        whenever(isNodeInCloudDriveUseCase(any())).thenReturn(true)
        whenever(isNodeInBackupsNodeUseCase(any())).thenReturn(true)
        assertThat(underTest.isNodeComesFromIncoming()).isTrue()
    }

    @ParameterizedTest(name = "when hide is {0}")
    @ValueSource(booleans = [true, false])
    fun `test updateNodeSensitiveUseCase is invoked correctly`(hide: Boolean) = runTest {
        val testNodeIds = listOf(NodeId(1L))
        underTest.hideOrUnhideNodes(testNodeIds, hide)
        verify(updateNodeSensitiveUseCase).invoke(testNodeIds.first(), hide)
    }

    @Test
    fun `test deleteMessageFromChatUseCase is invoked correctly`() = runTest {
        underTest.deleteMessageFromChat()
        verify(deleteNodeAttachmentMessageByIdsUseCase).invoke(any(), any())
    }

    @ParameterizedTest(name = "when launchSources is {0}")
    @ValueSource(ints = [FROM_CHAT, FILE_LINK_ADAPTER, FOLDER_LINK_ADAPTER, FROM_ALBUM_SHARING, FILE_BROWSER_ADAPTER])
    fun `test downloadEvent is updated correctly`(
        launchSource: Int,
    ) = runTest {
        savedStateHandle[INTENT_EXTRA_KEY_ADAPTER_TYPE] = launchSource
        when (launchSource) {
            FROM_CHAT -> whenever(
                getChatFileUseCase(any(), any(), any())
            ).thenReturn(mock<ChatDefaultFile>())


            FILE_LINK_ADAPTER -> {
                savedStateHandle[EXTRA_SERIALIZE_STRING] = "test"
                whenever(getPublicNodeFromSerializedDataUseCase(any())).thenReturn(mock<PublicLinkFile>())
            }

            FOLDER_LINK_ADAPTER ->
                whenever(getPublicChildNodeFromIdUseCase(any())).thenReturn(mock<PublicLinkFile>())

            FROM_ALBUM_SHARING -> {
                whenever(getPublicAlbumNodeDataUseCase(any())).thenReturn("test")
                whenever(getPublicNodeFromSerializedDataUseCase(any())).thenReturn(mock<PublicLinkFile>())
            }

            else -> whenever(getVideoNodeByHandleUseCase(any(), any())).thenReturn(mock())
        }

        underTest.updateClickedMenuAction(VideoPlayerDownloadAction)
        advanceUntilIdle()
        underTest.uiState.test {
            assertThat(analyticsExtension.events.first()).isInstanceOf(
                VideoPlayerSaveToDeviceMenuToolbarEvent::class.java
            )
            val actual = awaitItem()
            assertThat(actual.downloadEvent.triggeredContent()).isInstanceOf(
                DownloadTriggerEvent::class.java
            )
        }
    }

    @ParameterizedTest(name = "when launchSources is {0} and ClickedMenuAction is {1}")
    @MethodSource("provideMenuClickTestParams")
    fun `test menuOptionClickedContent is updated correctly`(
        launchSource: Int,
        action: VideoPlayerMenuAction,
    ) = runTest {
        savedStateHandle[INTENT_EXTRA_KEY_ADAPTER_TYPE] = launchSource
        val testFileLink = "testFileLink"
        savedStateHandle[URL_FILE_LINK] = testFileLink
        whenever(getVideoNodeByHandleUseCase(any(), any())).thenReturn(mock())
        underTest.updateClickedMenuAction(action)
        advanceUntilIdle()
        underTest.uiState.test {
            if (action != VideoPlayerRenameAction) {
                assertThat(analyticsExtension.events.first()).isInstanceOf(
                    when (action) {
                        VideoPlayerShareAction -> VideoPlayerShareMenuToolbarEvent::class.java
                        VideoPlayerGetLinkAction -> VideoPlayerGetLinkMenuToolbarEvent::class.java
                        VideoPlayerRemoveLinkAction -> VideoPlayerRemoveLinkMenuToolbarEvent::class.java
                        else -> null
                    }
                )
            }
            val actual = awaitItem()
            assertThat(actual.menuOptionClickedContent).isInstanceOf(
                when (action) {
                    VideoPlayerShareAction -> {
                        if (launchSource == FILE_LINK_ADAPTER) {
                            MenuOptionClickedContent.ShareLink::class.java
                        } else {
                            MenuOptionClickedContent.ShareNode::class.java
                        }
                    }

                    VideoPlayerGetLinkAction -> MenuOptionClickedContent.GetLink::class.java
                    VideoPlayerRemoveLinkAction -> MenuOptionClickedContent.RemoveLink::class.java
                    else -> MenuOptionClickedContent.Rename::class.java
                }
            )
            underTest.clearMenuOptionClickedContent()
            assertThat(awaitItem().menuOptionClickedContent).isNull()
        }
    }

    private fun provideMenuClickTestParams() = listOf(
        Arguments.of(FILE_LINK_ADAPTER, VideoPlayerShareAction),
        Arguments.of(FILE_BROWSER_ADAPTER, VideoPlayerShareAction),
        Arguments.of(FILE_BROWSER_ADAPTER, VideoPlayerGetLinkAction),
        Arguments.of(FILE_BROWSER_ADAPTER, VideoPlayerRemoveLinkAction),
        Arguments.of(FILE_BROWSER_ADAPTER, VideoPlayerRenameAction),
    )

    @ParameterizedTest(name = "when value is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that state is updated correctly when updatePlaybackStateWithReplay is invoked`(
        value: Boolean,
    ) =
        runTest {
            initViewModel()
            underTest.updatePlaybackStateWithReplay(value)
            advanceUntilIdle()
            verify(mediaPlayerGateway).setPlayWhenReady(value)
            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.mediaPlaybackState).isEqualTo(
                    if (value) {
                        MediaPlaybackState.Playing
                    } else {
                        MediaPlaybackState.Paused
                    }
                )
                assertThat(actual.isAutoReplay).isEqualTo(!value)
            }
        }

    @Test
    fun `test that getCurrentPlayingPosition returns as expected`() = runTest {
        whenever(mediaPlayerGateway.getCurrentPlayingPosition()).thenReturn(100)
        whenever(durationInSecondsTextMapper(100.milliseconds)).thenReturn(testDurationString)
        initViewModel()
        val actual = underTest.getCurrentPlayingPosition()
        assertThat(actual).isEqualTo(testDurationString)
    }

    @Test
    fun `test that expected function is invoked when seekToByHandle is invoked`() = runTest {
        val testItems = (0..2).map {
            initVideoPlayerItem(it.toLong(), it.toString())
        }
        underTest.seekToByHandle(1, testItems)
        verify(mediaPlayerGateway).playerSeekTo(1)
    }

    @Test
    fun `test that items are updated correctly when swapItems function is invoked`() = runTest {
        val testItems = (0..2).map {
            initVideoPlayerItem(it.toLong(), it.toString())
        }
        val mediaItems = (0..2).map {
            MediaItem.Builder().setMediaId(it.toString()).build()
        }
        initViewModel()
        underTest.swapItems(1, 2, testItems, mediaItems)
        underTest.uiState.test {
            val actual = awaitItem()
            assertThat(actual.items[1].nodeHandle).isEqualTo(2L)
            assertThat(actual.items[2].nodeHandle).isEqualTo(1L)
        }
    }

    @Test
    fun `test that mediaPlaySources are updated correctly when updateItemsAfterReorder is invoked`() =
        runTest {
            val testItems = (0..2).map {
                initVideoPlayerItem(it.toLong(), it.toString())
            }
            val mediaItems = (0..2).map {
                MediaItem.Builder().setMediaId(it.toString()).build()
            }
            initViewModel()
            underTest.swapItems(1, 2, testItems, mediaItems)
            underTest.updateItemsAfterReorder()
            verify(mediaPlayerGateway).buildPlaySources(any())
            advanceUntilIdle()
            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.mediaPlaySources?.mediaItems?.get(1)?.mediaId).isEqualTo("2")
                assertThat(actual.mediaPlaySources?.mediaItems?.get(2)?.mediaId).isEqualTo("1")
                cancelAndConsumeRemainingEvents()
            }
        }

    @ParameterizedTest(name = "if isParticipatingInChatCallUseCase returns {0}")
    @ValueSource(booleans = [true, false])
    fun `test that isParticipatingInChatCall returns correctly`(value: Boolean) = runTest {
        whenever(isParticipatingInChatCallUseCase()).thenReturn(value)
        initViewModel()
        val actual = underTest.isParticipatingInChatCall()
        assertThat(actual).isEqualTo(value)
    }

    @Test
    fun `test that action mode is updated as expected`() = runTest {
        initViewModel()
        underTest.uiState.test {
            assertThat(awaitItem().isActionMode).isFalse()
            underTest.updateActionMode(true)
            assertThat(awaitItem().isActionMode).isTrue()
            underTest.updateActionMode(false)
            assertThat(awaitItem().isActionMode).isFalse()
        }
    }

    @Test
    fun `test that search state is updated as expected`() = runTest {
        initViewModel()
        underTest.uiState.test {
            assertThat(awaitItem().searchState).isEqualTo(SearchWidgetState.COLLAPSED)
            underTest.searchWidgetStateUpdate()
            assertThat(awaitItem().searchState).isEqualTo(SearchWidgetState.EXPANDED)
            skipItems(1)
            underTest.searchWidgetStateUpdate()
            assertThat(awaitItem().searchState).isEqualTo(SearchWidgetState.COLLAPSED)
        }
    }

    @Test
    fun `test that states of the search feature are updated as expected`() = runTest {
        initViewModel()
        underTest.searchWidgetStateUpdate()
        underTest.searchQuery("")
        underTest.uiState.test {
            val initial = awaitItem()
            assertThat(initial.searchState).isEqualTo(SearchWidgetState.EXPANDED)
            assertThat(initial.query).isNotNull()
            underTest.closeSearch()
            val actual = awaitItem()
            assertThat(actual.searchState).isEqualTo(SearchWidgetState.COLLAPSED)
            assertThat(actual.query).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that state is updated correctly when updateItemInSelectionState is invoked`() =
        runTest {
            val testItems = (0..2).map {
                initVideoPlayerItem(it.toLong(), it.toString())
            }
            val testSelectItem = initVideoPlayerItem(handle = 1L, name = "1", isSelect = true)
            whenever(testItems[1].copy(isSelected = true)).thenReturn(testSelectItem)
            initViewModel()
            underTest.updateItemInSelectionState(1, testItems[1], testItems)
            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.items[1].isSelected).isTrue()
                assertThat(actual.selectedItemHandles.size).isEqualTo(1)
                assertThat(actual.selectedItemHandles.contains(1)).isTrue()
            }
        }

    @Test
    fun `test that state is updated correctly when clearAllSelected is invoked`() =
        runTest {
            val testItems = (0..2).map {
                initVideoPlayerItem(it.toLong(), it.toString())
            }
            val testSelectItem = initVideoPlayerItem(handle = 1L, name = "1", isSelect = true)
            whenever(testItems[1].copy(isSelected = true)).thenReturn(testSelectItem)
            whenever(testSelectItem.copy(isSelected = false)).thenReturn(testItems[1])
            initViewModel()
            underTest.updateItemInSelectionState(1, testItems[1], testItems)
            underTest.uiState.test {
                awaitItem().let {
                    assertThat(it.items[1].isSelected).isTrue()
                    assertThat(it.selectedItemHandles.size).isEqualTo(1)
                    assertThat(it.selectedItemHandles.contains(1)).isTrue()
                }
                underTest.clearAllSelected()
                awaitItem().let {
                    assertThat(it.items[1].isSelected).isFalse()
                    assertThat(it.selectedItemHandles).isEmpty()
                }
            }
        }

    @Test
    fun `test that state is updated correctly when removeSelectedItems is invoked`() =
        runTest {
            val testItems = (0..2).map {
                initVideoPlayerItem(it.toLong(), it.toString())
            }
            val mediaItems = (0..2).map {
                MediaItem.Builder().setMediaId(it.toString()).build()
            }
            val selectHandles = listOf(1L, 2L)
            initViewModel()
            underTest.removeSelectedItems(selectHandles, testItems, mediaItems)
            verify(mediaPlayerGateway).buildPlaySources(any())
            advanceUntilIdle()
            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.items.size).isEqualTo(1)
                assertThat(actual.items.first().nodeHandle).isEqualTo(0)
                assertThat(actual.mediaPlaySources?.mediaItems?.size).isEqualTo(1)
                assertThat(actual.mediaPlaySources?.mediaItems?.first()?.mediaId).isEqualTo("0")
                assertThat(actual.selectedItemHandles).isEmpty()
                cancelAndConsumeRemainingEvents()
            }
        }

    @ParameterizedTest(name = "and isFullscreen is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that isFullscreen is updated correctly when updateFullscreen is invoked`(
        isFullscreen: Boolean,
    ) = runTest {
        initViewModel()
        underTest.updateFullscreen(isFullscreen)
        testScheduler.advanceUntilIdle()
        assertThat(analyticsExtension.events.first()).isInstanceOf(
            if (isFullscreen) {
                VideoPlayerFullScreenPressedEvent::class.java
            } else {
                VideoPlayerOriginalPressedEvent::class.java
            }
        )
        underTest.uiState.test {
            assertThat(awaitItem().isFullscreen).isEqualTo(isFullscreen)
            cancelAndConsumeRemainingEvents()
        }
    }

    companion object {
        @JvmField
        @RegisterExtension
        val analyticsExtension = AnalyticsTestExtension()
    }
}