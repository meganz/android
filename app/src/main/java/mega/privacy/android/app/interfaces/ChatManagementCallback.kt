package mega.privacy.android.app.interfaces

import nz.mega.sdk.MegaChatListItem

interface ChatManagementCallback {

    /**
     * Confirms the action to leave a chat. Should launch the leave request.
     *
     * @param chatId Identifier of the chat to leave.
     */
    fun confirmLeaveChat(chatId: Long)

    /**
     * Confirms the action to leave several chats. Should launch the leave requests.
     *
     * @param chats List of chats to leave.
     */
    fun confirmLeaveChats(chats: List<MegaChatListItem>)

    /**
     * Confirms the leave chat action finished successfully.
     */
    fun leaveChatSuccess()
}