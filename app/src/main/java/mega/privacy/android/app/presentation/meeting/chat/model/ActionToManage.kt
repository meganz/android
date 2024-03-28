package mega.privacy.android.app.presentation.meeting.chat.model

/**
 * Sealed class defining all the possible actions to manage in view.
 */
sealed class ActionToManage {
    /**
     * Opens a Chat.
     *
     * @property chatId Chat id.
     */
    data class OpenChat(val chatId: Long) : ActionToManage()

    /**
     * Enables select mode.
     *
     */
    data object EnableSelectMode : ActionToManage()

    /**
     * Opens contact info.
     *
     * @property email Contact email.
     */
    data class OpenContactInfo(val email: String) : ActionToManage()

    /**
     * Close chat
     *
     */
    data object CloseChat : ActionToManage()
}
