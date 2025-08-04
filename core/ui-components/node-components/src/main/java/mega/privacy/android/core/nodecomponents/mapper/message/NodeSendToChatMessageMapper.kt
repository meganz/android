package mega.privacy.android.core.nodecomponents.mapper.message

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.core.nodecomponents.R
import mega.privacy.android.domain.entity.node.ChatRequestResult
import javax.inject.Inject

/**
 * Chat request message mapper to map strings
 * @property context [android.content.Context]
 */
class NodeSendToChatMessageMapper @Inject constructor(
    @ApplicationContext val context: Context,
) {
    /**
     * invoke
     * @param request [mega.privacy.android.domain.entity.node.ChatRequestResult]
     * @return message after operation of chat
     */
    operator fun invoke(request: ChatRequestResult): String? =
        when (request) {
            is ChatRequestResult.ChatRequestAttachNode -> {
                when {
                    request.isAllRequestError -> {
                        context.getString(R.string.files_send_to_chat_error)
                    }

                    request.isSuccess -> {
                        context.resources.getQuantityString(
                            R.plurals.files_send_to_chat_success,
                            request.successCount
                        )
                    }

                    else -> {
                        null
                    }
                }
            }
        }
}