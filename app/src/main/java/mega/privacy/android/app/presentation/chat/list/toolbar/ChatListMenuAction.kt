package mega.privacy.android.app.presentation.chat.list.toolbar

import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionString
import mega.android.core.ui.model.menu.MenuActionWithoutIcon
import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.IconPack

sealed interface ChatListMenuAction : MenuAction {

    /**
     * Open link action
     */
    object OpenLinkAction : MenuActionWithoutIcon(
        descriptionRes = R.string.action_open_link,
        testTag = TEST_TAG_CHAT_LIST_OPEN_LINK_ACTION
    ), ChatListMenuAction {
        override val orderInCategory = 110
    }

    /**
     * Do not disturb action
     */
    object DoNotDisturbAction : MenuActionWithoutIcon(
        descriptionRes = R.string.title_dialog_mute_chat_notifications,
        testTag = TEST_TAG_CHAT_LIST_DO_NOT_DISTURB_ACTION
    ), ChatListMenuAction {
        override val orderInCategory = 140
    }

    /**
     * Archived chats action
     */
    object ArchivedAction : MenuActionWithoutIcon(
        descriptionRes = R.string.archived_chats_title_section,
        testTag = TEST_TAG_CHAT_LIST_ARCHIVED_ACTION
    ), ChatListMenuAction {
        override val orderInCategory = 145
    }

    companion object {
        /**
         * Test tag for open link action
         */
        const val TEST_TAG_CHAT_LIST_OPEN_LINK_ACTION = "chat_list:action_open_link"

        /**
         * Test tag for do not disturb action
         */
        const val TEST_TAG_CHAT_LIST_DO_NOT_DISTURB_ACTION = "chat_list:action_do_not_disturb"

        /**
         * Test tag for archived action
         */
        const val TEST_TAG_CHAT_LIST_ARCHIVED_ACTION = "chat_list:action_archived"
    }
}

/**
 * Actions shown in the chat list toolbar while items are selected.
 */
sealed interface ChatListSelectModeMenuAction : MenuAction {

    object MuteAction : MenuActionString(
        icon = IconPack.Medium.Thin.Outline.BellOff,
        descriptionRes = R.string.general_mute,
        testTag = TEST_TAG_CHAT_LIST_SELECT_MODE_MUTE_ACTION,
    ), ChatListSelectModeMenuAction {
        override val orderInCategory = 10
    }

    object UnmuteAction : MenuActionString(
        icon = IconPack.Medium.Thin.Outline.Bell,
        descriptionRes = R.string.general_unmute,
        testTag = TEST_TAG_CHAT_LIST_SELECT_MODE_UNMUTE_ACTION,
    ), ChatListSelectModeMenuAction {
        override val orderInCategory = 10
    }

    object ArchiveAction : MenuActionString(
        icon = IconPack.Medium.Thin.Outline.Archive,
        descriptionRes = R.string.tab_archive_chat,
        testTag = TEST_TAG_CHAT_LIST_SELECT_MODE_ARCHIVE_ACTION,
    ), ChatListSelectModeMenuAction {
        override val orderInCategory = 20
    }

    object LeaveAction : MenuActionString(
        icon = IconPack.Medium.Thin.Outline.LogOut02,
        descriptionRes = R.string.general_leave,
        testTag = TEST_TAG_CHAT_LIST_SELECT_MODE_LEAVE_ACTION,
    ), ChatListSelectModeMenuAction {
        override val orderInCategory = 20
    }

    object SelectAllAction : MenuActionWithoutIcon(
        descriptionRes = R.string.action_select_all,
        testTag = TEST_TAG_CHAT_LIST_SELECT_MODE_SELECT_ALL_ACTION,
    ), ChatListSelectModeMenuAction {
        override val orderInCategory = 40
    }

    object UnselectAllAction : MenuActionWithoutIcon(
        descriptionRes = R.string.action_unselect_all,
        testTag = TEST_TAG_CHAT_LIST_SELECT_MODE_UNSELECT_ALL_ACTION,
    ), ChatListSelectModeMenuAction {
        override val orderInCategory = 40
    }

    companion object {
        const val TEST_TAG_CHAT_LIST_SELECT_MODE_MUTE_ACTION = "chat_list:select_mode_action_mute"
        const val TEST_TAG_CHAT_LIST_SELECT_MODE_UNMUTE_ACTION = "chat_list:select_mode_action_unmute"
        const val TEST_TAG_CHAT_LIST_SELECT_MODE_ARCHIVE_ACTION = "chat_list:select_mode_action_archive"
        const val TEST_TAG_CHAT_LIST_SELECT_MODE_LEAVE_ACTION = "chat_list:select_mode_action_leave"
        const val TEST_TAG_CHAT_LIST_SELECT_MODE_SELECT_ALL_ACTION = "chat_list:select_mode_action_select_all"
        const val TEST_TAG_CHAT_LIST_SELECT_MODE_UNSELECT_ALL_ACTION = "chat_list:select_mode_action_unselect_all"
    }
}
