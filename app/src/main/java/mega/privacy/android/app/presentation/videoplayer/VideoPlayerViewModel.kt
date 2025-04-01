package mega.privacy.android.app.presentation.videoplayer

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.Surface
import android.view.TextureView
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.di.mediaplayer.VideoPlayer
import mega.privacy.android.app.featuretoggle.ApiFeatures
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.model.MediaPlaySources
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemType
import mega.privacy.android.app.mediaplayer.service.Metadata
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent.DownloadTriggerEvent
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent.StartDownloadForOffline
import mega.privacy.android.app.presentation.videoplayer.mapper.LaunchSourceMapper
import mega.privacy.android.app.presentation.videoplayer.mapper.VideoPlayerItemMapper
import mega.privacy.android.app.presentation.videoplayer.model.MediaPlaybackState
import mega.privacy.android.app.presentation.videoplayer.model.MenuOptionClickedContent
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerItem
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerDownloadAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerGetLinkAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerRemoveLinkAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerRenameAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerShareAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerUiState
import mega.privacy.android.app.presentation.videoplayer.model.VideoSize
import mega.privacy.android.app.utils.Constants
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
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IS_PLAYLIST
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MEDIA_QUEUE_TITLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MSG_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ORDER_GET_CHILDREN
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_VIDEO_ADD_TO_ALBUM
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_VIDEO_COLLECTION_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_VIDEO_COLLECTION_TITLE
import mega.privacy.android.app.utils.Constants.INVALID_SIZE
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.LINKS_ADAPTER
import mega.privacy.android.app.utils.Constants.NODE_HANDLES
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
import mega.privacy.android.app.utils.ThumbnailUtils
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.node.chat.ChatFile
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.exception.BlockedMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.qualifier.MainDispatcher
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
import mega.privacy.mobile.analytics.event.VideoPlayerGetLinkMenuToolbarEvent
import mega.privacy.mobile.analytics.event.VideoPlayerRemoveLinkMenuToolbarEvent
import mega.privacy.mobile.analytics.event.VideoPlayerSaveToDeviceMenuToolbarEvent
import mega.privacy.mobile.analytics.event.VideoPlayerShareMenuToolbarEvent
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * ViewModel for video player.
 */
@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @VideoPlayer private val mediaPlayerGateway: MediaPlayerGateway,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val videoPlayerItemMapper: VideoPlayerItemMapper,
    private val getVideoNodeByHandleUseCase: GetVideoNodeByHandleUseCase,
    private val getVideoNodesUseCase: GetVideoNodesUseCase,
    private val getVideoNodesFromPublicLinksUseCase: GetVideoNodesFromPublicLinksUseCase,
    private val getVideoNodesFromInSharesUseCase: GetVideoNodesFromInSharesUseCase,
    private val getVideoNodesFromOutSharesUseCase: GetVideoNodesFromOutSharesUseCase,
    private val getVideoNodesByEmailUseCase: GetVideoNodesByEmailUseCase,
    private val getUserNameByEmailUseCase: GetUserNameByEmailUseCase,
    private val getRubbishNodeUseCase: GetRubbishNodeUseCase,
    private val getBackupsNodeUseCase: GetBackupsNodeUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val getVideosBySearchTypeUseCase: GetVideosBySearchTypeUseCase,
    private val getVideoNodesByParentHandleUseCase: GetVideoNodesByParentHandleUseCase,
    private val getVideoNodesByHandlesUseCase: GetVideoNodesByHandlesUseCase,
    private val getRootNodeFromMegaApiFolderUseCase: GetRootNodeFromMegaApiFolderUseCase,
    private val getParentNodeFromMegaApiFolderUseCase: GetParentNodeFromMegaApiFolderUseCase,
    private val getVideosByParentHandleFromMegaApiFolderUseCase: GetVideosByParentHandleFromMegaApiFolderUseCase,
    private val monitorSubFolderMediaDiscoverySettingsUseCase: MonitorSubFolderMediaDiscoverySettingsUseCase,
    private val getThumbnailUseCase: GetThumbnailUseCase,
    private val httpServerIsRunningUseCase: HttpServerIsRunningUseCase,
    private val httpServerStartUseCase: HttpServerStartUseCase,
    private val httpServerStopUseCase: HttpServerStopUseCase,
    private val getLocalFolderLinkUseCase: GetLocalFolderLinkUseCase,
    private val getFileTypeInfoByNameUseCase: GetFileTypeInfoByNameUseCase,
    private val getOfflineNodeInformationByIdUseCase: GetOfflineNodeInformationByIdUseCase,
    private val getOfflineNodesByParentIdUseCase: GetOfflineNodesByParentIdUseCase,
    private val getLocalLinkFromMegaApiUseCase: GetLocalLinkFromMegaApiUseCase,
    private val getLocalFilePathUseCase: GetLocalFilePathUseCase,
    private val getFingerprintUseCase: GetFingerprintUseCase,
    private val monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
    private val getFileByPathUseCase: GetFileByPathUseCase,
    private val monitorVideoRepeatModeUseCase: MonitorVideoRepeatModeUseCase,
    private val saveVideoRecentlyWatchedUseCase: SaveVideoRecentlyWatchedUseCase,
    private val setVideoRepeatModeUseCase: SetVideoRepeatModeUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val isHiddenNodesOnboardedUseCase: IsHiddenNodesOnboardedUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
    private val canRemoveFromChatUseCase: CanRemoveFromChatUseCase,
    private val isNodeInRubbishBinUseCase: IsNodeInRubbishBinUseCase,
    private val isNodeInBackupsNodeUseCase: IsNodeInBackupsUseCase,
    private val isNodeInCloudDriveUseCase: IsNodeInCloudDriveUseCase,
    private val checkChatNodesNameCollisionAndCopyUseCase: CheckChatNodesNameCollisionAndCopyUseCase,
    private val getPublicAlbumNodeDataUseCase: GetPublicAlbumNodeDataUseCase,
    private val getChatFileUseCase: GetChatFileUseCase,
    private val getPublicNodeFromSerializedDataUseCase: GetPublicNodeFromSerializedDataUseCase,
    private val getPublicChildNodeFromIdUseCase: GetPublicChildNodeFromIdUseCase,
    private val getFileUriUseCase: GetFileUriUseCase,
    private val disableExportUseCase: DisableExportUseCase,
    private val isAvailableOfflineUseCase: IsAvailableOfflineUseCase,
    private val updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase,
    private val checkNodesNameCollisionWithActionUseCase: CheckNodesNameCollisionWithActionUseCase,
    private val deleteNodeAttachmentMessageByIdsUseCase: DeleteNodeAttachmentMessageByIdsUseCase,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val launchSourceMapper: LaunchSourceMapper,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val uiState: StateFlow<VideoPlayerUiState>
        field: MutableStateFlow<VideoPlayerUiState> = MutableStateFlow(VideoPlayerUiState())

    private var needStopStreamingServer = false
    private var playerRetry = 0

    private val currentLaunchSources: Int by lazy {
        savedStateHandle[INTENT_EXTRA_KEY_ADAPTER_TYPE] ?: INVALID_VALUE
    }

    private val shouldShowAddTo: Boolean by lazy {
        savedStateHandle.get<Boolean>(INTENT_EXTRA_KEY_VIDEO_ADD_TO_ALBUM) == true
    }

    private val chatId: Long by lazy {
        savedStateHandle[INTENT_EXTRA_KEY_CHAT_ID] ?: INVALID_HANDLE
    }

    private val messageId: Long by lazy {
        savedStateHandle[INTENT_EXTRA_KEY_MSG_ID] ?: INVALID_HANDLE
    }

    private val canRemoveFromChatFunction =
        suspend { runCatching { canRemoveFromChatUseCase(chatId, messageId) }.getOrDefault(false) }

    private val collectionTitle: String? by lazy {
        savedStateHandle[INTENT_EXTRA_KEY_VIDEO_COLLECTION_TITLE]
    }

    private val collectionId: Long? by lazy {
        savedStateHandle[INTENT_EXTRA_KEY_VIDEO_COLLECTION_ID]
    }

    private val serialize: String? by lazy {
        savedStateHandle[EXTRA_SERIALIZE_STRING]
    }

    private val fileLink: String? by lazy {
        savedStateHandle[URL_FILE_LINK]
    }

    private val collision = SingleLiveEvent<NameCollision>()
    private val throwable = SingleLiveEvent<Throwable>()
    private val snackbarMessage = SingleLiveEvent<Int>()
    private val startChatFileOfflineDownload = SingleLiveEvent<ChatFile>()

    init {
        setupTransferListener()
        viewModelScope.launch {
            monitorVideoRepeatModeUseCase().conflate()
                .catch {
                    Timber.e(it)
                }.collectLatest { mode ->
                    uiState.update { it.copy(repeatToggleMode = mode) }
                }
        }

        viewModelScope.launch {
            if (isHiddenNodesActive()) {
                handleHiddenNodesUIFlow()
                monitorIsHiddenNodesOnboarded()
            }
        }

        refreshMenuActionsWhenNodeUpdates()
    }

    private fun refreshMenuActionsWhenNodeUpdates() {
        viewModelScope.launch {
            monitorNodeUpdatesUseCase().filter {
                it.changes.keys.any { node ->
                    node is FileNode && node.type is VideoFileTypeInfo
                }
            }.map { nodeUpdate -> nodeUpdate.changes }
                .conflate()
                .catch {
                    Timber.e(it)
                }.collectLatest { changes ->
                    val actions = getMenuActionsForVideo(
                        launchSource = currentLaunchSources,
                        videoNodeHandle = uiState.value.currentPlayingHandle,
                        isPaidUser = uiState.value.accountType?.isPaid == true,
                        isExpiredBusinessUser = uiState.value.isBusinessAccountExpired
                    )
                    uiState.update { it.copy(menuActions = actions) }
                }
        }
    }

    /**
     * Setup transfer listener
     */
    private fun setupTransferListener() =
        viewModelScope.launch {
            monitorTransferEventsUseCase()
                .catch {
                    Timber.e(it)
                }.collect { event ->
                    if (event is TransferEvent.TransferTemporaryErrorEvent) {
                        val error = event.error
                        val transfer = event.transfer
                        if (transfer.nodeHandle == uiState.value.currentPlayingHandle
                            && ((error is QuotaExceededMegaException
                                    && !transfer.isForeignOverQuota
                                    && error.value != 0L)
                                    || error is BlockedMegaException)
                        ) {
                            uiState.update { it.copy(error = error) }
                        }
                    }
                }
        }

    private fun handleHiddenNodesUIFlow() {
        combine(
            monitorAccountDetailUseCase(),
            monitorShowHiddenItemsUseCase(),
        ) { accountDetail, showHiddenItems ->
            val accountType = accountDetail.levelDetail?.accountType
            val businessStatus =
                if (accountType?.isBusinessAccount == true) {
                    getBusinessStatusUseCase()
                } else null

            val actions = getMenuActionsForVideo(
                launchSource = currentLaunchSources,
                videoNodeHandle = uiState.value.currentPlayingHandle,
                isPaidUser = accountType?.isPaid == true,
                isExpiredBusinessUser = businessStatus == BusinessAccountStatus.Expired
            )

            uiState.update {
                it.copy(
                    accountType = accountType,
                    isBusinessAccountExpired = businessStatus == BusinessAccountStatus.Expired,
                    hiddenNodeEnabled = true,
                    showHiddenItems = showHiddenItems,
                    menuActions = actions
                )
            }
        }.catch { Timber.e(it) }
            .launchIn(viewModelScope)
    }

    private fun monitorIsHiddenNodesOnboarded() {
        viewModelScope.launch {
            val isHiddenNodesOnboarded = isHiddenNodesOnboardedUseCase()
            uiState.update {
                it.copy(isHiddenNodesOnboarded = isHiddenNodesOnboarded)
            }
        }
    }

    internal fun initVideoPlaybackSources(intent: Intent?) {
        viewModelScope.launch {
            buildPlaybackSources(intent)
        }
    }

    private suspend fun buildPlaybackSources(intent: Intent?) {
        if (intent == null || !validateIntent(intent)) return

        val uri = intent.data
        val currentPlayingHandle = intent.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)
        val currentPlayingFileName = intent.getStringExtra(INTENT_EXTRA_KEY_FILE_NAME).orEmpty()
        needStopStreamingServer =
            intent.getBooleanExtra(INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, false)
        playerRetry = 0

        val currentPlayingUri =
            getCurrentPlayingUri(uri, currentLaunchSources, currentPlayingHandle)
        if (currentPlayingUri == null) {
            logInvalidParam("folder link uri is null")
            return
        }

        val currentPlayingMediaItem = MediaItem.Builder()
            .setUri(currentPlayingUri)
            .setMediaId(currentPlayingHandle.toString())
            .build()

        updateStateWithMediaItem(currentPlayingMediaItem, currentPlayingFileName)

        if (!intent.getBooleanExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, true)) {
            setPlayingItem(currentPlayingHandle, currentPlayingFileName, currentLaunchSources)
            return
        }

        if (currentLaunchSources != OFFLINE_ADAPTER && currentLaunchSources != ZIP_ADAPTER) {
            needStopStreamingServer =
                needStopStreamingServer || setupStreamingServer(currentLaunchSources)
        }

        withContext(ioDispatcher) {
            handlePlaybackSourceByLaunchSource(intent, currentLaunchSources, currentPlayingHandle)
        }
    }

    private fun validateIntent(intent: Intent): Boolean {
        val isValid = when {
            !intent.getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true) -> {
                logInvalidParam("Rebuild playlist param is false")
                false
            }

            intent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE) == INVALID_VALUE -> {
                logInvalidParam("Launch source is invalid")
                false
            }

            intent.data == null -> {
                logInvalidParam("URI is null")
                false
            }

            intent.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE) == INVALID_HANDLE -> {
                logInvalidParam("The first playing video handle is invalid")
                false
            }

            intent.getStringExtra(INTENT_EXTRA_KEY_FILE_NAME) == null -> {
                logInvalidParam("The first playing video file name is null")
                false
            }

            else -> true
        }
        return isValid
    }

    private fun logInvalidParam(message: String) {
        Timber.d("Build playback sources failed: $message")
        uiState.update { it.copy(isRetry = false) }
    }

    private suspend fun setupStreamingServer(launchSource: Int): Boolean {
        val isServerRunning = httpServerIsRunningUseCase(launchSource == FOLDER_LINK_ADAPTER)
        if (isServerRunning != 0) return false

        httpServerStartUseCase(launchSource == FOLDER_LINK_ADAPTER)
        return true
    }

    private suspend fun getCurrentPlayingUri(uri: Uri?, launchSource: Int, handle: Long) =
        when (launchSource) {
            FOLDER_LINK_ADAPTER -> {
                val url = getLocalFolderLinkUseCase(handle)
                url?.let { Uri.parse(it) }
            }

            else -> uri
        }

    private fun updateStateWithMediaItem(mediaItem: MediaItem, fileName: String) {
        MediaPlaySources(
            mediaItems = listOf(mediaItem),
            newIndexForCurrentItem = INVALID_VALUE,
            nameToDisplay = fileName
        ).also { sources ->
            uiState.update { it.copy(mediaPlaySources = sources) }
            buildPlaybackSourcesForPlayer(sources)
        }
    }

    private fun buildPlaybackSourcesForPlayer(mediaPlaySources: MediaPlaySources) =
        viewModelScope.launch(mainDispatcher) {
            Timber.d("Playback sources: ${mediaPlaySources.mediaItems.size} items")
            with(mediaPlayerGateway) {
                buildPlaySources(mediaPlaySources)
                setPlayWhenReady(true && mediaPlaySources.isRestartPlaying)
                playerPrepare()
            }
            mediaPlaySources.nameToDisplay?.let { name ->
                uiState.update { it.copy(metadata = Metadata(null, null, null, nodeName = name)) }
            }
        }

    private suspend fun setPlayingItem(handle: Long, fileName: String?, source: Int) {
        val node = getVideoNodeByHandleUseCase(handle)
        val thumbnail = getThumbnailForNode(node, handle, source)
        val playingItem = videoPlayerItemMapper(
            nodeHandle = handle,
            nodeName = fileName.orEmpty(),
            thumbnail = thumbnail,
            type = MediaQueueItemType.Playing,
            size = node?.size ?: INVALID_SIZE,
            duration = node?.duration ?: 0.seconds,
        )

        uiState.update { it.copy(items = listOf(playingItem)) }
    }

    private suspend fun getThumbnailForNode(
        node: TypedVideoNode?,
        handle: Long,
        source: Int,
    ) = when {
        node == null -> null
        source == OFFLINE_ADAPTER -> getThumbnailUseCase(handle)
        else -> runCatching {
            File(
                ThumbnailUtils.getThumbFolder(context),
                node.base64Id.plus(FileUtil.JPG_EXTENSION)
            )
        }.getOrNull()
    }

    private suspend fun handlePlaybackSourceByLaunchSource(
        intent: Intent,
        launchSource: Int,
        playingHandle: Long,
    ) {
        when (launchSource) {
            OFFLINE_ADAPTER -> handleOfflineSource(intent, playingHandle)
            ZIP_ADAPTER -> handleZipSource(intent, playingHandle)
            else -> handleGeneralSource(intent, launchSource, playingHandle)
        }
    }

    private suspend fun handleOfflineSource(intent: Intent, playingHandle: Long) {
        val parentId = intent.getIntExtra(INTENT_EXTRA_KEY_PARENT_ID, -1)
        val title = if (parentId == -1) {
            context.getString(R.string.section_saved_for_offline_new)
        } else {
            runCatching {
                getOfflineNodeInformationByIdUseCase(parentId)
            }.getOrNull()?.name.orEmpty()
        }
        buildPlaybackSourcesByOfflineNodes(title, parentId, playingHandle)
    }

    private suspend fun buildPlaybackSourcesByOfflineNodes(
        title: String,
        parentId: Int,
        firstPlayHandle: Long,
    ) {
        runCatching {
            getOfflineNodesByParentIdUseCase(parentId)
        }.onSuccess { list ->
            val mediaItems = mutableListOf<MediaItem>()
            var currentPlayingIndex = -1
            val videoPlayerItems = list.filter {
                it.fileTypeInfo is VideoFileTypeInfo && it.fileTypeInfo?.isSupported == true
            }.mapIndexed { index, item ->
                if (item.handle.toLong() == firstPlayHandle) currentPlayingIndex = index

                runCatching { Uri.parse(item.absolutePath) }.getOrNull()?.let {
                    mediaItems.add(
                        MediaItem.Builder()
                            .setUri(it)
                            .setMediaId(item.handle)
                            .build()
                    )
                }

                val thumbnailFile = runCatching {
                    item.thumbnail?.let { File(it) }
                }.getOrNull()

                videoPlayerItemMapper(
                    nodeHandle = item.handle.toLong(),
                    nodeName = item.name,
                    thumbnail = thumbnailFile,
                    type = getMediaQueueItemType(index, currentPlayingIndex),
                    size = item.totalSize,
                    duration = (item.fileTypeInfo as? VideoFileTypeInfo)?.duration ?: 0.seconds,
                )
            }

            updatePlaybackSources(
                videoPlayerItems = videoPlayerItems,
                mediaItems = mediaItems,
                title = title,
                currentPlayingIndex = currentPlayingIndex,
                firstPlayHandle = firstPlayHandle
            )
        }.onFailure {
            Timber.e(it)
        }
    }

    private suspend fun handleZipSource(intent: Intent, playingHandle: Long) {
        intent.getStringExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY)?.let { zipPath ->
            buildPlaybackSourcesByFiles(zipPath, playingHandle)
        }
    }

    private fun getMediaQueueItemType(currentIndex: Int, playingIndex: Int) =
        when {
            currentIndex == playingIndex -> MediaQueueItemType.Playing
            playingIndex == -1 || currentIndex < playingIndex -> MediaQueueItemType.Previous
            else -> MediaQueueItemType.Next
        }

    private fun updatePlaybackSources(
        videoPlayerItems: List<VideoPlayerItem>,
        mediaItems: List<MediaItem>,
        title: String,
        currentPlayingIndex: Int,
        firstPlayHandle: Long,
    ) {
        val mediaPlaySources = MediaPlaySources(
            mediaItems = mediaItems,
            newIndexForCurrentItem = currentPlayingIndex,
            nameToDisplay = null
        )

        uiState.update {
            it.copy(
                items = videoPlayerItems,
                mediaPlaySources = mediaPlaySources,
                playQueueTitle = title,
                currentPlayingIndex = currentPlayingIndex,
                currentPlayingHandle = firstPlayHandle
            )
        }
        buildPlaybackSourcesForPlayer(mediaPlaySources)
    }

    private suspend fun buildPlaybackSourcesByFiles(zipPath: String, firstPlayHandle: Long) {
        runCatching {
            val (title, files) = getFileByPathUseCase(zipPath)?.parentFile.let { parentFile ->
                parentFile?.name.orEmpty() to parentFile?.listFiles().orEmpty()
            }
            val mediaItems = mutableListOf<MediaItem>()
            var currentPlayingIndex = -1
            val videoPlayerItems = files.filter {
                it.isFile && getFileTypeInfoByNameUseCase(it.name) is VideoFileTypeInfo
            }.mapIndexed { index, file ->
                if (file.name.hashCode().toLong() == firstPlayHandle) currentPlayingIndex = index

                mediaItems.add(
                    MediaItem.Builder()
                        .setUri(FileUtil.getUriForFile(context, file))
                        .setMediaId(file.name.hashCode().toString())
                        .build()
                )

                videoPlayerItemMapper(
                    nodeHandle = file.name.hashCode().toLong(),
                    nodeName = file.name,
                    thumbnail = null,
                    type = getMediaQueueItemType(index, currentPlayingIndex),
                    size = file.length(),
                    duration = 0.seconds,
                )
            }

            updatePlaybackSources(
                videoPlayerItems = videoPlayerItems,
                mediaItems = mediaItems,
                title = title,
                currentPlayingIndex = currentPlayingIndex,
                firstPlayHandle = firstPlayHandle
            )
        }.onFailure {
            Timber.e(it)
        }
    }

    private suspend fun handleGeneralSource(
        intent: Intent,
        launchSource: Int,
        playingHandle: Long,
    ) {
        val parentHandle = intent.getLongExtra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, INVALID_HANDLE)
        val order = getSortOrderFromIntent(intent)
        val (title, videoNodes) = when (launchSource) {
            VIDEO_BROWSE_ADAPTER ->
                context.getString(R.string.sortby_type_video_first) to getVideoNodesUseCase(order)

            RECENTS_ADAPTER, RECENTS_BUCKET_ADAPTER -> {
                val videoNodes = intent.getLongArrayExtra(NODE_HANDLES)?.let { handles ->
                    getVideoNodesByHandlesUseCase(handles.toList())
                }.orEmpty()
                context.getString(R.string.section_recents) to videoNodes
            }

            FOLDER_LINK_ADAPTER -> {
                val parentNode = if (parentHandle == INVALID_HANDLE) {
                    getRootNodeFromMegaApiFolderUseCase()
                } else {
                    getParentNodeFromMegaApiFolderUseCase(parentHandle)
                }

                val videoNodes = parentNode?.let {
                    getVideosByParentHandleFromMegaApiFolderUseCase(
                        parentHandle = it.id.longValue,
                        order = order
                    )
                }.orEmpty()

                (parentNode?.name.orEmpty()) to videoNodes
            }

            SEARCH_BY_ADAPTER -> {
                val title = intent.getStringExtra(INTENT_EXTRA_KEY_MEDIA_QUEUE_TITLE).orEmpty()
                val videoNodes = intent.getLongArrayExtra(INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH)
                    ?.let { handles ->
                        getVideoNodesByHandlesUseCase(handles.toList())
                    }.orEmpty()
                title to videoNodes
            }

            FILE_BROWSER_ADAPTER,
            RUBBISH_BIN_ADAPTER,
            BACKUPS_ADAPTER,
            LINKS_ADAPTER,
            INCOMING_SHARES_ADAPTER,
            OUTGOING_SHARES_ADAPTER,
            CONTACT_FILE_ADAPTER,
            FROM_MEDIA_DISCOVERY,
            FROM_IMAGE_VIEWER,
            FROM_ALBUM_SHARING,
            FAVOURITES_ADAPTER,
                -> {
                when {
                    launchSource == LINKS_ADAPTER && parentHandle == INVALID_HANDLE ->
                        context.getString(R.string.tab_links_shares) to getVideoNodesFromPublicLinksUseCase(
                            order
                        )

                    launchSource == INCOMING_SHARES_ADAPTER && parentHandle == INVALID_HANDLE ->
                        context.getString(R.string.tab_incoming_shares) to getVideoNodesFromInSharesUseCase(
                            order
                        )

                    launchSource == OUTGOING_SHARES_ADAPTER && parentHandle == INVALID_HANDLE ->
                        context.getString(R.string.tab_outgoing_shares) to getVideoNodesFromOutSharesUseCase(
                            lastHandle = INVALID_HANDLE,
                            order = order
                        )

                    launchSource == CONTACT_FILE_ADAPTER && parentHandle == INVALID_HANDLE -> {
                        intent.getStringExtra(Constants.INTENT_EXTRA_KEY_CONTACT_EMAIL)
                            ?.let { email ->
                                val videoNodes = getVideoNodesByEmailUseCase(email).orEmpty()
                                val userName = getUserNameByEmailUseCase(email)
                                val title = if (userName == null) {
                                    ""
                                } else {
                                    "${context.getString(R.string.title_incoming_shares_with_explorer)} $userName"
                                }
                                title to videoNodes
                            } ?: ("" to emptyList())
                    }

                    else -> {
                        val parentNode =
                            if (parentHandle == INVALID_HANDLE) {
                                when (launchSource) {
                                    RUBBISH_BIN_ADAPTER -> getRubbishNodeUseCase()
                                    BACKUPS_ADAPTER -> getBackupsNodeUseCase()
                                    else -> getRootNodeUseCase()
                                }
                            } else {
                                getVideoNodeByHandleUseCase(parentHandle)
                            }
                        val title =
                            if (parentHandle == INVALID_HANDLE) {
                                context.getString(
                                    when (launchSource) {
                                        RUBBISH_BIN_ADAPTER -> R.string.section_rubbish_bin
                                        BACKUPS_ADAPTER -> R.string.home_side_menu_backups_title
                                        else -> R.string.section_cloud_drive
                                    }
                                )
                            } else {
                                parentNode?.name
                            }.orEmpty()

                        val videoNodes = parentNode?.let {
                            if (launchSource == FROM_MEDIA_DISCOVERY) {
                                getVideosBySearchTypeUseCase(
                                    handle = it.id.longValue,
                                    recursive = monitorSubFolderMediaDiscoverySettingsUseCase().first(),
                                    order = order
                                )
                            } else {
                                getVideoNodesByParentHandleUseCase(
                                    parentHandle = it.id.longValue,
                                    order = order
                                )
                            }
                        }.orEmpty()

                        title to videoNodes
                    }
                }
            }

            else -> {
                "" to emptyList()
            }
        }
        if (videoNodes.isNotEmpty()) {
            buildPlaybackSourcesByNodes(title, videoNodes, playingHandle, launchSource)
        }
    }

    private fun getSortOrderFromIntent(intent: Intent): SortOrder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(
                INTENT_EXTRA_KEY_ORDER_GET_CHILDREN,
                SortOrder::class.java
            ) ?: SortOrder.ORDER_DEFAULT_ASC
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN) as SortOrder?
                ?: SortOrder.ORDER_DEFAULT_ASC
        }

    private suspend fun buildPlaybackSourcesByNodes(
        title: String,
        videoNodes: List<TypedVideoNode>,
        firstPlayHandle: Long,
        launchSource: Int,
    ) {
        val mediaItems = mutableListOf<MediaItem>()
        var currentPlayingIndex = -1
        val videoPlayerItems = videoNodes.mapIndexed { index, node ->
            runCatching {
                if (node.id.longValue == firstPlayHandle) currentPlayingIndex = index

                getMediaItemForNode(node, launchSource)?.let { mediaItems.add(it) }

                videoPlayerItemMapper(
                    nodeHandle = node.id.longValue,
                    nodeName = node.name,
                    thumbnail = node.thumbnailPath?.let { path ->
                        File(path)
                    },
                    type = getMediaQueueItemType(index, currentPlayingIndex),
                    size = node.size,
                    duration = node.duration,
                )
            }.onFailure {
                Timber.e(it)
            }.getOrNull()
        }.filterNotNull()

        updatePlaybackSources(
            videoPlayerItems = videoPlayerItems,
            mediaItems = mediaItems,
            title = title,
            currentPlayingIndex = currentPlayingIndex,
            firstPlayHandle = firstPlayHandle
        )
    }

    private suspend fun getMediaItemForNode(node: TypedVideoNode, launchSource: Int) =
        getLocalFilePathUseCase(node).let { localPath ->
            if (localPath != null && isLocalFile(node, localPath)) {
                MediaItem.Builder()
                    .setUri(FileUtil.getUriForFile(context, File(localPath)))
                    .setMediaId(node.id.longValue.toString())
                    .build()
            } else {
                when (launchSource) {
                    FOLDER_LINK_ADAPTER -> getLocalFolderLinkUseCase(node.id.longValue)
                    else -> getLocalLinkFromMegaApiUseCase(node.id.longValue)
                }?.let { url ->
                    MediaItem.Builder()
                        .setUri(Uri.parse(url))
                        .setMediaId(node.id.longValue.toString())
                        .build()
                }
            }
        }

    private suspend fun isLocalFile(node: TypedFileNode, localPath: String): Boolean {
        val isFingerPrintAvailable =
            node.fingerprint?.let { it == getFingerprintUseCase(localPath) } == true
        return isOnMegaDownloads(node) || isFingerPrintAvailable
    }

    private fun isOnMegaDownloads(node: TypedFileNode): Boolean =
        File(FileUtil.getDownloadLocation(), node.name).let { file ->
            FileUtil.isFileAvailable(file) && file.length() == node.size
        }

    /**
     * onCleared
     */
    override fun onCleared() {
        super.onCleared()
        clear()
    }

    /**
     * Clear the state and flying task of this class, should be called in onDestroy.
     */
    private fun clear() {
        applicationScope.launch {
            if (needStopStreamingServer) {
                httpServerStopUseCase()
            }
        }
    }

    internal fun updateMetadata(metadata: Metadata) =
        uiState.update { it.copy(metadata = metadata) }

    internal fun updateCurrentPlayingVideoSize(videoSize: VideoSize?) =
        uiState.update { it.copy(currentPlayingVideoSize = videoSize) }

    internal fun updateCurrentPlayingHandle(
        handle: Long,
        items: List<VideoPlayerItem> = uiState.value.items,
    ) {
        val playingIndex = items.indexOfFirst { it.nodeHandle == handle }.takeIf { it != -1 } ?: 0
        viewModelScope.launch {
            val actions = getMenuActionsForVideo(
                launchSource = currentLaunchSources,
                videoNodeHandle = handle,
                isPaidUser = uiState.value.accountType?.isPaid == true,
                isExpiredBusinessUser = uiState.value.isBusinessAccountExpired
            )
            uiState.update {
                it.copy(
                    currentPlayingHandle = handle,
                    currentPlayingIndex = playingIndex,
                    menuActions = actions
                )
            }
        }
    }

    internal fun setRepeatToggleModeForPlayer(mode: RepeatToggleMode) = viewModelScope.launch {
        mediaPlayerGateway.setRepeatToggleMode(mode)
        setVideoRepeatModeUseCase(mode.ordinal)
    }

    internal fun updateRepeatToggleMode(mode: RepeatToggleMode) =
        uiState.update { it.copy(repeatToggleMode = mode) }

    internal fun saveVideoWatchedTime() = viewModelScope.launch {
        mediaPlayerGateway.getCurrentMediaItem()?.mediaId?.toLong()?.let {
            saveVideoRecentlyWatchedUseCase(
                it,
                Instant.now().toEpochMilli() / 1000,
                collectionId ?: 0L,
                collectionTitle
            )
        }
    }

    internal fun updatePlaybackState(state: MediaPlaybackState) =
        uiState.update { it.copy(mediaPlaybackState = state) }

    internal fun onPlayerError() {
        playerRetry++
        Timber.d("playerRetry: $playerRetry")
        uiState.update { it.copy(isRetry = playerRetry <= MAX_RETRY) }
    }

    internal fun updateSnackBarMessage(message: String?) =
        uiState.update { it.copy(snackBarMessage = message) }

    private suspend fun getMenuActionsForVideo(
        launchSource: Int,
        videoNodeHandle: Long,
        isPaidUser: Boolean,
        isExpiredBusinessUser: Boolean,
    ): List<VideoPlayerMenuAction> {
        val videoNode = getNodeByHandle(videoNodeHandle)
        return launchSourceMapper(
            launchSource = launchSource,
            videoNode = videoNode,
            shouldShowAddTo = shouldShowAddTo,
            canRemoveFromChat = canRemoveFromChatFunction,
            isPaidUser = isPaidUser,
            isExpiredBusinessUser = isExpiredBusinessUser,
        )
    }

    private suspend fun isHiddenNodesActive(): Boolean {
        return runCatching {
            getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)
        }.getOrNull() == true
    }

    internal fun updateClickedMenuAction(action: VideoPlayerMenuAction?) {
        val playingHandle = uiState.value.currentPlayingHandle
        when (action) {
            VideoPlayerDownloadAction -> handleDownloadAction(playingHandle)
            VideoPlayerShareAction -> handleShareAction(playingHandle)
            VideoPlayerGetLinkAction -> handleGetLinkAction(playingHandle)
            VideoPlayerRemoveLinkAction -> handleRemoveLinkAction(playingHandle)
            VideoPlayerRenameAction -> handleRenameAction(playingHandle)
            else -> uiState.update { it.copy(clickedMenuAction = action) }
        }
    }

    private fun handleDownloadAction(playingHandle: Long) {
        viewModelScope.launch {
            Analytics.tracker.trackEvent(VideoPlayerSaveToDeviceMenuToolbarEvent)
            val downloadEvent: DownloadTriggerEvent? = when (currentLaunchSources) {
                ZIP_ADAPTER -> {
                    val mediaItem = mediaPlayerGateway.getCurrentMediaItem()
                    val uri = mediaItem?.localConfiguration?.uri ?: return@launch
                    val nodeName = getCurrentPlayingItem()?.nodeName ?: return@launch
                    TransferTriggerEvent.CopyUri(nodeName, uri)
                }

                FROM_CHAT -> {
                    getChatFileUseCase(chatId, messageId)?.let { chatFile ->
                        TransferTriggerEvent.StartDownloadNode(
                            nodes = listOf(chatFile),
                            withStartMessage = true,
                        )
                    }
                }

                FILE_LINK_ADAPTER -> {
                    serialize?.let {
                        val nodes = listOfNotNull(getPublicNodeFromSerializedDataUseCase(it))
                        TransferTriggerEvent.StartDownloadNode(
                            nodes = nodes,
                            withStartMessage = true,
                        )
                    }
                }

                FOLDER_LINK_ADAPTER -> {
                    val nodes =
                        listOfNotNull(getPublicChildNodeFromIdUseCase(NodeId(playingHandle)))
                    TransferTriggerEvent.StartDownloadNode(
                        nodes = nodes,
                        withStartMessage = true,
                    )
                }

                FROM_ALBUM_SHARING -> {
                    val data = getPublicAlbumNodeDataUseCase(NodeId(playingHandle)) ?: return@launch
                    val nodes = listOfNotNull(getPublicNodeFromSerializedDataUseCase(data))
                    TransferTriggerEvent.StartDownloadNode(
                        nodes = nodes,
                        withStartMessage = true,
                    )
                }

                else -> {
                    val nodes = listOfNotNull(getVideoNodeByHandleUseCase(playingHandle))
                    TransferTriggerEvent.StartDownloadNode(
                        nodes = nodes,
                        withStartMessage = true,
                    )
                }
            }
            if (downloadEvent != null)
                uiState.update { it.copy(downloadEvent = triggered(downloadEvent)) }
        }
    }

    private fun handleShareAction(playingHandle: Long) {
        viewModelScope.launch {
            Analytics.tracker.trackEvent(VideoPlayerShareMenuToolbarEvent)
            when (currentLaunchSources) {
                OFFLINE_ADAPTER, ZIP_ADAPTER -> runCatching {
                    val path =
                        mediaPlayerGateway.getCurrentMediaItem()?.localConfiguration?.uri?.path
                            ?: return@launch

                    val file = File(path)
                    if (file.exists()) {
                        val contentUri =
                            getFileUriUseCase(file, Constants.AUTHORITY_STRING_FILE_PROVIDER)
                        val content =
                            MenuOptionClickedContent.ShareFile(Uri.parse(contentUri), file.name)
                        uiState.update { it.copy(menuOptionClickedContent = content) }
                    }
                }.onFailure {
                    Timber.e(it)
                }

                FILE_LINK_ADAPTER -> {
                    val nodeName = getCurrentPlayingItem()?.nodeName ?: ""
                    val content = MenuOptionClickedContent.ShareLink(fileLink, nodeName)
                    uiState.update { it.copy(menuOptionClickedContent = content) }
                }

                else -> {
                    val node = getMegaNode(playingHandle)
                    val content = MenuOptionClickedContent.ShareNode(node)
                    uiState.update { it.copy(menuOptionClickedContent = content) }
                }
            }
        }
    }

    private suspend fun getMegaNode(playingHandle: Long): MegaNode? {
        val serializedData = getNodeByHandle(playingHandle)?.serializedData ?: return null
        return MegaNode.unserialize(serializedData)
    }

    internal fun clearMenuOptionClickedContent() =
        uiState.update { it.copy(menuOptionClickedContent = null) }

    private fun handleGetLinkAction(playingHandle: Long) {
        viewModelScope.launch {
            Analytics.tracker.trackEvent(VideoPlayerGetLinkMenuToolbarEvent)
            val node = getMegaNode(playingHandle)
            val content = MenuOptionClickedContent.GetLink(node)
            uiState.update { it.copy(menuOptionClickedContent = content) }
        }
    }

    private fun handleRemoveLinkAction(playingHandle: Long) {
        viewModelScope.launch {
            Analytics.tracker.trackEvent(VideoPlayerRemoveLinkMenuToolbarEvent)
            val node = getMegaNode(playingHandle)
            val content = MenuOptionClickedContent.RemoveLink(node)
            uiState.update { it.copy(menuOptionClickedContent = content) }
        }
    }

    private fun handleRenameAction(playingHandle: Long) {
        viewModelScope.launch {
            val node = getMegaNode(playingHandle)
            val content = MenuOptionClickedContent.Rename(node)
            uiState.update { it.copy(menuOptionClickedContent = content) }
        }
    }

    internal fun removeLink() {
        viewModelScope.launch {
            val nodeId = NodeId(uiState.value.currentPlayingHandle)
            runCatching { disableExportUseCase(nodeId) }.onFailure { Timber.e(it) }
        }
    }

    internal suspend fun isNodeComesFromIncoming(): Boolean {
        val handle = uiState.value.currentPlayingHandle
        return runCatching {
            isNodeInRubbishBinUseCase(NodeId(handle)) &&
                    isNodeInCloudDriveUseCase(handle) &&
                    isNodeInBackupsNodeUseCase(handle)
        }.getOrDefault(false)
    }

    internal fun getCurrentPlayingItem() = uiState.value.items.firstOrNull {
        it.nodeHandle == uiState.value.currentPlayingHandle
    }

    /**
     * Imports a chat node if there is no name collision.
     *
     * @param newParentHandle   Parent handle in which the node will be copied.
     */
    fun importChatNode(
        newParentHandle: NodeId,
    ) = viewModelScope.launch {
        runCatching {
            checkChatNodesNameCollisionAndCopyUseCase(
                chatId = chatId,
                messageIds = listOf(messageId),
                newNodeParent = newParentHandle,
            )
        }.onSuccess {
            it.firstChatNodeCollisionOrNull?.let { item ->
                collision.value = item
            }
            it.moveRequestResult?.let { result ->
                snackbarMessage.value = if (result.isSuccess) {
                    R.string.context_correctly_copied
                } else {
                    R.string.context_no_copied
                }
            }
        }.onFailure {
            throwable.value = it
            Timber.e(it)
        }
    }

    /**
     * Moves a node if there is no name collision.
     *
     * @param nodeHandle        Node handle to move.
     * @param newParentHandle   Parent handle in which the node will be moved.
     */
    fun moveNode(nodeHandle: Long, newParentHandle: Long) {
        viewModelScope.launch {
            runCatching {
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(nodeHandle to newParentHandle),
                    type = NodeNameCollisionType.MOVE,
                )
            }.onSuccess {
                it.firstNodeCollisionOrNull?.let { item ->
                    collision.value = item
                }
                it.moveRequestResult?.let {
                    if (it.isSuccess) {
                        snackbarMessage.value = R.string.context_correctly_moved
                    } else {
                        snackbarMessage.value = R.string.context_no_moved
                    }
                }
            }.onFailure {
                Timber.e(it, "Error not moved")
                if (it is NodeDoesNotExistsException) {
                    snackbarMessage.value = R.string.general_error
                } else {
                    throwable.value = it
                }
            }
        }
    }

    /**
     * Copies a node if there is no name collision.
     *
     * @param nodeHandle        Node handle to copy.
     * @param newParentHandle   Parent handle in which the node will be copied.
     */
    fun copyNode(
        nodeHandle: Long? = null,
        newParentHandle: Long,
    ) {
        if (nodeHandle == null) return
        viewModelScope.launch {
            runCatching {
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(nodeHandle to newParentHandle),
                    type = NodeNameCollisionType.COPY,
                )
            }.onSuccess {
                it.firstNodeCollisionOrNull?.let { item ->
                    collision.value = item
                }
                it.moveRequestResult?.let { result ->
                    snackbarMessage.value = if (result.isSuccess) {
                        R.string.context_correctly_copied
                    } else {
                        R.string.context_no_copied
                    }
                }
            }.onFailure {
                Timber.e(it, "Error not copied")
                if (it is NodeDoesNotExistsException) {
                    snackbarMessage.value = R.string.general_error
                } else {
                    throwable.value = it
                }
            }
        }
    }

    internal fun updateIsVideoOptionPopupShown(value: Boolean) {
        uiState.update { it.copy(isVideoOptionPopupShown = value) }
    }

    /**
     * Capture the screenshot when video playing
     *
     * @param captureView the view that will be captured
     * @param successCallback the callback after the screenshot is saved successfully
     *
     */
    @SuppressLint("SimpleDateFormat")
    internal fun screenshotWhenVideoPlaying(
        rootPath: String,
        captureView: View,
        successCallback: (bitmap: Bitmap) -> Unit,
    ) {
        val textureView = captureView as? TextureView
        if (textureView == null || !textureView.isAvailable) {
            Timber.d("Capture screenshot error: TextureView is not available")
            return
        }
        // Using video size for the capture size to ensure the screenshot is complete.
        val (captureWidth, captureHeight) =
            uiState.value.currentPlayingVideoSize?.let { (width, height) ->
                width to height
            } ?: (captureView.width to captureView.height)
        try {
            val screenshotBitmap = Bitmap.createBitmap(
                captureWidth,
                captureHeight,
                Bitmap.Config.ARGB_8888
            )
            val surfaceView = Surface(textureView.surfaceTexture)
            PixelCopy.request(
                surfaceView,
                Rect(0, 0, captureWidth, captureHeight),
                screenshotBitmap,
                { copyResult ->
                    if (copyResult == PixelCopy.SUCCESS) {
                        viewModelScope.launch {
                            saveBitmapByMediaStore(
                                rootPath = rootPath,
                                bitmap = screenshotBitmap,
                                successCallback = successCallback
                            )
                        }
                    }
                },
                Handler(Looper.getMainLooper())
            )
        } catch (e: Exception) {
            Timber.e("Capture screenshot error: ${e.message}")
        }
    }

    @SuppressLint("SimpleDateFormat")
    private suspend fun saveBitmapByMediaStore(
        rootPath: String,
        bitmap: Bitmap,
        successCallback: (bitmap: Bitmap) -> Unit,
    ) = withContext(ioDispatcher) {
        val contentValues = organiseContentValues(rootPath)
        insertAndCompressBitmap(contentValues, bitmap, successCallback)
    }

    @SuppressLint("SimpleDateFormat")
    private fun organiseContentValues(rootPath: String): ContentValues {
        val screenshotFileName =
            SimpleDateFormat(DATE_FORMAT_PATTERN).format(Date(System.currentTimeMillis()))
        val screenshotFileFullName =
            "${SCREENSHOT_NAME_PREFIX}$screenshotFileName${SCREENSHOT_NAME_SUFFIX}"

        return ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, screenshotFileFullName)
            put(MediaStore.Images.Media.MIME_TYPE, MIME_TYPE_JPEG)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val screenshotsFolderPath =
                    "${rootPath}${File.separator}${MEGA_SCREENSHOTS_FOLDER_NAME}${File.separator}"
                val fileAbsolutePath = "$screenshotsFolderPath$screenshotFileFullName"

                put(MediaStore.Images.Media.DATA, fileAbsolutePath)
            } else {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${DCIM_FOLDER_NAME}$MEGA_SCREENSHOTS_FOLDER_NAME"
                )
            }
        }
    }

    private fun insertAndCompressBitmap(
        contentValues: ContentValues,
        bitmap: Bitmap,
        successCallback: (bitmap: Bitmap) -> Unit,
    ) {
        val contentResolver = context.contentResolver
        contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?.let { uri ->
                try {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        bitmap.compress(
                            Bitmap.CompressFormat.JPEG,
                            QUALITY_SCREENSHOT,
                            outputStream
                        )
                        successCallback(bitmap)
                    }
                } catch (e: Exception) {
                    Timber.e("Bitmap is saved error: ${e.message}")
                }
            }
    }

    internal fun resetDownloadNode() = uiState.update {
        it.copy(downloadEvent = consumed())
    }

    private suspend fun getNodeByHandle(handle: Long) =
        runCatching { getVideoNodeByHandleUseCase(handle) }.getOrNull()

    internal fun getCollision(): LiveData<NameCollision> = collision

    internal fun onSnackbarMessage(): LiveData<Int> = snackbarMessage

    internal fun onExceptionThrown(): LiveData<Throwable> = throwable

    internal fun onStartChatFileOfflineDownload(): LiveData<ChatFile> = startChatFileOfflineDownload

    internal fun startDownloadForOffline(chatFile: ChatFile) = uiState.update {
        it.copy(
            downloadEvent = triggered(
                StartDownloadForOffline(
                    node = chatFile,
                    withStartMessage = true,
                )
            )
        )
    }

    /**
     * Save chat node to offline
     *
     * @param chatId    Chat ID where the node is.
     * @param messageId Message ID where the node is.
     */
    fun saveChatNodeToOffline() {
        viewModelScope.launch {
            runCatching {
                val chatFile = getChatFileUseCase(chatId = chatId, messageId = messageId)
                    ?: throw IllegalStateException("Chat file not found")
                val isAvailableOffline = isAvailableOfflineUseCase(chatFile)
                if (isAvailableOffline) {
                    snackbarMessage.value = R.string.file_already_exists
                } else {
                    startChatFileOfflineDownload.value = chatFile
                }
            }.onFailure {
                Timber.e(it)
                throwable.value = it
            }
        }
    }

    internal fun hideOrUnhideNodes(nodeIds: List<NodeId>, hide: Boolean) = viewModelScope.launch {
        for (nodeId in nodeIds) {
            async {
                runCatching {
                    updateNodeSensitiveUseCase(nodeId = nodeId, isSensitive = hide)
                }.onFailure { Timber.e("Update sensitivity failed: $it") }
            }
        }
    }

    internal fun setHiddenNodesOnboarded() {
        uiState.update {
            it.copy(isHiddenNodesOnboarded = true)
        }
    }

    internal suspend fun deleteMessageFromChat() {
        deleteNodeAttachmentMessageByIdsUseCase(chatId, messageId)
    }

    companion object {
        private const val MAX_RETRY = 6

        private const val MEGA_SCREENSHOTS_FOLDER_NAME = "MEGA Screenshots/"
        private const val DCIM_FOLDER_NAME = "DCIM/"
        private const val MIME_TYPE_JPEG = "image/jpeg"
        private const val QUALITY_SCREENSHOT = 100
        private const val DATE_FORMAT_PATTERN = "yyyyMMdd-HHmmss"
        private const val SCREENSHOT_NAME_PREFIX = "Screenshot_"
        private const val SCREENSHOT_NAME_SUFFIX = ".jpg"
    }
}


