package mega.privacy.android.data.repository

import android.content.Context
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.R
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getChatRequestListener
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.gateway.chat.ChatStorageGateway
import mega.privacy.android.data.listener.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.ChatFilesFolderUserAttributeMapper
import mega.privacy.android.data.mapper.InviteContactRequestMapper
import mega.privacy.android.data.mapper.chat.ChatConnectionStatusMapper
import mega.privacy.android.data.mapper.chat.ChatHistoryLoadStatusMapper
import mega.privacy.android.data.mapper.chat.ChatInitStateMapper
import mega.privacy.android.data.mapper.chat.ChatListItemMapper
import mega.privacy.android.data.mapper.chat.ChatMessageMapper
import mega.privacy.android.data.mapper.chat.ChatPresenceConfigMapper
import mega.privacy.android.data.mapper.chat.ChatPreviewMapper
import mega.privacy.android.data.mapper.chat.ChatRequestMapper
import mega.privacy.android.data.mapper.chat.ChatRoomMapper
import mega.privacy.android.data.mapper.chat.CombinedChatRoomMapper
import mega.privacy.android.data.mapper.chat.ConnectionStateMapper
import mega.privacy.android.data.mapper.chat.MegaChatPeerListMapper
import mega.privacy.android.data.mapper.chat.PendingMessageListMapper
import mega.privacy.android.data.mapper.chat.messages.reactions.ReactionUpdateMapper
import mega.privacy.android.data.mapper.chat.paging.ChatGeolocationEntityMapper
import mega.privacy.android.data.mapper.chat.paging.ChatNodeEntityListMapper
import mega.privacy.android.data.mapper.chat.paging.GiphyEntityMapper
import mega.privacy.android.data.mapper.chat.paging.RichPreviewEntityMapper
import mega.privacy.android.data.mapper.chat.paging.TypedMessageEntityMapper
import mega.privacy.android.data.mapper.chat.update.ChatRoomMessageUpdateMapper
import mega.privacy.android.data.mapper.notification.ChatMessageNotificationBehaviourMapper
import mega.privacy.android.data.model.ChatRoomUpdate
import mega.privacy.android.data.model.ChatUpdate
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus
import mega.privacy.android.domain.entity.chat.ChatInitState
import mega.privacy.android.domain.entity.chat.ChatListItem
import mega.privacy.android.domain.entity.chat.ChatPendingChanges
import mega.privacy.android.domain.entity.chat.ChatPresenceConfig
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import mega.privacy.android.domain.entity.chat.RichLinkConfig
import mega.privacy.android.domain.entity.chat.messages.ChatMessageInfo
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageRequest
import mega.privacy.android.domain.entity.chat.notification.ChatMessageNotification
import mega.privacy.android.domain.entity.chat.room.update.ChatRoomMessageUpdate
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.settings.ChatSettings
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.chat.ParticipantAlreadyExistsException
import mega.privacy.android.domain.exception.chat.ResourceDoesNotExistChatException
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.ChatRepository
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatContainsMeta
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatNotificationListenerInterface
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaError.API_ENOENT
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
 * Chat repository impl
 *
 * @property megaChatApiGateway
 * @property megaApiGateway
 * @property chatRequestMapper
 * @property chatPreviewMapper
 * @property localStorageGateway
 * @property chatRoomMapper
 * @property combinedChatRoomMapper
 * @property chatListItemMapper
 * @property megaChatPeerListMapper
 * @property chatConnectionStatusMapper
 * @property connectionStateMapper
 * @property chatMessageMapper
 * @property chatMessageNotificationBehaviourMapper
 * @property chatHistoryLoadStatusMapper
 * @property chatInitStateMapper
 * @property sharingScope
 * @property ioDispatcher
 * @property appEventGateway
 * @property pendingMessageListMapper
 * @property megaLocalRoomGateway
 * @property databaseHandler
 * @property chatStorageGateway
 * @property typedMessageEntityMapper
 * @property richPreviewEntityMapper
 * @property giphyEntityMapper
 * @property chatGeolocationEntityMapper
 * @property chatNodeEntityListMapper
 * @property chatPresenceConfigMapper
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
    private val databaseHandler: Lazy<DatabaseHandler>,
    private val chatStorageGateway: ChatStorageGateway,
    private val typedMessageEntityMapper: TypedMessageEntityMapper,
    private val richPreviewEntityMapper: RichPreviewEntityMapper,
    private val giphyEntityMapper: GiphyEntityMapper,
    private val chatGeolocationEntityMapper: ChatGeolocationEntityMapper,
    private val chatNodeEntityListMapper: ChatNodeEntityListMapper,
    private val reactionUpdateMapper: ReactionUpdateMapper,
    private val chatRoomMessageUpdateMapper: ChatRoomMessageUpdateMapper,
    private val chatPresenceConfigMapper: ChatPresenceConfigMapper,
    @ApplicationContext private val context: Context,
    private val chatFilesFolderUserAttributeMapper: ChatFilesFolderUserAttributeMapper,
    private val inviteContactRequestMapper: InviteContactRequestMapper,
) : ChatRepository {
    private val richLinkConfig = MutableStateFlow(RichLinkConfig())
    private var chatRoomUpdates: HashMap<Long, Flow<ChatRoomUpdate>> = hashMapOf()
    private val joiningIds = mutableSetOf<Long>()
    private val joiningIdsFlow = MutableSharedFlow<MutableSet<Long>>()
    private val leavingIds = mutableSetOf<Long>()
    private val leavingIdsFlow = MutableSharedFlow<MutableSet<Long>>()
    private val openingChatWithLinkIds = mutableSetOf<Long>()

    private var myChatsFilesFolderIdFlow: MutableStateFlow<NodeId?> = MutableStateFlow(null)

    init {
        monitorChatsFilesFolderIdChanges()
    }

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

    override suspend fun getChatRooms(): List<ChatRoom> =
        withContext(ioDispatcher) {
            megaChatApiGateway.getChatRooms()
                .map { chatRoomMapper(it) }
        }

    override suspend fun getNoteToSelfChat(): ChatRoom? =
        withContext(ioDispatcher) {
            megaChatApiGateway.getNoteToSelfChat()?.let {
                chatRoomMapper(it)
            }
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
        }
    }

    override suspend fun checkChatLink(link: String): ChatRequest = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getChatRequestListener("checkChatLink") {
                chatRequestMapper(it)
            }
            megaChatApiGateway.checkChatLink(link, listener)
        }
    }

    override suspend fun queryChatLink(chatId: Long): ChatRequest =
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getChatRequestListener("queryChatLink") {
                chatRequestMapper(it)
            }
            megaChatApiGateway.queryChatLink(chatId, listener)
        }

    override suspend fun autojoinPublicChat(chatId: Long) = withContext(NonCancellable) {
        if (joiningIds.contains(chatId)) return@withContext
        joiningIds.add(chatId)
        joiningIdsFlow.emit(joiningIds)
        runCatching {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getChatRequestListener("autojoinPublicChat") {}
                megaChatApiGateway.autojoinPublicChat(chatId, listener)
            }
        }.also {
            joiningIds.remove(chatId)
            joiningIdsFlow.emit(joiningIds)
        }.getOrThrow()
    }

    override suspend fun autorejoinPublicChat(
        chatId: Long,
        publicHandle: Long,
    ) = withContext(NonCancellable) {
        if (joiningIds.contains(chatId) && joiningIds.contains(publicHandle)) return@withContext
        joiningIds.add(chatId)
        joiningIds.add(publicHandle)
        joiningIdsFlow.emit(joiningIds)
        runCatching {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getChatRequestListener("autorejoinPublicChat") {}
                megaChatApiGateway.autorejoinPublicChat(chatId, publicHandle, listener)
            }
        }.also {
            joiningIds.remove(chatId)
            joiningIdsFlow.emit(joiningIds)
        }.getOrThrow()
    }

    override suspend fun hasWaitingRoomChatOptions(chatOptionsBitMask: Int): Boolean =
        withContext(ioDispatcher) {
            MegaChatApi.hasChatOptionEnabled(
                MegaChatApi.CHAT_OPTION_WAITING_ROOM,
                chatOptionsBitMask
            )
        }

    override suspend fun inviteContact(email: String): InviteContactRequest =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaApiGateway.inviteContact(
                    email,
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = { request: MegaRequest, error: MegaError ->
                            launch {
                                continuation.resumeWith(runCatching {
                                    inviteContactRequestMapper(
                                        error,
                                        request.email,
                                        {
                                            megaApiGateway.getOutgoingContactRequests()
                                        },
                                        {
                                            megaApiGateway.getIncomingContactRequests()
                                        },
                                    )
                                }.onFailure { Timber.e(it) }
                                )
                            }
                        }
                    )
                )
            }
        }


    override suspend fun updateChatPermissions(
        chatId: Long,
        nodeId: NodeId,
        permission: ChatRoomPermission,
    ) = withContext(ioDispatcher) {
        suspendCoroutine { continuation ->
            val privilege = when (permission) {
                ChatRoomPermission.Moderator -> MegaChatRoom.PRIV_MODERATOR
                ChatRoomPermission.Standard -> MegaChatRoom.PRIV_STANDARD
                ChatRoomPermission.ReadOnly -> MegaChatRoom.PRIV_RO
                else -> MegaChatRoom.PRIV_UNKNOWN
            }
            megaChatApiGateway.updateChatPermissions(
                chatId, nodeId.longValue, privilege,
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
        }
    }

    override suspend fun leaveChat(chatId: Long): Unit = withContext(NonCancellable) {
        if (leavingIds.contains(chatId)) return@withContext
        leavingIds.add(chatId)
        leavingIdsFlow.emit(leavingIds)
        runCatching {
            suspendCancellableCoroutine { continuation ->
                val callback = continuation.getChatRequestListener(
                    methodName = "leaveChat",
                    chatRequestMapper::invoke
                )
                megaChatApiGateway.leaveChat(
                    chatId,
                    callback
                )
            }
        }.also {
            leavingIds.remove(chatId)
            leavingIdsFlow.emit(leavingIds)
        }.getOrThrow()
    }

    override fun monitorChatRoomUpdates(chatId: Long) =
        getChatRoomUpdates(chatId).filterIsInstance<ChatRoomUpdate.OnChatRoomUpdate>()
            .mapNotNull { it.chat }
            .map { chatRoomMapper(it) }
            .flowOn(ioDispatcher)


    override suspend fun loadMessages(chatId: Long, count: Int): ChatHistoryLoadStatus =
        withContext(ioDispatcher) {
            chatHistoryLoadStatusMapper(megaChatApiGateway.loadMessages(chatId, count))
        }

    override fun monitorOnMessageLoaded(chatId: Long) =
        getChatRoomUpdates(chatId).filterIsInstance<ChatRoomUpdate.OnMessageLoaded>()
            .map { it.msg?.let { message -> chatMessageMapper(message) } }
            .flowOn(ioDispatcher)

    override fun monitorMessageUpdates(chatId: Long): Flow<ChatRoomMessageUpdate> =
        getChatRoomUpdates(chatId).mapNotNull {
            chatRoomMessageUpdateMapper(it)
        }.flowOn(ioDispatcher)

    override fun monitorReactionUpdates(chatId: Long) =
        getChatRoomUpdates(chatId).filterIsInstance<ChatRoomUpdate.OnReactionUpdate>()
            .map { reactionUpdateMapper(it) }
            .flowOn(ioDispatcher)

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
                            continuation.resume(Unit)
                        } else {
                            continuation.failWithError(error, "clearChatHistory")
                        }
                    }
                )
                megaChatApiGateway.clearChatHistory(chatId = chatId, listener = listener)
            }
            chatStorageGateway.clearChatPendingMessages(chatId)
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

    override suspend fun createChat(isGroup: Boolean, userHandles: List<Long>?) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getChatRequestListener("onRequestCreateChatCompleted") {
                    it.chatHandle
                }
                megaChatApiGateway.createChat(
                    isGroup = isGroup,
                    peers = if (userHandles == null) null else megaChatPeerListMapper(userHandles),
                    listener = listener
                )
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
                ?: databaseHandler.get()
                    .findNonContactByHandle(handle.toString())?.shortName?.takeIf { it.isNotBlank() }
                ?: megaChatApiGateway.getUserAliasFromCache(handle)?.takeIf { it.isNotBlank() }
                ?: megaChatApiGateway.getUserFirstnameFromCache(handle)?.takeIf { it.isNotBlank() }
                ?: megaChatApiGateway.getUserLastnameFromCache(handle)?.takeIf { it.isNotBlank() }
                ?: if (contemplateEmail) {
                    megaChatApiGateway.getUserEmailFromCache(handle)?.takeIf { it.isNotBlank() }
                } else null
        }

    override suspend fun getParticipantFullName(handle: Long): String? = withContext(ioDispatcher) {
        megaLocalRoomGateway.getContactByHandle(handle)?.fullName?.takeIf { it.isNotBlank() }
            ?: databaseHandler.get()
                .findNonContactByHandle(handle.toString())?.fullName?.takeIf { it.isNotBlank() }
            ?: megaChatApiGateway.getUserAliasFromCache(handle)?.takeIf { it.isNotBlank() }
            ?: megaChatApiGateway.getUserFullNameFromCache(handle)?.takeIf { it.isNotBlank() }
            ?: megaChatApiGateway.getUserFirstnameFromCache(handle)?.takeIf { it.isNotBlank() }
            ?: megaChatApiGateway.getUserLastnameFromCache(handle)?.takeIf { it.isNotBlank() }
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

        }
    }

    override suspend fun enableGeolocation() = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("enableGeolocation") { }
            megaApiGateway.enableGeolocation(listener)
        }
    }

    private fun getChatRoomUpdates(chatId: Long) = synchronized(chatRoomUpdates) {
        chatRoomUpdates.getOrPut(chatId) {
            megaChatApiGateway.openChatRoom(chatId)
        }
    }

    override suspend fun getMyFullName(): String? = withContext(ioDispatcher) {
        megaChatApiGateway.getMyFullname()
    }

    override suspend fun sendMessage(chatId: Long, message: String) = withContext(ioDispatcher) {
        megaChatApiGateway.sendMessage(chatId, message)?.let { chatMessageMapper(it) }
    }

    override suspend fun setLastPublicHandle(handle: Long) {
        localStorageGateway.setLastPublicHandle(handle)
        localStorageGateway.setLastPublicHandleTimeStamp()
    }

    override suspend fun closeChatPreview(chatId: Long) {
        megaChatApiGateway.closeChatPreview(chatId)
    }

    override suspend fun shouldShowRichLinkWarning(): Boolean = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaError.API_OK || error.errorCode == MegaError.API_ENOENT) {
                        richLinkConfig.update { config ->
                            config.copy(
                                isShowRichLinkWarning = request.flag,
                                counterNotNowRichLinkWarning = request.number.toInt()
                            )
                        }
                        continuation.resumeWith(Result.success(request.flag))
                    } else {
                        continuation.failWithError(error, "shouldShowRichLinkWarning")
                    }
                }
            )
            megaApiGateway.shouldShowRichLinkWarning(listener)
        }
    }

    override suspend fun isRichPreviewsEnabled(): Boolean = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaError.API_OK || error.errorCode == MegaError.API_ENOENT) {
                        richLinkConfig.update { config ->
                            config.copy(isRichLinkEnabled = request.flag)
                        }
                        continuation.resumeWith(Result.success(request.flag))
                    } else {
                        continuation.failWithError(error, "isRichPreviewsEnabled")
                    }
                }
            )
            megaApiGateway.isRichPreviewsEnabled(listener)
        }
    }

    override suspend fun setRichLinkWarningCounterValue(value: Int): Int =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("setRichLinkWarningCounterValue") {
                    richLinkConfig.update { config ->
                        config.copy(
                            counterNotNowRichLinkWarning = value
                        )
                    }
                    value
                }
                megaApiGateway.setRichLinkWarningCounterValue(value, listener)
            }
        }


    override fun monitorRichLinkPreviewConfig(): Flow<RichLinkConfig> = richLinkConfig.asStateFlow()

    override fun hasUrl(url: String): Boolean = megaChatApiGateway.hasUrl(url)

    override suspend fun enableRichPreviews(enable: Boolean) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("enableRichLinkPreview") {
                richLinkConfig.update { config ->
                    config.copy(isRichLinkEnabled = enable)
                }
            }
            megaApiGateway.enableRichPreviews(enable, listener)
        }
    }

    override suspend fun storeMessages(messages: List<CreateTypedMessageRequest>) {
        withContext(ioDispatcher) {
            chatStorageGateway.storeMessages(
                messages = messages.map {
                    typedMessageEntityMapper(it)
                },
                richPreviews = messages.mapNotNull { request ->
                    request.chatRichPreviewInfo?.let {
                        richPreviewEntityMapper(
                            messageId = request.messageId,
                            info = it,
                        )
                    }
                },
                giphys = messages.mapNotNull { request ->
                    request.chatGifInfo?.let {
                        giphyEntityMapper(
                            messageId = request.messageId,
                            info = it,
                        )
                    }
                },
                geolocations = messages.mapNotNull { request ->
                    request.chatGeolocationInfo?.let {
                        chatGeolocationEntityMapper(
                            messageId = request.messageId,
                            info = it,
                        )
                    }
                },
                chatNodes = messages.map { request ->
                    chatNodeEntityListMapper(
                        messageId = request.messageId,
                        nodes = request.nodeList,
                    )
                }.flatten(),
            )
        }
    }

    override suspend fun clearChatMessages(chatId: Long) {
        withContext(ioDispatcher) {
            chatStorageGateway.clearChatMessages(chatId)
        }
    }

    override suspend fun getNextMessagePagingInfo(
        chatId: Long,
        timestamp: Long,
    ): ChatMessageInfo? =
        withContext(ioDispatcher) {
            chatStorageGateway.getNextMessage(chatId, timestamp)
        }

    override fun monitorJoiningChat(chatId: Long) = joiningIdsFlow
        .map { it.contains(chatId) }
        .distinctUntilChanged()
        .flowOn(ioDispatcher)

    override fun monitorLeavingChat(chatId: Long) = leavingIdsFlow
        .map { it.contains(chatId) }
        .distinctUntilChanged()
        .flowOn(ioDispatcher)

    override suspend fun sendGeolocation(
        chatId: Long,
        longitude: Float,
        latitude: Float,
        image: String,
    ) = withContext(ioDispatcher) {
        chatMessageMapper(megaChatApiGateway.sendGeolocation(chatId, longitude, latitude, image))
    }

    override suspend fun setChatDraftMessage(
        chatId: Long,
        draftMessage: String,
        editingMessageId: Long?,
    ) {
        val preference = megaLocalRoomGateway.monitorChatPendingChanges(chatId).firstOrNull()
            ?: ChatPendingChanges(chatId = chatId)
        megaLocalRoomGateway.setChatPendingChanges(
            preference.copy(
                draftMessage = draftMessage,
                editingMessageId = editingMessageId
            )
        )
    }

    override fun monitorChatPendingChanges(chatId: Long): Flow<ChatPendingChanges> {
        return megaLocalRoomGateway.monitorChatPendingChanges(chatId)
            .filterNotNull()
    }

    override fun getDefaultChatFolderName() = context.getString(R.string.my_chat_files_folder)

    override fun monitorChatMessages() =
        callbackFlow {
            val listener =
                MegaChatNotificationListenerInterface { _, chatId, message ->
                    trySend(Pair(chatId, message))
                }
            megaChatApiGateway.registerChatNotificationListener(listener)
            awaitClose {
                megaChatApiGateway.deregisterChatNotificationListener(listener)
            }
        }.map { (chatId, message) ->
            message?.let { ChatMessageNotification(chatId, chatMessageMapper(it)) }
        }.flowOn(ioDispatcher)

    override suspend fun setSFUid(sfuId: Int) = withContext(ioDispatcher) {
        megaChatApiGateway.setSFUid(sfuId)
    }

    override suspend fun setLimitsInCall(
        chatId: Long,
        callDur: Long?,
        numUsers: Long?,
        numClients: Long?,
        numClientsPerUser: Long?,
        divider: Long?,
    ) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getChatRequestListener("setLimitsInCall") {
                Timber.d("setLimitsInCall: $it")
            }
            megaChatApiGateway.setLimitsInCall(
                chatId,
                callDur,
                numUsers,
                numClients,
                numClientsPerUser,
                divider,
                listener
            )
        }
    }

    override suspend fun setChatRetentionTime(chatId: Long, period: Long) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getChatRequestListener("setChatRetentionTime") {
                    Timber.d("Establish the retention time successfully")
                }
                megaChatApiGateway.setChatRetentionTime(
                    chatId = chatId,
                    period = period,
                    listener = listener
                )
            }
        }

    override fun monitorUpgradeDialogClosed() = appEventGateway.monitorUpgradeDialogClosed()

    override suspend fun broadcastUpgradeDialogClosed() =
        appEventGateway.broadcastUpgradeDialogClosed()

    override suspend fun getChatPresenceConfig(): ChatPresenceConfig? = withContext(ioDispatcher) {
        megaChatApiGateway.getChatPresenceConfig()?.let { chatPresenceConfigMapper(it) }
    }

    override suspend fun getActiveChatListItems(): List<ChatListItem> =
        withContext(ioDispatcher) {
            megaChatApiGateway.getChatListItems(
                mask = MegaChatApi.CHAT_FILTER_BY_ACTIVE_OR_NON_ACTIVE + MegaChatApi.CHAT_FILTER_BY_ARCHIVED_OR_NON_ARCHIVED,
                filter = MegaChatApi.CHAT_GET_ACTIVE + MegaChatApi.CHAT_GET_NON_ARCHIVED
            )?.map(chatListItemMapper::invoke) ?: emptyList()
        }


    override suspend fun getArchivedChatListItems(): List<ChatListItem> =
        withContext(ioDispatcher) {
            megaChatApiGateway.getChatListItems(
                MegaChatApi.CHAT_FILTER_BY_ARCHIVED_OR_NON_ARCHIVED,
                MegaChatApi.CHAT_GET_ARCHIVED
            )?.map(chatListItemMapper::invoke) ?: emptyList()
        }

    override fun setChatOpeningWithLink(chatId: Long) {
        openingChatWithLinkIds.add(chatId)
    }

    override fun removeChatOpeningWithLink(chatId: Long) {
        openingChatWithLinkIds.remove(chatId)
    }

    override fun isChatOpeningWithLink(chatId: Long) = openingChatWithLinkIds.contains(chatId)

    override suspend fun setMyChatFilesFolder(nodeHandle: Long) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("setMyChatFilesFolder") {
                myChatsFilesFolderIdFlow.value = NodeId(nodeHandle)
                chatFilesFolderUserAttributeMapper(it.megaStringMap)?.let { value ->
                    megaApiGateway.base64ToHandle(value)
                        .takeIf { handle -> handle != megaApiGateway.getInvalidHandle() }
                }
            }
            megaApiGateway.setMyChatFilesFolder(nodeHandle, listener)
        }
    }

    override suspend fun getMyChatsFilesFolderId(): NodeId? =
        myChatsFilesFolderIdFlow.value ?: run {
            getMyChatsFilesFolderIdFromGateway()
        }

    private suspend fun getMyChatsFilesFolderIdFromGateway(): NodeId? = withContext(ioDispatcher) {
        runCatching {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("getMyChatFilesFolder") {
                    NodeId(it.nodeHandle)
                }
                megaApiGateway.getMyChatFilesFolder(listener)

            }
        }.getOrElse {
            //if error is API_ENOENT it means folder is not set, not an actual error. Otherwise re-throw the error
            if ((it as? MegaException)?.errorCode != API_ENOENT) {
                throw (it)
            } else {
                null
            }
        }?.also {
            myChatsFilesFolderIdFlow.value = it
        }
    }

    private fun monitorChatsFilesFolderIdChanges() {
        sharingScope.launch {
            megaApiGateway.globalUpdates
                .filterIsInstance<GlobalUpdate.OnUsersUpdate>()
                .filter {
                    println("filter") //TODO remove
                    val currentUserHandle = megaApiGateway.myUser?.handle
                    it.users?.any { user ->
                        user.isOwnChange == 0
                                && user.hasChanged(MegaUser.CHANGE_TYPE_MY_CHAT_FILES_FOLDER.toLong())
                                && user.handle == currentUserHandle
                    } == true
                }
                .catch { Timber.e(it) }
                .flowOn(ioDispatcher)
                .collect {
                    runCatching {
                        getMyChatsFilesFolderIdFromGateway()
                    }.onFailure {
                        Timber.e(it)
                    }
                }
        }
    }
}
