package mega.privacy.android.app.listeners

import mega.privacy.android.app.MegaApplication
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRequest.TYPE_CREATE_CHATROOM
import nz.mega.sdk.MegaError.API_OK

class CreateChatsListener(
    private val totalCount: Int,
    private val callback: (List<Long>, Int) -> Unit,
) : ChatBaseListener(MegaApplication.getInstance()) {

    private var successChats = ArrayList<Long>()
    private var failureCount = 0

    override fun onRequestFinish(api: MegaChatApiJava, request: MegaChatRequest, e: MegaChatError) {
        if (request.type == TYPE_CREATE_CHATROOM) {
            if (e.errorCode == API_OK) {
                successChats.add(request.chatHandle)
            } else {
                failureCount++
            }

            if (successChats.size + failureCount == totalCount) {
                callback(successChats, failureCount)
            }
        }
    }
}
