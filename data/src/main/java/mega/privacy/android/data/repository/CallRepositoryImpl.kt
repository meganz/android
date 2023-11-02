package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getChatRequestListener
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.listener.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.data.mapper.chat.ChatRequestMapper
import mega.privacy.android.data.mapper.chat.MegaChatPeerListMapper
import mega.privacy.android.data.mapper.handles.HandleListMapper
import mega.privacy.android.data.mapper.handles.MegaHandleListMapper
import mega.privacy.android.data.mapper.meeting.ChatCallMapper
import mega.privacy.android.data.mapper.meeting.ChatScheduledMeetingMapper
import mega.privacy.android.data.mapper.meeting.ChatScheduledMeetingOccurrMapper
import mega.privacy.android.data.mapper.meeting.ChatSessionMapper
import mega.privacy.android.data.mapper.meeting.MegaChatCallStatusMapper
import mega.privacy.android.data.mapper.meeting.MegaChatScheduledMeetingFlagsMapper
import mega.privacy.android.data.mapper.meeting.MegaChatScheduledMeetingRulesMapper
import mega.privacy.android.data.model.ScheduledMeetingUpdate
import mega.privacy.android.data.model.meeting.ChatCallUpdate
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.chat.ChatScheduledFlags
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.entity.chat.ChatScheduledRules
import mega.privacy.android.domain.entity.chat.ChatVideoUpdate
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.entity.meeting.ChatSession
import mega.privacy.android.domain.entity.meeting.ResultOccurrenceUpdate
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.CallRepository
import nz.mega.sdk.MegaChatError
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Default implementation of [CallRepository]
 *
 * @property megaChatApiGateway                     [MegaChatApiGateway]
 * @property chatCallMapper                         [ChatCallMapper]
 * @property chatSessionMapper                      [ChatSessionMapper]
 * @property chatRequestMapper                      [ChatRequestMapper]
 * @property chatScheduledMeetingMapper             [ChatScheduledMeetingMapper]
 * @property chatScheduledMeetingOccurrMapper       [ChatScheduledMeetingOccurrMapper]
 * @property megaChatCallStatusMapper               [MegaChatCallStatusMapper]
 * @property handleListMapper                       [HandleListMapper]
 * @property megaChatScheduledMeetingFlagsMapper    [MegaChatScheduledMeetingFlagsMapper]
 * @property megaChatScheduledMeetingRulesMapper    [MegaChatScheduledMeetingRulesMapper]
 * @property megaChatPeerListMapper                 [MegaChatPeerListMapper]
 * @property appEventGateway                        [AppEventGateway]
 * @property dispatcher                             [CoroutineDispatcher]
 */
internal class CallRepositoryImpl @Inject constructor(
    private val megaChatApiGateway: MegaChatApiGateway,
    private val chatCallMapper: ChatCallMapper,
    private val chatSessionMapper: ChatSessionMapper,
    private val chatRequestMapper: ChatRequestMapper,
    private val chatScheduledMeetingMapper: ChatScheduledMeetingMapper,
    private val chatScheduledMeetingOccurrMapper: ChatScheduledMeetingOccurrMapper,
    private val megaChatCallStatusMapper: MegaChatCallStatusMapper,
    private val handleListMapper: HandleListMapper,
    private val megaHandleListMapper: MegaHandleListMapper,
    private val megaChatScheduledMeetingFlagsMapper: MegaChatScheduledMeetingFlagsMapper,
    private val megaChatScheduledMeetingRulesMapper: MegaChatScheduledMeetingRulesMapper,
    private val megaChatPeerListMapper: MegaChatPeerListMapper,
    private val appEventGateway: AppEventGateway,
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

    override suspend fun getChatCallByCallId(callId: Long): ChatCall? =
        withContext(dispatcher) {
            megaChatApiGateway.getChatCallByCallId(callId)?.let(chatCallMapper::invoke)
        }

    override suspend fun startCallRinging(
        chatId: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
    ): ChatRequest = withContext(dispatcher) {
        suspendCancellableCoroutine { continuation ->
            val callback = continuation.getChatRequestListener(
                methodName = "startCallRinging",
                chatRequestMapper::invoke
            )

            megaChatApiGateway.startChatCall(
                chatId,
                enabledVideo,
                enabledAudio,
                callback
            )

            continuation.invokeOnCancellation { megaChatApiGateway.removeRequestListener(callback) }
        }
    }

    override suspend fun startCallNoRinging(
        chatId: Long,
        schedId: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
    ): ChatRequest = withContext(dispatcher) {
        suspendCancellableCoroutine { continuation ->
            val callback = continuation.getChatRequestListener(
                methodName = "startCallNoRinging",
                chatRequestMapper::invoke
            )

            megaChatApiGateway.startChatCallNoRinging(
                chatId,
                schedId,
                enabledVideo,
                enabledAudio,
                callback
            )

            continuation.invokeOnCancellation { megaChatApiGateway.removeRequestListener(callback) }
        }
    }

    override suspend fun startMeetingInWaitingRoomChat(
        chatId: Long,
        schedIdWr: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
    ): ChatRequest = withContext(dispatcher) {
        suspendCancellableCoroutine { continuation ->
            val callback = continuation.getChatRequestListener(
                methodName = "startMeetingInWaitingRoomChat",
                chatRequestMapper::invoke
            )

            megaChatApiGateway.startMeetingInWaitingRoomChat(
                chatId,
                schedIdWr,
                enabledVideo,
                enabledAudio,
                callback
            )

            continuation.invokeOnCancellation { megaChatApiGateway.removeRequestListener(callback) }
        }
    }

    override suspend fun answerChatCall(
        chatId: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
    ): ChatRequest = withContext(dispatcher) {
        suspendCancellableCoroutine { continuation ->
            val callback = continuation.getChatRequestListener(
                methodName = "answerChatCall",
                chatRequestMapper::invoke
            )

            megaChatApiGateway.answerChatCall(
                chatId,
                enabledVideo,
                enabledAudio,
                callback
            )

            continuation.invokeOnCancellation { megaChatApiGateway.removeRequestListener(callback) }
        }
    }

    override suspend fun hangChatCall(
        callId: Long,
    ): ChatRequest = withContext(dispatcher) {
        suspendCancellableCoroutine { continuation ->
            val callback = continuation.getChatRequestListener(
                methodName = "hangChatCall",
                chatRequestMapper::invoke
            )

            megaChatApiGateway.hangChatCall(
                callId, callback
            )

            continuation.invokeOnCancellation { megaChatApiGateway.removeRequestListener(callback) }
        }
    }

    override suspend fun holdChatCall(
        chatId: Long,
        setOnHold: Boolean,
    ): ChatRequest = withContext(dispatcher) {
        suspendCancellableCoroutine { continuation ->
            val callback = continuation.getChatRequestListener(
                methodName = "holdChatCall",
                chatRequestMapper::invoke
            )

            megaChatApiGateway.holdChatCall(
                chatId,
                setOnHold,
                callback
            )

            continuation.invokeOnCancellation { megaChatApiGateway.removeRequestListener(callback) }
        }
    }

    override suspend fun getAllScheduledMeetings(): List<ChatScheduledMeeting>? =
        withContext(dispatcher) {
            megaChatApiGateway.getAllScheduledMeetings()?.map { chatScheduledMeetingMapper(it) }
        }

    override suspend fun getScheduledMeeting(
        chatId: Long,
        scheduledMeetingId: Long,
    ): ChatScheduledMeeting? =
        withContext(dispatcher) {
            megaChatApiGateway.getScheduledMeeting(chatId, scheduledMeetingId)
                ?.let { chatScheduledMeetingMapper(it) }
        }

    override suspend fun getScheduledMeetingsByChat(chatId: Long): List<ChatScheduledMeeting>? =
        withContext(dispatcher) {
            megaChatApiGateway.getScheduledMeetingsByChat(chatId)
                ?.filter { it.parentSchedId() == megaChatApiGateway.getChatInvalidHandle() }
                ?.map { chatScheduledMeetingMapper(it) }
        }

    override suspend fun fetchScheduledMeetingOccurrencesByChat(
        chatId: Long,
        since: Long,
    ): List<ChatScheduledMeetingOccurr> =
        withContext(dispatcher) {
            suspendCancellableCoroutine { continuation ->
                val callback = continuation.getChatRequestListener(
                    methodName = "fetchScheduledMeetingOccurrencesByChat"
                ) { request ->
                    val occurrences = mutableListOf<ChatScheduledMeetingOccurr>()
                    request.megaChatScheduledMeetingOccurrList?.let { occursList ->
                        if (occursList.size() > 0) {
                            for (i in 0 until occursList.size()) {
                                occurrences.add(
                                    chatScheduledMeetingOccurrMapper(occursList.at(i))
                                )
                            }
                        }
                    }
                    occurrences
                }

                megaChatApiGateway.fetchScheduledMeetingOccurrencesByChat(
                    chatId,
                    since,
                    callback
                )

                continuation.invokeOnCancellation {
                    megaChatApiGateway.removeRequestListener(callback)
                }
            }
        }

    override suspend fun fetchScheduledMeetingOccurrencesByChat(
        chatId: Long,
        count: Int,
    ): List<ChatScheduledMeetingOccurr> =
        withContext(dispatcher) {
            val occurrences = mutableListOf<ChatScheduledMeetingOccurr>()
            var lastTimeStamp = 0L
            var fetch: Boolean

            do {
                val newOccurrences = fetchScheduledMeetingOccurrencesByChat(chatId, lastTimeStamp)
                if (newOccurrences.isNotEmpty()) {
                    occurrences.apply {
                        addAll(newOccurrences)
                        sortBy(ChatScheduledMeetingOccurr::startDateTime)
                    }
                    lastTimeStamp = newOccurrences.last().startDateTime!!
                    fetch = occurrences.size < count
                } else {
                    fetch = false
                }
            } while (fetch)

            occurrences.toList()
        }

    override suspend fun createChatroomAndSchedMeeting(
        peerList: List<Long>,
        isMeeting: Boolean,
        publicChat: Boolean,
        title: String,
        speakRequest: Boolean,
        waitingRoom: Boolean,
        openInvite: Boolean,
        timezone: String,
        startDate: Long,
        endDate: Long,
        description: String,
        flags: ChatScheduledFlags?,
        rules: ChatScheduledRules?,
        attributes: String?,
    ): ChatRequest = withContext(dispatcher) {
        suspendCancellableCoroutine { continuation ->
            val callback = continuation.getChatRequestListener(
                methodName = "createChatroomAndSchedMeeting",
                chatRequestMapper::invoke
            )

            megaChatApiGateway.createChatroomAndSchedMeeting(
                megaChatPeerListMapper(peerList),
                isMeeting,
                publicChat,
                title,
                speakRequest,
                waitingRoom,
                openInvite,
                timezone,
                startDate,
                endDate,
                description,
                megaChatScheduledMeetingFlagsMapper(flags),
                megaChatScheduledMeetingRulesMapper(rules),
                attributes,
                callback
            )

            continuation.invokeOnCancellation { megaChatApiGateway.removeRequestListener(callback) }
        }
    }

    override suspend fun updateScheduledMeeting(
        chatId: Long,
        schedId: Long,
        timezone: String,
        startDate: Long,
        endDate: Long,
        title: String,
        description: String,
        cancelled: Boolean,
        flags: ChatScheduledFlags?,
        rules: ChatScheduledRules?,
        updateChatTitle: Boolean
    ): ChatRequest = withContext(dispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getChatRequestListener(
                methodName = "updateScheduledMeeting",
                chatRequestMapper::invoke
            )

            megaChatApiGateway.updateScheduledMeeting(
                chatId,
                schedId,
                timezone,
                startDate,
                endDate,
                title,
                description,
                cancelled,
                megaChatScheduledMeetingFlagsMapper(flags),
                megaChatScheduledMeetingRulesMapper(rules),
                updateChatTitle,
                listener
            )

            continuation.invokeOnCancellation { megaChatApiGateway.removeRequestListener(listener) }
        }
    }

    override suspend fun updateScheduledMeetingOccurrence(
        chatId: Long,
        schedId: Long,
        overrides: Long,
        newStartDate: Long,
        newEndDate: Long,
        cancelled: Boolean,
    ): ChatRequest = withContext(dispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getChatRequestListener(
                methodName = "updateScheduledMeetingOccurrence",
                chatRequestMapper::invoke
            )

            megaChatApiGateway.updateScheduledMeetingOccurrence(
                chatId = chatId,
                schedId = schedId,
                overrides = overrides,
                newStartDate = newStartDate,
                newEndDate = newEndDate,
                cancelled = cancelled,
                listener = listener
            )

            continuation.invokeOnCancellation { megaChatApiGateway.removeRequestListener(listener) }
        }
    }

    override fun monitorChatCallUpdates(): Flow<ChatCall> = megaChatApiGateway.chatCallUpdates
        .filterIsInstance<ChatCallUpdate.OnChatCallUpdate>()
        .mapNotNull { it.item }
        .map { chatCallMapper(it) }
        .flowOn(dispatcher)

    override fun monitorChatSessionUpdates(): Flow<ChatSession> = megaChatApiGateway.chatCallUpdates
        .filterIsInstance<ChatCallUpdate.OnChatSessionUpdate>()
        .mapNotNull { it.session }
        .map { chatSessionMapper(it) }
        .flowOn(dispatcher)

    override fun monitorScheduledMeetingUpdates(): Flow<ChatScheduledMeeting> =
        megaChatApiGateway.scheduledMeetingUpdates
            .filterIsInstance<ScheduledMeetingUpdate.OnChatSchedMeetingUpdate>()
            .mapNotNull { it.item }
            .map { chatScheduledMeetingMapper(it) }
            .flowOn(dispatcher)


    override fun monitorScheduledMeetingOccurrencesUpdates(): Flow<ResultOccurrenceUpdate> =
        megaChatApiGateway.scheduledMeetingUpdates
            .filterIsInstance<ScheduledMeetingUpdate.OnSchedMeetingOccurrencesUpdate>()
            .mapNotNull { ResultOccurrenceUpdate(it.chatId, it.append) }
            .flowOn(dispatcher)

    override suspend fun getCallHandleList(state: ChatCallStatus) = withContext(dispatcher) {
        megaChatApiGateway.getChatCalls(megaChatCallStatusMapper(state))
            ?.let { handleListMapper(it) } ?: emptyList()
    }

    override suspend fun getChatCallIds(): List<Long> = withContext(dispatcher) {
        megaChatApiGateway.getChatCallIds()?.let(handleListMapper::invoke) ?: emptyList()
    }

    override fun monitorScheduledMeetingCanceled(): Flow<Int> =
        appEventGateway.monitorScheduledMeetingCanceled()

    override suspend fun broadcastScheduledMeetingCanceled(messageResId: Int) =
        appEventGateway.broadcastScheduledMeetingCanceled(messageResId)

    override suspend fun setChatVideoInDevice(device: String) = withContext(dispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getChatRequestListener("setChatVideoInDevice") {}

            megaChatApiGateway.setChatVideoInDevice(device, listener)

            continuation.invokeOnCancellation { megaChatApiGateway.removeRequestListener(listener) }
        }
    }

    override fun getChatLocalVideoUpdates(chatId: Long): Flow<ChatVideoUpdate> =
        megaChatApiGateway.getChatLocalVideoUpdates(chatId).flowOn(dispatcher)

    override suspend fun openVideoDevice() = withContext(dispatcher) {
        suspendCancellableCoroutine { continuation ->
            val methodName = "openVideoDevice"
            val listener = OptionalMegaChatRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaChatError.ERROR_OK) {
                        if (request.flag) {
                            continuation.resume(Unit)
                        } else {
                            continuation.resumeWithException(IllegalStateException("Video device error"))
                        }
                    } else {
                        Timber.e("Calling $methodName failed with error code ${error.errorCode}")
                        continuation.failWithError(error, methodName)
                    }
                }
            )

            megaChatApiGateway.openVideoDevice(listener)

            continuation.invokeOnCancellation { megaChatApiGateway.removeRequestListener(listener) }
        }
    }

    override suspend fun releaseVideoDevice() = withContext(dispatcher) {
        suspendCancellableCoroutine { continuation ->
            val methodName = "releaseVideoDevice"
            val listener = OptionalMegaChatRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaChatError.ERROR_OK) {
                        if (!request.flag) {
                            continuation.resume(Unit)
                        } else {
                            continuation.resumeWithException(IllegalStateException("Video device error"))
                        }
                    } else {
                        Timber.e("Calling $methodName failed with error code ${error.errorCode}")
                        continuation.failWithError(error, methodName)
                    }
                }
            )

            megaChatApiGateway.releaseVideoDevice(listener)

            continuation.invokeOnCancellation { megaChatApiGateway.removeRequestListener(listener) }
        }
    }

    override suspend fun pushUsersIntoWaitingRoom(
        chatId: Long,
        userList: List<Long>,
        all: Boolean,
    ): ChatRequest = withContext(dispatcher) {
        suspendCancellableCoroutine { continuation ->
            val callback = continuation.getChatRequestListener(
                methodName = "pushUsersIntoWaitingRoom",
                chatRequestMapper::invoke
            )

            megaChatApiGateway.pushUsersIntoWaitingRoom(
                chatId,
                megaHandleListMapper(userList),
                all,
                callback
            )

            continuation.invokeOnCancellation { megaChatApiGateway.removeRequestListener(callback) }
        }
    }

    override suspend fun kickUsersFromCall(chatId: Long, userList: List<Long>):
            ChatRequest = withContext(dispatcher) {
        suspendCancellableCoroutine { continuation ->
            val callback = continuation.getChatRequestListener(
                methodName = "kickUsersFromCall",
                chatRequestMapper::invoke
            )

            megaChatApiGateway.kickUsersFromCall(
                chatId,
                megaHandleListMapper(userList),
                callback
            )

            continuation.invokeOnCancellation { megaChatApiGateway.removeRequestListener(callback) }
        }
    }

    override suspend fun allowUsersJoinCall(
        chatId: Long,
        userList: List<Long>,
        all: Boolean,
    ): ChatRequest = withContext(dispatcher) {
        suspendCancellableCoroutine { continuation ->
            val callback = continuation.getChatRequestListener(
                methodName = "allowUsersJoinCall",
                chatRequestMapper::invoke
            )

            megaChatApiGateway.allowUsersJoinCall(
                chatId,
                megaHandleListMapper(userList),
                all,
                callback
            )

            continuation.invokeOnCancellation { megaChatApiGateway.removeRequestListener(callback) }
        }
    }
}
