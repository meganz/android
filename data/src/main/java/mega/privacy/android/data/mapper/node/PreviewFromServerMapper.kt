package mega.privacy.android.data.mapper.node

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.domain.exception.FetchChatMegaNodeException
import mega.privacy.android.domain.qualifier.IoDispatcher
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Mapper from mega node to method to get preview from server
 */
internal class PreviewFromServerMapper @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaApiGateway: MegaApiGateway,
    private val megaChatApiGateway: MegaChatApiGateway,
    private val megaNodeFromChatMessageMapper: MegaNodeFromChatMessageMapper,
) {
    /**
     * Mapper from mega node to method to get preview from server
     * Should only be used for non-chat images
     * @param megaNode [MegaNode]
     * @return a suspend block to be executed to get preview from server
     */
    operator fun invoke(
        megaNode: MegaNode,
    ): suspend (String) -> String {
        return { previewPath ->
            withContext(ioDispatcher) {
                suspendCancellableCoroutine { continuation ->
                    val listener =
                        continuation.getRequestListener("PreviewFromServerMapper") { previewPath }
                    megaApiGateway.getPreview(megaNode, previewPath, listener)
                }
            }
        }
    }

    /**
     * Mapper from chat message to method to get preview from server
     * This version delays fetching the MegaNode until download is actually needed,
     * Avoiding memory issues where GC-released chat messages cause MegaNode to become a dangling pointer.
     * @param chatId Chat room ID
     * @param messageId Message ID
     * @return a suspend block to be executed to get preview from server
     */
    operator fun invoke(
        chatId: Long,
        messageId: Long,
    ): suspend (String) -> String {
        return { previewPath ->
            withContext(ioDispatcher) {
                // Re-fetch the MegaNode when download is actually needed
                val megaNode = megaNodeFromChatMessageMapper(chatId, messageId)
                    ?: throw FetchChatMegaNodeException(chatId = chatId, messageId = messageId)

                suspendCancellableCoroutine { continuation ->
                    val listener =
                        continuation.getRequestListener("PreviewFromServerMapper") { previewPath }
                    megaApiGateway.getPreview(megaNode, previewPath, listener)
                }
            }
        }
    }
}
