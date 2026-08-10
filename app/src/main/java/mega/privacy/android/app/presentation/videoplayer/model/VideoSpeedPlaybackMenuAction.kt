package mega.privacy.android.app.presentation.videoplayer.model

import androidx.compose.runtime.Composable
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.app.mediaplayer.model.VideoSpeedPlaybackItem
import mega.privacy.mobile.analytics.core.event.identifier.EventIdentifier
import mega.privacy.mobile.analytics.event.SpeedOption0_5XPressedEvent
import mega.privacy.mobile.analytics.event.SpeedOption1_5XPressedEvent
import mega.privacy.mobile.analytics.event.SpeedOption2XPressedEvent
import mega.privacy.mobile.analytics.event.VideoSpeedOptionPressed_0_25XEvent
import mega.privacy.mobile.analytics.event.VideoSpeedOptionPressed_0_75XEvent
import mega.privacy.mobile.analytics.event.VideoSpeedOptionPressed_1XEvent
import mega.privacy.mobile.analytics.event.VideoSpeedOptionPressed_1_25XEvent
import mega.privacy.mobile.analytics.event.VideoSpeedOptionPressed_1_75XEvent

/**
 * Menu actions for video playback speed options in the revamp player bottom sheet.
 */
internal sealed class VideoSpeedPlaybackMenuAction : MenuAction {

    abstract val playbackItem: VideoSpeedPlaybackItem

    abstract val speedOptionPressedEvent: EventIdentifier

    @Composable
    override fun getDescription(): String = playbackItem.text

    data object Speed0_25 : VideoSpeedPlaybackMenuAction() {
        override val playbackItem = VideoSpeedPlaybackItem.PlaybackSpeed_0_25X
        override val speedOptionPressedEvent = VideoSpeedOptionPressed_0_25XEvent
        override val testTag = "video_player_revamp:speed_0_25"
    }

    data object Speed0_5 : VideoSpeedPlaybackMenuAction() {
        override val playbackItem = VideoSpeedPlaybackItem.PlaybackSpeed_0_5X
        override val speedOptionPressedEvent = SpeedOption0_5XPressedEvent
        override val testTag = "video_player_revamp:speed_0_5"
    }

    data object Speed0_75 : VideoSpeedPlaybackMenuAction() {
        override val playbackItem = VideoSpeedPlaybackItem.PlaybackSpeed_0_75X
        override val speedOptionPressedEvent = VideoSpeedOptionPressed_0_75XEvent
        override val testTag = "video_player_revamp:speed_0_75"
    }

    data object Speed1 : VideoSpeedPlaybackMenuAction() {
        override val playbackItem = VideoSpeedPlaybackItem.PlaybackSpeed_1X
        override val speedOptionPressedEvent = VideoSpeedOptionPressed_1XEvent
        override val testTag = "video_player_revamp:speed_1"
    }

    data object Speed1_25 : VideoSpeedPlaybackMenuAction() {
        override val playbackItem = VideoSpeedPlaybackItem.PlaybackSpeed_1_25X
        override val speedOptionPressedEvent = VideoSpeedOptionPressed_1_25XEvent
        override val testTag = "video_player_revamp:speed_1_25"
    }

    data object Speed1_5 : VideoSpeedPlaybackMenuAction() {
        override val playbackItem = VideoSpeedPlaybackItem.PlaybackSpeed_1_5X
        override val speedOptionPressedEvent = SpeedOption1_5XPressedEvent
        override val testTag = "video_player_revamp:speed_1_5"
    }

    data object Speed1_75 : VideoSpeedPlaybackMenuAction() {
        override val playbackItem = VideoSpeedPlaybackItem.PlaybackSpeed_1_75X
        override val speedOptionPressedEvent = VideoSpeedOptionPressed_1_75XEvent
        override val testTag = "video_player_revamp:speed_1_75"
    }

    data object Speed2 : VideoSpeedPlaybackMenuAction() {
        override val playbackItem = VideoSpeedPlaybackItem.PlaybackSpeed_2X
        override val speedOptionPressedEvent = SpeedOption2XPressedEvent
        override val testTag = "video_player_revamp:speed_2"
    }

    companion object {
        val entries: List<VideoSpeedPlaybackMenuAction> = listOf(
            Speed0_25,
            Speed0_5,
            Speed0_75,
            Speed1,
            Speed1_25,
            Speed1_5,
            Speed1_75,
            Speed2,
        )
    }
}
