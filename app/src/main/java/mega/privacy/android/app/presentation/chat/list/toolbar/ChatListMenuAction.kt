package mega.privacy.android.app.presentation.chat.list.toolbar

import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionWithoutIcon
import mega.privacy.android.app.R

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