package mega.privacy.android.data.test.gateway

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KFunction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.model.ChatRoomUpdate
import mega.privacy.android.data.model.ChatUpdate
import mega.privacy.android.data.model.ScheduledMeetingUpdate
import mega.privacy.android.data.model.meeting.ChatCallUpdate
import mega.privacy.android.data.test.engine.FakeGatewayEngine
import mega.privacy.android.data.test.engine.FakeGatewayStubbing
import mega.privacy.android.data.test.state.FakeChatState
import mega.privacy.android.data.test.stub.StubMegaChatError
import mega.privacy.android.data.test.stub.StubMegaChatMessage
import mega.privacy.android.data.test.stub.StubMegaChatRequest
import mega.privacy.android.data.test.stub.StubMegaHandleList
import mega.privacy.android.data.test.stub.StubMegaStringList
import mega.privacy.android.domain.entity.chat.ChatVideoUpdate
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatLoggerInterface
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatNotificationListenerInterface
import nz.mega.sdk.MegaChatPeerList
import nz.mega.sdk.MegaChatPresenceConfig
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRequestListenerInterface
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaChatScheduledFlags
import nz.mega.sdk.MegaChatScheduledMeeting
import nz.mega.sdk.MegaChatScheduledRules
import nz.mega.sdk.MegaChatVideoListenerInterface
import nz.mega.sdk.MegaHandleList
import nz.mega.sdk.MegaStringList

/**
 * In-process fake of [MegaChatApiGateway] for automated tests.
 *
 * Every gateway method is routed through a [FakeGatewayEngine], so every call is recorded and can
 * be stubbed via the [FakeGatewayStubbing] surface. Unstubbed calls answer with common-sense
 * defaults: reads are backed by [chatState] (mutate it directly to configure them), query methods
 * return "empty success" (null / false / 0 / empty stub lists / MEGACHAT_INVALID_HANDLE), and
 * listener-based commands complete successfully with [MegaChatError.ERROR_OK].
 *
 * The `MegaChatApiJava` parameter passed to listener callbacks is an inert instance that must
 * never be invoked, only held or ignored.
 *
 * ```kotlin
 * val gateway = FakeMegaChatApiGateway()
 *
 * // State-backed defaults
 * gateway.chatState.chatRooms[chatId] = StubMegaChatRoom()
 *
 * // Stub a query
 * gateway.stubResult(MegaChatApiGateway::getNumUnreadChats, 3)
 *
 * // Stub a listener-based request to fail
 * gateway.stubChatRequest(
 *     MegaChatApiGateway::archiveChat,
 *     error = StubMegaChatError(MegaChatError.ERROR_ACCESS),
 * )
 *
 * // Emit events into the gateway flows
 * gateway.emitChatUpdate(ChatUpdate.OnChatInitStateUpdate(MegaChatApi.INIT_ONLINE_SESSION))
 *
 * // Verify recorded invocations
 * assertThat(gateway.invocationsOf(MegaChatApiGateway::archiveChat)).hasSize(1)
 *
 * // Back to a pristine fake
 * gateway.resetToDefaults()
 * ```
 */
class FakeMegaChatApiGateway(
    private val engine: FakeGatewayEngine = FakeGatewayEngine(),
    val chatState: FakeChatState = FakeChatState(),
) : MegaChatApiGateway, FakeGatewayStubbing by engine {

    private val chatUpdatesFlow = MutableSharedFlow<ChatUpdate>(extraBufferCapacity = 64)
    private val chatCallUpdatesFlow = MutableSharedFlow<ChatCallUpdate>(extraBufferCapacity = 64)
    private val scheduledMeetingUpdatesFlow =
        MutableSharedFlow<ScheduledMeetingUpdate>(extraBufferCapacity = 64)
    private val chatRoomUpdateFlows =
        ConcurrentHashMap<Long, MutableSharedFlow<ChatRoomUpdate>>()
    private val localVideoUpdateFlows =
        ConcurrentHashMap<Long, MutableSharedFlow<ChatVideoUpdate>>()
    private val remoteVideoUpdateFlows =
        ConcurrentHashMap<Long, MutableSharedFlow<ChatVideoUpdate>>()

    /**
     * Chat request listeners registered via [addChatRequestListener], visible for tests.
     */
    val chatRequestListeners = CopyOnWriteArrayList<MegaChatRequestListenerInterface>()

    /**
     * Notification listeners registered via [registerChatNotificationListener], visible for tests.
     */
    val chatNotificationListeners = CopyOnWriteArrayList<MegaChatNotificationListenerInterface>()

    /**
     * Video listeners registered via [addChatLocalVideoListener] /
     * [addChatRemoteVideoListener], visible for tests.
     */
    val chatVideoListeners = CopyOnWriteArrayList<ChatVideoListenerRegistration>()

    /**
     * A video listener registration kept by the fake.
     *
     * @property chatId Chat the listener was registered for.
     * @property clientId Client id for remote listeners, null for local ones.
     * @property hiRes Resolution flag for remote listeners, null for local ones.
     * @property listener The registered listener.
     */
    data class ChatVideoListenerRegistration(
        val chatId: Long,
        val clientId: Long?,
        val hiRes: Boolean?,
        val listener: MegaChatVideoListenerInterface,
    )

    /**
     * Stub the outcome delivered to the listener of a listener-based gateway method.
     *
     * @param method Gateway method reference, e.g. `MegaChatApiGateway::archiveChat`.
     * @param error Error delivered in onRequestFinish, success by default.
     * @param request Request passed to the listener, or null for a default stub request carrying
     * the method's request type.
     * @param matcher Restricts the stub to calls whose arguments match.
     */
    fun stubChatRequest(
        method: KFunction<*>,
        error: MegaChatError = StubMegaChatError(MegaChatError.ERROR_OK),
        request: MegaChatRequest? = null,
        matcher: (List<Any?>) -> Boolean = { true },
    ) = engine.stubRequestOutcome(method, matcher, MegaChatRequestOutcome(request, error))

    /**
     * Restore the pristine fake: clears stubs, recorded invocations, registered listeners and
     * resets [chatState] to its defaults. Flows keep their identity so existing collectors
     * stay subscribed.
     */
    fun resetToDefaults() {
        engine.reset()
        chatState.reset()
        chatRequestListeners.clear()
        chatNotificationListeners.clear()
        chatVideoListeners.clear()
    }

    /**
     * Emit an update into [chatUpdates].
     */
    suspend fun emitChatUpdate(update: ChatUpdate) = chatUpdatesFlow.emit(update)

    /**
     * Emit an update into [chatCallUpdates].
     */
    suspend fun emitChatCallUpdate(update: ChatCallUpdate) = chatCallUpdatesFlow.emit(update)

    /**
     * Emit an update into [scheduledMeetingUpdates].
     */
    suspend fun emitScheduledMeetingUpdate(update: ScheduledMeetingUpdate) =
        scheduledMeetingUpdatesFlow.emit(update)

    /**
     * Emit an update into the flow returned by [openChatRoom] for [chatId].
     */
    suspend fun emitChatRoomUpdate(chatId: Long, update: ChatRoomUpdate) =
        chatRoomUpdateFlow(chatId).emit(update)

    /**
     * Emit an update into the flow returned by [getChatLocalVideoUpdates] for [chatId].
     */
    suspend fun emitChatLocalVideoUpdate(chatId: Long, update: ChatVideoUpdate) =
        localVideoUpdateFlow(chatId).emit(update)

    /**
     * Emit an update into the flow returned by [getChatRemoteVideoUpdates] for [chatId].
     * Remote video flows are keyed by chat id only.
     */
    suspend fun emitChatRemoteVideoUpdate(chatId: Long, update: ChatVideoUpdate) =
        remoteVideoUpdateFlow(chatId).emit(update)

    private fun chatRoomUpdateFlow(chatId: Long) =
        chatRoomUpdateFlows.getOrPut(chatId) { MutableSharedFlow(extraBufferCapacity = 64) }

    private fun localVideoUpdateFlow(chatId: Long) =
        localVideoUpdateFlows.getOrPut(chatId) { MutableSharedFlow(extraBufferCapacity = 64) }

    private fun remoteVideoUpdateFlow(chatId: Long) =
        remoteVideoUpdateFlows.getOrPut(chatId) { MutableSharedFlow(extraBufferCapacity = 64) }

    private fun completeChatRequest(
        method: KFunction<*>,
        args: List<Any?>,
        listener: MegaChatRequestListenerInterface?,
        requestType: Int,
    ) {
        engine.record(method, args)
        val outcome = engine.requestOutcomeFor(method, args) as? MegaChatRequestOutcome
            ?: MegaChatRequestOutcome(null, StubMegaChatError(MegaChatError.ERROR_OK))
        if (listener == null) return
        val request = outcome.request ?: StubMegaChatRequest(type = requestType)
        listener.onRequestStart(inertMegaChatApiJava, request)
        listener.onRequestFinish(inertMegaChatApiJava, request, outcome.error)
    }

    override val initState: Int
        get() = chatState.initState

    override fun init(session: String?): Int =
        engine.dispatchBlocking(MegaChatApiGateway::init, listOf(session)) {
            chatState.initState
        }

    override fun initAnonymous(): Int =
        engine.dispatchBlocking(MegaChatApiGateway::initAnonymous, emptyList()) {
            MegaChatApi.INIT_ANONYMOUS
        }

    override fun logout(listener: MegaChatRequestListenerInterface?) = completeChatRequest(
        MegaChatApiGateway::logout,
        listOf(listener),
        listener,
        MegaChatRequest.TYPE_LOGOUT,
    )

    override fun setLogger(logger: MegaChatLoggerInterface) {
        engine.dispatchBlocking(MegaChatApiGateway::setLogger, listOf(logger)) {}
    }

    override fun setLogLevel(logLevel: Int) {
        engine.dispatchBlocking(MegaChatApiGateway::setLogLevel, listOf(logLevel)) {}
    }

    override fun addChatRequestListener(listener: MegaChatRequestListenerInterface) {
        engine.dispatchBlocking(MegaChatApiGateway::addChatRequestListener, listOf(listener)) {
            chatRequestListeners.add(listener)
        }
    }

    override fun removeChatRequestListener(listener: MegaChatRequestListenerInterface) {
        engine.dispatchBlocking(MegaChatApiGateway::removeChatRequestListener, listOf(listener)) {
            chatRequestListeners.remove(listener)
        }
    }

    override fun pushReceived(beep: Boolean, listener: MegaChatRequestListenerInterface?) =
        completeChatRequest(
            MegaChatApiGateway::pushReceived,
            listOf(beep, listener),
            listener,
            MegaChatRequest.TYPE_PUSH_RECEIVED,
        )

    override fun retryPendingConnections(
        disconnect: Boolean,
        listener: MegaChatRequestListenerInterface?,
    ) = completeChatRequest(
        MegaChatApiGateway::retryPendingConnections,
        listOf(disconnect, listener),
        listener,
        MegaChatRequest.TYPE_RETRY_PENDING_CONNECTIONS,
    )

    override val chatUpdates: Flow<ChatUpdate> = chatUpdatesFlow

    override val chatCallUpdates: Flow<ChatCallUpdate> = chatCallUpdatesFlow

    override suspend fun requestLastGreen(userHandle: Long) {
        engine.dispatch(MegaChatApiGateway::requestLastGreen, listOf(userHandle)) {}
    }

    override fun createChat(
        isGroup: Boolean,
        peers: MegaChatPeerList?,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::createChat,
        listOf(isGroup, peers, listener),
        listener,
        MegaChatRequest.TYPE_CREATE_CHATROOM,
    )

    override fun createGroupChat(
        peers: MegaChatPeerList,
        title: String?,
        speakRequest: Boolean,
        waitingRoom: Boolean,
        openInvite: Boolean,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::createGroupChat,
        listOf(peers, title, speakRequest, waitingRoom, openInvite, listener),
        listener,
        MegaChatRequest.TYPE_CREATE_CHATROOM,
    )

    override fun createPublicChat(
        peers: MegaChatPeerList,
        title: String?,
        speakRequest: Boolean,
        waitingRoom: Boolean,
        openInvite: Boolean,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::createPublicChat,
        listOf(peers, title, speakRequest, waitingRoom, openInvite, listener),
        listener,
        MegaChatRequest.TYPE_CREATE_CHATROOM,
    )

    override fun leaveChat(
        chatId: Long,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::leaveChat,
        listOf(chatId, listener),
        listener,
        MegaChatRequest.TYPE_REMOVE_FROM_CHATROOM,
    )

    override fun setChatTitle(
        chatId: Long,
        title: String,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::setChatTitle,
        listOf(chatId, title, listener),
        listener,
        MegaChatRequest.TYPE_EDIT_CHATROOM_NAME,
    )

    override fun setOpenInvite(
        chatId: Long,
        enabled: Boolean,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::setOpenInvite,
        listOf(chatId, enabled, listener),
        listener,
        MegaChatRequest.TYPE_SET_CHATROOM_OPTIONS,
    )

    override fun setWaitingRoom(
        chatId: Long,
        enabled: Boolean,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::setWaitingRoom,
        listOf(chatId, enabled, listener),
        listener,
        MegaChatRequest.TYPE_SET_CHATROOM_OPTIONS,
    )

    override fun getChatRooms(): List<MegaChatRoom> =
        engine.dispatchBlocking(MegaChatApiGateway::getChatRooms, emptyList()) {
            chatState.chatRooms.values.toList()
        }

    override fun getNoteToSelfChat(): MegaChatRoom? =
        engine.dispatchBlocking(MegaChatApiGateway::getNoteToSelfChat, emptyList()) { null }

    override fun getMeetingChatRooms(): List<MegaChatRoom>? =
        engine.dispatchBlocking(MegaChatApiGateway::getMeetingChatRooms, emptyList()) {
            chatState.chatRooms.values.toList()
        }

    override fun getGroupChatRooms(): List<MegaChatRoom>? =
        engine.dispatchBlocking(MegaChatApiGateway::getGroupChatRooms, emptyList()) {
            chatState.chatRooms.values.toList()
        }

    override fun getIndividualChatRooms(): List<MegaChatRoom>? =
        engine.dispatchBlocking(MegaChatApiGateway::getIndividualChatRooms, emptyList()) {
            chatState.chatRooms.values.toList()
        }

    override fun getChatRoomByUser(userHandle: Long): MegaChatRoom? =
        engine.dispatchBlocking(MegaChatApiGateway::getChatRoomByUser, listOf(userHandle)) {
            null
        }

    override fun loadUserAttributes(
        chatId: Long,
        userList: MegaHandleList,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::loadUserAttributes,
        listOf(chatId, userList, listener),
        listener,
        MegaChatRequest.TYPE_GET_PEER_ATTRIBUTES,
    )

    override fun getUserEmailFromCache(userHandle: Long): String? =
        engine.dispatchBlocking(MegaChatApiGateway::getUserEmailFromCache, listOf(userHandle)) {
            if (userHandle == chatState.myUserHandle) chatState.myEmail else null
        }

    override fun getUserAliasFromCache(userHandle: Long): String? =
        engine.dispatchBlocking(MegaChatApiGateway::getUserAliasFromCache, listOf(userHandle)) {
            null
        }

    override fun getUserFirstnameFromCache(userHandle: Long): String? =
        engine.dispatchBlocking(
            MegaChatApiGateway::getUserFirstnameFromCache,
            listOf(userHandle),
        ) { null }

    override fun getUserLastnameFromCache(userHandle: Long): String? =
        engine.dispatchBlocking(
            MegaChatApiGateway::getUserLastnameFromCache,
            listOf(userHandle),
        ) { null }

    override fun getUserFullNameFromCache(userHandle: Long): String? =
        engine.dispatchBlocking(
            MegaChatApiGateway::getUserFullNameFromCache,
            listOf(userHandle),
        ) { if (userHandle == chatState.myUserHandle) chatState.myFullname else null }

    override fun getUserOnlineStatus(userHandle: Long): Int =
        engine.dispatchBlocking(MegaChatApiGateway::getUserOnlineStatus, listOf(userHandle)) {
            if (userHandle == chatState.myUserHandle) {
                chatState.onlineStatus
            } else {
                MegaChatApi.STATUS_OFFLINE
            }
        }

    override fun openChatRoom(chatId: Long): Flow<ChatRoomUpdate> =
        engine.dispatchBlocking(MegaChatApiGateway::openChatRoom, listOf(chatId)) {
            chatRoomUpdateFlow(chatId)
        }

    override fun getChatRoom(chatId: Long): MegaChatRoom? =
        engine.dispatchBlocking(MegaChatApiGateway::getChatRoom, listOf(chatId)) {
            chatState.chatRooms[chatId]
        }

    override fun getChatListItem(chatId: Long): MegaChatListItem? =
        engine.dispatchBlocking(MegaChatApiGateway::getChatListItem, listOf(chatId)) { null }

    override fun getChatListItems(mask: Int, filter: Int): List<MegaChatListItem>? =
        engine.dispatchBlocking(MegaChatApiGateway::getChatListItems, listOf(mask, filter)) {
            emptyList()
        }

    override fun getChatCall(chatId: Long): MegaChatCall? =
        engine.dispatchBlocking(MegaChatApiGateway::getChatCall, listOf(chatId)) { null }

    override fun getChatCallByCallId(callId: Long): MegaChatCall? =
        engine.dispatchBlocking(MegaChatApiGateway::getChatCallByCallId, listOf(callId)) { null }

    override fun getChatCalls(state: Int): MegaHandleList? =
        engine.dispatchBlocking(MegaChatApiGateway::getChatCalls, listOf(state)) {
            StubMegaHandleList()
        }

    override fun getChatCallIds(): MegaHandleList? =
        engine.dispatchBlocking(MegaChatApiGateway::getChatCallIds, emptyList()) {
            StubMegaHandleList()
        }

    override fun startChatCall(
        chatId: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::startChatCall,
        listOf(chatId, enabledVideo, enabledAudio, listener),
        listener,
        MegaChatRequest.TYPE_START_CHAT_CALL,
    )

    override fun startChatCallNoRinging(
        chatId: Long,
        schedId: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::startChatCallNoRinging,
        listOf(chatId, schedId, enabledVideo, enabledAudio, listener),
        listener,
        MegaChatRequest.TYPE_START_CHAT_CALL,
    )

    override fun startMeetingInWaitingRoomChat(
        chatId: Long,
        schedIdWr: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::startMeetingInWaitingRoomChat,
        listOf(chatId, schedIdWr, enabledVideo, enabledAudio, listener),
        listener,
        MegaChatRequest.TYPE_START_CHAT_CALL,
    )

    override fun ringIndividualInACall(
        chatId: Long,
        userId: Long,
        ringTimeout: Int,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::ringIndividualInACall,
        listOf(chatId, userId, ringTimeout, listener),
        listener,
        MegaChatRequest.TYPE_RING_INDIVIDUAL_IN_CALL,
    )

    override fun answerChatCall(
        chatId: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::answerChatCall,
        listOf(chatId, enabledVideo, enabledAudio, listener),
        listener,
        MegaChatRequest.TYPE_ANSWER_CHAT_CALL,
    )

    override fun hangChatCall(
        callId: Long,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::hangChatCall,
        listOf(callId, listener),
        listener,
        MegaChatRequest.TYPE_HANG_CHAT_CALL,
    )

    override fun holdChatCall(
        chatId: Long,
        setOnHold: Boolean,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::holdChatCall,
        listOf(chatId, setOnHold, listener),
        listener,
        MegaChatRequest.TYPE_SET_CALL_ON_HOLD,
    )

    override fun setChatVideoInDevice(
        device: String,
        listener: MegaChatRequestListenerInterface?,
    ) = completeChatRequest(
        MegaChatApiGateway::setChatVideoInDevice,
        listOf(device, listener),
        listener,
        MegaChatRequest.TYPE_CHANGE_VIDEO_STREAM,
    )

    override val scheduledMeetingUpdates: Flow<ScheduledMeetingUpdate> =
        scheduledMeetingUpdatesFlow

    override fun getAllScheduledMeetings(): List<MegaChatScheduledMeeting>? =
        engine.dispatchBlocking(MegaChatApiGateway::getAllScheduledMeetings, emptyList()) {
            emptyList()
        }

    override fun getScheduledMeeting(chatId: Long, schedId: Long): MegaChatScheduledMeeting? =
        engine.dispatchBlocking(
            MegaChatApiGateway::getScheduledMeeting,
            listOf(chatId, schedId),
        ) { null }

    override fun getScheduledMeetingsByChat(chatId: Long): List<MegaChatScheduledMeeting>? =
        engine.dispatchBlocking(
            MegaChatApiGateway::getScheduledMeetingsByChat,
            listOf(chatId),
        ) { emptyList() }

    override fun fetchScheduledMeetingOccurrencesByChat(
        chatId: Long,
        since: Long,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::fetchScheduledMeetingOccurrencesByChat,
        listOf(chatId, since, listener),
        listener,
        MegaChatRequest.TYPE_FETCH_SCHEDULED_MEETING_OCCURRENCES,
    )

    override fun inviteToChat(
        chatId: Long,
        userHandle: Long,
        listener: MegaChatRequestListenerInterface?,
    ) = completeChatRequest(
        MegaChatApiGateway::inviteToChat,
        listOf(chatId, userHandle, listener),
        listener,
        MegaChatRequest.TYPE_INVITE_TO_CHATROOM,
    )

    override fun openChatPreview(link: String, listener: MegaChatRequestListenerInterface?) =
        completeChatRequest(
            MegaChatApiGateway::openChatPreview,
            listOf(link, listener),
            listener,
            MegaChatRequest.TYPE_LOAD_PREVIEW,
        )

    override fun checkChatLink(link: String, listener: MegaChatRequestListenerInterface?) =
        completeChatRequest(
            MegaChatApiGateway::checkChatLink,
            listOf(link, listener),
            listener,
            MegaChatRequest.TYPE_LOAD_PREVIEW,
        )

    override fun setPublicChatToPrivate(
        chatId: Long,
        listener: MegaChatRequestListenerInterface?,
    ) = completeChatRequest(
        MegaChatApiGateway::setPublicChatToPrivate,
        listOf(chatId, listener),
        listener,
        MegaChatRequest.TYPE_SET_PRIVATE_MODE,
    )

    override fun queryChatLink(chatId: Long, listener: MegaChatRequestListenerInterface?) =
        completeChatRequest(
            MegaChatApiGateway::queryChatLink,
            listOf(chatId, listener),
            listener,
            MegaChatRequest.TYPE_CHAT_LINK_HANDLE,
        )

    override fun removeChatLink(chatId: Long, listener: MegaChatRequestListenerInterface?) =
        completeChatRequest(
            MegaChatApiGateway::removeChatLink,
            listOf(chatId, listener),
            listener,
            MegaChatRequest.TYPE_CHAT_LINK_HANDLE,
        )

    override fun createChatLink(chatId: Long, listener: MegaChatRequestListenerInterface?) =
        completeChatRequest(
            MegaChatApiGateway::createChatLink,
            listOf(chatId, listener),
            listener,
            MegaChatRequest.TYPE_CHAT_LINK_HANDLE,
        )

    override fun autojoinPublicChat(chatId: Long, listener: MegaChatRequestListenerInterface?) =
        completeChatRequest(
            MegaChatApiGateway::autojoinPublicChat,
            listOf(chatId, listener),
            listener,
            MegaChatRequest.TYPE_AUTOJOIN_PUBLIC_CHAT,
        )

    override fun autorejoinPublicChat(
        chatId: Long,
        publicHandle: Long,
        listener: MegaChatRequestListenerInterface?,
    ) = completeChatRequest(
        MegaChatApiGateway::autorejoinPublicChat,
        listOf(chatId, publicHandle, listener),
        listener,
        MegaChatRequest.TYPE_AUTOJOIN_PUBLIC_CHAT,
    )

    override fun getMyUserHandle(): Long =
        engine.dispatchBlocking(MegaChatApiGateway::getMyUserHandle, emptyList()) {
            chatState.myUserHandle
        }

    override fun getMyFullname(): String? =
        engine.dispatchBlocking(MegaChatApiGateway::getMyFullname, emptyList()) {
            chatState.myFullname
        }

    override fun getMyEmail(): String? =
        engine.dispatchBlocking(MegaChatApiGateway::getMyEmail, emptyList()) {
            chatState.myEmail
        }

    override fun getChatInvalidHandle(): Long =
        engine.dispatchBlocking(MegaChatApiGateway::getChatInvalidHandle, emptyList()) {
            MegaChatApiJava.MEGACHAT_INVALID_HANDLE
        }

    override fun getOnlineStatus(): Int =
        engine.dispatchBlocking(MegaChatApiGateway::getOnlineStatus, emptyList()) {
            chatState.onlineStatus
        }

    override fun removeFromChat(
        chatId: Long,
        handle: Long,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::removeFromChat,
        listOf(chatId, handle, listener),
        listener,
        MegaChatRequest.TYPE_REMOVE_FROM_CHATROOM,
    )

    override fun updateChatPermissions(
        chatId: Long,
        handle: Long,
        privilege: Int,
        listener: MegaChatRequestListenerInterface?,
    ) = completeChatRequest(
        MegaChatApiGateway::updateChatPermissions,
        listOf(chatId, handle, privilege, listener),
        listener,
        MegaChatRequest.TYPE_UPDATE_PEER_PERMISSIONS,
    )

    override fun getMessage(chatId: Long, messageId: Long): MegaChatMessage? =
        engine.dispatchBlocking(MegaChatApiGateway::getMessage, listOf(chatId, messageId)) {
            null
        }

    override fun getMessageFromNodeHistory(chatId: Long, messageId: Long): MegaChatMessage? =
        engine.dispatchBlocking(
            MegaChatApiGateway::getMessageFromNodeHistory,
            listOf(chatId, messageId),
        ) { null }

    override fun signalPresenceActivity(listener: MegaChatRequestListenerInterface) =
        completeChatRequest(
            MegaChatApiGateway::signalPresenceActivity,
            listOf(listener),
            listener,
            MegaChatRequest.TYPE_SIGNAL_ACTIVITY,
        )

    override fun clearChatHistory(chatId: Long, listener: MegaChatRequestListenerInterface) =
        completeChatRequest(
            MegaChatApiGateway::clearChatHistory,
            listOf(chatId, listener),
            listener,
            MegaChatRequest.TYPE_TRUNCATE_HISTORY,
        )

    override fun archiveChat(
        chatId: Long,
        archive: Boolean,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::archiveChat,
        listOf(chatId, archive, listener),
        listener,
        MegaChatRequest.TYPE_ARCHIVE_CHATROOM,
    )

    override suspend fun refreshUrl() {
        engine.dispatch(MegaChatApiGateway::refreshUrl, emptyList()) {}
    }

    override fun createChatroomAndSchedMeeting(
        peerList: MegaChatPeerList,
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
        flags: MegaChatScheduledFlags?,
        rules: MegaChatScheduledRules?,
        attributes: String?,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::createChatroomAndSchedMeeting,
        listOf(
            peerList, isMeeting, publicChat, title, speakRequest, waitingRoom, openInvite,
            timezone, startDate, endDate, description, flags, rules, attributes, listener,
        ),
        listener,
        MegaChatRequest.TYPE_CREATE_SCHEDULED_MEETING,
    )

    override fun updateScheduledMeeting(
        chatId: Long,
        schedId: Long,
        timezone: String,
        startDate: Long,
        endDate: Long,
        title: String,
        description: String,
        cancelled: Boolean,
        flags: MegaChatScheduledFlags?,
        rules: MegaChatScheduledRules?,
        updateChatTitle: Boolean,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::updateScheduledMeeting,
        listOf(
            chatId, schedId, timezone, startDate, endDate, title, description, cancelled,
            flags, rules, updateChatTitle, listener,
        ),
        listener,
        MegaChatRequest.TYPE_UPDATE_SCHEDULED_MEETING,
    )

    override fun updateScheduledMeetingOccurrence(
        chatId: Long,
        schedId: Long,
        overrides: Long,
        newStartDate: Long,
        newEndDate: Long,
        cancelled: Boolean,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::updateScheduledMeetingOccurrence,
        listOf(chatId, schedId, overrides, newStartDate, newEndDate, cancelled, listener),
        listener,
        MegaChatRequest.TYPE_UPDATE_SCHEDULED_MEETING_OCCURRENCE,
    )

    override fun getConnectedState(): Int =
        engine.dispatchBlocking(MegaChatApiGateway::getConnectedState, emptyList()) {
            MegaChatApi.CONNECTED
        }

    override fun getChatConnectionState(chatId: Long): Int =
        engine.dispatchBlocking(MegaChatApiGateway::getChatConnectionState, listOf(chatId)) {
            MegaChatApi.CHAT_CONNECTION_ONLINE
        }

    override suspend fun getNumUnreadChats(): Int =
        engine.dispatch(MegaChatApiGateway::getNumUnreadChats, emptyList()) { 0 }

    override suspend fun loadMessages(chatId: Long, count: Int): Int =
        engine.dispatch(MegaChatApiGateway::loadMessages, listOf(chatId, count)) {
            MegaChatApi.SOURCE_NONE
        }

    override fun setOnlineStatus(status: Int, listener: MegaChatRequestListenerInterface) =
        completeChatRequest(
            MegaChatApiGateway::setOnlineStatus,
            listOf(status, listener),
            listener,
            MegaChatRequest.TYPE_SET_ONLINE_STATUS,
        )

    override fun addChatLocalVideoListener(
        chatId: Long,
        listener: MegaChatVideoListenerInterface,
    ) {
        engine.dispatchBlocking(
            MegaChatApiGateway::addChatLocalVideoListener,
            listOf(chatId, listener),
        ) {
            chatVideoListeners.add(
                ChatVideoListenerRegistration(
                    chatId = chatId,
                    clientId = null,
                    hiRes = null,
                    listener = listener,
                )
            )
        }
    }

    override fun addChatRemoteVideoListener(
        chatId: Long,
        clientId: Long,
        hiRes: Boolean,
        listener: MegaChatVideoListenerInterface,
    ) {
        engine.dispatchBlocking(
            MegaChatApiGateway::addChatRemoteVideoListener,
            listOf(chatId, clientId, hiRes, listener),
        ) {
            chatVideoListeners.add(
                ChatVideoListenerRegistration(
                    chatId = chatId,
                    clientId = clientId,
                    hiRes = hiRes,
                    listener = listener,
                )
            )
        }
    }

    override fun removeChatVideoListener(
        chatId: Long,
        clientId: Long,
        hiRes: Boolean,
        listener: MegaChatVideoListenerInterface?,
    ) {
        engine.dispatchBlocking(
            MegaChatApiGateway::removeChatVideoListener,
            listOf(chatId, clientId, hiRes, listener),
        ) {
            chatVideoListeners.removeIf { it.chatId == chatId && it.listener === listener }
        }
    }

    override fun getChatLocalVideoUpdates(chatId: Long): Flow<ChatVideoUpdate> =
        engine.dispatchBlocking(MegaChatApiGateway::getChatLocalVideoUpdates, listOf(chatId)) {
            localVideoUpdateFlow(chatId)
        }

    override fun getChatRemoteVideoUpdates(
        chatId: Long,
        clientId: Long,
        hiRes: Boolean,
    ): Flow<ChatVideoUpdate> =
        engine.dispatchBlocking(
            MegaChatApiGateway::getChatRemoteVideoUpdates,
            listOf(chatId, clientId, hiRes),
        ) { remoteVideoUpdateFlow(chatId) }

    override fun openVideoDevice(listener: MegaChatRequestListenerInterface) =
        completeChatRequest(
            MegaChatApiGateway::openVideoDevice,
            listOf(listener),
            listener,
            MegaChatRequest.TYPE_OPEN_VIDEO_DEVICE,
        )

    override fun releaseVideoDevice(listener: MegaChatRequestListenerInterface) =
        completeChatRequest(
            MegaChatApiGateway::releaseVideoDevice,
            listOf(listener),
            listener,
            MegaChatRequest.TYPE_OPEN_VIDEO_DEVICE,
        )

    override fun enableVideo(chatId: Long, listener: MegaChatRequestListenerInterface) =
        completeChatRequest(
            MegaChatApiGateway::enableVideo,
            listOf(chatId, listener),
            listener,
            MegaChatRequest.TYPE_DISABLE_AUDIO_VIDEO_CALL,
        )

    override fun disableVideo(chatId: Long, listener: MegaChatRequestListenerInterface) =
        completeChatRequest(
            MegaChatApiGateway::disableVideo,
            listOf(chatId, listener),
            listener,
            MegaChatRequest.TYPE_DISABLE_AUDIO_VIDEO_CALL,
        )

    override fun enableAudio(chatId: Long, listener: MegaChatRequestListenerInterface) =
        completeChatRequest(
            MegaChatApiGateway::enableAudio,
            listOf(chatId, listener),
            listener,
            MegaChatRequest.TYPE_DISABLE_AUDIO_VIDEO_CALL,
        )

    override fun disableAudio(chatId: Long, listener: MegaChatRequestListenerInterface) =
        completeChatRequest(
            MegaChatApiGateway::disableAudio,
            listOf(chatId, listener),
            listener,
            MegaChatRequest.TYPE_DISABLE_AUDIO_VIDEO_CALL,
        )

    override fun pushUsersIntoWaitingRoom(
        chatId: Long,
        userList: MegaHandleList?,
        all: Boolean,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::pushUsersIntoWaitingRoom,
        listOf(chatId, userList, all, listener),
        listener,
        MegaChatRequest.TYPE_WR_PUSH,
    )

    override fun kickUsersFromCall(
        chatId: Long,
        userList: MegaHandleList?,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::kickUsersFromCall,
        listOf(chatId, userList, listener),
        listener,
        MegaChatRequest.TYPE_WR_KICK,
    )

    override fun allowUsersJoinCall(
        chatId: Long,
        userList: MegaHandleList?,
        all: Boolean,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::allowUsersJoinCall,
        listOf(chatId, userList, all, listener),
        listener,
        MegaChatRequest.TYPE_WR_ALLOW,
    )

    override fun attachNode(
        chatId: Long,
        nodeHandle: Long,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::attachNode,
        listOf(chatId, nodeHandle, listener),
        listener,
        MegaChatRequest.TYPE_ATTACH_NODE_MESSAGE,
    )

    override fun attachVoiceMessage(
        chatId: Long,
        nodeHandle: Long,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::attachVoiceMessage,
        listOf(chatId, nodeHandle, listener),
        listener,
        MegaChatRequest.TYPE_ATTACH_NODE_MESSAGE,
    )

    override suspend fun hasCallInChatRoom(chatId: Long): Boolean =
        engine.dispatch(MegaChatApiGateway::hasCallInChatRoom, listOf(chatId)) { false }

    override suspend fun isAudioLevelMonitorEnabled(chatId: Long): Boolean =
        engine.dispatch(MegaChatApiGateway::isAudioLevelMonitorEnabled, listOf(chatId)) { false }

    override suspend fun enableAudioLevelMonitor(enable: Boolean, chatId: Long) {
        engine.dispatch(
            MegaChatApiGateway::enableAudioLevelMonitor,
            listOf(enable, chatId),
        ) {}
    }

    override fun raiseHandToSpeak(chatId: Long, listener: MegaChatRequestListenerInterface) =
        completeChatRequest(
            MegaChatApiGateway::raiseHandToSpeak,
            listOf(chatId, listener),
            listener,
            MegaChatRequest.TYPE_RAISE_HAND_TO_SPEAK,
        )

    override fun lowerHandToStopSpeak(chatId: Long, listener: MegaChatRequestListenerInterface) =
        completeChatRequest(
            MegaChatApiGateway::lowerHandToStopSpeak,
            listOf(chatId, listener),
            listener,
            MegaChatRequest.TYPE_RAISE_HAND_TO_SPEAK,
        )

    override fun requestHiResVideo(
        chatId: Long,
        clientId: Long,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::requestHiResVideo,
        listOf(chatId, clientId, listener),
        listener,
        MegaChatRequest.TYPE_REQUEST_HIGH_RES_VIDEO,
    )

    override fun stopHiResVideo(
        chatId: Long,
        clientIds: MegaHandleList?,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::stopHiResVideo,
        listOf(chatId, clientIds, listener),
        listener,
        MegaChatRequest.TYPE_REQUEST_HIGH_RES_VIDEO,
    )

    override fun requestLowResVideo(
        chatId: Long,
        clientIds: MegaHandleList?,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::requestLowResVideo,
        listOf(chatId, clientIds, listener),
        listener,
        MegaChatRequest.TYPE_REQUEST_LOW_RES_VIDEO,
    )

    override fun stopLowResVideo(
        chatId: Long,
        clientIds: MegaHandleList?,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::stopLowResVideo,
        listOf(chatId, clientIds, listener),
        listener,
        MegaChatRequest.TYPE_REQUEST_LOW_RES_VIDEO,
    )

    override fun endChatCall(callId: Long, listener: MegaChatRequestListenerInterface) =
        completeChatRequest(
            MegaChatApiGateway::endChatCall,
            listOf(callId, listener),
            listener,
            MegaChatRequest.TYPE_HANG_CHAT_CALL,
        )

    override fun sendMessage(chatId: Long, message: String): MegaChatMessage? =
        engine.dispatchBlocking(MegaChatApiGateway::sendMessage, listOf(chatId, message)) {
            StubMegaChatMessage()
        }

    override suspend fun closeChatPreview(chatId: Long) {
        engine.dispatch(MegaChatApiGateway::closeChatPreview, listOf(chatId)) {}
    }

    override fun hasUrl(content: String): Boolean =
        engine.dispatchBlocking(MegaChatApiGateway::hasUrl, listOf(content)) { false }

    override fun sendGeolocation(
        chatId: Long,
        longitude: Float,
        latitude: Float,
        image: String,
    ): MegaChatMessage =
        engine.dispatchBlocking(
            MegaChatApiGateway::sendGeolocation,
            listOf(chatId, longitude, latitude, image),
        ) { StubMegaChatMessage() }

    override fun mutePeers(
        chatId: Long,
        clientId: Long,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::mutePeers,
        listOf(chatId, clientId, listener),
        listener,
        MegaChatRequest.TYPE_MUTE,
    )

    override fun addReaction(
        chatId: Long,
        msgId: Long,
        reaction: String,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::addReaction,
        listOf(chatId, msgId, reaction, listener),
        listener,
        MegaChatRequest.TYPE_MANAGE_REACTION,
    )

    override fun registerChatNotificationListener(
        listener: MegaChatNotificationListenerInterface,
    ) {
        engine.dispatchBlocking(
            MegaChatApiGateway::registerChatNotificationListener,
            listOf(listener),
        ) { chatNotificationListeners.add(listener) }
    }

    override fun deregisterChatNotificationListener(
        listener: MegaChatNotificationListenerInterface,
    ) {
        engine.dispatchBlocking(
            MegaChatApiGateway::deregisterChatNotificationListener,
            listOf(listener),
        ) { chatNotificationListeners.remove(listener) }
    }

    override fun getMessageReactions(chatId: Long, msgId: Long): MegaStringList =
        engine.dispatchBlocking(
            MegaChatApiGateway::getMessageReactions,
            listOf(chatId, msgId),
        ) { StubMegaStringList() }

    override fun getMessageReactionCount(chatId: Long, msgId: Long, reaction: String): Int =
        engine.dispatchBlocking(
            MegaChatApiGateway::getMessageReactionCount,
            listOf(chatId, msgId, reaction),
        ) { 0 }

    override fun getReactionUsers(chatId: Long, msgId: Long, reaction: String): MegaHandleList =
        engine.dispatchBlocking(
            MegaChatApiGateway::getReactionUsers,
            listOf(chatId, msgId, reaction),
        ) { StubMegaHandleList() }

    override suspend fun setSFUid(sfuId: Int) {
        engine.dispatch(MegaChatApiGateway::setSFUid, listOf(sfuId)) {}
    }

    override fun setMessageSeen(chatId: Long, msgId: Long): Boolean =
        engine.dispatchBlocking(MegaChatApiGateway::setMessageSeen, listOf(chatId, msgId)) {
            true
        }

    override fun getLastMessageSeenId(chatId: Long): Long =
        engine.dispatchBlocking(MegaChatApiGateway::getLastMessageSeenId, listOf(chatId)) {
            MegaChatApiJava.MEGACHAT_INVALID_HANDLE
        }

    override fun delReaction(
        chatId: Long,
        msgId: Long,
        reaction: String,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::delReaction,
        listOf(chatId, msgId, reaction, listener),
        listener,
        MegaChatRequest.TYPE_MANAGE_REACTION,
    )

    override fun sendGiphy(
        chatId: Long,
        srcMp4: String?,
        srcWebp: String?,
        sizeMp4: Long,
        sizeWebp: Long,
        width: Int,
        height: Int,
        title: String?,
    ): MegaChatMessage =
        engine.dispatchBlocking(
            MegaChatApiGateway::sendGiphy,
            listOf(chatId, srcMp4, srcWebp, sizeMp4, sizeWebp, width, height, title),
        ) { StubMegaChatMessage() }

    override fun attachContacts(chatId: Long, contactHandles: MegaHandleList): MegaChatMessage =
        engine.dispatchBlocking(
            MegaChatApiGateway::attachContacts,
            listOf(chatId, contactHandles),
        ) { StubMegaChatMessage() }

    override suspend fun forwardContact(
        sourceChatId: Long,
        msgId: Long,
        targetChatId: Long,
    ): MegaChatMessage? =
        engine.dispatch(
            MegaChatApiGateway::forwardContact,
            listOf(sourceChatId, msgId, targetChatId),
        ) { StubMegaChatMessage() }

    override suspend fun deleteMessage(chatId: Long, msgId: Long): MegaChatMessage? =
        engine.dispatch(MegaChatApiGateway::deleteMessage, listOf(chatId, msgId)) {
            StubMegaChatMessage()
        }

    override suspend fun revokeAttachmentMessage(chatId: Long, msgId: Long): MegaChatMessage? =
        engine.dispatch(MegaChatApiGateway::revokeAttachmentMessage, listOf(chatId, msgId)) {
            StubMegaChatMessage()
        }

    override suspend fun editMessage(chatId: Long, msgId: Long, msg: String): MegaChatMessage? =
        engine.dispatch(MegaChatApiGateway::editMessage, listOf(chatId, msgId, msg)) {
            StubMegaChatMessage()
        }

    override suspend fun editGeolocation(
        chatId: Long,
        msgId: Long,
        longitude: Float,
        latitude: Float,
        img: String,
    ): MegaChatMessage? =
        engine.dispatch(
            MegaChatApiGateway::editGeolocation,
            listOf(chatId, msgId, longitude, latitude, img),
        ) { StubMegaChatMessage() }

    override fun setLimitsInCall(
        chatId: Long,
        callDur: Long?,
        numUsers: Long?,
        numClients: Long?,
        numClientsPerUser: Long?,
        divider: Long?,
        listener: MegaChatRequestListenerInterface?,
    ) = completeChatRequest(
        MegaChatApiGateway::setLimitsInCall,
        listOf(chatId, callDur, numUsers, numClients, numClientsPerUser, divider, listener),
        listener,
        MegaChatRequest.TYPE_SET_LIMIT_CALL,
    )

    override suspend fun removeFailedMessage(chatId: Long, rowId: Long) {
        engine.dispatch(MegaChatApiGateway::removeFailedMessage, listOf(chatId, rowId)) {}
    }

    override fun setChatRetentionTime(
        chatId: Long,
        period: Long,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::setChatRetentionTime,
        listOf(chatId, period, listener),
        listener,
        MegaChatRequest.TYPE_SET_RETENTION_TIME,
    )

    override suspend fun getChatPresenceConfig(): MegaChatPresenceConfig? =
        engine.dispatch(MegaChatApiGateway::getChatPresenceConfig, emptyList()) { null }

    override suspend fun setIgnoredCall(chatId: Long): Boolean =
        engine.dispatch(MegaChatApiGateway::setIgnoredCall, listOf(chatId)) { true }

    override fun createMeeting(
        title: String,
        speakRequest: Boolean,
        waitingRoom: Boolean,
        openInvite: Boolean,
        listener: MegaChatRequestListenerInterface,
    ) = completeChatRequest(
        MegaChatApiGateway::createMeeting,
        listOf(title, speakRequest, waitingRoom, openInvite, listener),
        listener,
        MegaChatRequest.TYPE_CREATE_CHATROOM,
    )

    override suspend fun setUserTyping(chatId: Long) {
        engine.dispatch(MegaChatApiGateway::setUserTyping, listOf(chatId)) {}
    }

    override suspend fun setUserStoppedTyping(chatId: Long) {
        engine.dispatch(MegaChatApiGateway::setUserStoppedTyping, listOf(chatId)) {}
    }
}
