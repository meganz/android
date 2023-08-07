package mega.privacy.android.app.listeners

import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.interfaces.AttachNodeToChatListener
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.interfaces.showSnackbarWithChat
import mega.privacy.android.data.model.chat.AndroidMegaChatMessage
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRequest.TYPE_ATTACH_NODE_MESSAGE
import nz.mega.sdk.MegaError.API_OK


class AttachNodesListener(
    private val totalCount: Int,
    private val snackbarChatId: Long,
    private val snackbarShower: SnackbarShower,
    private val forceNonChatSnackbar: Boolean,
    private val attachNodeToChatListener: AttachNodeToChatListener?,
    private val onFinish: () -> Unit,
) : ChatBaseListener(MegaApplication.getInstance()) {

    private var successCount = 0
    private var failureCount = 0

    override fun onRequestFinish(api: MegaChatApiJava, request: MegaChatRequest, e: MegaChatError) {
        if (request.type == TYPE_ATTACH_NODE_MESSAGE) {
            if (e.errorCode == API_OK) {
                successCount++

                attachNodeToChatListener?.onSendSuccess(
                    AndroidMegaChatMessage(request.megaChatMessage)
                )
            } else {
                failureCount++
            }

            if (successCount + failureCount == totalCount) {

                if (successCount > 0) {
                    if (forceNonChatSnackbar) {
                        snackbarShower.showSnackbar(
                            context.resources.getQuantityString(
                                R.plurals.files_send_to_chat_success,
                                successCount
                            )
                        )
                    } else {
                        snackbarShower.showSnackbarWithChat(null, snackbarChatId)
                    }
                } else {
                    snackbarShower.showSnackbar(context.getString(R.string.files_send_to_chat_error))
                }

                onFinish()
            }
        }
    }
}
