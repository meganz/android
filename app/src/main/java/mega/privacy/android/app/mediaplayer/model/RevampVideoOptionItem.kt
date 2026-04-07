package mega.privacy.android.app.mediaplayer.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Video player (Revamp) overflow menu options. Fullscreen is toolbar-only; not listed here.
 *
 * @property icon Option icon
 * @property optionTitleId String resource for the option title
 */
enum class RevampVideoOptionItem(
    val icon: ImageVector,
    @get:StringRes
    val optionTitleId: Int,
) {
    /**
     * Capture snapshot
     */
    Snapshot(
        icon = IconPack.Medium.Thin.Outline.Screenshot,
        optionTitleId = R.string.media_player_video_option_snapshot_title
    ),

    /**
     * Subtitles
     */
    Subtitle(
        icon = IconPack.Medium.Thin.Outline.Subtitles02,
        optionTitleId = R.string.media_player_video_enable_subtitle_dialog_title
    ),

    /**
     * Playlist / play queue
     */
    Playlist(
        icon = IconPack.Medium.Thin.Outline.Playlist,
        optionTitleId = sharedR.string.video_player_more_options_playlist_option
    ),

    /**
     * Lock controls
     */
    Lock(
        icon = IconPack.Medium.Thin.Outline.Lock,
        optionTitleId = sharedR.string.video_player_more_options_lock_option
    ),
}
