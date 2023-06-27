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
import mega.privacy.android.data.mapper.HandleListMapper
import mega.privacy.android.data.mapper.chat.ChatRequestMapper
import mega.privacy.android.data.mapper.chat.MegaChatPeerListMapper
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
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.entity.meeting.ChatSession
import mega.privacy.android.domain.entity.meeting.ResultOccurrenceUpdate
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.CallRepository
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
    private val megaChatScheduledMeetingFlagsMapper: MegaChatScheduledMeetingFlagsMapper,
    private val megaChatScheduledMeetingRulesMapper: MegaChatScheduledMeetingRulesMapper,
    private val megaChatPeerListMapper: MegaChatPeerListMapper,
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

    override suspend fun hangChatCall(
        callId: Long,
    ): ChatRequest = withContext(dispatcher) {
        suspendCoroutine { continuation ->
            val callback = OptionalMegaChatRequestListenerInterface(
                onRequestFinish = onRequestCompleted(continuation)
            )

            megaChatApiGateway.hangChatCall(
                callId, callback
            )
        }
    }

    override suspend fun holdChatCall(
        chatId: Long,
        setOnHold: Boolean,
    ): ChatRequest = withContext(dispatcher) {
        suspendCoroutine { continuation ->
            val callback = OptionalMegaChatRequestListenerInterface(
                onRequestFinish = onRequestCompleted(continuation)
            )

            megaChatApiGateway.holdChatCall(
                chatId,
                setOnHold,
                callback
            )
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
            suspendCoroutine { continuation ->
                megaChatApiGateway.fetchScheduledMeetingOccurrencesByChat(
                    chatId,
                    since,
                    OptionalMegaChatRequestListenerInterface(
                        onRequestFinish = { request: MegaChatRequest, error: MegaChatError ->
                            if (error.errorCode == MegaChatError.ERROR_OK) {
                                val occurrences = mutableListOf<ChatScheduledMeetingOccurr>()
                                request.megaChatScheduledMeetingOccurrList?.let { occursList ->
                                    if (occursList.size() > 0) {
                                        for (i in 0 until occursList.size()) {
                                            occurrences.add(
                                                chatScheduledMeetingOccurrMapper(
                                                    occursList.at(
                                                        i
                                                    )
                                                )
                                            )
                                        }
                                    }
                                }

                                continuation.resume(occurrences)
                            } else {
                                continuation.failWithError(
                                    error,
                                    "fetchScheduledMeetingOccurrencesByChat"
                                )
                            }
                        }
                    )
                )
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

    override suspend fun getNextScheduledMeetingOccurrence(chatId: Long): ChatScheduledMeetingOccurr? =
        withContext(dispatcher) {
            val now = Instant.now().atZone(ZoneOffset.UTC)
            fetchScheduledMeetingOccurrencesByChat(
                chatId,
                now.minus(1L, ChronoUnit.HALF_DAYS).toEpochSecond()
            )
                .sortedBy(ChatScheduledMeetingOccurr::startDateTime)
                .firstOrNull { occurr ->
                    !occurr.isCancelled
                            && (occurr.startDateTime?.toZonedDateTime()?.isAfter(now) == true
                            || occurr.endDateTime?.toZonedDateTime()?.isAfter(now) == true)
                }
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
        suspendCoroutine { continuation ->
            val callback = OptionalMegaChatRequestListenerInterface(
                onRequestFinish = onRequestCompleted(continuation)
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

    private fun onRequestCompleted(continuation: Continuation<ChatRequest>) =
        { request: MegaChatRequest, error: MegaChatError ->
            if (error.errorCode == MegaChatError.ERROR_OK) {
                continuation.resumeWith(Result.success(chatRequestMapper(request)))
            } else {
                continuation.failWithError(error, "onRequestCompleted")
            }
        }

    private fun Long.toZonedDateTime(): ZonedDateTime =
        ZonedDateTime.ofInstant(Instant.ofEpochSecond(this), ZoneOffset.UTC)

    override suspend fun getCallHandleList(state: ChatCallStatus) = withContext(dispatcher) {
        megaChatApiGateway.getChatCalls(megaChatCallStatusMapper(state))
            ?.let { handleListMapper(it) } ?: emptyList()
    }

    override suspend fun getChatCallIds(): List<Long> = withContext(dispatcher) {
        megaChatApiGateway.getChatCallIds()?.let(handleListMapper::invoke) ?: emptyList()
    }
}