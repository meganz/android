package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.chat.ChatAvatarItem
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.app.presentation.meeting.RingingViewModel
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.chat.ChatConnectionStatus

/**
 * Data class defining the state of [RingingViewModel]+
 *
 * @property chatId                                 Chat id.
 * @property chat                                   [ChatRoom]
 * @property call                                   [ChatCall]
 * @property avatar                                 [ChatAvatarItem]
 * @property callAnsweredInAnotherClient            True, if call was answered in another client. False, if not.
 * @property myUserHandle                           My user handle
 * @property finish                                 True, if should finish. False, if not.
 * @property showSnackbar                           True, if should show snackbar. False, if not.
 * @property isAnswerWithAudioClicked               True, if audio button was clicked. False, if not.
 * @property isAnswerWithVideoClicked               True, if video button was clicked. False, if not.
 * @property isCallAnsweredAndWaitingForCallInfo    True, If the call has been answered and I am waiting to receive the information from the call.. False, is in the ringing call screen.
 * @property chatConnectionStatus                   [ChatConnectionStatus]
 * @property showMissedCallNotification             True, show missed call notification. False, if not.
 **/
data class RingingUIState(
    val chatId: Long = -1L,
    val chat: ChatRoom? = null,
    val call: ChatCall? = null,
    val avatar: ChatAvatarItem? = null,
    val callAnsweredInAnotherClient: Boolean = false,
    val myUserHandle: Long? = null,
    val isAnswerWithAudioClicked: Boolean = false,
    val isAnswerWithVideoClicked: Boolean = false,
    val showSnackbar: Boolean = false,
    val isCallAnsweredAndWaitingForCallInfo: Boolean = false,
    val finish: Boolean = false,
    val chatConnectionStatus: ChatConnectionStatus? = null,
    val showMissedCallNotification: Boolean = false
) {
    /**
     * Get title
     */
    val getTitle
        get():String? = chat?.title

    /**
     * Check if should be finish
     */
    val shouldFinish
        get():Boolean = call?.status == ChatCallStatus.Destroyed || callAnsweredInAnotherClient || finish

    /**
     * Check if is one to one call
     */
    val isOneToOneCall
        get():Boolean = chat?.let {
            !it.isGroup && !it.isMeeting
        } ?: false

}
