package mega.privacy.android.app.presentation.meeting.chat.model

import mega.privacy.android.app.R
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionString
import mega.privacy.android.core.ui.model.MenuActionWithoutIcon

/**
 * Chat room menu action.
 * All the actions which may be available in normal mode should be defined here.
 *
 * @constructor Create empty Chat room menu action
 */
sealed interface ChatRoomMenuAction : MenuAction {

    /**
     * Audio call
     */
    class AudioCall(override val enabled: Boolean) : MenuActionString(
        iconRes = R.drawable.ic_phone,
        descriptionRes = R.string.call_button,
        testTag = TEST_TAG_AUDIO_CALL_ACTION,
    ), ChatRoomMenuAction {
        override val orderInCategory = 100
    }

    /**
     * Video call
     */
    class VideoCall(override val enabled: Boolean) : MenuActionString(
        iconRes = R.drawable.ic_video_action,
        descriptionRes = R.string.video_button,
        testTag = TEST_TAG_VIDEO_CALL_ACTION,
    ), ChatRoomMenuAction {
        override val orderInCategory = 100
    }

    /**
     * Add participants
     */
    object AddParticipants : MenuActionWithoutIcon(
        descriptionRes = R.string.add_participants_menu_item,
        testTag = TEST_TAG_ADD_PARTICIPANTS_ACTION,
    ), ChatRoomMenuAction {
        override val orderInCategory = 110
    }

    object Mute : MenuActionWithoutIcon(
        descriptionRes = R.string.general_mute,
        testTag = TEST_TAG_MUTE_ACTION,
    ), ChatRoomMenuAction {
        override val orderInCategory = 150
    }

    object Unmute : MenuActionWithoutIcon(
        descriptionRes = R.string.general_unmute,
        testTag = TEST_TAG_UNMUTE_ACTION,
    ), ChatRoomMenuAction {
        override val orderInCategory = 155
    }

    /**
     * Info
     */
    object Info : MenuActionWithoutIcon(
        descriptionRes = R.string.general_info,
        testTag = TEST_TAG_INFO_ACTION,
    ), ChatRoomMenuAction {
        override val orderInCategory: Int = 125
    }

    /**
     * Clear
     */
    object Clear : MenuActionWithoutIcon(
        descriptionRes = R.string.general_clear,
        testTag = TEST_TAG_CLEAR_ACTION,
    ), ChatRoomMenuAction {
        override val orderInCategory: Int = 130
    }

    /**
     * Archive
     *
     */
    object Archive : MenuActionWithoutIcon(
        descriptionRes = R.string.general_archive,
        testTag = TEST_TAG_ARCHIVE_ACTION,
    ), ChatRoomMenuAction {
        override val orderInCategory: Int = 135
    }

    /**
     * Unarchive
     *
     */
    object Unarchive : MenuActionWithoutIcon(
        descriptionRes = R.string.general_unarchive,
        testTag = TEST_TAG_UNARCHIVE_ACTION,
    ), ChatRoomMenuAction {
        override val orderInCategory: Int = 135
    }

    /**
     * Add participants
     */
    object EndCallForAll : MenuActionWithoutIcon(
        descriptionRes = R.string.meetings_chat_screen_menu_option_end_call_for_all,
        testTag = TEST_TAG_END_CALL_FOR_ALL_ACTION,
    ), ChatRoomMenuAction {
        override val orderInCategory = 145
    }

    /**
     * Select
     *
     */
    object Select : MenuActionWithoutIcon(
        descriptionRes = R.string.general_select,
        testTag = TEST_TAG_SELECT_ACTION,
    ), ChatRoomMenuAction {
        override val orderInCategory = 110
    }

    /**
     * Selection mode action.
     * All the actions which may be available in select mode should be defined here.
     */
    sealed interface SelectionModeAction : ChatRoomMenuAction {
    }

    companion object {
        /**
         * Test Tag Audio Call Action
         */
        const val TEST_TAG_AUDIO_CALL_ACTION = "chat_view:action_chat_audio_call"

        /**
         * Test Tag Video Call Action
         */
        const val TEST_TAG_VIDEO_CALL_ACTION = "chat_view:action_chat_video_call"

        /**
         * Test Tag Info Action
         */
        const val TEST_TAG_INFO_ACTION = "chat_view:action_chat_info"

        /**
         * Test Tag Add Participants Action
         */
        const val TEST_TAG_ADD_PARTICIPANTS_ACTION = "chat_view:action_add_participants"

        /**
         * Test Tag Unmute Action

         */
        const val TEST_TAG_UNMUTE_ACTION = "chat_view:action_unmute"

        /**
         * Test Tag Mute Action
         */
        const val TEST_TAG_MUTE_ACTION = "chat_view:action_mute"

        /**
         * Test Tag Clear Action
         */
        const val TEST_TAG_CLEAR_ACTION = "chat_view:action_chat_clear"

        /**
         * Test Tag Archive Action
         */
        const val TEST_TAG_ARCHIVE_ACTION = "chat_view:action_chat_archive"

        /**
         * Test Tag Unarchive Action
         */
        const val TEST_TAG_UNARCHIVE_ACTION = "chat_view:action_chat_unarchive"

        /**
         * Test Tag End Call For All Action
         */
        const val TEST_TAG_END_CALL_FOR_ALL_ACTION = "chat_view:action_end_call_for_all"

        /**
         * Test Tag Select Action
         */
        const val TEST_TAG_SELECT_ACTION = "chat_view:action_select"
    }
}