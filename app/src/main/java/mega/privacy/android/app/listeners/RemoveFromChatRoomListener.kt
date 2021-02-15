package mega.privacy.android.app.listeners

import android.content.Context
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.interfaces.ChatManagementCallback
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util.showSnackbar
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatError.ERROR_OK
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRequest.TYPE_REMOVE_FROM_CHATROOM

class RemoveFromChatRoomListener : ChatBaseListener {

    private var chatManagementCallback: ChatManagementCallback? = null

    constructor(context: Context) : super(context)

    constructor(
        context: Context,
        chatManagementCallback: ChatManagementCallback
    ) : super(context) {
        this.chatManagementCallback = chatManagementCallback
    }

    override fun onRequestStart(api: MegaChatApiJava, request: MegaChatRequest) {
        if (request.type != TYPE_REMOVE_FROM_CHATROOM) return

        MegaApplication.getChatManagement().addLeavingChatId(request.chatHandle)
    }

    override fun onRequestFinish(api: MegaChatApiJava, request: MegaChatRequest, e: MegaChatError) {
        if (request.type != TYPE_REMOVE_FROM_CHATROOM) return

        if (request.userHandle == INVALID_HANDLE) {
            MegaApplication.getChatManagement().removeLeavingChatId(request.chatHandle)
        }

        if (e.errorCode == ERROR_OK) {
            if (request.userHandle == INVALID_HANDLE) chatManagementCallback?.leaveChatSuccess()
        } else {
            showSnackbar(context, StringResourcesUtils.getTranslatedErrorString(e))
        }
    }
}