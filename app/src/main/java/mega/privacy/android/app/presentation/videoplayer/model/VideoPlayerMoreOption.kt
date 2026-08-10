package mega.privacy.android.app.presentation.videoplayer.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import mega.android.core.ui.model.menu.MenuActionString
import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Menu actions for the video player "more options" bottom sheet.
 * Each case maps to a [MenuActionString] row (icon, label, test tag).
 */
sealed class VideoPlayerMoreOption(
    icon: ImageVector,
    @StringRes descriptionRes: Int,
    testTag: String,
) : MenuActionString(icon, descriptionRes, testTag) {

    data object Snapshot : VideoPlayerMoreOption(
        IconPack.Medium.Thin.Outline.Screenshot,
        R.string.media_player_video_option_snapshot_title,
        VIDEO_PLAYER_MORE_OPTIONS_SNAPSHOT_TILE_TEST_TAG,
    )

    data object Subtitle : VideoPlayerMoreOption(
        IconPack.Medium.Thin.Outline.Subtitles02,
        R.string.media_player_video_enable_subtitle_dialog_title,
        VIDEO_PLAYER_MORE_OPTIONS_SUBTITLE_TILE_TEST_TAG,
    )

    data object Playlist : VideoPlayerMoreOption(
        IconPack.Medium.Thin.Outline.Playlist,
        sharedR.string.video_player_more_options_playlist_option,
        VIDEO_PLAYER_MORE_OPTIONS_PLAYLIST_TILE_TEST_TAG,
    )

    data object Lock : VideoPlayerMoreOption(
        IconPack.Medium.Thin.Outline.Lock,
        sharedR.string.video_player_more_options_lock_option,
        VIDEO_PLAYER_MORE_OPTIONS_LOCK_TILE_TEST_TAG,
    )

    data object PIP : VideoPlayerMoreOption(
        IconPack.Medium.Thin.Outline.Contract,
        sharedR.string.video_player_more_options_pip_option,
        VIDEO_PLAYER_MORE_OPTIONS_PIP_TILE_TEST_TAG,
    )
}

const val VIDEO_PLAYER_MORE_OPTIONS_SNAPSHOT_TILE_TEST_TAG =
    "video_player_more_options:bottom_sheet_tile_snapshot"

const val VIDEO_PLAYER_MORE_OPTIONS_SUBTITLE_TILE_TEST_TAG =
    "video_player_more_options:bottom_sheet_tile_subtitle"

const val VIDEO_PLAYER_MORE_OPTIONS_PLAYLIST_TILE_TEST_TAG =
    "video_player_more_options:bottom_sheet_tile_playlist"

const val VIDEO_PLAYER_MORE_OPTIONS_LOCK_TILE_TEST_TAG =
    "video_player_more_options:bottom_sheet_tile_lock"

const val VIDEO_PLAYER_MORE_OPTIONS_PIP_TILE_TEST_TAG =
    "video_player_more_options:bottom_sheet_tile_pip"
