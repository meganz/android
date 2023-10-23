package mega.privacy.android.app.presentation.meeting.chat.model

import mega.privacy.android.app.R
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionString

/**
 * Chat room menu action.
 * All the actions which may be available in normal mode should be defined here.
 *
 * @constructor Create empty Chat room menu action
 */
sealed interface ChatRoomMenuAction : MenuAction {

    class AudioCall(override val enabled: Boolean) : MenuActionString(
        iconRes = R.drawable.ic_phone,
        descriptionRes = R.string.call_button,
        testTag = TEST_TAG_AUDIO_CALL_ACTION,
    ), ChatRoomMenuAction {
        override val orderInCategory = 100
    }

    class VideoCall(override val enabled: Boolean) : MenuActionString(
        iconRes = R.drawable.ic_video_action,
        descriptionRes = R.string.video_button,
        testTag = TEST_TAG_VIDEO_CALL_ACTION,
    ), ChatRoomMenuAction {
        override val orderInCategory = 100
    }

    /**
     * Selection mode action.
     * All the actions which may be available in select mode should be defined here.

     */
    sealed interface SelectionModeAction : ChatRoomMenuAction {
    }

    companion object {
        const val TEST_TAG_AUDIO_CALL_ACTION = "chat_view:action_chat_audio_call"
        const val TEST_TAG_VIDEO_CALL_ACTION = "chat_view:action_chat_video_call"
    }
}