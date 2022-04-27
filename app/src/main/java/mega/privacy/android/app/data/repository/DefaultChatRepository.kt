package mega.privacy.android.app.data.repository

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import mega.privacy.android.app.domain.repository.ChatRepository
import mega.privacy.android.app.listeners.OptionalMegaChatRequestListenerInterface
import nz.mega.sdk.*
import javax.inject.Inject


/**
 * Default implementation of [ChatRepository]
 *
 * @property chatApi
 */
class DefaultChatRepository @Inject constructor(
    private val chatApi: MegaChatApiAndroid
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

            chatApi.addChatRequestListener(listener)

            awaitClose { chatApi.removeChatRequestListener(listener) }
        }
    }
}
