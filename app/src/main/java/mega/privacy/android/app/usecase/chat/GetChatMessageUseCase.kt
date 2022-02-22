package mega.privacy.android.app.usecase.chat

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.di.MegaApi
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
                megaApi.authorizeChatNode(node, chatRoom.authorizationToken)
            } else {
                node
            }
        }
}
