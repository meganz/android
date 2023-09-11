package mega.privacy.android.app.mediaplayer.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import mega.privacy.android.app.R

/**
 * The enum class for video option
 *
 * @param iconId icon id
 * @param optionTitleId the option title sting id
 */
enum class VideoOptionItem(
    @get:DrawableRes
    val iconId: Int,
    @get:StringRes
    val optionTitleId: Int,
) {
    /**
     * The snapshot option
     */
    VIDEO_OPTION_SNAPSHOT(
        iconId = R.drawable.ic_screenshot,
        optionTitleId = R.string.media_player_video_option_snapshot_title
    ),

    /**
     * The lock option
     */
    VIDEO_OPTION_LOCK(
        iconId = R.drawable.ic_lock,
        optionTitleId = R.string.media_player_video_option_lock_title
    ),

    /**
     * The zoom to fill option
     */
    VIDEO_OPTION_ZOOM_TO_FILL(
        iconId = R.drawable.ic_full_screen,
        optionTitleId = R.string.media_player_video_option_zoom_to_fill_title
    ),

    /**
     * The original option
     */
    VIDEO_OPTION_ORIGINAL(
        iconId = R.drawable.ic_original,
        optionTitleId = R.string.media_player_video_option_original_title
    ),
}
