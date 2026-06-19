package mega.privacy.android.app.presentation.videoplayer.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.mediaplayer.model.MediaPlaySources
import mega.privacy.android.app.mediaplayer.model.SpeedPlaybackItem
import mega.privacy.android.app.mediaplayer.model.VideoSpeedPlaybackItem
import mega.privacy.android.app.mediaplayer.service.Metadata
import mega.privacy.android.app.presentation.node.model.MoveOrRemoveNodeResult
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent.DownloadTriggerEvent
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState

/**
 * The UI state for the video player feature.
 *
 * @property items the list of video player items
 * @property mediaPlaySources the media play sources
 * @property currentPlayingHandle the current playing handle
 * @property currentPlayingIndex the current playing index
 * @property metadata the metadata
 * @property playQueueTitle the play queue title
 * @property isRetry whether it is retry (legacy video player)
 * @property retryEvent event triggered when playback should be retried (new video player)
 * @property retryFailedEvent event triggered when all retries are exhausted or playback build fails (new video player)
 * @property repeatToggleMode the repeat toggle mode
 * @property currentPlayingVideoSize the current playing video size
 * @property mediaPlaybackState the playback state
 * @property snackBarMessage the snack bar message
 * @property isFullscreen whether it is full screen
 * @property isMoreOptionShown whether the video more option is shown
 * @property menuActions the list of video player menu actions
 * @property accountType the account type
 * @property isBusinessAccountExpired whether the business account is expired
 * @property hiddenNodeEnabled whether the hidden node is enabled
 * @property isHiddenNodesOnboarded whether the hidden nodes are onboarded
 * @property showHiddenItems whether to show hidden items
 * @property clickedMenuAction the clicked menu action
 * @property downloadEvent the download event
 * @property menuOptionClickedContent menu option click content
 * @property isAutoReplay whether is re-play the video automatically
 * @property selectedItemHandles the selected item handles
 * @property isActionMode whether the action mode is activated
 * @property searchState SearchWidgetState
 * @property query search query
 * @property searchedItems searched video player items
 * @property isLocked whether the video player is locked
 * @property isSpeedOptionsShown whether the playback speed options bottom sheet is shown
 * @property currentSpeedPlayback current SpeedPlaybackItem
 * @property showPlaybackDialog whether the playback dialog is shown
 * @property playbackPosition the playback position
 * @property currentPlayingItemName the current playing item name
 * @property showSubTitlesOptions whether the subtitle options are shown
 * @property subtitleSelectedStatus the subtitle selected status
 * @property matchedSubtitleInfo the matched subtitle info
 * @property addedSubtitleInfo the added subtitle info
 * @property navigateToSelectSubtitleScreen whether to navigate to select subtitle screen
 * @property blockedError the blocked error event
 * @property isClosedAfterHidingNode whether to close the video player after hiding node.
 * @property nodeSourceType the source type of the current playing node
 * @property fileLinkUrl the public file link URL; non-null only when [nodeSourceType] is
 *   [mega.privacy.android.domain.entity.node.NodeSourceType.FILE_LINK]. Used to fetch the node
 *   via GetPublicNodeUseCase when the video player displays bottom-sheet options.
 * @property localFilePath the absolute path to a local file; non-null only when [nodeSourceType] is
 *   [mega.privacy.android.domain.entity.node.NodeSourceType.VIDEO_PLAYER_ZIP_FILE]. Used to build
 *   a synthetic ZipFileTypedNode when displaying bottom-sheet options.
 * @property isConnected whether the device is connected to the internet
 * @property playerErrorType the type of player error, null if no error
 * @property moveOrRemoveNodeEvent one-shot event emitted while moving or removing the
 *   current playing node, used to drive confirmation dialogs and snack bars from the activity.
 * @property isPipEnabled whether the Picture in Picture feature is enabled via feature flag
 * @property isInPipMode whether the video player is currently displayed in PIP mode
 * @property isFromLink whether the video was opened from a public link (file link, folder link, or album sharing)
 * @property isLoggedIn whether the user is currently logged in; used together with [isFromLink] to determine if session validation is required
 * @property isAlbumSharingLink whether the video was opened from an album sharing link specifically (as opposed to a folder link)
 */
data class VideoPlayerUiState(
    val items: List<VideoPlayerItem> = emptyList(),
    val mediaPlaySources: MediaPlaySources? = null,
    val currentPlayingHandle: Long = -1,
    val currentPlayingIndex: Int? = null,
    val metadata: Metadata = Metadata(null, null, null, ""),
    val playQueueTitle: String? = null,
    val isRetry: Boolean? = null,
    val retryEvent: StateEvent = consumed,
    val retryFailedEvent: StateEvent = consumed,
    val repeatToggleMode: RepeatToggleMode = RepeatToggleMode.REPEAT_NONE,
    val currentPlayingVideoSize: VideoSize? = null,
    val mediaPlaybackState: MediaPlaybackState = MediaPlaybackState.Playing,
    val snackBarMessage: String? = null,
    val isFullscreen: Boolean = false,
    val isMoreOptionShown: Boolean = false,
    val menuActions: List<VideoPlayerMenuAction> = emptyList(),
    val accountType: AccountType? = null,
    val isBusinessAccountExpired: Boolean = false,
    val hiddenNodeEnabled: Boolean = false,
    val isHiddenNodesOnboarded: Boolean = false,
    val showHiddenItems: Boolean? = null,
    val clickedMenuAction: VideoPlayerMenuAction? = null,
    val downloadEvent: StateEventWithContent<DownloadTriggerEvent> = consumed(),
    val menuOptionClickedContent: MenuOptionClickedContent? = null,
    val isAutoReplay: Boolean = false,
    val selectedItemHandles: List<Long> = emptyList(),
    val isActionMode: Boolean = false,
    val searchState: SearchWidgetState = SearchWidgetState.COLLAPSED,
    val query: String? = null,
    val searchedItems: List<VideoPlayerItem> = emptyList(),
    val isLocked: Boolean = false,
    val isSpeedOptionsShown: Boolean = false,
    val currentSpeedPlayback: SpeedPlaybackItem = VideoSpeedPlaybackItem.PlaybackSpeed_1X,
    val showPlaybackDialog: Boolean = false,
    val playbackPosition: Long? = null,
    val currentPlayingItemName: String? = null,
    val showSubTitlesOptions: Boolean = false,
    val subtitleSelectedStatus: SubtitleSelectedStatus = SubtitleSelectedStatus.Off,
    val matchedSubtitleInfo: SubtitleFileInfo? = null,
    val addedSubtitleInfo: SubtitleFileInfo? = null,
    val navigateToSelectSubtitleScreen: Boolean = false,
    val blockedError: StateEvent = consumed,
    val isClosedAfterHidingNode: Boolean = false,
    val nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
    val fileLinkUrl: String? = null,
    val localFilePath: String? = null,
    val isConnected: Boolean = true,
    val playerErrorType: PlayerErrorType? = null,
    val moveOrRemoveNodeEvent: StateEventWithContent<MoveOrRemoveNodeResult> = consumed(),
    val isPipEnabled: Boolean = false,
    val isInPipMode: Boolean = false,
    val isFromLink: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isAlbumSharingLink: Boolean = false,
    val serializedData: String? = null,
)
