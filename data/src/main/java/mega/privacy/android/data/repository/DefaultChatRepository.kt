package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.listener.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.data.mapper.ChatRequestMapper
import mega.privacy.android.data.mapper.ChatRoomMapper
import mega.privacy.android.data.mapper.ChatScheduledMeetingMapper
import mega.privacy.android.data.mapper.ChatScheduledMeetingOccurrMapper
import mega.privacy.android.data.model.ChatRoomUpdate
import mega.privacy.android.data.model.ScheduleMeetingUpdate
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.entity.node.NodeId
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
 * @property megaChatApiGateway     [MegaChatApiGateway]
 * @property megaApiGateway         [MegaApiGateway]
 * @property ioDispatcher           [CoroutineDispatcher]
 * @property chatRequestMapper      [ChatRequestMapper]
 * @property localStorageGateway    [MegaLocalStorageGateway]
 * @property chatRoomMapper         [ChatRoomMapper]
 */
internal class DefaultChatRepository @Inject constructor(
    private val megaChatApiGateway: MegaChatApiGateway,
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val chatRequestMapper: ChatRequestMapper,
    private val localStorageGateway: MegaLocalStorageGateway,
    private val chatRoomMapper: ChatRoomMapper,
    private val chatScheduledMeetingMapper: ChatScheduledMeetingMapper,
    private val chatScheduledMeetingOccurrMapper: ChatScheduledMeetingOccurrMapper,
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

            megaChatApiGateway.addChatRequestListener(listener)

            awaitClose { megaChatApiGateway.removeChatRequestListener(listener) }
        }
    }

    override suspend fun setOpenInvite(chatId: Long): Boolean =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaChatApiGateway.getChatRoom(chatId)?.let { chat ->
                    megaChatApiGateway.setOpenInvite(chatId,
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
                continuation.resumeWith(Result.success(request.flag))
            } else {
                continuation.failWithError(error)
            }
        }


    override suspend fun startChatCall(chatId: Long, enabledVideo: Boolean, enabledAudio: Boolean) =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaChatApiGateway.startChatCall(chatId,
                    enabledVideo,
                    enabledAudio,
                    OptionalMegaChatRequestListenerInterface(
                        onRequestFinish = onRequestChatCallCompleted(continuation)
                    ))
            }
        }

    override suspend fun answerChatCall(
        chatId: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
        enabledSpeaker: Boolean,
    ): ChatRequest = withContext(ioDispatcher) {
        suspendCoroutine { continuation ->
            megaChatApiGateway.answerChatCall(chatId,
                enabledVideo,
                enabledAudio,
                OptionalMegaChatRequestListenerInterface(
                    onRequestFinish = onRequestChatCallCompleted(continuation)
                ))
        }
    }

    private fun onRequestChatCallCompleted(continuation: Continuation<ChatRequest>) =
        { request: MegaChatRequest, error: MegaChatError ->
            if (error.errorCode == MegaChatError.ERROR_OK) {
                continuation.resumeWith(Result.success(chatRequestMapper(request)))
            } else {
                continuation.failWithError(error)
            }
        }

    override suspend fun getChatFilesFolderId(): NodeId? =
        localStorageGateway.getChatFilesFolderHandle()?.let { NodeId(it) }

    override fun monitorChatRoomUpdates(chatId: Long) =
        megaChatApiGateway.getChatRoomUpdates(chatId)
            .filterIsInstance<ChatRoomUpdate.OnChatRoomUpdate>()
            .mapNotNull { it.chat }
            .map { chatRoomMapper(it) }
            .flowOn(ioDispatcher)

    override suspend fun getChatRoom(chatId: Long): ChatRoom? =
        withContext(ioDispatcher) {
            megaChatApiGateway.getChatRoom(chatId)?.let {
                chatRoomMapper(it)
            }
        }

    override fun monitorScheduledMeetingsUpdates(): Flow<ChatScheduledMeeting> =
        megaChatApiGateway.scheduledMeetingUpdates
            .filterIsInstance<ScheduleMeetingUpdate.OnChatSchedMeetingUpdate>()
            .mapNotNull { it.item }
            .map(chatScheduledMeetingMapper)
            .flowOn(ioDispatcher)

    override fun monitorScheduledMeetingOccurrencesUpdates(): Flow<Long> =
        megaChatApiGateway.scheduledMeetingUpdates
            .filterIsInstance<ScheduleMeetingUpdate.OnSchedMeetingOccurrencesUpdate>()
            .mapNotNull { it.chatId }
            .flowOn(ioDispatcher)

    override fun getScheduledMeeting(chatId: Long, schedId: Long): ChatScheduledMeeting? =
        megaChatApiGateway.getScheduledMeeting(chatId, schedId)?.let(chatScheduledMeetingMapper)

    override fun getScheduledMeetingsByChat(chatId: Long): List<ChatScheduledMeeting>? =
        megaChatApiGateway.getScheduledMeetingsByChat(chatId)?.map(chatScheduledMeetingMapper)

    override suspend fun fetchScheduledMeetingOccurrencesByChat(chatId: Long): List<ChatScheduledMeetingOccurr>? =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaChatApiGateway.fetchScheduledMeetingOccurrencesByChat(
                    chatId,
                    OptionalMegaChatRequestListenerInterface(
                        onRequestFinish = { request: MegaChatRequest, error: MegaChatError ->
                            if (error.errorCode == MegaChatError.ERROR_OK) {
                                val occurrences = mutableListOf<ChatScheduledMeetingOccurr>()
                                request.megaChatScheduledMeetingOccurrList.apply {
                                    for (i in 0..size()) {
                                        occurrences.add(chatScheduledMeetingOccurrMapper(at(i)))
                                    }
                                }
                                continuation.resumeWith(Result.success(occurrences))
                            } else {
                                continuation.failWithError(error)
                            }
                        }
                    ))
            }
        }

    override suspend fun inviteToChat(chatId: Long, contactsData: List<String>) =
        withContext(ioDispatcher) {
            contactsData.forEach { email ->
                val userHandle = megaApiGateway.getContact(email)?.handle ?: -1
                megaChatApiGateway.inviteToChat(chatId, userHandle, null)
            }
        }
}
