package mega.privacy.android.app.presentation.videoplayer.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.mediaplayer.model.MediaPlaySources
import mega.privacy.android.app.mediaplayer.service.Metadata
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent.DownloadTriggerEvent
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.exception.MegaException
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
 * @property isRetry whether it is retry
 * @property error the [MegaException]
 * @property repeatToggleMode the repeat toggle mode
 * @property currentPlayingVideoSize the current playing video size
 * @property mediaPlaybackState the playback state
 * @property snackBarMessage the snack bar message
 * @property isFullScreen whether it is full screen
 * @property isVideoOptionPopupShown whether the video option popup is shown
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
 */
data class VideoPlayerUiState(
    val items: List<VideoPlayerItem> = emptyList(),
    val mediaPlaySources: MediaPlaySources? = null,
    val currentPlayingHandle: Long = -1,
    val currentPlayingIndex: Int? = null,
    val metadata: Metadata = Metadata(null, null, null, ""),
    val playQueueTitle: String? = null,
    val isRetry: Boolean? = null,
    val error: MegaException? = null,
    val repeatToggleMode: RepeatToggleMode = RepeatToggleMode.REPEAT_NONE,
    val currentPlayingVideoSize: VideoSize? = null,
    val mediaPlaybackState: MediaPlaybackState = MediaPlaybackState.Playing,
    val snackBarMessage: String? = null,
    val isFullScreen: Boolean = false,
    val isVideoOptionPopupShown: Boolean = false,
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
    val searchedItems: List<VideoPlayerItem> = emptyList()
)
