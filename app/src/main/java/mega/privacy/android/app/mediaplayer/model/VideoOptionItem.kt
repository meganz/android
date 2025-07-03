package mega.privacy.android.app.mediaplayer.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.IconPack

/**
 * The enum class for video option
 *
 * @param iconId icon id
 * @param optionTitleId the option title sting id
 */
enum class VideoOptionItem(
    val icon: ImageVector,
    @get:StringRes
    val optionTitleId: Int,
) {
    /**
     * The snapshot option
     */
    VIDEO_OPTION_SNAPSHOT(
        icon = IconPack.Medium.Regular.Outline.Screenshot,
        optionTitleId = R.string.media_player_video_option_snapshot_title
    ),

    /**
     * The lock option
     */
    VIDEO_OPTION_LOCK(
        icon = IconPack.Medium.Regular.Outline.Lock,
        optionTitleId = R.string.media_player_video_option_lock_title
    ),

    /**
     * The zoom to fill option
     */
    VIDEO_OPTION_ZOOM_TO_FILL(
        icon = IconPack.Medium.Regular.Outline.Maximize02,
        optionTitleId = R.string.media_player_video_option_zoom_to_fill_title
    ),

    /**
     * The original option
     */
    VIDEO_OPTION_ORIGINAL(
        icon = IconPack.Medium.Regular.Outline.Minimize02,
        optionTitleId = R.string.media_player_video_option_original_title
    ),
}
