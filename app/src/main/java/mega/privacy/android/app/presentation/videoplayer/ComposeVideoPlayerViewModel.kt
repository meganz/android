package mega.privacy.android.app.presentation.videoplayer

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
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
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.model.MediaPlaySources
import mega.privacy.android.app.mediaplayer.model.SpeedPlaybackItem
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemType
import mega.privacy.android.app.mediaplayer.service.Metadata
import mega.privacy.android.app.presentation.videoplayer.mapper.PlayerErrorTypeMapper
import mega.privacy.android.app.presentation.videoplayer.mapper.VideoPlayerItemMapper
import mega.privacy.android.app.presentation.videoplayer.model.MediaPlaybackState
import mega.privacy.android.app.presentation.videoplayer.model.SubtitleSelectedStatus
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerItem
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerLaunchSource
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerUiState
import mega.privacy.android.app.presentation.videoplayer.model.VideoSize
import mega.privacy.android.app.utils.Constants.CONTACT_FILE_ADAPTER
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FROM_ALBUM_SHARING
import mega.privacy.android.app.utils.Constants.FROM_CHAT
import mega.privacy.android.app.utils.Constants.FROM_IMAGE_VIEWER
import mega.privacy.android.app.utils.Constants.FROM_MEDIA_DISCOVERY
import mega.privacy.android.app.utils.Constants.INVALID_SIZE
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.RECENTS_ADAPTER
import mega.privacy.android.app.utils.Constants.RECENTS_BUCKET_ADAPTER
import mega.privacy.android.app.utils.Constants.SEARCH_BY_ADAPTER
import mega.privacy.android.app.utils.Constants.VERSIONS_ADAPTER
import mega.privacy.android.app.utils.Constants.VIDEO_BROWSE_ADAPTER
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.ThumbnailUtils
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.continuewhereleftoff.CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS
import mega.privacy.android.domain.entity.continuewhereleftoff.CWLO_NEAR_COMPLETION_THRESHOLD_MS
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.entity.mediaplayer.PlaybackInformation
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.exception.BlockedMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.featuretoggle.ApiFeatures
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
import mega.privacy.android.domain.usecase.MonitorPlaybackTimesUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.call.IsParticipatingInChatCallUseCase
import mega.privacy.android.domain.usecase.continuewhereleftoff.RemoveRecentlyUsedItemUseCase
import mega.privacy.android.domain.usecase.continuewhereleftoff.SaveRecentlyUsedItemUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.GetFileByPathUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.mediaplayer.GetLocalFolderLinkUseCase
import mega.privacy.android.domain.usecase.mediaplayer.HttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.HttpServerStartUseCase
import mega.privacy.android.domain.usecase.mediaplayer.HttpServerStopUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetSRTSubtitleFileListUseCase
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
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.SavePlaybackTimesUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.SetVideoRepeatModeUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.TrackPlaybackPositionUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInCloudDriveUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.backup.GetBackupsNodeUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineNodeInformationByIdUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.setting.MonitorSubFolderMediaDiscoverySettingsUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.BroadcastTransferOverQuotaUseCase
import mega.privacy.android.domain.usecase.videosection.SaveVideoRecentlyWatchedUseCase
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.BACKUPS_ADAPTER
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.FAVOURITES_ADAPTER
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.FILE_BROWSER_ADAPTER
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.FILE_LINK_ADAPTER
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.INCOMING_SHARES_ADAPTER
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.LINKS_ADAPTER
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.OUTGOING_SHARES_ADAPTER
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.RUBBISH_BIN_ADAPTER
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.LockButtonPressedEvent
import mega.privacy.mobile.analytics.event.OffOptionForHideSubtitlePressedEvent
import mega.privacy.mobile.analytics.event.UnlockButtonPressedEvent
import mega.privacy.mobile.analytics.event.VideoPlaybackAviStartedEvent
import mega.privacy.mobile.analytics.event.VideoPlaybackMkvStartedEvent
import mega.privacy.mobile.analytics.event.VideoPlaybackMovStartedEvent
import mega.privacy.mobile.analytics.event.VideoPlaybackMp4StartedEvent
import mega.privacy.mobile.analytics.event.VideoPlaybackOtherStartedEvent
import mega.privacy.mobile.analytics.event.VideoPlayerFullScreenPressedEvent
import mega.privacy.mobile.analytics.event.VideoPlayerIsActivatedEvent
import mega.privacy.mobile.analytics.event.VideoPlayerOriginalPressedEvent
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Collections
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * ViewModel for video player.
 */
@HiltViewModel(assistedFactory = ComposeVideoPlayerViewModel.Factory::class)
class ComposeVideoPlayerViewModel @AssistedInject constructor(
    @ApplicationContext private val context: Context,
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
    private val saveRecentlyUsedItemUseCase: SaveRecentlyUsedItemUseCase,
    private val removeRecentlyUsedItemUseCase: RemoveRecentlyUsedItemUseCase,
    private val setVideoRepeatModeUseCase: SetVideoRepeatModeUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val isHiddenNodesOnboardedUseCase: IsHiddenNodesOnboardedUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
    private val isNodeInRubbishBinUseCase: IsNodeInRubbishBinUseCase,
    private val isNodeInBackupsNodeUseCase: IsNodeInBackupsUseCase,
    private val isNodeInCloudDriveUseCase: IsNodeInCloudDriveUseCase,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper,
    private val isParticipatingInChatCallUseCase: IsParticipatingInChatCallUseCase,
    private val trackPlaybackPositionUseCase: TrackPlaybackPositionUseCase,
    private val monitorPlaybackTimesUseCase: MonitorPlaybackTimesUseCase,
    private val savePlaybackTimesUseCase: SavePlaybackTimesUseCase,
    private val getSRTSubtitleFileListUseCase: GetSRTSubtitleFileListUseCase,
    private val broadcastTransferOverQuotaUseCase: BroadcastTransferOverQuotaUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val playerErrorTypeMapper: PlayerErrorTypeMapper,
    private val mediaPlayerManager: MediaPlayerManager,
    @Assisted private val args: Args,
    @Assisted private val initialLaunchSource: VideoPlayerLaunchSource?,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : ViewModel() {

    val uiState: StateFlow<VideoPlayerUiState>
        field: MutableStateFlow<VideoPlayerUiState> = MutableStateFlow(
            VideoPlayerUiState(
                fileLinkUrl = args.fileLinkUrl,
                localFilePath = args.localFilePath,
            )
        )

    private var needStopStreamingServer = false
    private var playerRetry = 0

    private val snackbarMessage = SingleLiveEvent<Int>()
    private var searchQuery: String = ""
    private val mediaItemsDuringChanged = mutableListOf<MediaItem>()
    private var searchJob: Job? = null
    private val mutex = Mutex()
    private var playbackPositionJob: Job? = null
    private var hasCheckedPlaybackPosition = false
    private var launchSource: VideoPlayerLaunchSource? = null

    private var isPausedByUser = false
    private var allowUpdatePausedByUser = true
    private var wasPlayingBeforeSubtitleDialog = false

    /**
     * The [ExoPlayer] owned by this ViewModel and shared with the UI. Built by [MediaPlayerManager]
     * eagerly so the gateway's player is ready before [initVideoPlayerData] builds the playback
     * sources, and released in [onCleared] — its lifetime is tied to this route's ViewModel.
     */
    val player: ExoPlayer = mediaPlayerManager.createPlayer(
        onMetadataChanged = { title, artist, album ->
            updateMetadata(Metadata(title, artist, album, uiState.value.currentPlayingItemName ?: ""))
        },
        onMediaItemTransition = ::onMediaItemTransition,
        onRepeatModeChanged = ::updateRepeatToggleMode,
        onPlayWhenReadyChanged = ::onPlayWhenReadyChanged,
        onPlaybackStateChanged = ::onPlaybackStateChanged,
        onPlayerError = ::onPlayerError,
        onVideoSizeChanged = ::updateCurrentPlayingVideoSize,
    )

    init {
        uiState.update {
            it.copy(
                nodeSourceType = adapterTypeToNodeSourceType(),
            )
        }
        setupTransferListener()
        handleHiddenNodesUIFlow()
        monitorIsHiddenNodesOnboarded()
        updateNameWhenNodeUpdates()
        monitorConnectivity()
        loadPipFeatureFlag()
        checkLaunchSource()
    }

    private fun checkLaunchSource() {
        initialLaunchSource?.let { source ->
            initVideoPlayerData(source)
            handleAutoReplayIfPaused()
        } ?: uiState.update { it.copy(invalidLaunchSourceEvent = triggered) }
    }

    private fun loadPipFeatureFlag() {
        viewModelScope.launch {
            runCatching {
                val enabled = getFeatureFlagValueUseCase(ApiFeatures.VideoPlayerPictureInPicture)
                uiState.update { it.copy(isPipEnabled = enabled) }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun monitorConnectivity() {
        monitorConnectivityUseCase()
            .onEach { isConnected ->
                uiState.update {
                    it.copy(isConnected = isConnected)
                }
            }
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)
    }

    private fun adapterTypeToNodeSourceType(): NodeSourceType =
        when (args.adapterType) {
            OFFLINE_ADAPTER -> NodeSourceType.OFFLINE
            RUBBISH_BIN_ADAPTER -> NodeSourceType.RUBBISH_BIN
            FOLDER_LINK_ADAPTER, FROM_ALBUM_SHARING -> NodeSourceType.FOLDER_LINK
            FROM_CHAT -> NodeSourceType.CHAT
            FILE_LINK_ADAPTER -> NodeSourceType.FILE_LINK
            FROM_IMAGE_VIEWER -> NodeSourceType.VIDEO_PLAYER_IMAGE_VIEWER
            VERSIONS_ADAPTER -> NodeSourceType.VIDEO_PLAYER_VERSIONS
            ZIP_ADAPTER -> NodeSourceType.VIDEO_PLAYER_ZIP_FILE
            else -> NodeSourceType.VIDEO_PLAYER_DEFAULT
        }

    private fun updateNameWhenNodeUpdates() {
        viewModelScope.launch {
            monitorNodeUpdatesUseCase().filter {
                it.changes.entries.any { (node, changeList) ->
                    node is FileNode && node.type is VideoFileTypeInfo &&
                            NodeChanges.Name in changeList
                }
            }.map { nodeUpdate -> nodeUpdate.changes }
                .catch {
                    Timber.e(it)
                }.collectLatest { changes ->
                    val currentHandle = uiState.value.currentPlayingHandle
                    val renamedNode = changes.entries
                        .firstOrNull { (node, changeList) ->
                            node.id.longValue == currentHandle &&
                                    NodeChanges.Name in changeList
                        }?.key
                    uiState.update { state ->
                        state.copy(
                            metadata = renamedNode
                                ?.let { state.metadata.copy(nodeName = it.name) }
                                ?: state.metadata,
                            currentPlayingItemName = renamedNode?.name
                                ?: state.currentPlayingItemName,
                        )
                    }
                }
        }
    }

    /**
     * Setup transfer listener
     */
    private fun setupTransferListener() =
        viewModelScope.launch {
            monitorTransferEventsUseCase()
                .filter {
                    it is TransferEvent.TransferTemporaryErrorEvent
                            && it.transfer.nodeHandle == uiState.value.currentPlayingHandle
                }
                .catch { Timber.e(it) }
                .collect { event ->
                    when (val error = (event as TransferEvent.TransferTemporaryErrorEvent).error) {
                        is QuotaExceededMegaException -> {
                            if (!event.transfer.isForeignOverQuota && error.value != 0L) {
                                viewModelScope.launch { broadcastTransferOverQuotaUseCase(true) }
                            }
                        }

                        is BlockedMegaException -> {
                            uiState.update { it.copy(blockedError = triggered) }
                        }

                        else -> {}
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

            uiState.update {
                it.copy(
                    accountType = accountType,
                    isBusinessAccountExpired = businessStatus == BusinessAccountStatus.Expired,
                    hiddenNodeEnabled = true,
                    showHiddenItems = showHiddenItems,
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

    internal fun initVideoPlayerData(source: VideoPlayerLaunchSource?) {
        launchSource = source
        checkPlaybackPositionBeforePlayback(args.handle) {
            initVideoPlaybackSources()
        }
        hasCheckedPlaybackPosition = true
    }

    private fun initVideoPlaybackSources() {
        viewModelScope.launch {
            buildPlaybackSources(launchSource)
            trackPlaybackPositionUseCase {
                PlaybackInformation(
                    mediaPlayerManager.getCurrentMediaItem()?.mediaId?.toLong(),
                    mediaPlayerManager.getCurrentItemDuration(),
                    mediaPlayerManager.getCurrentPlayingPosition()
                )
            }
        }
    }

    private suspend fun buildPlaybackSources(source: VideoPlayerLaunchSource?) {
        if (source == null || !validateLaunchSource(source)) return

        val uri = source.contentUri
        val currentPlayingHandle = source.handle
        val currentPlayingFileName = source.fileName.orEmpty()
        val fileExtension = currentPlayingFileName.substringAfterLast(".", "").lowercase()
        when (fileExtension) {
            "mp4" -> Analytics.tracker.trackEvent(VideoPlaybackMp4StartedEvent)
            "avi" -> Analytics.tracker.trackEvent(VideoPlaybackAviStartedEvent)
            "mkv" -> Analytics.tracker.trackEvent(VideoPlaybackMkvStartedEvent)
            "mov" -> Analytics.tracker.trackEvent(VideoPlaybackMovStartedEvent)
            else -> Analytics.tracker.trackEvent(VideoPlaybackOtherStartedEvent)
        }
        needStopStreamingServer = source.needStopHttpServer
        playerRetry = 0

        val currentPlayingUri =
            getCurrentPlayingUri(uri, args.adapterType, currentPlayingHandle)
        if (currentPlayingUri == null) {
            logInvalidParam("folder link uri is null")
            return
        }

        val currentPlayingMediaItem = MediaItem.Builder()
            .setUri(currentPlayingUri)
            .setMediaId(currentPlayingHandle.toString())
            .build()

        updateStateWithMediaItem(
            mediaItem = currentPlayingMediaItem,
            fileName = currentPlayingFileName,
            currentPlayingHandle = currentPlayingHandle
        )

        if (!source.isPlaylist) {
            setPlayingItem(currentPlayingHandle, currentPlayingFileName, args.adapterType)
            return
        }

        if (args.adapterType != OFFLINE_ADAPTER && args.adapterType != ZIP_ADAPTER) {
            needStopStreamingServer =
                needStopStreamingServer || setupStreamingServer(args.adapterType)
        }

        withContext(ioDispatcher) {
            handlePlaybackSourceByLaunchSource(source, args.adapterType, currentPlayingHandle)
        }
    }

    private fun validateLaunchSource(source: VideoPlayerLaunchSource): Boolean {
        val isValid = when {
            !source.rebuildPlaylist -> {
                logInvalidParam("Rebuild playlist param is false")
                false
            }

            source.adapterType == INVALID_VALUE -> {
                logInvalidParam("Launch source is invalid")
                false
            }

            source.contentUri == null -> {
                logInvalidParam("URI is null")
                false
            }

            source.handle == INVALID_HANDLE -> {
                logInvalidParam("The first playing video handle is invalid")
                false
            }

            source.fileName == null -> {
                logInvalidParam("The first playing video file name is null")
                false
            }

            else -> true
        }
        return isValid
    }

    private fun logInvalidParam(message: String) {
        Timber.d("Build playback sources failed: $message")
        uiState.update { it.copy(retryFailedEvent = triggered) }
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
                url?.toUri()
            }

            else -> uri
        }

    private fun updateStateWithMediaItem(
        mediaItem: MediaItem,
        fileName: String,
        currentPlayingHandle: Long,
    ) {
        MediaPlaySources(
            mediaItems = listOf(mediaItem),
            newIndexForCurrentItem = INVALID_VALUE,
            nameToDisplay = fileName
        ).also { sources ->
            uiState.update {
                it.copy(
                    mediaPlaySources = sources,
                    currentPlayingHandle = currentPlayingHandle
                )
            }
            buildPlaybackSourcesForPlayer(sources)
        }
    }

    private fun buildPlaybackSourcesForPlayer(mediaPlaySources: MediaPlaySources) =
        viewModelScope.launch(mainDispatcher) {
            Timber.d("Playback sources: ${mediaPlaySources.mediaItems.size} items")
            with(mediaPlayerManager) {
                buildPlaySources(mediaPlaySources)
                setPlayWhenReady(
                    mediaPlaySources.isRestartPlaying &&
                            !uiState.value.showSubTitlesOptions &&
                            !isPausedByUser
                )
                playerPrepare()
            }
            // Apply the seek during BUFFERING so it is queued by ExoPlayer and executed on
            // STATE_READY. The subsequent STATE_READY callback in onPlaybackStateChanged will
            // find playbackPosition = null (already cleared here) and be a no-op.
            applyPendingSavedPlaybackPositionSeek()
            mediaPlaySources.nameToDisplay?.let { name ->
                uiState.update {
                    it.copy(
                        currentPlayingItemName = name,
                        metadata = Metadata(
                            null, null, null, nodeName = name
                        )
                    )
                }
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
            isSensitive = if (node == null) {
                false
            } else {
                node.isMarkedSensitive || node.isSensitiveInherited
            }
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
        source: VideoPlayerLaunchSource,
        launchSource: Int,
        playingHandle: Long,
    ) {
        when (launchSource) {
            OFFLINE_ADAPTER -> handleOfflineSource(source, playingHandle)
            ZIP_ADAPTER -> handleZipSource(source, playingHandle)
            else -> handleGeneralSource(source, launchSource, playingHandle)
        }
    }

    private suspend fun handleOfflineSource(source: VideoPlayerLaunchSource, playingHandle: Long) {
        val parentId = source.parentId
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

                runCatching { item.absolutePath.toUri() }.getOrNull()?.let {
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
                    isSensitive = false
                )
            }

            if (videoPlayerItems.isNotEmpty() && mediaItems.isNotEmpty()) {
                updatePlaybackSources(
                    videoPlayerItems = videoPlayerItems,
                    mediaItems = mediaItems,
                    title = title,
                    currentPlayingIndex = currentPlayingIndex,
                    firstPlayHandle = firstPlayHandle
                )
            }
        }.onFailure {
            Timber.e(it)
        }
    }

    private suspend fun handleZipSource(source: VideoPlayerLaunchSource, playingHandle: Long) {
        source.offlinePathDirectory?.let { zipPath ->
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

        val updatedItems = videoPlayerItems.mapIndexed { index, item ->
            val newType = when {
                index == currentPlayingIndex -> MediaQueueItemType.Playing
                index < currentPlayingIndex -> MediaQueueItemType.Previous
                else -> MediaQueueItemType.Next
            }
            item.takeIf { it.type == newType } ?: item.copy(type = newType)
        }

        uiState.update {
            it.copy(
                items = updatedItems,
                mediaPlaySources = mediaPlaySources,
                playQueueTitle = title,
                currentPlayingIndex = currentPlayingIndex,
                currentPlayingHandle = firstPlayHandle,
                currentPlayingItemName = args.fileName
            )
        }
        buildPlaybackSourcesForPlayer(mediaPlaySources)
    }

    private suspend fun buildPlaybackSourcesByFiles(zipPath: String, firstPlayHandle: Long) {
        runCatching {
            val (title, files) = getFileByPathUseCase(zipPath).let { zipFile ->
                zipFile?.parentFile?.name.orEmpty() to zipFile?.listFiles()?.toList().orEmpty()
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
                    isSensitive = false
                )
            }

            if (videoPlayerItems.isNotEmpty() && mediaItems.isNotEmpty()) {
                updatePlaybackSources(
                    videoPlayerItems = videoPlayerItems,
                    mediaItems = mediaItems,
                    title = title,
                    currentPlayingIndex = currentPlayingIndex,
                    firstPlayHandle = firstPlayHandle
                )
            }
        }.onFailure {
            Timber.e(it)
        }
    }

    private suspend fun handleGeneralSource(
        source: VideoPlayerLaunchSource,
        launchSource: Int,
        playingHandle: Long,
    ) {
        val parentHandle = source.parentHandle
        val order = source.sortOrder
        val (title, videoNodes) = when (launchSource) {
            VIDEO_BROWSE_ADAPTER ->
                context.getString(R.string.sortby_type_video_first) to getVideoNodesUseCase(order)

            RECENTS_ADAPTER, RECENTS_BUCKET_ADAPTER -> {
                val videoNodes = source.nodeHandles?.let { handles ->
                    getVideoNodesByHandlesUseCase(handles)
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
                val title = source.mediaQueueTitle.orEmpty()
                val videoNodes = source.searchedItems
                    ?.let { handles ->
                        getVideoNodesByHandlesUseCase(handles)
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
                when (launchSource) {
                    LINKS_ADAPTER if parentHandle == INVALID_HANDLE ->
                        context.getString(sharedR.string.shares_screen_links_shares_tab_title) to getVideoNodesFromPublicLinksUseCase(
                            order
                        )

                    INCOMING_SHARES_ADAPTER if parentHandle == INVALID_HANDLE ->
                        context.getString(sharedR.string.shares_screen_incoming_shares_tab_title) to getVideoNodesFromInSharesUseCase(
                            order
                        )

                    OUTGOING_SHARES_ADAPTER if parentHandle == INVALID_HANDLE ->
                        context.getString(sharedR.string.shares_screen_outgoing_shares_tab_title) to getVideoNodesFromOutSharesUseCase(
                            lastHandle = INVALID_HANDLE,
                            order = order
                        )

                    CONTACT_FILE_ADAPTER if parentHandle == INVALID_HANDLE -> {
                        source.contactEmail
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
                                        RUBBISH_BIN_ADAPTER -> sharedR.string.general_section_rubbish_bin
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
            val filteredVideoNodes = filterNonSensitiveNodes(videoNodes.filterTakeDownNodes())
            if (filteredVideoNodes.isNotEmpty()) {
                buildPlaybackSourcesByNodes(title, filteredVideoNodes, playingHandle, launchSource)
            }
        }
    }

    private fun List<TypedVideoNode>.filterTakeDownNodes(): List<TypedVideoNode> =
        filter { !it.isTakenDown }

    private suspend fun filterNonSensitiveNodes(nodes: List<TypedVideoNode>): List<TypedVideoNode> {
        val state = uiState.value
        val showHiddenItems = runCatching {
            (state.showHiddenItems ?: monitorShowHiddenItemsUseCase().firstOrNull()) == true
        }.getOrDefault(false)

        val (accountType, isExpired) = runCatching {
            state.accountType?.let {
                it to state.isBusinessAccountExpired
            } ?: run {
                val account = monitorAccountDetailUseCase().firstOrNull()?.levelDetail?.accountType
                val businessStatus = if (account?.isBusinessAccount == true) {
                    getBusinessStatusUseCase()
                } else null
                account to (businessStatus == BusinessAccountStatus.Expired)
            }
        }.getOrDefault(null to false)

        return when {
            accountType == null -> if (showHiddenItems) {
                nodes
            } else {
                nodes.filter { !it.isMarkedSensitive && !it.isSensitiveInherited }
            }

            showHiddenItems || !accountType.isPaid || isExpired -> nodes
            else -> nodes.filter { !it.isMarkedSensitive && !it.isSensitiveInherited }
        }
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
                    isSensitive = node.isMarkedSensitive || node.isSensitiveInherited
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
                        .setUri(url.toUri())
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
        mediaPlayerManager.release()
    }

    /**
     * Retry playback after a player error.
     */
    fun retry() {
        viewModelScope.launch {
            mediaPlayerManager.retry()
        }
    }

    /**
     * Clear the state and flying task of this class, should be called in onDestroy.
     */
    private fun clear() {
        searchJob?.cancel()
        saveRecentlyUsedItemIfQualifies()
        applicationScope.launch {
            if (needStopStreamingServer) {
                httpServerStopUseCase()
            }
            savePlaybackTimesUseCase()
        }
    }

    /**
     * Decide whether the current item belongs in the Continue Where Left Off index when the
     * user leaves the player (or playback ends). The item is added only once it has been played
     * past [CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS] and is still more than
     * [CWLO_NEAR_COMPLETION_THRESHOLD_MS] from its end; otherwise it is removed so that briefly
     * opened, finished, or near-completion items are not surfaced back as resumable.
     *
     * Caveat: in repeat mode ExoPlayer may loop directly without firing STATE_ENDED, or fire
     * it after position has wrapped to 0. In that case the ticker path is the source of truth.
     */
    private fun saveRecentlyUsedItemIfQualifies() {
        val handle = mediaPlayerManager.getCurrentMediaItem()?.mediaId?.toLongOrNull() ?: return
        val duration = mediaPlayerManager.getCurrentItemDuration()
        val position = mediaPlayerManager.getCurrentPlayingPosition()
        // Duration not yet known: cannot evaluate, leave the CWLO index untouched.
        if (duration <= 0L) return
        val qualifies = position > CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS
                && duration - position >= CWLO_NEAR_COMPLETION_THRESHOLD_MS
        applicationScope.launch {
            runCatching {
                if (qualifies) {
                    saveRecentlyUsedItemUseCase(
                        nodeHandle = handle,
                        type = RecentlyUsedType.Video,
                        fileName = uiState.value.metadata.nodeName,
                    )
                } else {
                    removeRecentlyUsedItemUseCase(handle)
                }
            }.onFailure { Timber.e(it, "Failed to update CWLO item on leave") }
        }
    }

    internal fun updateMetadata(metadata: Metadata) =
        uiState.update { it.copy(metadata = metadata) }

    internal fun onMediaItemTransition(handle: String?, isUpdateName: Boolean) {
        updateCurrentPlayingVideoSize(null)
        if (handle == null) return
        if (isPausedByUser) {
            isPausedByUser = false
        }
        if (uiState.value.currentPlayingHandle != handle.toLong())
            Analytics.tracker.trackEvent(VideoPlayerIsActivatedEvent)
        updateCurrentPlayingHandle(handle.toLong(), isUpdateName)
        saveVideoWatchedTime()
        if (isUpdateName) {
            val items = uiState.value.items
            val playingIndex = items.indexOfFirst { it.nodeHandle == handle.toLong() }
            if (playingIndex == -1) return
            val nodeName = items[playingIndex].nodeName
            //After the video transition, clear the subtitle info
            clearSubtitleInfo(playingIndex)
            uiState.update {
                it.copy(
                    currentPlayingItemName = nodeName,
                    metadata = Metadata(null, null, null, nodeName)
                )
            }
        }
    }

    internal fun updateCurrentPlayingVideoSize(videoSize: VideoSize?) =
        uiState.update { it.copy(currentPlayingVideoSize = videoSize) }

    internal fun updateCurrentPlayingHandle(
        handle: Long,
        isCheckPlaybackPosition: Boolean,
        items: List<VideoPlayerItem> = uiState.value.items,
    ) {
        viewModelScope.launch {
            savePlaybackTimesUseCase()
            handlePlaybackPositionAfterVideoTransition(handle, isCheckPlaybackPosition)
            updatePlaybackStatus(handle, items)
        }
    }

    private fun handlePlaybackPositionAfterVideoTransition(
        handle: Long,
        isCheckPlaybackPosition: Boolean,
    ) {
        //If hasCheckedPlaybackPosition is true, avoid checking playback position again
        if (!hasCheckedPlaybackPosition && isCheckPlaybackPosition) {
            // Pause the video before check playback position
            mediaPlayerManager.setPlayWhenReady(false)
            checkPlaybackPositionBeforePlayback(handle) {
                // Apply the seek here, inside the callback, where the position is guaranteed
                // to already be stored in state. This avoids the race condition where ExoPlayer
                // could reach STATE_READY before the position coroutine completes, which would
                // cause applyPendingSavedPlaybackPositionSeek() in onPlaybackStateChanged to be
                // a no-op and silently discard the saved position (especially for local/offline files).
                applyPendingSavedPlaybackPositionSeek()
                mediaPlayerManager.setPlayWhenReady(true)
            }
        } else {
            applyPendingSavedPlaybackPositionSeek()
            ensurePlayingAfterPlaybackPositionHandling()
        }

        // Reset hasCheckedPlaybackPosition to re-check playback position on video transition
        hasCheckedPlaybackPosition = false
    }

    private fun updatePlaybackStatus(handle: Long, items: List<VideoPlayerItem>) {
        if (items.isEmpty()) return
        val playingIndex = items.indexOfFirst { it.nodeHandle == handle }.takeIf { it != -1 } ?: 0
        val playingItemName =
            items.firstOrNull { it.nodeHandle == handle }?.nodeName ?: args.fileName

        val updatedItems = items.mapIndexed { index, item ->
            val newType = when {
                index == playingIndex -> MediaQueueItemType.Playing
                index < playingIndex -> MediaQueueItemType.Previous
                else -> MediaQueueItemType.Next
            }
            item.takeIf { it.type == newType } ?: item.copy(type = newType)
        }

        uiState.update {
            it.copy(
                items = updatedItems,
                currentPlayingHandle = handle,
                currentPlayingIndex = playingIndex,
                currentPlayingItemName = playingItemName
            )
        }
    }

    private fun applyPendingSavedPlaybackPositionSeek() {
        val position = uiState.value.playbackPosition ?: return
        if (position <= 0L) return
        mediaPlayerManager.playerSeekToPositionInMs(position)
        uiState.update { it.copy(playbackPosition = null) }
    }

    private fun ensurePlayingAfterPlaybackPositionHandling() {
        if (!mediaPlayerManager.getPlayWhenReady() && !isPausedByUser) {
            mediaPlayerManager.setPlayWhenReady(true)
        }
    }

    internal fun onPlaybackStateChanged(state: Int) {
        val playbackState = uiState.value.mediaPlaybackState
        when (state) {
            MEDIA_PLAYER_STATE_ENDED if playbackState == MediaPlaybackState.Playing -> {
                saveRecentlyUsedItemIfQualifies()
                updatePlaybackState(MediaPlaybackState.Paused)
            }

            MEDIA_PLAYER_STATE_READY -> {
                applyPendingSavedPlaybackPositionSeek()
                if (playbackState == MediaPlaybackState.Paused
                    && !mediaPlayerManager.getPlayWhenReady()
                    && !uiState.value.isAutoReplay
                    && !uiState.value.showSubTitlesOptions
                    && !isPausedByUser
                ) {
                    mediaPlayerManager.setPlayWhenReady(true)
                }
            }
        }
    }

    internal fun setRepeatToggleModeForPlayer(mode: RepeatToggleMode) = viewModelScope.launch {
        mediaPlayerManager.setRepeatToggleMode(mode)
        setVideoRepeatModeUseCase(mode.ordinal)
    }

    internal fun initRepeatToggleMode() {
        viewModelScope.launch {
            runCatching { monitorVideoRepeatModeUseCase().first() }.onSuccess { mode ->
                updateRepeatToggleMode(mode)
                mediaPlayerManager.setRepeatToggleMode(mode)
            }.onFailure { Timber.e(it) }
        }
    }

    internal fun updateRepeatToggleMode(mode: RepeatToggleMode) =
        uiState.update { it.copy(repeatToggleMode = mode) }

    internal fun saveVideoWatchedTime() {
        val watchedAt = Instant.now().toEpochMilli() / 1000
        mediaPlayerManager.getCurrentMediaItem()?.mediaId?.toLong()?.let { handle ->
            viewModelScope.launch {
                runCatching {
                    saveVideoRecentlyWatchedUseCase(
                        handle,
                        watchedAt,
                        args.collectionId ?: 0L,
                        args.collectionTitle
                    )
                }.onFailure { Timber.e(it, "Failed to save video recently watched") }
            }
        }
    }

    internal fun pauseForBackground() {
        val wasPlaying = mediaPlayerManager.getPlayWhenReady() && !isPausedByUser
        if (wasPlaying) {
            allowUpdatePausedByUser = false
            mediaPlayerManager.setPlayWhenReady(false)
        }
        uiState.update {
            it.copy(
                mediaPlaybackState = MediaPlaybackState.Paused,
                isAutoReplay = wasPlaying,
            )
        }
    }

    /**
     * Pauses playback for a non-user reason (e.g. system / app policy). Clears
     * [allowUpdatePausedByUser] around the gateway pause so ExoPlayer does not treat this stop as a
     * user request, keeping [shouldResumeOnAudioFocusGain] accurate for code that resumes after
     * transient interruptions. If the player was already not playing, only UI state is set to pause.
     */
    internal fun pausePlaybackNonUserInitiated() {
        if (mediaPlayerManager.getPlayWhenReady()) {
            allowUpdatePausedByUser = false
            mediaPlayerManager.setPlayWhenReady(false)
        }
        updatePlaybackState(MediaPlaybackState.Paused)
    }

    internal fun onPlayWhenReadyChanged(state: MediaPlaybackState, isPausedByUser: Boolean) {
        updatePlaybackState(state)
        if (allowUpdatePausedByUser) {
            when {
                isPausedByUser -> this.isPausedByUser = true
                state == MediaPlaybackState.Playing -> this.isPausedByUser = false
                // Paused for other reasons (audio focus, remote, etc.): keep user pause intent so
                // AUDIOFOCUS_GAIN does not resume after the user explicitly tapped pause.
            }
        }
        allowUpdatePausedByUser = true
    }

    /**
     * When `false`, the user explicitly paused and playback must not auto-resume on audio focus
     * gain until they press play.
     */
    internal fun shouldResumeOnAudioFocusGain(): Boolean = !isPausedByUser

    internal fun handleAutoReplayIfPaused() {
        val shouldAutoReplay = uiState.value.mediaPlaybackState == MediaPlaybackState.Paused &&
                uiState.value.isAutoReplay &&
                !uiState.value.showSubTitlesOptions &&
                !isPausedByUser

        if (shouldAutoReplay) {
            updatePlaybackStateWithReplay(true)
        }
    }

    internal fun updatePlaybackState(state: MediaPlaybackState) =
        uiState.update { it.copy(mediaPlaybackState = state) }

    internal fun onPlayerError(errorCode: Int) {
        playerRetry++
        Timber.d("playerRetry: $playerRetry, errorCode: $errorCode")
        val errorType = playerErrorTypeMapper(
            errorCode = errorCode,
            isConnected = uiState.value.isConnected,
        )
        if (playerRetry <= MAX_RETRY) {
            uiState.update { it.copy(retryEvent = triggered, playerErrorType = errorType) }
        } else {
            uiState.update { it.copy(retryFailedEvent = triggered, playerErrorType = errorType) }
        }
    }

    internal fun onRetryConsumed() = uiState.update { it.copy(retryEvent = consumed) }

    internal fun onRetryFailedConsumed() = uiState.update { it.copy(retryFailedEvent = consumed) }

    internal fun updateSnackBarMessage(message: String?) =
        uiState.update { it.copy(snackBarMessage = message) }

    internal suspend fun isNodeComesFromIncoming(): Boolean {
        val handle = uiState.value.currentPlayingHandle
        return runCatching {
            isNodeInRubbishBinUseCase(NodeId(handle)) &&
                    isNodeInCloudDriveUseCase(handle) &&
                    isNodeInBackupsNodeUseCase(handle)
        }.getOrDefault(false)
    }

    internal fun updateIsInPipMode(isInPipMode: Boolean) =
        uiState.update { it.copy(isInPipMode = isInPipMode) }

    internal fun updateIsMoreOptionShown(value: Boolean) {
        uiState.update { it.copy(isMoreOptionShown = value) }
    }

    internal fun updateCurrentSpeedPlaybackItem(item: SpeedPlaybackItem) {
        mediaPlayerManager.updatePlaybackSpeed(item)
        uiState.update { it.copy(currentSpeedPlayback = item) }
    }

    internal fun isMediaPlayerPlaying() =
        runCatching { mediaPlayerManager.mediaPlayerIsPlaying() }.getOrDefault(false)

    /**
     * Capture the screenshot when video playing.
     *
     * @param captureView the view that will be captured
     * @param successCallback invoked on [ioDispatcher] after the screenshot is saved successfully.
     * Do not perform UI work here (Compose state, View updates, Snackbar, etc.); use
     * `withContext(Dispatchers.Main.immediate)` (or equivalent) inside the callback when needed.
     */
    @SuppressLint("SimpleDateFormat")
    internal fun screenshotWhenVideoPlaying(
        rootPath: String,
        captureView: View,
        successCallback: suspend (bitmap: Bitmap) -> Unit,
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
            val screenshotBitmap = createBitmap(captureWidth, captureHeight)
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
        successCallback: suspend (bitmap: Bitmap) -> Unit,
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

    private suspend fun insertAndCompressBitmap(
        contentValues: ContentValues,
        bitmap: Bitmap,
        successCallback: suspend (bitmap: Bitmap) -> Unit,
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
                    } ?: return@let
                    successCallback(bitmap)
                } catch (e: Exception) {
                    Timber.e("Bitmap is saved error: ${e.message}")
                }
            }
    }

    internal fun onSnackbarMessage(): LiveData<Int> = snackbarMessage

    internal fun updatePlaybackStateWithReplay(value: Boolean) {
        if (mediaPlayerManager.getPlayWhenReady() && !isPausedByUser) {
            allowUpdatePausedByUser = false
        }
        mediaPlayerManager.setPlayWhenReady(value)
        uiState.update {
            it.copy(
                mediaPlaybackState = if (value) {
                    MediaPlaybackState.Playing
                } else {
                    MediaPlaybackState.Paused
                },
                isAutoReplay = !value
            )
        }
    }

    internal fun seekToByHandle(handle: Long, items: List<VideoPlayerItem> = uiState.value.items) {
        val index = items.indexOfFirst { it.nodeHandle == handle }
        if (index in items.indices) {
            mediaPlayerManager.playerSeekTo(index)
        }
    }

    internal fun swapItems(
        from: Int,
        to: Int,
        list: List<VideoPlayerItem> = uiState.value.items,
        mediaItems: List<MediaItem>? = uiState.value.mediaPlaySources?.mediaItems,
    ) {
        if (list.isEmpty() || mediaItems.isNullOrEmpty() || list.size != mediaItems.size) return
        viewModelScope.launch {
            val newItems = withContext(ioDispatcher) {
                mutex.withLock {
                    if (mediaItemsDuringChanged.isEmpty()) {
                        mediaItemsDuringChanged.addAll(mediaItems)
                    } else if (mediaItemsDuringChanged.size != list.size) {
                        mediaItemsDuringChanged.clear()
                        mediaItemsDuringChanged.addAll(mediaItems)
                    }

                    val items = list.toMutableList()
                    val indicesOfItems = list.indices
                    if (from in indicesOfItems && to in indicesOfItems &&
                        from in mediaItemsDuringChanged.indices && to in mediaItemsDuringChanged.indices
                    ) {
                        Collections.swap(items, from, to)
                        Collections.swap(mediaItemsDuringChanged, from, to)
                    }
                    items
                }
            }
            uiState.update { it.copy(items = newItems) }
        }
    }

    internal fun updateItemsAfterReorder() {
        viewModelScope.launch {
            val reorderedMediaItems = mutex.withLock {
                if (mediaItemsDuringChanged.isEmpty()) return@withLock null
                val items = mediaItemsDuringChanged.toList()
                mediaItemsDuringChanged.clear()
                items
            } ?: return@launch

            val index = uiState.value.currentPlayingIndex ?: 0
            val mediaPlaySources = MediaPlaySources(
                mediaItems = reorderedMediaItems,
                newIndexForCurrentItem = index,
                nameToDisplay = null
            )
            uiState.update { it.copy(mediaPlaySources = mediaPlaySources) }
            mediaPlayerManager.buildPlaySources(mediaPlaySources)
        }
    }

    internal fun getCurrentPlayingPosition() =
        mediaPlayerManager.getCurrentPlayingPosition().formatToString(durationInSecondsTextMapper)

    private fun Long.formatToString(durationInSecondsTextMapper: DurationInSecondsTextMapper) =
        durationInSecondsTextMapper(this.milliseconds)

    internal suspend fun isParticipatingInChatCall() = runCatching {
        isParticipatingInChatCallUseCase()
    }.getOrDefault(false)

    internal fun updateActionMode(actionMode: Boolean) =
        uiState.update { it.copy(isActionMode = actionMode) }

    internal fun searchWidgetStateUpdate() {
        val searchState = when (uiState.value.searchState) {
            SearchWidgetState.EXPANDED -> SearchWidgetState.COLLAPSED

            SearchWidgetState.COLLAPSED -> SearchWidgetState.EXPANDED
        }
        uiState.update { it.copy(searchState = searchState) }
        if (searchState == SearchWidgetState.EXPANDED) {
            searchQuery("")
        }
    }

    internal fun closeSearch() {
        searchQuery = ""
        uiState.update {
            it.copy(
                searchedItems = emptyList(),
                query = null,
                searchState = SearchWidgetState.COLLAPSED
            )
        }
    }

    internal fun searchQuery(queryString: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch(ioDispatcher) {
            searchQuery = queryString
            val items = uiState.value.items.filter { item ->
                item.nodeName.contains(searchQuery, true)
            }
            withContext(mainDispatcher) {
                uiState.update { it.copy(searchedItems = items, query = queryString) }
            }
        }
    }

    internal fun updateItemInSelectionState(
        index: Int,
        item: VideoPlayerItem,
        items: List<VideoPlayerItem> = uiState.value.items,
    ) {
        val isSelected = !item.isSelected
        val selectedHandles = uiState.value.selectedItemHandles.updateSelectedHandles(
            id = item.nodeHandle,
            isSelected = isSelected
        )
        val updateItems = items.updateItemSelectedState(index, isSelected)
        uiState.update {
            it.copy(
                items = updateItems,
                selectedItemHandles = selectedHandles
            )
        }
    }

    private fun List<Long>.updateSelectedHandles(
        id: Long,
        isSelected: Boolean,
    ) = toMutableList().also { handles ->
        if (isSelected) {
            handles.add(id)
        } else {
            handles.remove(id)
        }
    }

    private fun List<VideoPlayerItem>.updateItemSelectedState(
        index: Int,
        isSelected: Boolean,
    ) =
        if (index in indices) {
            toMutableList().also { list ->
                list[index] = list[index].copy(isSelected = isSelected)
            }
        } else this

    internal fun clearAllSelected(items: List<VideoPlayerItem> = uiState.value.items) {
        val updateItems = clearSelected(items)
        uiState.update {
            it.copy(
                items = updateItems,
                selectedItemHandles = emptyList()
            )
        }
    }

    private fun clearSelected(items: List<VideoPlayerItem>) =
        items.map { it.copy(isSelected = false) }

    internal fun removeSelectedItems(
        selectedHandles: List<Long> = uiState.value.selectedItemHandles,
        items: List<VideoPlayerItem> = uiState.value.items,
        mediaItems: List<MediaItem>? = uiState.value.mediaPlaySources?.mediaItems,
    ) {
        if (
            selectedHandles.isEmpty() ||
            items.isEmpty() ||
            mediaItems.isNullOrEmpty() ||
            items.size != mediaItems.size
        ) return

        val updatedItems = items.filterNot { it.nodeHandle in selectedHandles }
        val updatedMediaItems = mediaItems.filterNot { it.mediaId.toLong() in selectedHandles }
        val newPlayingIndex =
            updatedItems.indexOfFirst { it.nodeHandle == uiState.value.currentPlayingHandle }
                .takeIf { it != -1 } ?: 0

        val mediaPlaySources = MediaPlaySources(
            mediaItems = updatedMediaItems,
            newIndexForCurrentItem = newPlayingIndex,
            nameToDisplay = null
        )

        uiState.update {
            it.copy(
                items = updatedItems,
                currentPlayingIndex = newPlayingIndex,
                mediaPlaySources = mediaPlaySources,
                selectedItemHandles = emptyList(),
            )
        }

        mediaPlayerManager.buildPlaySources(mediaPlaySources)
    }

    internal fun updateFullscreen(value: Boolean) {
        Analytics.tracker.trackEvent(
            if (value) {
                VideoPlayerFullScreenPressedEvent
            } else {
                VideoPlayerOriginalPressedEvent
            }
        )
        uiState.update { it.copy(isFullscreen = value) }
    }

    internal fun updateLockStatus(value: Boolean) {
        Analytics.tracker.trackEvent(
            if (value) {
                LockButtonPressedEvent
            } else {
                UnlockButtonPressedEvent
            }
        )
        uiState.update { it.copy(isLocked = value) }
    }

    private fun checkPlaybackPositionBeforePlayback(handle: Long, noPlaybackPosition: () -> Unit) {
        playbackPositionJob?.cancel()
        playbackPositionJob = viewModelScope.launch {
            val currentItemName = uiState.value.currentPlayingItemName ?: args.fileName
            val playbackPosition =
                monitorPlaybackTimesUseCase().firstOrNull()?.get(handle)?.currentPosition

            if (playbackPosition != null && playbackPosition > 0) {
                uiState.update {
                    it.copy(
                        playbackPosition = playbackPosition,
                        currentPlayingItemName = currentItemName
                    )
                }
            }
            noPlaybackPosition()
        }
    }

    internal suspend fun getMatchedSubtitleFileInfo(): SubtitleFileInfo? =
        runCatching {
            getSRTSubtitleFileListUseCase().firstOrNull { subtitleFileInfo ->
                val subtitleName = subtitleFileInfo.name.substringBeforeLast(".")
                val currentPlayingItemName =
                    uiState.value.currentPlayingItemName ?: args.fileName
                val mediaItemName = currentPlayingItemName.substringBeforeLast(".")
                subtitleName == mediaItemName
            }
        }.onFailure {
            Timber.e(it)
        }.getOrNull()

    internal fun updateSubtitleSelectedStatus(
        status: SubtitleSelectedStatus,
        info: SubtitleFileInfo? = null,
    ) {
        when (status) {
            SubtitleSelectedStatus.Off -> onOffItemClicked()
            SubtitleSelectedStatus.AddSubtitleItem -> if (info == null) {
                onAddedSubtitleOptionClicked()
            } else {
                updateAddedSubtitleInfo(info)
            }

            SubtitleSelectedStatus.SelectMatchedItem -> onAutoMatchItemClicked(info)
        }
        if (!mediaPlayerManager.getPlayWhenReady())
            mediaPlayerManager.setPlayWhenReady(true)
    }

    private fun onOffItemClicked() {
        if (uiState.value.subtitleSelectedStatus != SubtitleSelectedStatus.Off) {
            Analytics.tracker.trackEvent(OffOptionForHideSubtitlePressedEvent)
            mediaPlayerManager.hideSubtitle()
        }

        uiState.update {
            it.copy(
                showSubTitlesOptions = false,
                subtitleSelectedStatus = SubtitleSelectedStatus.Off
            )
        }
    }

    private fun onAddedSubtitleOptionClicked() {
        mediaPlayerManager.showSubtitle()
        uiState.update {
            it.copy(
                showSubTitlesOptions = false,
                subtitleSelectedStatus = SubtitleSelectedStatus.AddSubtitleItem
            )
        }
    }

    private fun updateAddedSubtitleInfo(info: SubtitleFileInfo?) {
        info?.url?.let {
            addSubtitleAndUpdatePlaybackSources(it)
        }

        uiState.update {
            it.copy(
                showSubTitlesOptions = false,
                addedSubtitleInfo = info,
                matchedSubtitleInfo = null,
                subtitleSelectedStatus = if (info?.url == null)
                    SubtitleSelectedStatus.Off
                else
                    SubtitleSelectedStatus.AddSubtitleItem,
            )
        }
    }

    private fun addSubtitleAndUpdatePlaybackSources(url: String) {
        mediaPlayerManager.addSubtitle(url)
        updatePlaySourcesAfterSubtitleChange(uiState.value.currentPlayingIndex ?: 0)
    }

    private fun updatePlaySourcesAfterSubtitleChange(currentPlayingIndex: Int) {
        uiState.value.mediaPlaySources?.let { sources ->
            val newSources = MediaPlaySources(
                mediaItems = sources.mediaItems,
                newIndexForCurrentItem = currentPlayingIndex,
                nameToDisplay = null
            )
            mediaPlayerManager.buildPlaySources(newSources)
        }
    }

    private fun onAutoMatchItemClicked(info: SubtitleFileInfo?) {
        val matchedInfo = uiState.value.matchedSubtitleInfo
        val addedInfo = uiState.value.addedSubtitleInfo

        if (matchedInfo != null && addedInfo == null) {
            mediaPlayerManager.showSubtitle()
        } else {
            val url = matchedInfo?.url ?: info?.url
            if (url != null) addSubtitleAndUpdatePlaybackSources(url)
        }

        uiState.update {
            it.copy(
                showSubTitlesOptions = false,
                matchedSubtitleInfo = info,
                addedSubtitleInfo = null,
                subtitleSelectedStatus = if (info?.url == null) {
                    SubtitleSelectedStatus.Off
                } else {
                    SubtitleSelectedStatus.SelectMatchedItem
                }
            )
        }
    }

    internal fun navigateToSelectSubtitle() {
        uiState.update {
            it.copy(
                showSubTitlesOptions = false,
                navigateToSelectSubtitleScreen = true,
            )
        }
    }

    internal fun updateNavigateToSelectSubtitle(value: Boolean) {
        uiState.update { it.copy(navigateToSelectSubtitleScreen = value) }
    }

    /**
     * Toggle the in-place play queue overlay (Compose route has no separate queue destination).
     */
    internal fun updatePlayQueueVisibility(value: Boolean) {
        uiState.update { it.copy(isPlayQueueVisible = value) }
    }

    internal fun updateShowSubtitleDialog(value: Boolean) {
        if (value) {
            wasPlayingBeforeSubtitleDialog = mediaPlayerManager.getPlayWhenReady()
            mediaPlayerManager.setPlayWhenReady(false)
        } else {
            if (wasPlayingBeforeSubtitleDialog) {
                mediaPlayerManager.setPlayWhenReady(true)
            }
            wasPlayingBeforeSubtitleDialog = false
        }
        uiState.update { it.copy(showSubTitlesOptions = value) }
    }

    internal fun isShowSubtitleIcon() = args.adapterType != OFFLINE_ADAPTER

    internal fun clearSubtitleInfo(currentPlayingIndex: Int) {
        if (uiState.value.addedSubtitleInfo != null) {
            uiState.update {
                it.copy(
                    matchedSubtitleInfo = null,
                    addedSubtitleInfo = null,
                    subtitleSelectedStatus = SubtitleSelectedStatus.Off
                )
            }
            updatePlaySourcesAfterSubtitleChange(currentPlayingIndex)
        }
    }

    internal fun onBlockedErrorConsumed() = uiState.update { it.copy(blockedError = consumed) }

    internal fun onInvalidLaunchSourceConsumed() =
        uiState.update { it.copy(invalidLaunchSourceEvent = consumed) }

    companion object {
        private const val MEDIA_PLAYER_STATE_ENDED = 4
        private const val MEDIA_PLAYER_STATE_READY = 3

        private const val MAX_RETRY = 6

        private const val MEGA_SCREENSHOTS_FOLDER_NAME = "MEGA Screenshots/"
        private const val DCIM_FOLDER_NAME = "DCIM/"
        private const val MIME_TYPE_JPEG = "image/jpeg"
        private const val QUALITY_SCREENSHOT = 100
        private const val DATE_FORMAT_PATTERN = "yyyyMMdd-HHmmss"
        private const val SCREENSHOT_NAME_PREFIX = "Screenshot_"
        private const val SCREENSHOT_NAME_SUFFIX = ".jpg"
    }

    @AssistedFactory
    interface Factory {
        fun create(
            args: Args,
            initialLaunchSource: VideoPlayerLaunchSource?,
        ): ComposeVideoPlayerViewModel
    }

    data class Args(
        val fileLinkUrl: String?,
        val localFilePath: String?,
        val adapterType: Int,
        val handle: Long,
        val fileName: String,
        val collectionTitle: String?,
        val collectionId: Long?,
    )
}

