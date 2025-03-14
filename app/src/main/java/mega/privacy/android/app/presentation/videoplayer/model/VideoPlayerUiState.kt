package mega.privacy.android.app.presentation.videoplayer.model

import mega.privacy.android.app.mediaplayer.model.MediaPlaySources
import mega.privacy.android.app.mediaplayer.service.Metadata
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.exception.MegaException

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
    val isNodeInRubbishBin: Boolean = false,
    val nodeIsNull: Boolean = false,
    val isHideMenuActionVisible: Boolean = false,
    val isUnhideMenuActionVisible: Boolean = false,
    val canRemoveFromChat: Boolean = false,
    val shouldShowShare: Boolean = false,
    val shouldShowGetLink: Boolean = true,
    val shouldShowRemoveLink: Boolean = true,
    val isAccess: Boolean = true,
    val isRubbishBinShown: Boolean = false,
    val shouldShowAddTo: Boolean = false,
    val accountType: AccountType? = null,
    val isBusinessAccountExpired: Boolean = false,
    val hiddenNodeEnabled: Boolean = false,
    val isHiddenNodesOnboarded: Boolean = false,
    val showHiddenItems: Boolean? = null,
)
