package mega.privacy.android.app.globalmanagement

import mega.privacy.android.app.components.ChatManagement
import javax.inject.Inject

class AutoJoinPublicChatHandler @Inject constructor(
    private val chatManagement: ChatManagement,
) {
    fun handleResponse(chatHandle: Long, userHandle: Long) {
        chatManagement.removeJoiningChatId(chatHandle)
        chatManagement.removeJoiningChatId(userHandle)
        chatManagement.broadcastJoinedSuccessfully()
    }

}