package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getChatRequestListener
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.listener.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.chat.ChatConnectionStatusMapper
import mega.privacy.android.data.mapper.chat.ChatHistoryLoadStatusMapper
import mega.privacy.android.data.mapper.chat.ChatInitStateMapper
import mega.privacy.android.data.mapper.chat.ChatListItemMapper
import mega.privacy.android.data.mapper.chat.ChatMessageMapper
import mega.privacy.android.data.mapper.chat.ChatPreviewMapper
import mega.privacy.android.data.mapper.chat.ChatRequestMapper
import mega.privacy.android.data.mapper.chat.ChatRoomMapper
import mega.privacy.android.data.mapper.chat.CombinedChatRoomMapper
import mega.privacy.android.data.mapper.chat.ConnectionStateMapper
import mega.privacy.android.data.mapper.chat.MegaChatPeerListMapper
import mega.privacy.android.data.mapper.chat.PendingMessageListMapper
import mega.privacy.android.data.mapper.notification.ChatMessageNotificationBehaviourMapper
import mega.privacy.android.data.model.ChatRoomUpdate
import mega.privacy.android.data.model.ChatUpdate
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus
import mega.privacy.android.domain.entity.chat.ChatInitState
import mega.privacy.android.domain.entity.chat.ChatListItem
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.settings.ChatSettings
import mega.privacy.android.domain.exception.chat.ParticipantAlreadyExistsException
import mega.privacy.android.domain.exception.chat.ResourceDoesNotExistChatException
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.ChatRepository
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatContainsMeta
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaUser
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Default implementation of [ChatRepository]
 *
 * @property megaChatApiGateway                     [MegaChatApiGateway]
 * @property megaApiGateway                         [MegaApiGateway]
 * @property chatRequestMapper                      [ChatRequestMapper]
 * @property localStorageGateway                    [MegaLocalStorageGateway]
 * @property chatRoomMapper                         [ChatRoomMapper]
 * @property combinedChatRoomMapper                 [CombinedChatRoomMapper]
 * @property chatListItemMapper                     [ChatListItemMapper]
 * @property megaChatPeerListMapper                 [MegaChatPeerListMapper]
 * @property chatConnectionStatusMapper             [ChatConnectionStatusMapper]
 * @property connectionStateMapper                  [ConnectionStateMapper]
 * @property chatMessageMapper                      [ChatMessageMapper]
 * @property chatMessageNotificationBehaviourMapper [ChatMessageNotificationBehaviourMapper]
 * @property chatHistoryLoadStatusMapper            [ChatHistoryLoadStatusMapper]
 * @property sharingScope                           [CoroutineScope]
 * @property ioDispatcher                           [CoroutineDispatcher]
 * @property appEventGateway                        [AppEventGateway]
 */
@Singleton
internal class ChatRepositoryImpl @Inject constructor(
    private val megaChatApiGateway: MegaChatApiGateway,
    private val megaApiGateway: MegaApiGateway,
    private val chatRequestMapper: ChatRequestMapper,
    private val chatPreviewMapper: ChatPreviewMapper,
    private val localStorageGateway: MegaLocalStorageGateway,
    private val chatRoomMapper: ChatRoomMapper,
    private val combinedChatRoomMapper: CombinedChatRoomMapper,
    private val chatListItemMapper: ChatListItemMapper,
    private val megaChatPeerListMapper: MegaChatPeerListMapper,
    private val chatConnectionStatusMapper: ChatConnectionStatusMapper,
    private val connectionStateMapper: ConnectionStateMapper,
    private val chatMessageMapper: ChatMessageMapper,
    private val chatMessageNotificationBehaviourMapper: ChatMessageNotificationBehaviourMapper,
    private val chatHistoryLoadStatusMapper: ChatHistoryLoadStatusMapper,
    private val chatInitStateMapper: ChatInitStateMapper,
    @ApplicationScope private val sharingScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val appEventGateway: AppEventGateway,
    private val pendingMessageListMapper: PendingMessageListMapper,
    private val megaLocalRoomGateway: MegaLocalRoomGateway,
    private val databaseHandler: DatabaseHandler,
) : ChatRepository {

    private var chatRoomUpdates: HashMap<Long, Flow<ChatRoomUpdate>> = hashMapOf()
    private val chatRoomUpdatesMutex = Mutex()
    private val joiningIds = mutableSetOf<Long>()

    override suspend fun getChatInitState(): ChatInitState = withContext(ioDispatcher) {
        chatInitStateMapper(megaChatApiGateway.initState)
    }

    override suspend fun initAnonymousChat(): ChatInitState = withContext(ioDispatcher) {
        chatInitStateMapper(megaChatApiGateway.initAnonymous())
    }

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

    override suspend fun getChatRoom(chatId: Long): ChatRoom? =
        withContext(ioDispatcher) {
            megaChatApiGateway.getChatRoom(chatId)?.let { chat ->
                return@withContext chatRoomMapper(chat)
            }
        }

    override suspend fun getChatRoomByUser(userHandle: Long): ChatRoom? =
        withContext(ioDispatcher) {
            megaChatApiGateway.getChatRoomByUser(userHandle)?.let { chat ->
                return@withContext chatRoomMapper(chat)
            }
        }

    override suspend fun getChatListItem(chatId: Long): ChatListItem? =
        withContext(ioDispatcher) {
            megaChatApiGateway.getChatListItem(chatId)?.let(chatListItemMapper::invoke)
        }

    override suspend fun getAllChatListItems(): List<ChatListItem> =
        withContext(ioDispatcher) {
            megaChatApiGateway.getChatListItems(
                MegaChatApi.CHAT_FILTER_BY_NO_FILTER,
                MegaChatApi.CHAT_GET_GROUP
            )?.map { chatListItemMapper(it) } ?: emptyList()
        }

    override suspend fun getUnreadNonMeetingChatListItems(): List<ChatListItem> =
        withContext(ioDispatcher) {
            megaChatApiGateway.getChatListItems(
                mask = MegaChatApi.CHAT_FILTER_BY_READ_OR_UNREAD or MegaChatApi.CHAT_FILTER_BY_MEETING_OR_NON_MEETING,
                filter = MegaChatApi.CHAT_GET_UNREAD or MegaChatApi.CHAT_GET_NON_MEETING
            )?.map(chatListItemMapper::invoke) ?: emptyList()
        }

    override suspend fun getUnreadMeetingChatListItems(): List<ChatListItem> =
        withContext(ioDispatcher) {
            megaChatApiGateway.getChatListItems(
                mask = MegaChatApi.CHAT_FILTER_BY_READ_OR_UNREAD or MegaChatApi.CHAT_FILTER_BY_MEETING_OR_NON_MEETING,
                filter = MegaChatApi.CHAT_GET_UNREAD or MegaChatApi.CHAT_GET_MEETING
            )?.map(chatListItemMapper::invoke) ?: emptyList()
        }

    override suspend fun setOpenInvite(chatId: Long): Boolean =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaChatApiGateway.getChatRoom(chatId)?.let { chat ->
                    megaChatApiGateway.setOpenInvite(
                        chatId,
                        !chat.isOpenInvite,
                        OptionalMegaChatRequestListenerInterface(
                            onRequestFinish = onRequestSetOpenInviteCompleted(continuation)
                        )
                    )
                }
            }
        }

    override suspend fun setOpenInvite(
        chatId: Long,
        isOpenInvite: Boolean,
    ): ChatRequest = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaChatRequestListenerInterface(
                onRequestFinish = { request: MegaChatRequest, error: MegaChatError ->
                    if (error.errorCode == MegaChatError.ERROR_OK || error.errorCode == MegaChatError.ERROR_EXIST) {
                        continuation.resumeWith(Result.success(chatRequestMapper(request)))
                    } else {
                        continuation.failWithError(error, "onRequestCompleted")
                    }
                }
            )

            megaChatApiGateway.setOpenInvite(
                chatId,
                isOpenInvite,
                listener
            )

            continuation.invokeOnCancellation { megaChatApiGateway.removeRequestListener(listener) }
        }
    }

    override suspend fun setWaitingRoom(
        chatId: Long,
        enabled: Boolean,
    ): ChatRequest = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaChatRequestListenerInterface(
                onRequestFinish = onRequestCompleted(continuation)
            )

            megaChatApiGateway.setWaitingRoom(
                chatId,
                enabled,
                listener
            )

            continuation.invokeOnCancellation { megaChatApiGateway.removeRequestListener(listener) }
        }
    }

    private fun onRequestSetOpenInviteCompleted(continuation: Continuation<Boolean>) =
        { request: MegaChatRequest, error: MegaChatError ->
            if (error.errorCode == MegaChatError.ERROR_OK) {
                continuation.resumeWith(Result.success(request.flag))
            } else {
                continuation.failWithError(error, "onRequestSetOpenInviteCompleted")
            }
        }

    override suspend fun setChatTitle(chatId: Long, title: String) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaChatRequestListenerInterface(
                onRequestFinish = onRequestCompleted(continuation)
            )

            megaChatApiGateway.setChatTitle(
                chatId,
                title,
                listener
            )

            continuation.invokeOnCancellation { megaChatApiGateway.removeRequestListener(listener) }
        }
    }

    private fun onRequestCompleted(continuation: Continuation<ChatRequest>) =
        { request: MegaChatRequest, error: MegaChatError ->
            if (error.errorCode == MegaChatError.ERROR_OK) {
                continuation.resumeWith(Result.success(chatRequestMapper(request)))
            } else {
                continuation.failWithError(error, "onRequestCompleted")
            }
        }

    override suspend fun getChatFilesFolderId(): NodeId? = withContext(ioDispatcher) {
        localStorageGateway.getChatFilesFolderHandle()?.let { NodeId(it) }
    }

    override suspend fun getAllChatRooms(): List<CombinedChatRoom> =
        withContext(ioDispatcher) {
            megaChatApiGateway.getChatRooms().mapNotNull { chatRoom ->
                megaChatApiGateway.getChatListItem(chatRoom.chatId)?.let { chatListItem ->
                    combinedChatRoomMapper(chatRoom, chatListItem)
                }
            }
        }

    override suspend fun getMeetingChatRooms(): List<CombinedChatRoom> =
        withContext(ioDispatcher) {
            megaChatApiGateway.getMeetingChatRooms()?.mapNotNull { chatRoom ->
                megaChatApiGateway.getChatListItem(chatRoom.chatId)?.let { chatListItem ->
                    combinedChatRoomMapper(chatRoom, chatListItem)
                }
            } ?: emptyList()
        }

    override suspend fun getNonMeetingChatRooms(): List<CombinedChatRoom> =
        withContext(ioDispatcher) {
            mutableListOf<MegaChatRoom>().apply {
                megaChatApiGateway.getIndividualChatRooms()?.let(::addAll)
                megaChatApiGateway.getGroupChatRooms()?.let(::addAll)
            }.mapNotNull { chatRoom ->
                megaChatApiGateway.getChatListItem(chatRoom.chatId)?.let { chatListItem ->
                    combinedChatRoomMapper(chatRoom, chatListItem)
                }
            }
        }

    override suspend fun getArchivedChatRooms(): List<CombinedChatRoom> =
        withContext(ioDispatcher) {
            megaChatApiGateway.getChatListItems(
                MegaChatApi.CHAT_FILTER_BY_ARCHIVED_OR_NON_ARCHIVED,
                MegaChatApi.CHAT_GET_ARCHIVED
            )?.mapNotNull { item ->
                megaChatApiGateway.getChatRoom(item.chatId)?.let { chatRoom ->
                    combinedChatRoomMapper(chatRoom, item)
                }
            } ?: emptyList()
        }

    override suspend fun getCombinedChatRoom(chatId: Long): CombinedChatRoom? =
        withContext(ioDispatcher) {
            val chatRoom = megaChatApiGateway.getChatRoom(chatId) ?: return@withContext null
            val chatListItem = megaChatApiGateway.getChatListItem(chatId) ?: return@withContext null
            combinedChatRoomMapper(chatRoom, chatListItem)
        }

    override suspend fun inviteToChat(chatId: Long, contactsData: List<String>) =
        withContext(ioDispatcher) {
            contactsData.forEach { email ->
                val userHandle = megaApiGateway.getContact(email)?.handle ?: -1
                megaChatApiGateway.inviteToChat(chatId, userHandle, null)
            }
        }

    override suspend fun inviteParticipantToChat(chatId: Long, handle: Long): ChatRequest =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->

                val listener = OptionalMegaChatRequestListenerInterface(
                    onRequestFinish = { request: MegaChatRequest, error: MegaChatError ->
                        when (error.errorCode) {
                            MegaChatError.ERROR_OK -> continuation.resumeWith(
                                Result.success(chatRequestMapper(request))
                            )

                            MegaChatError.ERROR_EXIST -> continuation.resumeWith(
                                Result.failure(ParticipantAlreadyExistsException())
                            )

                            else -> continuation.failWithError(error, "inviteParticipantToChat")
                        }
                    }
                )

                megaChatApiGateway.inviteToChat(
                    chatId,
                    handle,
                    listener
                )

                continuation.invokeOnCancellation {
                    megaChatApiGateway.removeRequestListener(
                        listener
                    )
                }
            }
        }

    override suspend fun setPublicChatToPrivate(chatId: Long): ChatRequest =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaChatApiGateway.setPublicChatToPrivate(
                    chatId,
                    OptionalMegaChatRequestListenerInterface(
                        onRequestFinish = onRequestCompleted(continuation)
                    )
                )
            }
        }

    override suspend fun createChatLink(chatId: Long): ChatRequest = withContext(ioDispatcher) {
        suspendCoroutine { continuation ->
            megaChatApiGateway.createChatLink(
                chatId,
                OptionalMegaChatRequestListenerInterface(
                    onRequestFinish = onRequestCompleted(continuation)
                )
            )
        }
    }

    override suspend fun removeChatLink(chatId: Long): ChatRequest =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaChatApiGateway.removeChatLink(
                    chatId,
                    OptionalMegaChatRequestListenerInterface(
                        onRequestFinish = onRequestCompleted(continuation)
                    )
                )
            }
        }

    override suspend fun openChatPreview(link: String) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaChatRequestListenerInterface(
                onRequestFinish = { request: MegaChatRequest, error: MegaChatError ->
                    when (error.errorCode) {
                        MegaChatError.ERROR_OK, MegaChatError.ERROR_EXIST -> {
                            continuation.resume(chatPreviewMapper(request, error.errorCode))
                        }

                        MegaChatError.ERROR_NOENT -> {
                            continuation.resumeWithException(ResourceDoesNotExistChatException())
                        }

                        else -> {
                            continuation.failWithError(error, "openChatPreview")
                        }
                    }
                }
            )

            megaChatApiGateway.openChatPreview(link, listener)

            continuation.invokeOnCancellation {
                megaChatApiGateway.removeRequestListener(listener)
            }
        }
    }

    override suspend fun checkChatLink(link: String): ChatRequest = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getChatRequestListener("checkChatLink") {
                chatRequestMapper(it)
            }

            megaChatApiGateway.checkChatLink(link, listener)

            continuation.invokeOnCancellation {
                megaChatApiGateway.removeRequestListener(listener)
            }
        }
    }

    override suspend fun queryChatLink(chatId: Long): ChatRequest =
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getChatRequestListener("queryChatLink") {
                chatRequestMapper(it)
            }

            megaChatApiGateway.queryChatLink(chatId, listener)

            continuation.invokeOnCancellation {
                megaChatApiGateway.removeRequestListener(listener)
            }
        }

    override suspend fun autojoinPublicChat(chatId: Long) = withContext(ioDispatcher) {
        if (joiningIds.contains(chatId)) return@withContext
        joiningIds.add(chatId)
        runCatching {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getChatRequestListener("autojoinPublicChat") {}

                megaChatApiGateway.autojoinPublicChat(chatId, listener)

                continuation.invokeOnCancellation {
                    megaChatApiGateway.removeRequestListener(listener)
                }
            }
        }.also {
            joiningIds.remove(chatId)
        }.getOrThrow()
    }

    override suspend fun autorejoinPublicChat(
        chatId: Long,
        publicHandle: Long,
    ) = withContext(ioDispatcher) {
        if (joiningIds.contains(chatId) && joiningIds.contains(publicHandle)) return@withContext
        joiningIds.add(chatId)
        joiningIds.add(publicHandle)
        runCatching {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getChatRequestListener("autorejoinPublicChat") {}

                megaChatApiGateway.autorejoinPublicChat(chatId, publicHandle, listener)

                continuation.invokeOnCancellation {
                    megaChatApiGateway.removeRequestListener(listener)
                }
            }
        }.also {
            joiningIds.remove(chatId)
        }.getOrThrow()
    }

    override suspend fun hasWaitingRoomChatOptions(chatOptionsBitMask: Int): Boolean =
        withContext(ioDispatcher) {
            MegaChatApi.hasChatOptionEnabled(
                MegaChatApi.CHAT_OPTION_WAITING_ROOM,
                chatOptionsBitMask
            )
        }

    private fun onRequestQueryChatLinkCompleted(continuation: Continuation<ChatRequest>) =
        { request: MegaChatRequest, error: MegaChatError ->
            if (error.errorCode == MegaChatError.ERROR_OK || error.errorCode == MegaChatError.ERROR_NOENT) {
                continuation.resumeWith(Result.success(chatRequestMapper(request)))
            } else {
                continuation.failWithError(error, "onRequestQueryChatLinkCompleted")
            }
        }

    override suspend fun inviteContact(email: String): InviteContactRequest =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaApiGateway.inviteContact(
                    email,
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = onRequestInviteContactCompleted(continuation)
                    )
                )
            }
        }

    private fun onRequestInviteContactCompleted(continuation: Continuation<InviteContactRequest>) =
        { request: MegaRequest, error: MegaError ->
            when (error.errorCode) {
                MegaError.API_OK -> continuation.resumeWith(Result.success(InviteContactRequest.Sent))
                MegaError.API_EEXIST -> {
                    if (megaApiGateway.outgoingContactRequests()
                            .any { it.targetEmail == request.email }
                    ) {
                        continuation.resumeWith(Result.success(InviteContactRequest.AlreadySent))
                    } else {
                        continuation.resumeWith(Result.success(InviteContactRequest.AlreadyContact))
                    }
                }

                MegaError.API_EARGS -> continuation.resumeWith(Result.success(InviteContactRequest.InvalidEmail))
                else -> continuation.failWithError(error, "onRequestInviteContactCompleted")
            }
        }

    override suspend fun updateChatPermissions(
        chatId: Long,
        handle: Long,
        permission: ChatRoomPermission,
    ) =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                val privilege = when (permission) {
                    ChatRoomPermission.Moderator -> MegaChatRoom.PRIV_MODERATOR
                    ChatRoomPermission.Standard -> MegaChatRoom.PRIV_STANDARD
                    ChatRoomPermission.ReadOnly -> MegaChatRoom.PRIV_RO
                    else -> MegaChatRoom.PRIV_UNKNOWN
                }
                megaChatApiGateway.updateChatPermissions(
                    chatId, handle, privilege,
                    OptionalMegaChatRequestListenerInterface(
                        onRequestFinish = onRequestCompleted(continuation)
                    )
                )
            }
        }

    override suspend fun removeFromChat(
        chatId: Long, handle: Long,
    ): ChatRequest = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val callback = continuation.getChatRequestListener(
                methodName = "removeFromChat",
                chatRequestMapper::invoke
            )

            megaChatApiGateway.removeFromChat(
                chatId,
                handle,
                callback
            )

            continuation.invokeOnCancellation { megaChatApiGateway.removeRequestListener(callback) }
        }
    }

    override suspend fun leaveChat(
        chatId: Long,
    ): ChatRequest = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val callback = continuation.getChatRequestListener(
                methodName = "leaveChat",
                chatRequestMapper::invoke
            )
            megaChatApiGateway.leaveChat(
                chatId,
                callback
            )

            continuation.invokeOnCancellation { megaChatApiGateway.removeRequestListener(callback) }
        }
    }

    override fun monitorChatRoomUpdates(chatId: Long) = flow {
        getChatRoomUpdates(chatId)?.let { updates ->
            emitAll(updates.filterIsInstance<ChatRoomUpdate.OnChatRoomUpdate>()
                .mapNotNull { it.chat }
                .map { chatRoomMapper(it) }
                .flowOn(ioDispatcher))
        }
    }

    override suspend fun loadMessages(chatId: Long, count: Int): ChatHistoryLoadStatus =
        withContext(ioDispatcher) {
            chatHistoryLoadStatusMapper(megaChatApiGateway.loadMessages(chatId, count))
        }

    override fun monitorOnMessageLoaded(chatId: Long) = flow {
        getChatRoomUpdates(chatId)?.let { updates ->
            emitAll(updates.filterIsInstance<ChatRoomUpdate.OnMessageLoaded>()
                .map { it.msg?.let { message -> chatMessageMapper(message) } }
                .flowOn(ioDispatcher))
        }
    }

    override fun monitorChatListItemUpdates(): Flow<ChatListItem> =
        megaChatApiGateway.chatUpdates
            .filterIsInstance<ChatUpdate.OnChatListItemUpdate>()
            .mapNotNull { it.item }
            .map { chatListItemMapper(it) }
            .flowOn(ioDispatcher)

    override suspend fun isChatNotifiable(chatId: Long): Boolean =
        withContext(ioDispatcher) {
            megaApiGateway.isChatNotifiable(chatId)
        }

    override suspend fun isChatLastMessageGeolocation(chatId: Long): Boolean =
        withContext(ioDispatcher) {
            val chat = megaChatApiGateway.getChatListItem(chatId) ?: return@withContext false
            val lastMessage = megaChatApiGateway.getMessage(chatId, chat.lastMessageId)
            chat.lastMessageType == MegaChatMessage.TYPE_CONTAINS_META
                    && lastMessage?.containsMeta?.type == MegaChatContainsMeta.CONTAINS_META_GEOLOCATION
        }

    override fun monitorMyEmail(): Flow<String?> = megaApiGateway.globalUpdates
        .filterIsInstance<GlobalUpdate.OnUsersUpdate>()
        .mapNotNull {
            it.users?.find { user ->
                user.isOwnChange <= 0 && user.hasChanged(MegaUser.CHANGE_TYPE_EMAIL.toLong()) && user.email == megaApiGateway.accountEmail
            }
        }
        .map {
            megaChatApiGateway.getMyEmail()
        }
        .catch { Timber.e(it) }
        .flowOn(ioDispatcher)
        .shareIn(sharingScope, SharingStarted.WhileSubscribed(), replay = 1)

    override fun monitorMyName(): Flow<String?> = megaApiGateway.globalUpdates
        .filterIsInstance<GlobalUpdate.OnUsersUpdate>()
        .mapNotNull {
            it.users?.find { user ->
                user.isOwnChange <= 0 &&
                        (user.hasChanged(MegaUser.CHANGE_TYPE_FIRSTNAME.toLong()) ||
                                user.hasChanged(MegaUser.CHANGE_TYPE_LASTNAME.toLong())) &&
                        user.email == megaApiGateway.accountEmail
            }
        }
        .map {
            megaChatApiGateway.getMyFullname()
        }
        .catch { Timber.e(it) }
        .flowOn(ioDispatcher)
        .shareIn(sharingScope, SharingStarted.WhileSubscribed(), replay = 1)

    override suspend fun resetChatSettings() = withContext(ioDispatcher) {
        if (localStorageGateway.getChatSettings() == null) {
            localStorageGateway.setChatSettings(ChatSettings())
        }
    }

    override suspend fun signalPresenceActivity() =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaChatApiGateway.signalPresenceActivity(
                    OptionalMegaChatRequestListenerInterface(
                        onRequestFinish = { _: MegaChatRequest, error: MegaChatError ->
                            if (error.errorCode == MegaChatError.ERROR_OK) {
                                continuation.resume(Unit)
                            } else {
                                continuation.failWithError(error, "signalPresenceActivity")
                            }
                        }
                    )
                )
            }
        }

    override suspend fun clearChatHistory(chatId: Long) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = OptionalMegaChatRequestListenerInterface(
                    onRequestFinish = { _: MegaChatRequest, error: MegaChatError ->
                        if (error.errorCode == MegaChatError.ERROR_OK) {
                            localStorageGateway.removePendingMessageByChatId(chatId)
                            continuation.resume(Unit)
                        } else {
                            continuation.failWithError(error, "clearChatHistory")
                        }
                    }
                )
                megaChatApiGateway.clearChatHistory(chatId = chatId, listener = listener)
                continuation.invokeOnCancellation {
                    megaChatApiGateway.removeRequestListener(listener)
                }
            }
        }

    override suspend fun archiveChat(chatId: Long, archive: Boolean) =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaChatApiGateway.archiveChat(
                    chatId = chatId,
                    archive = archive,
                    OptionalMegaChatRequestListenerInterface(
                        onRequestFinish = { _: MegaChatRequest, error: MegaChatError ->
                            if (error.errorCode == MegaChatError.ERROR_OK) {
                                continuation.resume(Unit)
                            } else {
                                continuation.failWithError(error, "archiveChat")
                            }
                        }
                    )
                )
            }
        }

    override suspend fun getPeerHandle(chatId: Long, peerNo: Long): Long? =
        withContext(ioDispatcher) {
            val chatRoom = megaChatApiGateway.getChatRoom(chatId)
            chatRoom?.getPeerHandle(peerNo)
        }

    override suspend fun createChat(isGroup: Boolean, userHandles: List<Long>) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getChatRequestListener("onRequestCreateChatCompleted") {
                    it.chatHandle
                }

                megaChatApiGateway.createChat(
                    isGroup = isGroup,
                    peers = megaChatPeerListMapper(userHandles),
                    listener = listener
                )
                continuation.invokeOnCancellation {
                    megaChatApiGateway.removeChatRequestListener(
                        listener
                    )
                }
            }

        }

    override suspend fun createGroupChat(
        title: String?,
        userHandles: List<Long>,
        speakRequest: Boolean,
        waitingRoom: Boolean,
        openInvite: Boolean,
    ): Long = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getChatRequestListener("createGroupChat") {
                it.chatHandle
            }

            megaChatApiGateway.createGroupChat(
                title = title,
                peers = megaChatPeerListMapper(userHandles),
                speakRequest = speakRequest,
                waitingRoom = waitingRoom,
                openInvite = openInvite,
                listener = listener
            )

            continuation.invokeOnCancellation {
                megaChatApiGateway.removeChatRequestListener(listener)
            }
        }
    }

    override suspend fun createPublicChat(
        title: String?,
        userHandles: List<Long>,
        speakRequest: Boolean,
        waitingRoom: Boolean,
        openInvite: Boolean,
    ): Long = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getChatRequestListener("createPublicChat") {
                it.chatHandle
            }

            megaChatApiGateway.createPublicChat(
                title = title,
                peers = megaChatPeerListMapper(userHandles),
                speakRequest = speakRequest,
                waitingRoom = waitingRoom,
                openInvite = openInvite,
                listener = listener
            )

            continuation.invokeOnCancellation {
                megaChatApiGateway.removeChatRequestListener(listener)
            }
        }
    }

    override suspend fun getContactHandle(email: String): Long? =
        withContext(ioDispatcher) {
            megaApiGateway.getContact(email)?.handle
        }

    override suspend fun getConnectionState() = withContext(ioDispatcher) {
        connectionStateMapper(megaChatApiGateway.getConnectedState())
    }

    override suspend fun getChatConnectionState(chatId: Long) = withContext(ioDispatcher) {
        chatConnectionStatusMapper(megaChatApiGateway.getChatConnectionState(chatId))
    }

    override fun monitorChatArchived(): Flow<String> = appEventGateway.monitorChatArchived()

    override suspend fun broadcastChatArchived(chatTitle: String) =
        appEventGateway.broadcastChatArchived(chatTitle)

    override suspend fun getNumUnreadChats() = withContext(ioDispatcher) {
        megaChatApiGateway.getNumUnreadChats()
    }

    override fun monitorJoinedSuccessfully(): Flow<Boolean> =
        appEventGateway.monitorJoinedSuccessfully()

    override suspend fun broadcastJoinedSuccessfully() =
        appEventGateway.broadcastJoinedSuccessfully()

    override fun monitorLeaveChat(): Flow<Long> = appEventGateway.monitorLeaveChat()

    override suspend fun broadcastLeaveChat(chatId: Long) =
        appEventGateway.broadcastLeaveChat(chatId)

    override suspend fun getMessage(chatId: Long, msgId: Long) = withContext(ioDispatcher) {
        megaChatApiGateway.getMessage(chatId, msgId)?.let { chatMessageMapper(it) }
    }

    override suspend fun getMessageFromNodeHistory(chatId: Long, msgId: Long) =
        withContext(ioDispatcher) {
            megaChatApiGateway.getMessageFromNodeHistory(chatId, msgId)
                ?.let { chatMessageMapper(it) }
        }

    override suspend fun getChatMessageNotificationBehaviour(
        beep: Boolean,
        defaultSound: String?,
    ) = withContext(ioDispatcher) {
        chatMessageNotificationBehaviourMapper(
            localStorageGateway.getChatSettings(),
            beep,
            defaultSound
        )
    }

    override suspend fun getPendingMessages(chatId: Long) = withContext(ioDispatcher) {
        pendingMessageListMapper(localStorageGateway.findPendingMessagesNotSent(chatId))
    }

    override suspend fun updatePendingMessage(
        idMessage: Long,
        transferTag: Int,
        nodeHandle: String?,
        state: Int,
    ) = withContext(ioDispatcher) {
        localStorageGateway.updatePendingMessage(
            idMessage,
            transferTag,
            nodeHandle,
            state
        )
    }

    override suspend fun createEphemeralAccountPlusPlus(
        firstName: String,
        lastName: String,
    ): String = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("createEphemeralAccountPlusPlus") {
                it.sessionKey
            }

            megaApiGateway.createEphemeralAccountPlusPlus(
                firstName = firstName,
                lastName = lastName,
                listener = listener,
            )

            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }

    override suspend fun attachNode(chatId: Long, nodeHandle: Long) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getChatRequestListener("attachNode") {
                it.megaChatMessage.tempId
            }

            megaChatApiGateway.attachNode(
                chatId = chatId,
                nodeHandle = nodeHandle,
                listener = listener,
            )

            continuation.invokeOnCancellation {
                megaChatApiGateway.removeRequestListener(listener)
            }
        }
    }

    override suspend fun attachVoiceMessage(chatId: Long, nodeHandle: Long) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getChatRequestListener("attachVoiceMessage") {
                    it.megaChatMessage.tempId
                }

                megaChatApiGateway.attachVoiceMessage(
                    chatId = chatId,
                    nodeHandle = nodeHandle,
                    listener = listener,
                )

                continuation.invokeOnCancellation {
                    megaChatApiGateway.removeRequestListener(listener)
                }
            }
        }

    override suspend fun getOwnPrivilege(chatId: Long): ChatRoomPermission =
        withContext(ioDispatcher) {
            getChatRoom(chatId)?.ownPrivilege ?: ChatRoomPermission.Unknown
        }

    override suspend fun getUserPrivilege(chatId: Long, userHandle: Long): ChatRoomPermission =
        withContext(ioDispatcher) {
            getChatRoom(chatId)?.peerPrivilegesByHandles?.get(userHandle)
                ?: ChatRoomPermission.Unknown
        }

    override fun getChatInvalidHandle(): Long = megaChatApiGateway.getChatInvalidHandle()

    override suspend fun hasCallInChatRoom(chatId: Long) = withContext(ioDispatcher) {
        megaChatApiGateway.hasCallInChatRoom(chatId)
    }

    override suspend fun getParticipantFirstName(handle: Long, contemplateEmail: Boolean): String? =
        withContext(ioDispatcher) {
            megaLocalRoomGateway.getContactByHandle(handle)?.shortName?.takeIf { it.isNotBlank() }
                ?: databaseHandler.findNonContactByHandle(handle.toString())?.shortName?.takeIf { it.isNotBlank() }
                ?: megaChatApiGateway.getUserAliasFromCache(handle)?.takeIf { it.isNotBlank() }
                ?: megaChatApiGateway.getUserFirstnameFromCache(handle)?.takeIf { it.isNotBlank() }
                ?: megaChatApiGateway.getUserLastnameFromCache(handle)?.takeIf { it.isNotBlank() }
                ?: if (contemplateEmail) {
                    megaChatApiGateway.getUserEmailFromCache(handle)?.takeIf { it.isNotBlank() }
                } else null
        }

    override suspend fun getParticipantFullName(handle: Long): String? = withContext(ioDispatcher) {
        megaLocalRoomGateway.getContactByHandle(handle)?.fullName?.takeIf { it.isNotBlank() }
            ?: databaseHandler.findNonContactByHandle(handle.toString())?.fullName?.takeIf { it.isNotBlank() }
            ?: megaChatApiGateway.getUserFullNameFromCache(handle)?.takeIf { it.isNotBlank() }
            ?: megaChatApiGateway.getUserEmailFromCache(handle)?.takeIf { it.isNotBlank() }
    }

    override suspend fun getMyUserHandle() = withContext(ioDispatcher) {
        megaChatApiGateway.getMyUserHandle()
    }

    override suspend fun isAudioLevelMonitorEnabled(chatId: Long): Boolean =
        withContext(ioDispatcher) {
            megaChatApiGateway.isAudioLevelMonitorEnabled(chatId)
        }

    override suspend fun enableAudioLevelMonitor(enable: Boolean, chatId: Long) =
        withContext(ioDispatcher) {
            megaChatApiGateway.enableAudioLevelMonitor(enable = enable, chatId = chatId)
        }

    override suspend fun endChatCall(chatId: Long): Boolean = withContext(ioDispatcher) {
        val chatCall = megaChatApiGateway.getChatCall(chatId) ?: return@withContext false
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getChatRequestListener("endChatCall") {
                it.flag
            }
            megaChatApiGateway.endChatCall(chatCall.callId, listener)

            continuation.invokeOnCancellation {
                megaChatApiGateway.removeRequestListener(listener)
            }
        }
    }

    override suspend fun isGeolocationEnabled() = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    if (error.errorCode == MegaChatError.ERROR_OK) {
                        continuation.resume(true)
                    } else {
                        continuation.resume(false)
                    }
                })

            megaApiGateway.isGeolocationEnabled(listener)

            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }

    override suspend fun enableGeolocation() = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("enableGeolocation") { }
            megaApiGateway.enableGeolocation(listener)
            continuation.invokeOnCancellation { megaApiGateway.removeRequestListener(listener) }
        }
    }

    private suspend fun getChatRoomUpdates(chatId: Long) = chatRoomUpdatesMutex.withLock {
        if (!chatRoomUpdates.containsKey(chatId)) {
            chatRoomUpdates[chatId] = megaChatApiGateway.openChatRoom(chatId).onCompletion {
                chatRoomUpdates.remove(chatId)
            }
        }

        chatRoomUpdates[chatId]
    }

    override suspend fun getMyFullName(): String? = withContext(ioDispatcher) {
        megaChatApiGateway.getMyFullname()
    }

    override suspend fun sendMessage(chatId: Long, message: String) = withContext(ioDispatcher) {
        chatMessageMapper(megaChatApiGateway.sendMessage(chatId, message))
    }

    override suspend fun setLastPublicHandle(handle: Long) {
        localStorageGateway.setLastPublicHandle(handle)
        localStorageGateway.setLastPublicHandleTimeStamp()
    }
}
