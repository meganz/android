package mega.privacy.android.app.presentation.videoplayer.model

import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.android.shared.original.core.ui.model.MenuActionPlural
import mega.privacy.android.shared.original.core.ui.model.MenuActionString
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithoutIcon

/**
 * Video Player menu actions
 */
sealed interface VideoPlayerMenuAction : MenuAction {
    /**
     * Video player download action
     */
    object VideoPlayerDownloadAction : MenuActionString(
        iconRes = iconPackR.drawable.ic_download_medium_regular_outline,
        descriptionRes = R.string.general_save_to_device,
        testTag = TEST_TAG_VIDEO_PLAYER_DOWNLOAD_ACTION
    ), VideoPlayerMenuAction {
        override val orderInCategory = 100
    }

    /**
     * Video player share action
     */
    object VideoPlayerShareAction : MenuActionString(
        iconRes = iconPackR.drawable.ic_share_network_medium_regular_outline,
        descriptionRes = R.string.general_share,
        testTag = TEST_TAG_VIDEO_PLAYER_SHARE_ACTION
    ), VideoPlayerMenuAction {
        override val orderInCategory = 105
    }

    /**
     * Video player send to chat action
     */
    object VideoPlayerSendToChatAction : MenuActionString(
        iconRes = iconPackR.drawable.ic_message_arrow_up_medium_regular_outline,
        descriptionRes = R.string.context_send_file_to_chat,
        testTag = TEST_TAG_VIDEO_PLAYER_SEND_TO_CHAT_ACTION
    ), VideoPlayerMenuAction {
        override val orderInCategory = 110
    }

    /**
     * Video player get link action
     */
    object VideoPlayerGetLinkAction : MenuActionPlural(
        iconRes = iconPackR.drawable.ic_link_01_medium_regular_outline,
        descriptionRes = sharedR.plurals.label_share_links,
        amount = 1,
        testTag = TEST_TAG_VIDEO_PLAYER_GET_LINK_ACTION
    ), VideoPlayerMenuAction {
        override val orderInCategory = 115
    }

    /**
     * Video player remove link action
     */
    object VideoPlayerRemoveLinkAction : MenuActionString(
        iconRes = iconPackR.drawable.ic_link_off_01_medium_regular_outline,
        descriptionRes = R.string.context_remove_link_menu,
        testTag = TEST_TAG_VIDEO_PLAYER_REMOVE_LINK_ACTION
    ), VideoPlayerMenuAction {
        override val orderInCategory = 120
    }

    /**
     * Video player file info action
     */
    object VideoPlayerFileInfoAction : MenuActionWithoutIcon(
        descriptionRes = R.string.general_file_info,
        testTag = TEST_TAG_VIDEO_PLAYER_FILE_INFO_ACTION
    ), VideoPlayerMenuAction {
        override val orderInCategory = 125
    }

    /**
     * Video player rename action
     */
    object VideoPlayerRenameAction : MenuActionWithoutIcon(
        descriptionRes = R.string.context_rename,
        testTag = TEST_TAG_VIDEO_PLAYER_RENAME_ACTION
    ), VideoPlayerMenuAction {
        override val orderInCategory = 130
    }

    /**
     * Video player hide action
     */
    object VideoPlayerHideAction : MenuActionWithoutIcon(
        descriptionRes = R.string.general_hide_node,
        testTag = TEST_TAG_VIDEO_PLAYER_HIDE_ACTION
    ), VideoPlayerMenuAction {
        override val orderInCategory = 135
    }

    /**
     * Video player unhide action
     */
    object VideoPlayerUnhideAction : MenuActionWithoutIcon(
        descriptionRes = R.string.general_unhide_node,
        testTag = TEST_TAG_VIDEO_PLAYER_UNHIDE_ACTION
    ), VideoPlayerMenuAction {
        override val orderInCategory = 140
    }

    /**
     * Video player move move action
     */
    object VideoPlayerMoveAction : MenuActionWithoutIcon(
        descriptionRes = R.string.general_move,
        testTag = TEST_TAG_VIDEO_PLAYER_MOVE_ACTION
    ), VideoPlayerMenuAction {
        override val orderInCategory = 145
    }

    /**
     * Video player copy action
     */
    object VideoPlayerCopyAction : MenuActionWithoutIcon(
        descriptionRes = R.string.context_copy,
        testTag = TEST_TAG_VIDEO_PLAYER_COPY_ACTION
    ), VideoPlayerMenuAction {
        override val orderInCategory = 150
    }

    /**
     * Video player add to action
     */
    object VideoPlayerAddToAction : MenuActionWithoutIcon(
        descriptionRes = sharedR.string.album_add_to_media,
        testTag = TEST_TAG_VIDEO_PLAYER_ADD_TO_ACTION
    ), VideoPlayerMenuAction {
        override val orderInCategory = 155
    }

    /**
     * Video player move to rubbish bin action
     */
    object VideoPlayerRubbishBinAction : MenuActionWithoutIcon(
        descriptionRes = R.string.context_move_to_trash,
        testTag = TEST_TAG_VIDEO_PLAYER_RUBBISH_BIN_ACTION
    ), VideoPlayerMenuAction {
        override val orderInCategory = 160
    }

    /**
     * Video player remove action
     */
    object VideoPlayerRemoveAction : MenuActionWithoutIcon(
        descriptionRes = R.string.context_remove,
        testTag = TEST_TAG_VIDEO_PLAYER_REMOVE_ACTION
    ), VideoPlayerMenuAction {
        override val orderInCategory = 165
    }

    /**
     * Video player chat import action
     */
    object VideoPlayerChatImportAction : MenuActionString(
        iconRes = iconPackR.drawable.ic_cloud_upload_medium_regular_outline,
        descriptionRes = R.string.add_to_cloud,
        testTag = TEST_TAG_VIDEO_PLAYER_CHAT_IMPORT_ACTION
    ), VideoPlayerMenuAction {
        override val orderInCategory = 170
    }

    /**
     * Video player save for offline action
     */
    object VideoPlayerSaveForOfflineAction : MenuActionString(
        iconRes = iconPackR.drawable.ic_arrow_down_circle_medium_regular_outline,
        descriptionRes = R.string.file_properties_available_offline,
        testTag = TEST_TAG_VIDEO_PLAYER_SAVE_FOR_OFFLINE_ACTION
    ), VideoPlayerMenuAction {
        override val orderInCategory = 175
    }

    companion object {
        /**
         * Test tag for video player download action
         */
        const val TEST_TAG_VIDEO_PLAYER_DOWNLOAD_ACTION = "video_player:action_download"

        /**
         * Test tag for video player share action
         */
        const val TEST_TAG_VIDEO_PLAYER_SHARE_ACTION = "video_player:action_share"

        /**
         * Test tag for video player send to chat action
         */
        const val TEST_TAG_VIDEO_PLAYER_SEND_TO_CHAT_ACTION = "video_player:action_send_to_chat"

        /**
         * Test tag for video player get link action
         */
        const val TEST_TAG_VIDEO_PLAYER_GET_LINK_ACTION = "video_player:action_get_link"

        /**
         * Test tag for video player remove link action
         */
        const val TEST_TAG_VIDEO_PLAYER_REMOVE_LINK_ACTION = "video_player:action_remove_link"

        /**
         * Test tag for video player file info action
         */
        const val TEST_TAG_VIDEO_PLAYER_FILE_INFO_ACTION = "video_player:action_file_info"

        /**
         * Test tag for video player rename action
         */
        const val TEST_TAG_VIDEO_PLAYER_RENAME_ACTION = "video_player:action_rename"

        /**
         * Test tag for video player hide action
         */
        const val TEST_TAG_VIDEO_PLAYER_HIDE_ACTION = "video_player:action_hide"

        /**
         * Test tag for video player unhide action
         */
        const val TEST_TAG_VIDEO_PLAYER_UNHIDE_ACTION = "video_player:action_unhide"

        /**
         * Test tag for video player move action
         */
        const val TEST_TAG_VIDEO_PLAYER_MOVE_ACTION = "video_player:action_move"

        /**
         * Test tag for video player copy action
         */
        const val TEST_TAG_VIDEO_PLAYER_COPY_ACTION = "video_player:action_copy"

        /**
         * Test tag for video player add to action
         */
        const val TEST_TAG_VIDEO_PLAYER_ADD_TO_ACTION = "video_player:action_add_to"

        /**
         * Test tag for video player move to rubbish bin action
         */
        const val TEST_TAG_VIDEO_PLAYER_RUBBISH_BIN_ACTION = "video_player:action_rubbish_bin"

        /**
         * Test tag for video player remove action
         */
        const val TEST_TAG_VIDEO_PLAYER_REMOVE_ACTION = "video_player:action_remove"

        /**
         * Test tag for video player chat import action
         */
        const val TEST_TAG_VIDEO_PLAYER_CHAT_IMPORT_ACTION = "video_player:action_chat_import"

        /**
         * Test tag for video player save for offline action
         */
        const val TEST_TAG_VIDEO_PLAYER_SAVE_FOR_OFFLINE_ACTION =
            "video_player:action_save_for_offline"
    }
}