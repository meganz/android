package mega.privacy.android.app.mediaplayer

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
import android.view.SurfaceView
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.di.mediaplayer.VideoPlayer
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.mapper.PlaylistItemMapper
import mega.privacy.android.app.mediaplayer.model.MediaPlaySources
import mega.privacy.android.app.mediaplayer.model.PlaybackPositionState
import mega.privacy.android.app.mediaplayer.model.SpeedPlaybackItem
import mega.privacy.android.app.mediaplayer.model.SubtitleDisplayState
import mega.privacy.android.app.mediaplayer.model.VideoControllerPadding
import mega.privacy.android.app.mediaplayer.model.VideoPlayerUiState
import mega.privacy.android.app.mediaplayer.playlist.PlaylistAdapter.Companion.TYPE_NEXT
import mega.privacy.android.app.mediaplayer.playlist.PlaylistAdapter.Companion.TYPE_PLAYING
import mega.privacy.android.app.mediaplayer.playlist.PlaylistAdapter.Companion.TYPE_PREVIOUS
import mega.privacy.android.app.mediaplayer.playlist.PlaylistItem
import mega.privacy.android.app.mediaplayer.playlist.finalizeItem
import mega.privacy.android.app.mediaplayer.playlist.updateNodeName
import mega.privacy.android.app.mediaplayer.service.Metadata
import mega.privacy.android.app.presentation.extensions.getStateFlow
import mega.privacy.android.app.presentation.extensions.parcelableArrayList
import mega.privacy.android.app.search.callback.SearchCallback
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.BACKUPS_ADAPTER
import mega.privacy.android.app.utils.Constants.CONTACT_FILE_ADAPTER
import mega.privacy.android.app.utils.Constants.FILE_BROWSER_ADAPTER
import mega.privacy.android.app.utils.Constants.FILE_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FROM_ALBUM_SHARING
import mega.privacy.android.app.utils.Constants.FROM_CHAT
import mega.privacy.android.app.utils.Constants.FROM_IMAGE_VIEWER
import mega.privacy.android.app.utils.Constants.FROM_MEDIA_DISCOVERY
import mega.privacy.android.app.utils.Constants.INCOMING_SHARES_ADAPTER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ARRAY_OFFLINE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FROM_DOWNLOAD_SERVICE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IS_PLAYLIST
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ORDER_GET_CHILDREN
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.app.utils.Constants.INVALID_SIZE
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.LINKS_ADAPTER
import mega.privacy.android.app.utils.Constants.NODE_HANDLES
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.OUTGOING_SHARES_ADAPTER
import mega.privacy.android.app.utils.Constants.PHOTO_SYNC_ADAPTER
import mega.privacy.android.app.utils.Constants.RECENTS_ADAPTER
import mega.privacy.android.app.utils.Constants.RECENTS_BUCKET_ADAPTER
import mega.privacy.android.app.utils.Constants.RUBBISH_BIN_ADAPTER
import mega.privacy.android.app.utils.Constants.SEARCH_BY_ADAPTER
import mega.privacy.android.app.utils.Constants.VIDEO_BROWSE_ADAPTER
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.OfflineUtils.getOfflineFile
import mega.privacy.android.app.utils.ThumbnailUtils
import mega.privacy.android.app.utils.wrapper.GetOfflineThumbnailFileWrapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.mediaplayer.PlaybackInformation
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.statistics.MediaPlayerStatisticsEvents
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.exception.BlockedMegaException
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.AreCredentialsNullUseCase
import mega.privacy.android.domain.usecase.GetBackupsNodeUseCase
import mega.privacy.android.domain.usecase.GetLocalFilePathUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.GetLocalLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.GetParentNodeFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.GetRootNodeFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.GetRubbishNodeUseCase
import mega.privacy.android.domain.usecase.GetUserNameByEmailUseCase
import mega.privacy.android.domain.usecase.MonitorPlaybackTimesUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerStartUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerStopUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStopUseCase
import mega.privacy.android.domain.usecase.mediaplayer.SendStatisticsMediaPlayerUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.DeletePlaybackInformationUseCase
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
import mega.privacy.android.domain.usecase.setting.MonitorSubFolderMediaDiscoverySettingsUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.mobile.analytics.event.OffOptionForHideSubtitlePressedEvent
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaCancelToken
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * ViewModel for video player.
 *
 * @property ioDispatcher CoroutineDispatcher
 * @property sendStatisticsMediaPlayerUseCase SendStatisticsMediaPlayerUseCase
 */
@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @VideoPlayer private val mediaPlayerGateway: MediaPlayerGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val sendStatisticsMediaPlayerUseCase: SendStatisticsMediaPlayerUseCase,
    private val offlineThumbnailFileWrapper: GetOfflineThumbnailFileWrapper,
    private val monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
    private val playlistItemMapper: PlaylistItemMapper,
    private val trackPlaybackPositionUseCase: TrackPlaybackPositionUseCase,
    private val monitorPlaybackTimesUseCase: MonitorPlaybackTimesUseCase,
    private val savePlaybackTimesUseCase: SavePlaybackTimesUseCase,
    private val deletePlaybackInformationUseCase: DeletePlaybackInformationUseCase,
    private val megaApiFolderHttpServerIsRunningUseCase: MegaApiFolderHttpServerIsRunningUseCase,
    private val megaApiFolderHttpServerStartUseCase: MegaApiFolderHttpServerStartUseCase,
    private val megaApiFolderHttpServerStopUseCase: MegaApiFolderHttpServerStopUseCase,
    private val megaApiHttpServerIsRunningUseCase: MegaApiHttpServerIsRunningUseCase,
    private val megaApiHttpServerStartUseCase: MegaApiHttpServerStartUseCase,
    private val megaApiHttpServerStop: MegaApiHttpServerStopUseCase,
    private val areCredentialsNullUseCase: AreCredentialsNullUseCase,
    private val getLocalFilePathUseCase: GetLocalFilePathUseCase,
    private val getLocalFolderLinkFromMegaApiFolderUseCase: GetLocalFolderLinkFromMegaApiFolderUseCase,
    private val getLocalFolderLinkFromMegaApiUseCase: GetLocalFolderLinkFromMegaApiUseCase,
    private val getLocalLinkFromMegaApiUseCase: GetLocalLinkFromMegaApiUseCase,
    private val getBackupsNodeUseCase: GetBackupsNodeUseCase,
    private val getParentNodeFromMegaApiFolderUseCase: GetParentNodeFromMegaApiFolderUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val getRootNodeFromMegaApiFolderUseCase: GetRootNodeFromMegaApiFolderUseCase,
    private val getRubbishNodeUseCase: GetRubbishNodeUseCase,
    private val getVideoNodeByHandleUseCase: GetVideoNodeByHandleUseCase,
    private val getVideoNodesFromPublicLinksUseCase: GetVideoNodesFromPublicLinksUseCase,
    private val getVideoNodesFromInSharesUseCase: GetVideoNodesFromInSharesUseCase,
    private val getVideoNodesFromOutSharesUseCase: GetVideoNodesFromOutSharesUseCase,
    private val getVideoNodesUseCase: GetVideoNodesUseCase,
    private val getVideoNodesByEmailUseCase: GetVideoNodesByEmailUseCase,
    private val getUserNameByEmailUseCase: GetUserNameByEmailUseCase,
    private val getVideosByParentHandleFromMegaApiFolderUseCase: GetVideosByParentHandleFromMegaApiFolderUseCase,
    private val getVideoNodesByParentHandleUseCase: GetVideoNodesByParentHandleUseCase,
    private val getVideoNodesByHandlesUseCase: GetVideoNodesByHandlesUseCase,
    private val getFingerprintUseCase: GetFingerprintUseCase,
    private val getSRTSubtitleFileListUseCase: GetSRTSubtitleFileListUseCase,
    private val setVideoRepeatModeUseCase: SetVideoRepeatModeUseCase,
    private val getVideosBySearchTypeUseCase: GetVideosBySearchTypeUseCase,
    private val monitorSubFolderMediaDiscoverySettingsUseCase: MonitorSubFolderMediaDiscoverySettingsUseCase,
    monitorVideoRepeatModeUseCase: MonitorVideoRepeatModeUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), SearchCallback.Data {

    private val compositeDisposable = CompositeDisposable()

    private var currentIntent: Intent? = null

    private var currentMediaPlayerMediaId: String? = null

    private val _state = MutableStateFlow(VideoPlayerUiState())
    internal val uiState = _state.asStateFlow()

    /**
     * SelectState for updating the background color of add subtitle dialog options
     */
    internal var selectOptionState by mutableIntStateOf(SUBTITLE_SELECTED_STATE_OFF)
        private set

    private val _screenLockState = MutableStateFlow(false)
    internal val screenLockState: StateFlow<Boolean> = _screenLockState

    private val _playerSourcesState =
        MutableStateFlow(MediaPlaySources(emptyList(), INVALID_VALUE, null))
    internal val playerSourcesState: StateFlow<MediaPlaySources> = _playerSourcesState

    private val _mediaItemToRemoveState = MutableStateFlow<Int?>(null)
    internal val mediaItemToRemoveState: StateFlow<Int?> = _mediaItemToRemoveState

    private val _playlistItemsState =
        MutableStateFlow<Pair<List<PlaylistItem>, Int>>(Pair(emptyList(), 0))
    internal val playlistItemsState: StateFlow<Pair<List<PlaylistItem>, Int>> = _playlistItemsState

    private val _playlistTitleState = MutableStateFlow<String?>(null)
    internal val playlistTitleState: StateFlow<String?> = _playlistTitleState

    private val _retryState = MutableStateFlow<Boolean?>(null)
    internal val retryState: StateFlow<Boolean?> = _retryState

    private val _errorState = MutableStateFlow<MegaException?>(null)
    internal val errorState: StateFlow<MegaException?> = _errorState

    private val _itemsClearedState = MutableStateFlow<Boolean?>(null)
    internal val itemsClearedState: StateFlow<Boolean?> = _itemsClearedState

    private val _actionModeState = MutableStateFlow(false)
    internal val actionModeState: StateFlow<Boolean> = _actionModeState

    private val _itemsSelectedCountState = MutableStateFlow(0)
    internal val itemsSelectedCountState: StateFlow<Int> = _itemsSelectedCountState

    private val _mediaPlaybackState = MutableStateFlow(false)
    internal val mediaPlaybackState: StateFlow<Boolean> = _mediaPlaybackState

    private val _showPlaybackPositionDialogState = MutableStateFlow(PlaybackPositionState())
    internal val showPlaybackPositionDialogState: StateFlow<PlaybackPositionState> =
        _showPlaybackPositionDialogState

    private val _playerControllerPaddingState = MutableStateFlow(VideoControllerPadding())
    internal val playerControllerPaddingState: StateFlow<VideoControllerPadding> =
        _playerControllerPaddingState

    internal var videoPlayType = VIDEO_TYPE_SHOW_PLAYBACK_POSITION_DIALOG
        private set

    private var isSearchMode = false

    private val _metadataState = MutableStateFlow(Metadata(null, null, null, ""))
    internal val metadataState: StateFlow<Metadata> = _metadataState

    private val playlistItems = mutableListOf<PlaylistItem>()
    private val itemsSelectedMap = mutableMapOf<Long, PlaylistItem>()
    private var playlistSearchQuery: String? = null
    private var playingHandle = INVALID_HANDLE
    private var playerRetry = 0
    private var needStopStreamingServer = false
    private var playSourceChanged: MutableList<MediaItem> = mutableListOf()
    private var playlistItemsChanged: MutableList<PlaylistItem> = mutableListOf()
    private var playingPosition = 0

    // The value for confirming the video whether revert to be played when VideoPlayerFragment is created.
    private var isPlayingReverted = false

    private var cancelToken: MegaCancelToken? = null

    /**
     * The subtitle file info by add subtitles
     */
    internal var subtitleInfoByAddSubtitles: SubtitleFileInfo? = null
        private set

    private val _addSubtitleState = MutableStateFlow(false)

    internal val subtitleDialogShowKey = "SUBTITLE_DIALOG_SHOW"
    internal val subtitleShowKey = "SUBTITLE_SHOW"
    internal val videoPlayerPausedForPlaylistKey = "VIDEO_PLAYER_PAUSED_FOR_PLAYLIST"
    internal val currentSubtitleFileInfoKey = "CURRENT_SUBTITLE_FILE_INFO"

    private var currentPlayingVideoSize: Pair<Int, Int>? = null

    private val _isSubtitleDialogShown = savedStateHandle.getStateFlow(
        viewModelScope,
        subtitleDialogShowKey,
        false
    )

    private val _isSubtitleShown = savedStateHandle.getStateFlow(
        viewModelScope,
        subtitleShowKey,
        false
    )

    private val _currentSubtitleFileInfo: MutableStateFlow<SubtitleFileInfo?> =
        savedStateHandle.getStateFlow(
            viewModelScope,
            currentSubtitleFileInfoKey,
            null
        )

    private val _videoRepeatToggleMode = monitorVideoRepeatModeUseCase().stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        RepeatToggleMode.REPEAT_NONE
    )

    init {
        viewModelScope.launch {
            combine(
                _isSubtitleShown,
                _isSubtitleDialogShown,
                _addSubtitleState,
                _currentSubtitleFileInfo,
                _videoRepeatToggleMode,
                ::mapToVideoPlayerUIState
            ).collectLatest { newState ->
                _state.update {
                    newState
                }
            }
        }
        setupTransferListener()
    }

    internal fun setPlayingReverted(value: Boolean) {
        isPlayingReverted = value
    }

    internal fun setSearchMode(value: Boolean) {
        isSearchMode = value
    }

    internal fun isPlayingReverted() = isPlayingReverted

    internal fun setCurrentPlayingVideoSize(videoSize: Pair<Int, Int>?) {
        Timber.d("screenshotWhenVideoPlaying videoSize ${videoSize?.first} : ${videoSize?.second}")
        currentPlayingVideoSize = videoSize
    }

    internal fun updatePlayerControllerPaddingState(left: Int, right: Int, bottom: Int) =
        _playerControllerPaddingState.update {
            it.copy(paddingLeft = left, paddingRight = right, paddingBottom = bottom)
        }

    internal fun updateIsFullScreen(isFullScreen: Boolean) =
        _state.update { it.copy(isFullScreen = isFullScreen) }

    internal fun updateIsSpeedPopupShown(isShown: Boolean) =
        _state.update { it.copy(isSpeedPopupShown = isShown) }

    internal fun updateIsVideoOptionPopupShown(isShown: Boolean) =
        _state.update { it.copy(isVideoOptionPopupShown = isShown) }

    internal fun updateCurrentSpeedPlaybackItem(item: SpeedPlaybackItem) =
        _state.update { it.copy(currentSpeedPlayback = item) }

    internal fun updateShowPlaybackPositionDialogState(newState: PlaybackPositionState) =
        _showPlaybackPositionDialogState.update { newState }

    internal fun updateMetadataState(newState: Metadata) = _metadataState.update { newState }

    internal fun setVideoPlayType(type: Int) {
        videoPlayType = type
    }

    /**
     * Update the lock status
     *
     * @param isLock true is screen is locked, otherwise is screen is not locked
     */
    internal fun updateLockStatus(isLock: Boolean) = _screenLockState.update { isLock }


    /**
     * Update media playback state
     * @param newState the media playback state, true is paused, otherwise is false
     */
    internal fun updateMediaPlaybackState(newState: Boolean) {
        _mediaPlaybackState.update { newState }
    }

    private fun mapToVideoPlayerUIState(
        subtitleShown: Boolean,
        subtitleDialogShown: Boolean,
        isAddSubtitle: Boolean,
        subtitleFileInfo: SubtitleFileInfo?,
        videoRepeatToggleMode: RepeatToggleMode,
    ) = VideoPlayerUiState(
        subtitleDisplayState = SubtitleDisplayState(
            isSubtitleShown = subtitleShown,
            isSubtitleDialogShown = subtitleDialogShown,
            isAddSubtitle = isAddSubtitle,
            subtitleFileInfo = subtitleFileInfo
        ),
        videoRepeatToggleMode = videoRepeatToggleMode
    )

    /**
     * Update the subtitle file info by added subtitles
     *
     * @param subtitleFileInfo [SubtitleFileInfo]
     */
    private fun updateSubtitleInfoByAddSubtitles(subtitleFileInfo: SubtitleFileInfo?) {
        subtitleInfoByAddSubtitles = subtitleFileInfo
        subtitleFileInfo?.let { info ->
            if (_currentSubtitleFileInfo.value?.id != info.id) {
                _currentSubtitleFileInfo.update { info }
                _addSubtitleState.update { true }
            } else {
                _addSubtitleState.update { false }
            }
        }
    }

    /**
     * Update the current media player media id
     *
     * @param mediaId media item id
     */
    internal fun updateCurrentMediaId(mediaId: String?) {
        if (currentMediaPlayerMediaId != mediaId) {
            if (currentMediaPlayerMediaId != null) {
                _isSubtitleShown.update { false }
                subtitleInfoByAddSubtitles = null
            }
            currentMediaPlayerMediaId = mediaId
        }
    }

    /**
     * Update isAddSubtitle state
     */
    internal fun updateAddSubtitleState() =
        _addSubtitleState.update {
            _state.value.subtitleDisplayState.let { subtitleState ->
                subtitleState.isAddSubtitle && subtitleState.subtitleFileInfo == null
            }
        }

    /**
     * Update current subtitle file info
     *
     * @param subtitleFileInfo [SubtitleFileInfo]
     */
    private fun updateCurrentSubtitleFileInfo(subtitleFileInfo: SubtitleFileInfo) {
        if (subtitleFileInfo.id != _currentSubtitleFileInfo.value?.id) {
            _currentSubtitleFileInfo.update { subtitleFileInfo }
            currentMediaPlayerMediaId = subtitleFileInfo.id.toString()
            _addSubtitleState.update { true }
        } else {
            _addSubtitleState.update { false }
        }
    }

    /**
     * The function is for showing the add subtitle dialog
     */
    internal fun showAddSubtitleDialog() {
        _isSubtitleShown.update { true }
        _addSubtitleState.update { false }
        _isSubtitleDialogShown.update { true }
        sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.SubtitleDialogShownEvent())
    }

    /**
     * The function is for the added subtitle option is clicked
     */
    internal fun onAddedSubtitleOptionClicked() {
        _isSubtitleShown.update { true }
        _isSubtitleDialogShown.update { false }
        selectOptionState = SUBTITLE_SELECTED_STATE_ADD_SUBTITLE_ITEM
    }

    /**
     * The function is for the adding subtitle file
     *
     * @param info the added subtitle file info
     * @param isReset true is reset data and set the state to off, otherwise keep previous data and state
     */
    internal fun onAddSubtitleFile(info: SubtitleFileInfo?, isReset: Boolean = false) {
        info?.let {
            it.url?.let {
                _isSubtitleShown.update { true }
                updateSubtitleInfoByAddSubtitles(info)
                selectOptionState = SUBTITLE_SELECTED_STATE_ADD_SUBTITLE_ITEM
            } ?: Timber.d("The subtitle file url is null")
        } ?: let {
            if (isReset) {
                selectOptionState = SUBTITLE_SELECTED_STATE_OFF
                updateSubtitleInfoByAddSubtitles(null)
            }
            _addSubtitleState.update { false }
            _isSubtitleShown.update {
                selectOptionState != SUBTITLE_SELECTED_STATE_OFF
            }
        }
        _isSubtitleDialogShown.update { false }
    }

    /**
     * The function is for the off item is clicked
     */
    internal fun onOffItemClicked() {
        // Only when the subtitle file has been loaded and shown, send hide subtitle event if off item is clicked
        if (_currentSubtitleFileInfo.value != null && selectOptionState != SUBTITLE_SELECTED_STATE_OFF) {
            sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.HideSubtitleEvent())
            Analytics.tracker.trackEvent(OffOptionForHideSubtitlePressedEvent)
        }
        _addSubtitleState.update { false }
        _isSubtitleShown.update { false }
        _isSubtitleDialogShown.update { false }
        selectOptionState = SUBTITLE_SELECTED_STATE_OFF
    }

    /**
     * The function is for the dialog dismiss request
     */
    internal fun onDismissRequest() {
        _addSubtitleState.update { false }
        _isSubtitleShown.update {
            selectOptionState != SUBTITLE_SELECTED_STATE_OFF
        }
        _isSubtitleDialogShown.update { false }
    }

    /**
     * The function is for the auto matched item is clicked
     *
     * @param info matched subtitle file info
     */
    internal fun onAutoMatchItemClicked(info: SubtitleFileInfo) {
        info.url?.let {
            updateCurrentSubtitleFileInfo(info)
            _isSubtitleShown.update { true }
            updateSubtitleInfoByAddSubtitles(null)
            selectOptionState = SUBTITLE_SELECTED_STATE_MATCHED_ITEM
        } ?: Timber.d("The subtitle file url is null")
        _isSubtitleDialogShown.update { false }
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
        captureView: View,
        successCallback: (bitmap: Bitmap) -> Unit,
    ) {
        val screenshotFileName =
            SimpleDateFormat(DATE_FORMAT_PATTERN).format(Date(System.currentTimeMillis()))
        val fileName = "${SCREENSHOT_NAME_PREFIX}$screenshotFileName${SCREENSHOT_NAME_SUFFIX}"
        // Using video size for the capture size to ensure the screenshot is complete.
        val (captureWidth, captureHeight) = currentPlayingVideoSize?.let { (width, height) ->
            width to height
        } ?: (captureView.width to captureView.height)
        try {
            val screenshotBitmap = Bitmap.createBitmap(
                captureWidth,
                captureHeight,
                Bitmap.Config.ARGB_8888
            )
            PixelCopy.request(
                captureView as SurfaceView,
                Rect(0, 0, captureWidth, captureHeight),
                screenshotBitmap,
                { copyResult ->
                    if (copyResult == PixelCopy.SUCCESS) {
                        viewModelScope.launch {
                            saveBitmap(fileName, screenshotBitmap, successCallback)
                        }
                    }
                },
                Handler(Looper.getMainLooper())
            )
        } catch (e: Exception) {
            Timber.e("Capture screenshot error: ${e.message}")
        }
    }

    /**
     * Build player source from start intent.
     *
     * @param intent intent received from onStartCommand
     * @return if there is no error
     */
    private suspend fun buildPlayerSource(intent: Intent?): Boolean {
        if (intent == null || !intent.getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true)) {
            Timber.d(
                "buildPlayerSource error: " +
                        if (intent == null) {
                            "intent is null"
                        } else {
                            "rebuild playlist is false"
                        }
            )
            _retryState.update { false }
            return false
        }

        val type = intent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)
        val uri = intent.data

        if (type == INVALID_VALUE || uri == null) {
            Timber.d(
                "buildPlayerSource error: " +
                        if (type == INVALID_VALUE) {
                            "type = $type"
                        } else {
                            "type = $type uri is null"
                        }
            )
            _retryState.update { false }
            return false
        }

        val samePlaylist = isSamePlaylist(type, intent)
        currentIntent = intent

        val firstPlayHandle = intent.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)
        if (firstPlayHandle == INVALID_HANDLE) {
            Timber.d("buildPlayerSource error: firstPlayHandle = $firstPlayHandle")
            _retryState.update { false }
            return false
        }

        val firstPlayNodeName = intent.getStringExtra(INTENT_EXTRA_KEY_FILE_NAME)
        if (firstPlayNodeName == null) {
            Timber.d("buildPlayerSource error: firstPlayNodeName is null")
            _retryState.update { false }
            return false
        }

        // Because the same instance will be used if user creates another audio playlist,
        // so if we need stop streaming server in previous creation, we still need
        // stop it even if the new creation indicates we don't need to stop it,
        // otherwise the streaming server won't be stopped at the end.
        needStopStreamingServer = needStopStreamingServer || intent.getBooleanExtra(
            INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, false
        )

        playerRetry = 0

        // if we are already playing this music, then the metadata is already
        // in LiveData (_metadata of AudioPlayerService), we don't need (and shouldn't)
        // emit node name.
        val displayNodeNameFirst = !(samePlaylist && firstPlayHandle == playingHandle)


        val firstPlayUri = if (type == FOLDER_LINK_ADAPTER) {
            if (isMegaApiFolder(type)) {
                getLocalFolderLinkFromMegaApiFolderUseCase(firstPlayHandle)
            } else {
                getLocalFolderLinkFromMegaApiUseCase(firstPlayHandle)
            }?.let { url ->
                Uri.parse(url)
            }
        } else {
            uri
        }

        if (firstPlayUri == null) {
            Timber.d("buildPlayerSource error: firstPlayUri is null")
            _retryState.update { false }
            return false
        }

        val mediaItem = MediaItem.Builder()
            .setUri(firstPlayUri)
            .setMediaId(firstPlayHandle.toString())
            .build()
        MediaPlaySources(
            listOf(mediaItem),
            // we will emit a single item list at first, and the current playing item
            // will always be at index 0 in that single item list.
            if (samePlaylist && firstPlayHandle == playingHandle) 0 else INVALID_VALUE,
            if (displayNodeNameFirst) firstPlayNodeName else null
        ).let { playSource ->
            playSource(playSource)
            _playerSourcesState.value = playSource
        }

        if (intent.getBooleanExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, true)) {
            if (type != OFFLINE_ADAPTER && type != ZIP_ADAPTER) {
                needStopStreamingServer =
                    needStopStreamingServer || setupStreamingServer(type)
            }
            viewModelScope.launch(ioDispatcher) {
                when (type) {
                    OFFLINE_ADAPTER -> {
                        _playlistTitleState.update {
                            OfflineUtils.getOfflineFolderName(
                                context,
                                firstPlayHandle
                            )
                        }
                        buildPlaylistFromOfflineNodes(intent, firstPlayHandle)
                    }

                    VIDEO_BROWSE_ADAPTER -> {
                        _playlistTitleState.update {
                            context.getString(R.string.sortby_type_video_first)
                        }
                        buildPlaySourcesByTypedVideoNodes(
                            type = type,
                            typedNodes = getVideoNodesUseCase(getSortOrderFromIntent(intent)),
                            firstPlayHandle = firstPlayHandle
                        )
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
                    -> {
                        val parentHandle =
                            intent.getLongExtra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, INVALID_HANDLE)
                        val order = getSortOrderFromIntent(intent)

                        if (MegaNodeUtil.isInRootLinksLevel(type, parentHandle)) {
                            _playlistTitleState.update {
                                context.getString(R.string.tab_links_shares)
                            }
                            buildPlaySourcesByTypedVideoNodes(
                                type = type,
                                typedNodes = getVideoNodesFromPublicLinksUseCase(order),
                                firstPlayHandle = firstPlayHandle
                            )
                            return@launch
                        }

                        if (type == INCOMING_SHARES_ADAPTER && parentHandle == INVALID_HANDLE) {
                            _playlistTitleState.update {
                                context.getString(R.string.tab_incoming_shares)
                            }
                            buildPlaySourcesByTypedVideoNodes(
                                type = type,
                                typedNodes = getVideoNodesFromInSharesUseCase(order),
                                firstPlayHandle = firstPlayHandle
                            )
                            return@launch
                        }

                        if (type == OUTGOING_SHARES_ADAPTER && parentHandle == INVALID_HANDLE) {
                            _playlistTitleState.update {
                                context.getString(R.string.tab_outgoing_shares)
                            }
                            buildPlaySourcesByTypedVideoNodes(
                                type = type,
                                typedNodes = getVideoNodesFromOutSharesUseCase(
                                    lastHandle = INVALID_HANDLE,
                                    order = order
                                ),
                                firstPlayHandle = firstPlayHandle
                            )
                            return@launch
                        }

                        if (type == CONTACT_FILE_ADAPTER && parentHandle == INVALID_HANDLE) {
                            intent.getStringExtra(Constants.INTENT_EXTRA_KEY_CONTACT_EMAIL)
                                ?.let { email ->
                                    getVideoNodesByEmailUseCase(email)?.let { nodes ->
                                        getUserNameByEmailUseCase(email)?.let {
                                            context.getString(R.string.title_incoming_shares_with_explorer)
                                                .let { sharesTitle ->
                                                    _playlistTitleState.update { "$sharesTitle $it" }
                                                }
                                        }
                                        buildPlaySourcesByTypedVideoNodes(
                                            type = type,
                                            typedNodes = nodes,
                                            firstPlayHandle = firstPlayHandle
                                        )
                                    }
                                }
                            return@launch
                        }

                        if (parentHandle == INVALID_HANDLE) {
                            when (type) {
                                RUBBISH_BIN_ADAPTER -> getRubbishNodeUseCase()
                                BACKUPS_ADAPTER -> getBackupsNodeUseCase()
                                else -> getRootNodeUseCase()
                            }
                        } else {
                            getVideoNodeByHandleUseCase(parentHandle)
                        }?.let { parent ->
                            if (parentHandle == INVALID_HANDLE) {
                                context.getString(
                                    when (type) {
                                        RUBBISH_BIN_ADAPTER -> R.string.section_rubbish_bin
                                        BACKUPS_ADAPTER -> R.string.home_side_menu_backups_title
                                        else -> R.string.section_cloud_drive
                                    }
                                )
                            } else {
                                parent.name
                            }.let { title ->
                                _playlistTitleState.update { title }
                            }

                            if (type == FROM_MEDIA_DISCOVERY) {
                                getVideosBySearchTypeUseCase(
                                    handle = parentHandle,
                                    recursive = monitorSubFolderMediaDiscoverySettingsUseCase().first(),
                                    order = order
                                )
                            } else {
                                getVideoNodesByParentHandleUseCase(
                                    parentHandle = parent.id.longValue,
                                    order = getSortOrderFromIntent(intent)
                                )
                            }?.let { children ->
                                buildPlaySourcesByTypedVideoNodes(
                                    type = type,
                                    typedNodes = children,
                                    firstPlayHandle = firstPlayHandle
                                )
                            }
                        }
                    }

                    RECENTS_ADAPTER, RECENTS_BUCKET_ADAPTER -> {
                        _playlistTitleState.update {
                            context.getString(R.string.section_recents)
                        }
                        intent.getLongArrayExtra(NODE_HANDLES)?.let { handles ->
                            buildPlaylistFromHandles(
                                type = type,
                                handles = handles.toList(),
                                firstPlayHandle = firstPlayHandle
                            )
                        }
                    }

                    FOLDER_LINK_ADAPTER -> {
                        val parentHandle = intent.getLongExtra(
                            INTENT_EXTRA_KEY_PARENT_NODE_HANDLE,
                            INVALID_HANDLE
                        )
                        val order = getSortOrderFromIntent(intent)

                        (if (parentHandle == INVALID_HANDLE) {
                            getRootNodeFromMegaApiFolderUseCase()
                        } else {
                            getParentNodeFromMegaApiFolderUseCase(parentHandle)
                        })?.let { parent ->
                            _playlistTitleState.update { parent.name }

                            getVideosByParentHandleFromMegaApiFolderUseCase(
                                parentHandle = parent.id.longValue,
                                order = order
                            )?.let { children ->
                                buildPlaySourcesByTypedVideoNodes(
                                    type = type,
                                    typedNodes = children,
                                    firstPlayHandle = firstPlayHandle
                                )
                            }
                        }
                    }

                    ZIP_ADAPTER -> {
                        intent.getStringExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY)
                            ?.let { zipPath ->
                                _playlistTitleState.update {
                                    File(zipPath).parentFile?.name ?: ""
                                }
                                File(zipPath).parentFile?.listFiles()?.let { files ->
                                    buildPlaySourcesByFiles(
                                        files = files.asList(),
                                        firstPlayHandle = firstPlayHandle
                                    )
                                }
                            }
                    }

                    SEARCH_BY_ADAPTER -> {
                        intent.getLongArrayExtra(INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH)
                            ?.let { handles ->
                                buildPlaylistFromHandles(
                                    type = type,
                                    handles = handles.toList(),
                                    firstPlayHandle = firstPlayHandle
                                )
                            }
                    }
                }
            }
        } else {
            playlistItems.clear()

            val node = getVideoNodeByHandleUseCase(firstPlayHandle)
            val thumbnail = when {
                type == OFFLINE_ADAPTER -> {
                    offlineThumbnailFileWrapper.getThumbnailFile(
                        context,
                        firstPlayHandle.toString()
                    )
                }

                node == null -> {
                    null
                }

                else -> {
                    File(
                        ThumbnailUtils.getThumbFolder(context),
                        node.base64Id.plus(FileUtil.JPG_EXTENSION)
                    )
                }
            }

            val duration = node?.duration ?: 0

            playlistItemMapper(
                firstPlayHandle,
                firstPlayNodeName,
                thumbnail,
                0,
                TYPE_PLAYING,
                node?.size ?: INVALID_SIZE,
                duration,
            ).let { playlistItem ->
                playlistItems.add(playlistItem)
            }

            recreateAndUpdatePlaylistItems()
        }

        return true
    }

    /**
     * Build play sources by node OfflineNodes
     *
     * @param intent Intent
     * @param firstPlayHandle the index of first playing item
     */
    private fun buildPlaylistFromOfflineNodes(
        intent: Intent,
        firstPlayHandle: Long,
    ) {
        intent.parcelableArrayList<MegaOffline>(INTENT_EXTRA_KEY_ARRAY_OFFLINE)
            ?.let { offlineFiles ->
                playlistItems.clear()

                val mediaItems = mutableListOf<MediaItem>()
                var firstPlayIndex = 0

                offlineFiles.filter {
                    getOfflineFile(context, it).let { file ->
                        FileUtil.isFileAvailable(file) && file.isFile && filterByNodeName(it.name)
                    }
                }.mapIndexed { currentIndex, megaOffline ->
                    mediaItems.add(
                        mediaItemFromFile(
                            getOfflineFile(context, megaOffline),
                            megaOffline.handle
                        )
                    )
                    if (megaOffline.handle.toLong() == firstPlayHandle) {
                        firstPlayIndex = currentIndex
                    }

                    playlistItemMapper(
                        megaOffline.handle.toLong(),
                        megaOffline.name,
                        offlineThumbnailFileWrapper.getThumbnailFile(context, megaOffline),
                        currentIndex,
                        TYPE_NEXT,
                        megaOffline.getSize(context),
                        0
                    )
                        .let { playlistItem ->
                            playlistItems.add(playlistItem)
                        }
                }

                updatePlaySources(mediaItems, playlistItems, firstPlayIndex)
            }
    }

    /**
     * Build play sources by node TypedNodes
     *
     * @param type adapter type
     * @param typedNodes [TypedVideoNode] list
     * @param firstPlayHandle the index of first playing item
     */
    private suspend fun buildPlaySourcesByTypedVideoNodes(
        type: Int,
        typedNodes: List<TypedVideoNode>,
        firstPlayHandle: Long,
    ) {
        playlistItems.clear()

        val mediaItems = ArrayList<MediaItem>()
        var firstPlayIndex = 0

        typedNodes.mapIndexed { currentIndex, typedNode ->
            getLocalFilePathUseCase(typedNode).let { localPath ->
                if (localPath != null && isLocalFile(typedNode, localPath)) {
                    mediaItemFromFile(File(localPath), typedNode.id.longValue.toString())
                } else {
                    val url =
                        if (type == FOLDER_LINK_ADAPTER) {
                            if (isMegaApiFolder(type)) {
                                getLocalFolderLinkFromMegaApiFolderUseCase(typedNode.id.longValue)
                            } else {
                                getLocalFolderLinkFromMegaApiUseCase(typedNode.id.longValue)
                            }
                        } else {
                            getLocalLinkFromMegaApiUseCase(typedNode.id.longValue)
                        }
                    if (url == null) {
                        null
                    } else {
                        MediaItem.Builder()
                            .setUri(Uri.parse(url))
                            .setMediaId(typedNode.id.longValue.toString())
                            .build()
                    }
                }?.let {
                    mediaItems.add(it)
                }
            }

            if (typedNode.id.longValue == firstPlayHandle) {
                firstPlayIndex = currentIndex
            }
            val thumbnail = typedNode.thumbnailPath?.let { path ->
                File(path)
            }

            val duration = typedNode.duration

            playlistItemMapper(
                typedNode.id.longValue,
                typedNode.name,
                thumbnail,
                currentIndex,
                TYPE_NEXT,
                typedNode.size,
                duration,
            ).let { playlistItem ->
                playlistItems.add(playlistItem)
            }
        }
        updatePlaySources(mediaItems, playlistItems, firstPlayIndex)
    }

    /**
     * Build play sources by node handles
     *
     * @param type adapter type
     * @param handles node handles
     * @param firstPlayHandle the index of first playing item
     */
    private suspend fun buildPlaylistFromHandles(
        type: Int,
        handles: List<Long>,
        firstPlayHandle: Long,
    ) {
        buildPlaySourcesByTypedVideoNodes(
            type = type,
            typedNodes = getVideoNodesByHandlesUseCase(handles),
            firstPlayHandle = firstPlayHandle
        )
    }

    /**
     * Build play sources by files
     *
     * @param files media files
     * @param firstPlayHandle the index of first playing item
     */
    private fun buildPlaySourcesByFiles(
        files: List<File>,
        firstPlayHandle: Long,
    ) {
        playlistItems.clear()

        val mediaItems = ArrayList<MediaItem>()
        var firstPlayIndex = 0

        files.filter {
            it.isFile && filterByNodeName(it.name)
        }.mapIndexed { currentIndex, file ->
            mediaItems.add(mediaItemFromFile(file, file.name.hashCode().toString()))

            if (file.name.hashCode().toLong() == firstPlayHandle) {
                firstPlayIndex = currentIndex
            }

            playlistItemMapper(
                file.name.hashCode().toLong(),
                file.name,
                null,
                currentIndex,
                TYPE_NEXT,
                file.length(),
                0
            )
                .let { playlistItem ->
                    playlistItems.add(playlistItem)
                }
        }
        updatePlaySources(mediaItems, playlistItems, firstPlayIndex)
    }

    /**
     * Update play sources for media player and playlist
     *
     * @param mediaItems media items
     * @param items playlist items
     * @param firstPlayIndex the index of first playing item
     */
    private fun updatePlaySources(
        mediaItems: List<MediaItem>,
        items: List<PlaylistItem>,
        firstPlayIndex: Int,
    ) {
        // If the playlist items are not more than 1, don't need to update play sources.
        if (mediaItems.size > 1 && items.size > 1) {
            _playerSourcesState.update {
                MediaPlaySources(mediaItems, firstPlayIndex, null)
            }
            recreateAndUpdatePlaylistItems(originalItems = items)
        }
    }

    internal fun initVideoSources(intent: Intent?) {
        viewModelScope.launch {
            buildPlayerSource(intent)
            trackPlayback {
                PlaybackInformation(
                    mediaPlayerGateway.getCurrentMediaItem()?.mediaId?.toLong(),
                    mediaPlayerGateway.getCurrentItemDuration(),
                    mediaPlayerGateway.getCurrentPlayingPosition()
                )
            }
        }
    }

    internal fun playSource(mediaPlaySources: MediaPlaySources) {
        Timber.d("playSource ${mediaPlaySources.mediaItems.size} items")

        mediaPlaySources.nameToDisplay?.let { name ->
            _metadataState.update {
                Metadata(title = null, artist = null, album = null, nodeName = name)
            }
        }

        mediaPlayerGateway.buildPlaySources(mediaPlaySources)

        if (_mediaPlaybackState.value) {
            mediaPlayerGateway.setPlayWhenReady(true)
        }

        mediaPlayerGateway.playerPrepare()
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
                        if (transfer.nodeHandle == playingHandle
                            && ((error is QuotaExceededMegaException
                                    && !transfer.isForeignOverQuota
                                    && error.value != 0L)
                                    || error is BlockedMegaException)
                        ) {
                            _errorState.update { error }
                        }
                    }
                }
        }

    /**
     * Handle player error.
     */
    internal fun onPlayerError() {
        playerRetry++
        Timber.d("playerRetry: $playerRetry")
        _retryState.update { playerRetry <= MAX_RETRY }
    }

    /**
     * Check if the new intent would create the same playlist as current one.
     *
     * @param type new adapter type
     * @param intent new intent
     * @return if the new intent would create the same playlist as current one
     */
    private fun isSamePlaylist(type: Int, intent: Intent): Boolean {
        val oldIntent = currentIntent ?: return false
        val oldType = oldIntent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)

        if (
            intent.getBooleanExtra(INTENT_EXTRA_KEY_FROM_DOWNLOAD_SERVICE, false)
            && oldIntent.getBooleanExtra(INTENT_EXTRA_KEY_FROM_DOWNLOAD_SERVICE, false)
        ) {
            return true
        }

        when (type) {
            OFFLINE_ADAPTER -> {
                val oldDir =
                    oldIntent.getStringExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY)
                        ?: return false
                val newDir =
                    intent.getStringExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY)
                        ?: return false
                return oldDir == newDir
            }

            VIDEO_BROWSE_ADAPTER,
            FROM_CHAT,
            FILE_LINK_ADAPTER,
            PHOTO_SYNC_ADAPTER,
            -> {
                return oldType == type
            }

            FILE_BROWSER_ADAPTER,
            RUBBISH_BIN_ADAPTER,
            BACKUPS_ADAPTER,
            LINKS_ADAPTER,
            INCOMING_SHARES_ADAPTER,
            OUTGOING_SHARES_ADAPTER,
            CONTACT_FILE_ADAPTER,
            FOLDER_LINK_ADAPTER,
            -> {
                val oldParentHandle =
                    oldIntent.getLongExtra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, INVALID_HANDLE)
                val newParentHandle =
                    intent.getLongExtra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, INVALID_HANDLE)
                return oldType == type && oldParentHandle == newParentHandle
            }

            RECENTS_ADAPTER, RECENTS_BUCKET_ADAPTER -> {
                val oldHandles = oldIntent.getLongArrayExtra(NODE_HANDLES) ?: return false
                val newHandles = intent.getLongArrayExtra(NODE_HANDLES) ?: return false
                return oldHandles.contentEquals(newHandles)
            }

            ZIP_ADAPTER -> {
                val oldZipPath = oldIntent.getStringExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY)
                    ?: return false
                val newZipPath =
                    intent.getStringExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY) ?: return false
                return oldZipPath == newZipPath
            }

            SEARCH_BY_ADAPTER -> {
                val oldHandles = oldIntent.getLongArrayExtra(INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH)
                    ?: return false
                val newHandles =
                    intent.getLongArrayExtra(INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH) ?: return false
                return oldHandles.contentEquals(newHandles)
            }

            else -> {
                return false
            }
        }
    }

    private fun filterByNodeName(name: String): Boolean =
        MimeTypeList.typeForName(name).let { mime ->
            mime.isVideo && mime.isVideoMimeType && !mime.isVideoNotSupported
        }

    private fun getSortOrderFromIntent(intent: Intent): SortOrder {
        val order =
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
        return order
    }

    override fun initNewSearch(): MegaCancelToken {
        cancelSearch()
        return MegaCancelToken.createInstance()
    }

    override fun cancelSearch() {
        cancelToken?.cancel()
    }

    private fun mediaItemFromFile(file: File, handle: String): MediaItem =
        MediaItem.Builder()
            .setUri(FileUtil.getUriForFile(context, file))
            .setMediaId(handle)
            .build()

    /**
     * Recreate and update playlist items
     *
     * @param originalItems playlist items
     * @param isScroll true is scroll to target position, otherwise is false.
     */
    private fun recreateAndUpdatePlaylistItems(
        originalItems: List<PlaylistItem?> = playlistItems,
        isScroll: Boolean = true,
    ) {
        viewModelScope.launch(ioDispatcher) {
            Timber.d("recreateAndUpdatePlaylistItems ${originalItems.size} items")
            if (originalItems.isEmpty()) {
                return@launch
            }
            val items = originalItems.filterNotNull()
            playingPosition = items.indexOfFirst { (nodeHandle) ->
                nodeHandle == playingHandle
            }.takeIf { index ->
                index in originalItems.indices
            } ?: 0

            val searchQuery = playlistSearchQuery
            val recreatedItems = items.toMutableList()

            if (isSearchMode && !searchQuery.isNullOrEmpty()) {
                filterPlaylistItems(recreatedItems, searchQuery)
            } else {
                for ((index, item) in recreatedItems.withIndex()) {
                    val type = when {
                        index < playingPosition -> TYPE_PREVIOUS
                        playingPosition == index -> TYPE_PLAYING
                        else -> TYPE_NEXT
                    }
                    recreatedItems[index] =
                        item.finalizeItem(
                            index = index,
                            type = type,
                            isSelected = item.isSelected,
                            duration = item.duration,
                        )
                }
                if (playingPosition > 0) {
                    recreatedItems[0] = recreatedItems[0].copy(headerIsVisible = true)
                }
                recreatedItems[playingPosition] =
                    recreatedItems[playingPosition].copy(headerIsVisible = true)

                recreatedItems
            }.let { updatedList ->
                Timber.d("recreateAndUpdatePlaylistItems post ${updatedList.size} items")
                val scrollPosition = if (isScroll) {
                    playingPosition
                } else {
                    -1
                }
                _playlistItemsState.update {
                    it.copy(updatedList, scrollPosition)
                }
            }
        }
    }

    private fun filterPlaylistItems(
        items: List<PlaylistItem>,
        filter: String,
    ): MutableList<PlaylistItem> {
        val filteredItems = ArrayList<PlaylistItem>()
        items.forEachIndexed { index, item ->
            if (item.nodeName.contains(filter, true)) {
                // Filter only affects displayed playlist, it doesn't affect what
                // ExoPlayer is playing, so we still need use the index before filter.
                filteredItems.add(item.finalizeItem(index, TYPE_PREVIOUS))
            }
        }
        return filteredItems.toMutableList()
    }

    /**
     * Set new text for playlist search query
     *
     * @param newText the new text string
     */
    internal fun searchQueryUpdate(newText: String?) {
        playlistSearchQuery = newText
        recreateAndUpdatePlaylistItems()
    }

    /**
     * Get the handle of the current playing item
     *
     * @return the handle of the current playing item
     */
    internal fun getCurrentPlayingHandle() = playingHandle

    /**
     *  Set the handle of the current playing item
     *
     *  @param handle MegaNode handle
     */
    internal fun setCurrentPlayingHandle(handle: Long) {
        playingHandle = handle
        _playlistItemsState.value.first.let { playlistItems ->
            playingPosition = playlistItems.indexOfFirst { (nodeHandle) ->
                nodeHandle == handle
            }.takeIf { index -> index in playlistItems.indices } ?: 0
            recreateAndUpdatePlaylistItems(
                originalItems = _playlistItemsState.value.first
            )
        }
    }

    /**
     * Get playlist item
     *
     * @param handle MegaNode handle
     * @return PlaylistItem
     */
    internal fun getPlaylistItem(handle: String?): PlaylistItem? =
        handle?.let {
            playlistItems.toList().firstOrNull { (nodeHandle) ->
                nodeHandle == handle.toLong()
            }
        }

    /**
     * Remove item
     *
     * @param handle the handle that is removed
     */
    internal fun removeItem(handle: Long) {
        initPlayerSourceChanged()
        val newItems = removeSingleItem(handle)
        if (newItems.isNotEmpty()) {
            resetRetryState()
            recreateAndUpdatePlaylistItems(originalItems = newItems)
        } else {
            _playlistItemsState.update {
                it.copy(emptyList(), 0)
            }
            _itemsClearedState.update { true }
        }
    }

    private fun removeSingleItem(handle: Long): List<PlaylistItem> =
        _playlistItemsState.value.first.let { items ->
            val newItems = items.toMutableList()
            items.indexOfFirst { (nodeHandle) ->
                nodeHandle == handle
            }.takeIf { index ->
                index in playlistItems.indices
            }?.let { index ->
                _mediaItemToRemoveState.update { index }
                newItems.removeIf { (nodeHandle) ->
                    nodeHandle == handle
                }
                playlistItems.removeIf { (nodeHandle) ->
                    nodeHandle == handle
                }
                playSourceChanged.removeIf { mediaItem ->
                    mediaItem.mediaId.toLong() == handle
                }
            }
            newItems
        }

    /**
     * Remove the selected items
     */
    internal fun removeAllSelectedItems() {
        if (itemsSelectedMap.isNotEmpty()) {
            itemsSelectedMap.forEach {
                removeSingleItem(it.value.nodeHandle).let { newItems ->
                    _playlistItemsState.update { flow ->
                        flow.copy(newItems, playingPosition)
                    }
                }
            }
            itemsSelectedMap.clear()
            _itemsSelectedCountState.update { itemsSelectedMap.size }
            _actionModeState.update { false }
        }
    }

    /**
     * Saved or remove the selected items
     * @param handle node handle of selected item
     */
    internal fun itemSelected(handle: Long) {
        _playlistItemsState.update {
            it.copy(
                it.first.toMutableList().let { playlistItems ->
                    playlistItems.indexOfFirst { (nodeHandle) ->
                        nodeHandle == handle
                    }.takeIf { index ->
                        index in playlistItems.indices
                    }?.let { selectedIndex ->
                        playlistItems[selectedIndex].let { item ->
                            val isSelected = !item.isSelected
                            playlistItems[selectedIndex] = item.copy(isSelected = isSelected)
                            if (playlistItems[selectedIndex].isSelected) {
                                itemsSelectedMap[handle] = item
                            } else {
                                itemsSelectedMap.remove(handle)
                            }
                            _itemsSelectedCountState.update { itemsSelectedMap.size }
                        }
                    }
                    playlistItems
                }
            )
        }
    }

    /**
     * Clear the all selections
     */
    internal fun clearSelections() {
        _playlistItemsState.update {
            it.copy(
                it.first.toMutableList().let { playlistItems ->
                    playlistItems.map { item ->
                        item.copy(isSelected = false)
                    }
                }
            )
        }
        itemsSelectedMap.clear()
        _actionModeState.update { false }

    }

    /**
     * Set the action mode
     * @param isActionMode whether the action mode is activated
     */
    internal fun setActionMode(isActionMode: Boolean) {
        _actionModeState.update { isActionMode }
        if (isActionMode) {
            recreateAndUpdatePlaylistItems(
                originalItems = _playlistItemsState.value.first,
                isScroll = false
            )
        }
    }

    /**
     * Reset retry state
     */
    internal fun resetRetryState() {
        playerRetry = 0
        _retryState.update { true }
    }

    /**
     * Track the playback information
     *
     * @param getCurrentPlaybackInformation get current playback information
     */
    private suspend fun trackPlayback(getCurrentPlaybackInformation: () -> PlaybackInformation) {
        trackPlaybackPositionUseCase(getCurrentPlaybackInformation)
    }

    /**
     * Monitor playback times
     *
     * @param mediaId the media id of target media item
     * @param seekToPosition the callback for seek to playback position history. If the current item contains the playback history,
     * then invoke the callback and the playback position history is parameter
     */
    internal fun monitorPlaybackTimes(
        mediaId: Long?,
        seekToPosition: (positionInMs: Long?) -> Unit,
    ) = viewModelScope.launch {
        seekToPosition(
            monitorPlaybackTimesUseCase().firstOrNull()
                ?.get(mediaId)?.currentPosition
        )
    }

    /**
     * Save the playback times
     */
    internal fun savePlaybackTimes() = viewModelScope.launch {
        savePlaybackTimesUseCase()
    }

    /**
     * Delete playback information
     *
     * @param mediaId the media id of deleted item
     */
    internal fun deletePlaybackInformation(mediaId: Long) = viewModelScope.launch {
        deletePlaybackInformationUseCase(mediaId)
    }

    /**
     * Update item name
     *
     * @param handle MegaNode handle
     * @param newName the new name string
     */
    internal fun updateItemName(handle: Long, newName: String) =
        _playlistItemsState.update {
            it.copy(
                it.first.map { item ->
                    if (item.nodeHandle == handle) {
                        _metadataState.update { metadata ->
                            metadata.copy(nodeName = newName)
                        }
                        item.updateNodeName(newName)
                    } else {
                        item
                    }
                }
            )
        }

    /**
     * Get playlist items
     *
     * @return List<PlaylistItem>
     */
    internal fun getPlaylistItems() = _playlistItemsState.value.first

    /**
     * Set repeat mode for video
     *
     * @param repeatToggleMode RepeatToggleMode
     */
    internal fun setVideoRepeatMode(repeatToggleMode: RepeatToggleMode) =
        viewModelScope.launch {
            _state.update {
                it.copy(videoRepeatToggleMode = repeatToggleMode)
            }
            setVideoRepeatModeUseCase(repeatToggleMode.ordinal)
        }

    /**
     * Clear the state and flying task of this class, should be called in onDestroy.
     */
    private fun clear() {
        viewModelScope.launch {
            compositeDisposable.dispose()

            if (needStopStreamingServer) {
                megaApiHttpServerStop()
                megaApiFolderHttpServerStopUseCase()
            }
        }
    }

    private suspend fun isMegaApiFolder(type: Int) =
        type == FOLDER_LINK_ADAPTER && areCredentialsNullUseCase()

    /**
     * Swap the items
     * @param current the position of from item
     * @param target the position of to item
     */
    internal fun swapItems(current: Int, target: Int) {
        if (playlistItemsChanged.isEmpty()) {
            playlistItemsChanged.addAll(_playlistItemsState.value.first)
        }
        Collections.swap(playlistItemsChanged, current, target)
        val index = playlistItemsChanged[current].index
        playlistItemsChanged[current] =
            playlistItemsChanged[current].copy(index = playlistItemsChanged[target].index)
        playlistItemsChanged[target] = playlistItemsChanged[target].copy(index = index)

        initPlayerSourceChanged()
        // Swap the items of play source
        Collections.swap(playSourceChanged, current, target)
    }

    /**
     * Get the index from playlistItems to keep the play order is correct after reordered
     * @param item clicked item
     * @return the index of clicked item in playlistItems or null
     */
    internal fun getIndexFromPlaylistItems(item: PlaylistItem): Int? =
        /* The media items of ExoPlayer are still the original order even the shuffleEnable is true,
         so the index of media item should be got from original playlist items */
        playlistItems.indexOfFirst {
            it.nodeHandle == item.nodeHandle
        }.takeIf { index ->
            index in playlistItems.indices
        }

    /**
     * Updated the play source of exoplayer after reordered.
     */
    internal fun updatePlaySource() {
        _playlistItemsState.update {
            it.copy(playlistItemsChanged.toList())
        }
        _playerSourcesState.update {
            it.copy(
                mediaItems = playSourceChanged.toList(),
                newIndexForCurrentItem = playingPosition
            )
        }
        playSourceChanged.clear()
        playlistItemsChanged.clear()
    }

    /**
     * Get the position of playing item
     *
     * @return the position of playing item
     */
    internal fun getPlayingPosition(): Int = playingPosition

    /**
     * Scroll to the position of playing item
     */
    internal fun scrollToPlayingPosition() =
        recreateAndUpdatePlaylistItems(
            originalItems = _playlistItemsState.value.first
        )

    /**
     * Get the subtitle file info that is same name as playing media item
     *
     * @return SubtitleFileInfo
     */
    internal suspend fun getMatchedSubtitleFileInfoForPlayingItem(): SubtitleFileInfo? =
        getSRTSubtitleFileListUseCase().firstOrNull { subtitleFileInfo ->
            val subtitleName = subtitleFileInfo.name.let { name ->
                name.substring(0, name.lastIndexOf("."))
            }
            val mediaItemName =
                playlistItems.elementAtOrNull(playingPosition)?.nodeName?.let { name ->
                    name.substring(0, name.lastIndexOf("."))
                }
            subtitleName == mediaItemName
        }

    private fun initPlayerSourceChanged() {
        if (playSourceChanged.isEmpty()) {
            // Get the play source
            playSourceChanged.addAll(_playerSourcesState.value.mediaItems)
        }
    }

    private suspend fun setupStreamingServer(type: Int): Boolean {
        if (isMegaApiFolder(type)) {
            if (megaApiFolderHttpServerIsRunningUseCase() != 0) {
                return false
            }
            megaApiFolderHttpServerStartUseCase()
        } else {
            if (megaApiHttpServerIsRunningUseCase() != 0) {
                return false
            }
            megaApiHttpServerStartUseCase()
        }

        return true
    }

    private suspend fun isLocalFile(node: TypedFileNode, localPath: String?): Boolean =
        node.fingerprint.let { fingerprint ->
            localPath != null &&
                    (isOnMegaDownloads(node) || (fingerprint != null
                            && fingerprint == getFingerprintUseCase(localPath)))
        }

    private fun isOnMegaDownloads(node: TypedFileNode): Boolean =
        File(FileUtil.getDownloadLocation(), node.name).let { file ->
            FileUtil.isFileAvailable(file) && file.length() == node.size
        }

    private suspend fun saveBitmap(
        fileName: String,
        bitmap: Bitmap,
        successCallback: (bitmap: Bitmap) -> Unit,
    ) =
        withContext(ioDispatcher) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, MEGA_SCREENSHOTS_FOLDER_NAME)
            }

            val contentResolver = context.contentResolver
            contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?.let { uri ->
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        try {
                            bitmap.compress(
                                Bitmap.CompressFormat.JPEG,
                                QUALITY_SCREENSHOT,
                                outputStream
                            )
                            successCallback(bitmap)
                        } catch (e: Exception) {
                            Timber.e("Bitmap is saved error: ${e.message}")
                        }
                    }
                }
        }

    /**
     * Format milliseconds to time string
     * @param milliseconds time value that unit is milliseconds
     * @return strings of time
     */
    fun formatMillisecondsToString(milliseconds: Long): String {
        val totalSeconds = (milliseconds / 1000).toInt()
        return formatSecondsToString(seconds = totalSeconds)
    }

    /**
     * Format seconds to time string
     * @param seconds time value that unit is seconds
     * @return strings of time
     */
    private fun formatSecondsToString(seconds: Int): String {
        val hour = TimeUnit.SECONDS.toHours(seconds.toLong())
        val minutes =
            TimeUnit.SECONDS.toMinutes(seconds.toLong()) - TimeUnit.HOURS.toMinutes(hour)
        val resultSeconds =
            seconds.toLong() - TimeUnit.MINUTES.toSeconds(
                TimeUnit.SECONDS.toMinutes(
                    seconds.toLong()
                )
            )

        return if (hour >= 1) {
            String.format("%2d:%02d:%02d", hour, minutes, resultSeconds)
        } else {
            String.format("%02d:%02d", minutes, resultSeconds)
        }
    }

    /**
     * Send OpenSelectSubtitlePageEvent
     */
    fun sendOpenSelectSubtitlePageEvent() =
        sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.OpenSelectSubtitlePageEvent())

    /**
     * Send LoopButtonEnabledEvent
     */
    fun sendLoopButtonEnabledEvent() =
        sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.LoopButtonEnabledEvent())

    /**
     * Send ScreenLockedEvent
     */
    fun sendScreenLockedEvent() =
        sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.ScreenLockedEvent())

    /**
     * Send ScreenUnlockedEvent
     */
    fun sendScreenUnlockedEvent() =
        sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.ScreenUnlockedEvent())

    /**
     * Send SnapshotButtonClickedEvent
     */
    fun sendSnapshotButtonClickedEvent() =
        sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.SnapshotButtonClickedEvent())

    /**
     * Send InfoButtonClickedEvent
     */
    fun sendInfoButtonClickedEvent() =
        sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.InfoButtonClickedEvent())

    /**
     * Send SaveToDeviceButtonClickedEvent
     */
    fun sendSaveToDeviceButtonClickedEvent() =
        sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.SaveToDeviceButtonClickedEvent())

    /**
     * Send SendToChatButtonClickedEvent
     */
    fun sendSendToChatButtonClickedEvent() =
        sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.SendToChatButtonClickedEvent())

    /**
     * Send ShareButtonClickedEvent
     */
    fun sendShareButtonClickedEvent() =
        sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.ShareButtonClickedEvent())

    /**
     * Send GetLinkButtonClickedEvent
     */
    fun sendGetLinkButtonClickedEvent() =
        sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.GetLinkButtonClickedEvent())

    /**
     * Send RemoveLinkButtonClickedEvent
     */
    fun sendRemoveLinkButtonClickedEvent() =
        sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.RemoveLinkButtonClickedEvent())

    /**
     * Send VideoPlayerActivatedEvent
     */
    fun sendVideoPlayerActivatedEvent() =
        sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.VideoPlayerActivatedEvent())

    /**
     * Send AutoMatchSubtitleClickedEvent
     */
    fun sendAutoMatchSubtitleClickedEvent() =
        sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.AutoMatchSubtitleClickedEvent())

    /**
     * Send MediaPlayerStatisticsEvent
     *
     * @param event MediaPlayerStatisticsEvents
     */
    private fun sendMediaPlayerStatisticsEvent(event: MediaPlayerStatisticsEvents) {
        viewModelScope.launch {
            sendStatisticsMediaPlayerUseCase(event)
        }
    }

    /**
     * onCleared
     */
    override fun onCleared() {
        super.onCleared()
        cancelSearch()
        clear()
    }

    companion object {
        private const val MEGA_SCREENSHOTS_FOLDER_NAME = "DCIM/MEGA Screenshots/"
        private const val QUALITY_SCREENSHOT = 100
        private const val DATE_FORMAT_PATTERN = "yyyyMMdd-HHmmss"
        private const val SCREENSHOT_NAME_PREFIX = "Screenshot_"
        private const val SCREENSHOT_NAME_SUFFIX = ".jpg"

        /**
         * The state for the off is selected
         */
        const val SUBTITLE_SELECTED_STATE_OFF = 900

        /**
         * The state for the matched item is selected
         */
        const val SUBTITLE_SELECTED_STATE_MATCHED_ITEM = 901

        /**
         * The state for the add subtitle item is selected
         */
        const val SUBTITLE_SELECTED_STATE_ADD_SUBTITLE_ITEM = 902

        private const val MAX_RETRY = 6

        internal const val VIDEO_TYPE_RESUME_PLAYBACK_POSITION = 123
        internal const val VIDEO_TYPE_RESTART_PLAYBACK_POSITION = 124
        internal const val VIDEO_TYPE_SHOW_PLAYBACK_POSITION_DIALOG = 125
    }
}