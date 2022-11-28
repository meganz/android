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
import mega.privacy.android.data.mapper.ChatListItemMapper
import mega.privacy.android.data.mapper.ChatRequestMapper
import mega.privacy.android.data.mapper.ChatRoomMapper
import mega.privacy.android.data.mapper.ChatScheduledMeetingMapper
import mega.privacy.android.data.mapper.ChatScheduledMeetingOccurrMapper
import mega.privacy.android.data.model.ChatRoomUpdate
import mega.privacy.android.data.model.ChatUpdate
import mega.privacy.android.data.model.ScheduledMeetingUpdate
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
 * @property chatListItemMapper     [ChatListItemMapper]
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
    private val chatListItemMapper: ChatListItemMapper,
) : ChatRepository {

    override fun notifyChatLogout(): Flow<Boolean> =
        callbackFlow {
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
                        onRequestFinish = onRequestCompleted(continuation)
                    ))
            }
        }

    override suspend fun answerChatCall(
        chatId: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
        enabledSpeaker: Boolean,
    ): ChatRequest =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaChatApiGateway.answerChatCall(chatId,
                    enabledVideo,
                    enabledAudio,
                    OptionalMegaChatRequestListenerInterface(
                        onRequestFinish = onRequestCompleted(continuation)
                    ))
            }
        }

    override suspend fun leaveChat(chatId: Long): ChatRequest =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaChatApiGateway.leaveChat(chatId,
                    OptionalMegaChatRequestListenerInterface(
                        onRequestFinish = onRequestCompleted(continuation)
                    ))
            }
        }

    private fun onRequestCompleted(continuation: Continuation<ChatRequest>) =
        { request: MegaChatRequest, error: MegaChatError ->
            if (error.errorCode == MegaChatError.ERROR_OK) {
                continuation.resumeWith(Result.success(chatRequestMapper(request)))
            } else {
                continuation.failWithError(error)
            }
        }

    override suspend fun getChatFilesFolderId(): NodeId? =
        localStorageGateway.getChatFilesFolderHandle()?.let { NodeId(it) }

    override fun monitorChatRoomUpdates(chatId: Long): Flow<ChatRoom> =
        megaChatApiGateway.getChatRoomUpdates(chatId)
            .filterIsInstance<ChatRoomUpdate.OnChatRoomUpdate>()
            .mapNotNull { it.chat }
            .map(chatRoomMapper)
            .flowOn(ioDispatcher)

    override suspend fun getMeetingChatRooms(): List<ChatRoom>? =
        withContext(ioDispatcher) {
            megaChatApiGateway.getMeetingChatRooms()?.map(chatRoomMapper)
        }

    override suspend fun getChatRoom(chatId: Long): ChatRoom? =
        withContext(ioDispatcher) {
            megaChatApiGateway.getChatRoom(chatId)?.let(chatRoomMapper)
        }

    override fun monitorScheduledMeetingsUpdates(): Flow<ChatScheduledMeeting> =
        megaChatApiGateway.scheduledMeetingUpdates
            .filterIsInstance<ScheduledMeetingUpdate.OnChatSchedMeetingUpdate>()
            .mapNotNull { it.item }
            .map(chatScheduledMeetingMapper)
            .flowOn(ioDispatcher)

    override fun monitorScheduledMeetingOccurrencesUpdates(): Flow<Long> =
        megaChatApiGateway.scheduledMeetingUpdates
            .filterIsInstance<ScheduledMeetingUpdate.OnSchedMeetingOccurrencesUpdate>()
            .mapNotNull { it.chatId }
            .flowOn(ioDispatcher)

    override suspend fun getAllScheduledMeetings(): List<ChatScheduledMeeting>? =
        withContext(ioDispatcher) {
            megaChatApiGateway.getAllScheduledMeetings()?.map(chatScheduledMeetingMapper)
        }

    override suspend fun getScheduledMeeting(chatId: Long, schedId: Long): ChatScheduledMeeting? =
        withContext(ioDispatcher) {
            megaChatApiGateway.getScheduledMeeting(chatId, schedId)?.let(chatScheduledMeetingMapper)
        }

    override suspend fun getScheduledMeetingsByChat(chatId: Long): List<ChatScheduledMeeting> =
        withContext(ioDispatcher) {
            val newList = ArrayList<ChatScheduledMeeting>()

            megaChatApiGateway.getScheduledMeetingsByChat(chatId)?.let { listRecived ->
                listRecived.forEach { schedMeet ->
                    val item: ChatScheduledMeeting = chatScheduledMeetingMapper(schedMeet)
                    newList.add(item)
                }
            }

            return@withContext newList
        }

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

    override suspend fun setPublicChatToPrivate(chatId: Long): ChatRequest =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaChatApiGateway.setPublicChatToPrivate(chatId,
                    OptionalMegaChatRequestListenerInterface(
                        onRequestFinish = onRequestCompleted(continuation)
                    ))
            }
        }

    override suspend fun queryChatLink(chatId: Long): ChatRequest =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaChatApiGateway.queryChatLink(chatId,
                    OptionalMegaChatRequestListenerInterface(
                        onRequestFinish = onRequestCompleted(continuation)
                    ))
            }
        }

    override suspend fun removeChatLink(chatId: Long): ChatRequest =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaChatApiGateway.removeChatLink(chatId,
                    OptionalMegaChatRequestListenerInterface(
                        onRequestFinish = onRequestCompleted(continuation)
                    ))
            }
        }

    override fun monitorChatListItemUpdates() =
        megaChatApiGateway.chatUpdates
            .filterIsInstance<ChatUpdate.OnChatListItemUpdate>()
            .mapNotNull { it.item }
            .map(chatListItemMapper)
            .flowOn(ioDispatcher)
}
