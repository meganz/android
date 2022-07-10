package mega.privacy.android.app.data.repository

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import mega.privacy.android.app.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.app.listeners.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.domain.repository.ChatRepository
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaError
import javax.inject.Inject


/**
 * Default implementation of [ChatRepository]
 *
 * @property chatGateway
 */
class DefaultChatRepository @Inject constructor(
    private val chatGateway: MegaChatApiGateway,
) : ChatRepository {
    override fun notifyChatLogout(): Flow<Boolean> {
        return callbackFlow {
            val listener = OptionalMegaChatRequestListenerInterface(
                onRequestFinish = { request, e ->
                    if (request.type == MegaChatRequest.TYPE_LOGOUT) {
                        if (e.errorCode == MegaError.API_OK) {
                            trySend(true)
                        }
                    }
                }
            )

            chatGateway.addChatRequestListener(listener)

            awaitClose { chatGateway.removeChatRequestListener(listener) }
        }
    }
}
