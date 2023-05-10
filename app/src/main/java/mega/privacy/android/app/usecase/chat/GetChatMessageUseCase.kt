package mega.privacy.android.app.usecase.chat

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.usecase.exception.AttachmentDoesNotExistException
import mega.privacy.android.app.usecase.exception.ChatDoesNotExistException
import mega.privacy.android.app.usecase.exception.MessageDoesNotExistException
import mega.privacy.android.app.utils.RxUtil.blockingGetOrNull
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Main use case to retrieve Mega Chat Message information.
 *
 * @property megaChatApi    Mega Chat API needed to call node information.
 */
class GetChatMessageUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid
) {

    fun get(chatRoomId: Long, messageId: Long): Single<MegaChatMessage> =
        Single.fromCallable {
            megaChatApi.getMessage(chatRoomId, messageId)
                ?: megaChatApi.getMessageFromNodeHistory(chatRoomId, messageId)
        }

    fun getChatNode(chatRoomId: Long, messageId: Long): Single<MegaNode> =
        Single.fromCallable {
            val chatRoom = megaChatApi.getChatRoom(chatRoomId)
            val chatMessage = get(chatRoomId, messageId).blockingGet()
            val node = chatMessage.megaNodeList[0]
            if (chatRoom.isPreview) {
                megaApi.authorizeChatNode(node, chatRoom.authorizationToken) ?: node
            } else {
                node
            }
        }

    /**
     * Gets the attachment MegaNodes given a chat identifier and a message identifier.
     * Notes:
     *  * A MegaNode attached to a chat cannot be get from its handle and must be get from
     *  the MegaChatMessage.
     *  * A MegaChatMessage can contain more than one attached MegaNode.
     *
     * @param chatRoomId    Chat identifier.
     * @param messageId     Message identifier.
     * @return Single with the list of attached MegaNodes.
     */
    fun getChatNodes(chatRoomId: Long, messageId: Long): Single<MutableList<MegaNode>> =
        Single.create { emitter ->
            val chat = megaChatApi.getChatRoom(chatRoomId)

            if (chat == null) {
                emitter.onError(ChatDoesNotExistException())
                return@create
            }

            val message = get(chatRoomId, messageId).blockingGetOrNull()

            if (message == null) {
                emitter.onError(MessageDoesNotExistException())
                return@create
            }

            val attachments = message.megaNodeList

            if (attachments == null || attachments.size() <= 0) {
                emitter.onError(AttachmentDoesNotExistException())
                return@create
            }

            val nodes = mutableListOf<MegaNode>()

            for (i in 0 until attachments.size()) {
                nodes.add(
                    if (chat.isPreview) {
                        megaApi.authorizeChatNode(attachments.get(i), chat.authorizationToken)
                            ?: attachments.get(i)
                    } else {
                        attachments.get(i)
                    }
                )
            }

            when {
                emitter.isDisposed -> return@create
                else -> emitter.onSuccess(nodes)
            }
        }
}
