package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.listener.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.data.mapper.meeting.ChatCallMapper
import mega.privacy.android.data.mapper.ChatRequestMapper
import mega.privacy.android.data.model.ChatCallUpdate
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.CallRepository
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

internal class DefaultCallRepository @Inject constructor(
    private val megaChatApiGateway: MegaChatApiGateway,
    private val chatCallMapper: ChatCallMapper,
    private val chatRequestMapper: ChatRequestMapper,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : CallRepository {

    override suspend fun getChatCall(chatId: Long?): ChatCall? =
        withContext(dispatcher) {
            chatId?.let {
                megaChatApiGateway.getChatCall(chatId)?.let { call ->
                    return@withContext chatCallMapper(call)
                }
            }

            null
        }

    override suspend fun startCallRinging(
        chatId: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
    ): ChatRequest = withContext(dispatcher) {
        suspendCoroutine { continuation ->
            val callback = OptionalMegaChatRequestListenerInterface(
                onRequestFinish = onRequestCompleted(continuation)
            )

            megaChatApiGateway.startChatCall(
                chatId,
                enabledVideo,
                enabledAudio,
                callback
            )
        }
    }

    override suspend fun startCallNoRinging(
        chatId: Long,
        schedId: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
    ): ChatRequest = withContext(dispatcher) {
        suspendCoroutine { continuation ->
            val callback = OptionalMegaChatRequestListenerInterface(
                onRequestFinish = onRequestCompleted(continuation)
            )

            megaChatApiGateway.startChatCallNoRinging(
                chatId,
                schedId,
                enabledVideo,
                enabledAudio,
                callback
            )
        }
    }

    override suspend fun answerChatCall(
        chatId: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
    ): ChatRequest = withContext(dispatcher) {
        suspendCoroutine { continuation ->
            val callback = OptionalMegaChatRequestListenerInterface(
                onRequestFinish = onRequestCompleted(continuation)
            )

            megaChatApiGateway.answerChatCall(
                chatId,
                enabledVideo,
                enabledAudio,
                callback
            )
        }
    }

    override fun monitorChatCallUpdates(): Flow<ChatCall> =
        megaChatApiGateway.chatCallUpdates
            .filterIsInstance<ChatCallUpdate.OnChatCallUpdate>()
            .mapNotNull { it.item }
            .map { chatCallMapper(it) }
            .flowOn(dispatcher)

    private fun onRequestCompleted(continuation: Continuation<ChatRequest>) =
        { request: MegaChatRequest, error: MegaChatError ->
            if (error.errorCode == MegaChatError.ERROR_OK) {
                continuation.resumeWith(Result.success(chatRequestMapper(request)))
            } else {
                Timber.e("Error: ${error.errorString}")
                continuation.failWithError(error)
            }
        }
}