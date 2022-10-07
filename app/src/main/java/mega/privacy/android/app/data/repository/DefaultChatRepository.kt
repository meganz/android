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
import timber.log.Timber
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

    override suspend fun setOpenInvite(chatId: Long): Boolean =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                chatGateway.getChatRoom(chatId)?.let { chat ->
                    chatGateway.setOpenInvite(chatId,
                        !chat.isOpenInvite,
                        OptionalMegaChatRequestListenerInterface(
                            onRequestFinish = onRequestSetOpenInviteCompleted(continuation)
                        ))
                }
            }
        }

    private fun onRequestSetOpenInviteCompleted(continuation: Continuation<Boolean>) =
        { request: MegaChatRequest, error: MegaChatError ->
            if (error.errorCode == MegaChatError.ERROR_OK) {
                Timber.d("******************** set open invite OK -> state? ${request.flag}")
                continuation.resumeWith(Result.success(request.flag))
            } else {
                continuation.failWithError(error)
            }
        }
}