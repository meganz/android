package mega.privacy.android.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.extensions.failWithError
import mega.privacy.android.app.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.app.listeners.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.ChatRepository
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaError
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine


/**
 * Default implementation of [ChatRepository]
 *
 * @property chatGateway
 */
class DefaultChatRepository @Inject constructor(
    private val chatGateway: MegaChatApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
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

    override suspend fun setOpenInvite(chatId: Long, enabled: Boolean): Long =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                chatGateway.setOpenInvite(chatId,
                    enabled,
                    OptionalMegaChatRequestListenerInterface(
                        onRequestFinish = onRequestCreateChatCompleted(continuation)
                    ))
            }
        }

    private fun onRequestCreateChatCompleted(continuation: Continuation<Long>) =
        { request: MegaChatRequest, error: MegaChatError ->
            if (error.errorCode == MegaChatError.ERROR_OK) {
                continuation.resumeWith(Result.success(request.chatHandle))
            } else {
                continuation.failWithError(error)
            }
        }
}