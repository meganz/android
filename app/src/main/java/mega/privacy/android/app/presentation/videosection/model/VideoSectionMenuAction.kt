package mega.privacy.android.app.presentation.videosection.model

import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.android.shared.original.core.ui.model.MenuActionPlural
import mega.privacy.android.shared.original.core.ui.model.MenuActionString
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithoutIcon

/**
 * Video section menu action
 */
sealed interface VideoSectionMenuAction : MenuAction {

    /**
     * Video section download action
     */
    object VideoSectionDownloadAction : MenuActionString(
        iconRes = iconPackR.drawable.ic_download_medium_regular_outline,
        descriptionRes = R.string.general_save_to_device,
        testTag = TEST_TAG_VIDEO_SECTION_DOWNLOAD_ACTION
    ), VideoSectionMenuAction {
        override val orderInCategory = 100
    }

    /**
     * Video section get link action
     */
    object VideoSectionGetLinkAction : MenuActionPlural(
        iconRes = iconPackR.drawable.ic_link_01_medium_regular_outline,
        descriptionRes = sharedR.plurals.label_share_links,
        amount = 1,
        testTag = TEST_TAG_VIDEO_SECTION_GET_LINK_ACTION
    ), VideoSectionMenuAction {
        override val orderInCategory = 105
    }

    /**
     * Video section send to chat action
     */
    object VideoSectionSendToChatAction : MenuActionString(
        iconRes = iconPackR.drawable.ic_message_arrow_up_medium_regular_outline,
        descriptionRes = R.string.context_send_file_to_chat,
        testTag = TEST_TAG_VIDEO_SECTION_SEND_TO_CHAT_ACTION
    ), VideoSectionMenuAction {
        override val orderInCategory = 110
    }

    /**
     * Video section share action
     */
    object VideoSectionShareAction : MenuActionString(
        iconRes = iconPackR.drawable.ic_share_network_medium_regular_outline,
        descriptionRes = R.string.general_share,
        testTag = TEST_TAG_VIDEO_SECTION_SHARE_ACTION
    ), VideoSectionMenuAction {
        override val orderInCategory = 115
    }

    /**
     * Video section select all action
     */
    object VideoSectionSelectAllAction : MenuActionWithoutIcon(
        descriptionRes = R.string.action_select_all,
        testTag = TEST_TAG_VIDEO_SECTION_SELECT_ALL_ACTION
    ), VideoSectionMenuAction {
        override val orderInCategory = 120
    }

    /**
     * Video section clear selection action
     */
    object VideoSectionClearSelectionAction : MenuActionWithoutIcon(
        descriptionRes = R.string.action_unselect_all,
        testTag = TEST_TAG_VIDEO_SECTION_CLEAR_SELECTION_ACTION
    ), VideoSectionMenuAction {
        override val orderInCategory = 125
    }

    /**
     * Video section hide action
     */
    object VideoSectionHideAction : MenuActionWithoutIcon(
        descriptionRes = R.string.general_hide_node,
        testTag = TEST_TAG_VIDEO_SECTION_HIDE_ACTION
    ), VideoSectionMenuAction {
        override val orderInCategory = 130
    }

    /**
     * Video section unhide action
     */
    object VideoSectionUnhideAction : MenuActionWithoutIcon(
        descriptionRes = R.string.general_unhide_node,
        testTag = TEST_TAG_VIDEO_SECTION_UNHIDE_ACTION
    ), VideoSectionMenuAction {
        override val orderInCategory = 135
    }

    /**
     * Video section remove link action
     */
    object VideoSectionRemoveLinkAction : MenuActionWithoutIcon(
        descriptionRes = R.string.context_remove_link_menu,
        testTag = TEST_TAG_VIDEO_SECTION_REMOVE_LINK_ACTION
    ), VideoSectionMenuAction {
        override val orderInCategory = 140
    }

    /**
     * Video section rename action
     */
    object VideoSectionRenameAction : MenuActionWithoutIcon(
        descriptionRes = R.string.context_rename,
        testTag = TEST_TAG_VIDEO_SECTION_RENAME_ACTION
    ), VideoSectionMenuAction {
        override val orderInCategory = 145
    }

    /**
     * Video section move move action
     */
    object VideoSectionMoveAction : MenuActionWithoutIcon(
        descriptionRes = R.string.general_move,
        testTag = TEST_TAG_VIDEO_SECTION_MOVE_ACTION
    ), VideoSectionMenuAction {
        override val orderInCategory = 150
    }

    /**
     * Video section copy action
     */
    object VideoSectionCopyAction : MenuActionWithoutIcon(
        descriptionRes = R.string.context_copy,
        testTag = TEST_TAG_VIDEO_SECTION_COPY_ACTION
    ), VideoSectionMenuAction {
        override val orderInCategory = 155
    }

    /**
     * Video section move to rubbish bin action
     */
    object VideoSectionRubbishBinAction : MenuActionWithoutIcon(
        descriptionRes = R.string.context_move_to_trash,
        testTag = TEST_TAG_VIDEO_SECTION_RUBBISH_BIN_ACTION
    ), VideoSectionMenuAction {
        override val orderInCategory = 160
    }

    /**
     * Video section remove action
     */
    object VideoSectionRemoveAction : MenuActionString(
        iconRes = iconPackR.drawable.ic_trash_medium_regular_outline,
        descriptionRes = R.string.context_remove,
        testTag = TEST_TAG_VIDEO_SECTION_REMOVE_ACTION
    ), VideoSectionMenuAction {
        override val orderInCategory = 165
    }

    /**
     * Video section more action
     */
    object VideoSectionMoreAction : MenuActionString(
        iconRes = iconPackR.drawable.ic_more_vertical_medium_regular_outline,
        descriptionRes = R.string.label_more,
        testTag = TEST_TAG_VIDEO_SECTION_MORE_ACTION
    ), VideoSectionMenuAction {
        override val orderInCategory = 170
    }

    /**
     * Video section video recently watched action
     */
    object VideoRecentlyWatchedAction : MenuActionString(
        iconRes = iconPackR.drawable.ic_video_recently_watched,
        descriptionRes = sharedR.string.video_section_title_video_recently_watched,
        testTag = TEST_TAG_VIDEO_SECTION_RECENTLY_WATCHED_ACTION
    ), VideoSectionMenuAction {
        override val orderInCategory = 175
    }

    /**
     * Video section video recently watched action
     */
    object VideoRecentlyWatchedClearAction : MenuActionString(
        iconRes = iconPackR.drawable.ic_trash_medium_regular_outline,
        descriptionRes = R.string.general_clear,
        testTag = TEST_TAG_VIDEO_SECTION_RECENTLY_WATCHED_CLEAR_ACTION
    ), VideoSectionMenuAction {
        override val orderInCategory = 180
    }

    companion object {
        /**
         * Test tag for video section download action
         */
        const val TEST_TAG_VIDEO_SECTION_DOWNLOAD_ACTION = "video_section:action_download"

        /**
         * Test tag for video section get link action
         */
        const val TEST_TAG_VIDEO_SECTION_GET_LINK_ACTION = "video_section:action_get_link"

        /**
         * Test tag for video section send to chat action
         */
        const val TEST_TAG_VIDEO_SECTION_SEND_TO_CHAT_ACTION = "video_section:action_send_to_chat"

        /**
         * Test tag for video section share action
         */
        const val TEST_TAG_VIDEO_SECTION_SHARE_ACTION = "video_section:action_share"

        /**
         * Test tag for video section select all action
         */
        const val TEST_TAG_VIDEO_SECTION_SELECT_ALL_ACTION = "video_section:action_select_all"

        /**
         * Test tag for video section clear selection action
         */
        const val TEST_TAG_VIDEO_SECTION_CLEAR_SELECTION_ACTION =
            "video_section:action_clear_selection"

        /**
         * Test tag for video section remove link action
         */
        const val TEST_TAG_VIDEO_SECTION_REMOVE_LINK_ACTION = "video_section:action_remove_link"

        /**
         * Test tag for video section hide action
         */
        const val TEST_TAG_VIDEO_SECTION_HIDE_ACTION = "video_section:action_hide"

        /**
         * Test tag for video section unhide action
         */
        const val TEST_TAG_VIDEO_SECTION_UNHIDE_ACTION = "video_section:action_unhide"

        /**
         * Test tag for video section rename action
         */
        const val TEST_TAG_VIDEO_SECTION_RENAME_ACTION = "video_section:action_rename"

        /**
         * Test tag for video section move action
         */
        const val TEST_TAG_VIDEO_SECTION_MOVE_ACTION = "video_section:action_move"

        /**
         * Test tag for video section copy action
         */
        const val TEST_TAG_VIDEO_SECTION_COPY_ACTION = "video_section:action_copy"

        /**
         * Test tag for video section move to rubbish bin action
         */
        const val TEST_TAG_VIDEO_SECTION_RUBBISH_BIN_ACTION = "video_section:action_rubbish_bin"

        /**
         * Test tag for video section remove action
         */
        const val TEST_TAG_VIDEO_SECTION_REMOVE_ACTION = "video_section:action_remove"

        /**
         * Test tag for video section more action
         */
        const val TEST_TAG_VIDEO_SECTION_MORE_ACTION = "video_section:action_more"

        /**
         * Test tag for video section video recently watched action
         */
        const val TEST_TAG_VIDEO_SECTION_RECENTLY_WATCHED_ACTION =
            "video_section:action_recently_watched"

        /**
         * Test tag for video section video recently watched clear action
         */
        const val TEST_TAG_VIDEO_SECTION_RECENTLY_WATCHED_CLEAR_ACTION =
            "video_section:action_recently_watched_clear"
    }
}