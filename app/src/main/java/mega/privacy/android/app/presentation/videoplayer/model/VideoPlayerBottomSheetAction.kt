package mega.privacy.android.app.presentation.videoplayer.model

import androidx.compose.ui.graphics.vector.ImageVector
import mega.android.core.ui.model.menu.MenuActionString
import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Menu actions for the video player menu bottom sheet.
 * Each case maps to a [MenuActionString] row (icon, label, test tag).
 * These correspond 1:1 with [VideoPlayerMenuAction] entries.
 */
sealed class VideoPlayerBottomSheetAction(
    icon: ImageVector,
    descriptionRes: Int,
    testTag: String,
) : MenuActionString(icon, descriptionRes, testTag) {

    data object Download : VideoPlayerBottomSheetAction(
        IconPack.Medium.Thin.Outline.Download,
        R.string.general_save_to_device,
        TEST_TAG_VIDEO_PLAYER_MENU_V2_DOWNLOAD,
    )

    data object Share : VideoPlayerBottomSheetAction(
        IconPack.Medium.Thin.Outline.ShareNetwork,
        R.string.general_share,
        TEST_TAG_VIDEO_PLAYER_MENU_V2_SHARE,
    )

    data object SendToChat : VideoPlayerBottomSheetAction(
        IconPack.Medium.Thin.Outline.MessageArrowUp,
        R.string.context_send_file_to_chat,
        TEST_TAG_VIDEO_PLAYER_MENU_V2_SEND_TO_CHAT,
    )

    data object GetLink : VideoPlayerBottomSheetAction(
        IconPack.Medium.Thin.Outline.Link01,
        R.string.context_get_link,
        TEST_TAG_VIDEO_PLAYER_MENU_V2_GET_LINK,
    )

    data object RemoveLink : VideoPlayerBottomSheetAction(
        IconPack.Medium.Thin.Outline.LinkOff01,
        R.string.context_remove_link_menu,
        TEST_TAG_VIDEO_PLAYER_MENU_V2_REMOVE_LINK,
    )

    data object FileInfo : VideoPlayerBottomSheetAction(
        IconPack.Medium.Thin.Outline.Info,
        R.string.general_file_info,
        TEST_TAG_VIDEO_PLAYER_MENU_V2_FILE_INFO,
    )

    data object Rename : VideoPlayerBottomSheetAction(
        IconPack.Medium.Thin.Outline.Edit,
        sharedR.string.context_rename,
        TEST_TAG_VIDEO_PLAYER_MENU_V2_RENAME,
    )

    data object Hide : VideoPlayerBottomSheetAction(
        IconPack.Medium.Thin.Outline.EyeOff,
        R.string.general_hide_node,
        TEST_TAG_VIDEO_PLAYER_MENU_V2_HIDE,
    )

    data object Unhide : VideoPlayerBottomSheetAction(
        IconPack.Medium.Thin.Outline.Eye,
        R.string.general_unhide_node,
        TEST_TAG_VIDEO_PLAYER_MENU_V2_UNHIDE,
    )

    data object Move : VideoPlayerBottomSheetAction(
        IconPack.Medium.Thin.Outline.Move,
        R.string.general_move,
        TEST_TAG_VIDEO_PLAYER_MENU_V2_MOVE,
    )

    data object Copy : VideoPlayerBottomSheetAction(
        IconPack.Medium.Thin.Outline.Copy01,
        R.string.context_copy,
        TEST_TAG_VIDEO_PLAYER_MENU_V2_COPY,
    )

    data object AddTo : VideoPlayerBottomSheetAction(
        IconPack.Medium.Thin.Outline.RectangleStackPlus,
        sharedR.string.album_add_to_media,
        TEST_TAG_VIDEO_PLAYER_MENU_V2_ADD_TO,
    )

    data object RubbishBin : VideoPlayerBottomSheetAction(
        IconPack.Medium.Thin.Outline.Trash,
        R.string.context_move_to_trash,
        TEST_TAG_VIDEO_PLAYER_MENU_V2_RUBBISH_BIN,
    )

    data object Remove : VideoPlayerBottomSheetAction(
        IconPack.Medium.Thin.Outline.Trash,
        R.string.context_remove,
        TEST_TAG_VIDEO_PLAYER_MENU_V2_REMOVE,
    )

    data object ChatImport : VideoPlayerBottomSheetAction(
        IconPack.Medium.Thin.Outline.CloudUpload,
        R.string.add_to_cloud,
        TEST_TAG_VIDEO_PLAYER_MENU_V2_CHAT_IMPORT,
    )

    data object SaveForOffline : VideoPlayerBottomSheetAction(
        IconPack.Medium.Thin.Outline.ArrowDownCircle,
        R.string.file_properties_available_offline,
        TEST_TAG_VIDEO_PLAYER_MENU_V2_SAVE_FOR_OFFLINE,
    )

    companion object {
        const val TEST_TAG_VIDEO_PLAYER_MENU_V2_DOWNLOAD = "video_player_menu:action_download"
        const val TEST_TAG_VIDEO_PLAYER_MENU_V2_SHARE = "video_player_menu:action_share"
        const val TEST_TAG_VIDEO_PLAYER_MENU_V2_SEND_TO_CHAT = "video_player_menu:action_send_to_chat"
        const val TEST_TAG_VIDEO_PLAYER_MENU_V2_GET_LINK = "video_player_menu:action_get_link"
        const val TEST_TAG_VIDEO_PLAYER_MENU_V2_REMOVE_LINK = "video_player_menu:action_remove_link"
        const val TEST_TAG_VIDEO_PLAYER_MENU_V2_FILE_INFO = "video_player_menu:action_file_info"
        const val TEST_TAG_VIDEO_PLAYER_MENU_V2_RENAME = "video_player_menu:action_rename"
        const val TEST_TAG_VIDEO_PLAYER_MENU_V2_HIDE = "video_player_menu:action_hide"
        const val TEST_TAG_VIDEO_PLAYER_MENU_V2_UNHIDE = "video_player_menu:action_unhide"
        const val TEST_TAG_VIDEO_PLAYER_MENU_V2_MOVE = "video_player_menu:action_move"
        const val TEST_TAG_VIDEO_PLAYER_MENU_V2_COPY = "video_player_menu:action_copy"
        const val TEST_TAG_VIDEO_PLAYER_MENU_V2_ADD_TO = "video_player_menu:action_add_to"
        const val TEST_TAG_VIDEO_PLAYER_MENU_V2_RUBBISH_BIN = "video_player_menu:action_rubbish_bin"
        const val TEST_TAG_VIDEO_PLAYER_MENU_V2_REMOVE = "video_player_menu:action_remove"
        const val TEST_TAG_VIDEO_PLAYER_MENU_V2_CHAT_IMPORT = "video_player_menu:action_chat_import"
        const val TEST_TAG_VIDEO_PLAYER_MENU_V2_SAVE_FOR_OFFLINE = "video_player_menu:action_save_for_offline"
    }
}
