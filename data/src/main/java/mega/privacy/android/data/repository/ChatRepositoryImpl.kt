package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getChatRequestListener
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.DeviceEventGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.listener.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.chat.ChatConnectionStatusMapper
import mega.privacy.android.data.mapper.chat.ChatListItemMapper
import mega.privacy.android.data.mapper.chat.ChatMessageMapper
import mega.privacy.android.data.mapper.chat.ChatRequestMapper
import mega.privacy.android.data.mapper.chat.ChatRoomMapper
import mega.privacy.android.data.mapper.chat.CombinedChatRoomMapper
import mega.privacy.android.data.mapper.chat.ConnectionStateMapper
import mega.privacy.android.data.mapper.chat.MegaChatPeerListMapper
import mega.privacy.android.data.model.ChatRoomUpdate
import mega.privacy.android.data.model.ChatSettings
import mega.privacy.android.data.model.ChatUpdate
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatListItem
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.ChatRepository
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatContainsMeta
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaUser
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Default implementation of [ChatRepository]
 *
 * @property megaChatApiGateway                 [MegaChatApiGateway]
 * @property megaApiGateway                     [MegaApiGateway]
 * @property ioDispatcher                       [CoroutineDispatcher]
 * @property chatRequestMapper                  [ChatRequestMapper]
 * @property localStorageGateway                [MegaLocalStorageGateway]
 * @property chatRoomMapper                     [ChatRoomMapper]
 * @property combinedChatRoomMapper             [CombinedChatRoomMapper]
 * @property chatListItemMapper                 [ChatListItemMapper]
 * @property sharingScope                       [CoroutineScope]
 * @property ioDispatcher                       [CoroutineDispatcher]
 * @property deviceEventGateway                 [DeviceEventGateway]
 * @property megaChatPeerListMapper             [MegaChatPeerListMapper]
 * @property appEventGateway                    [AppEventGateway]
 */
internal class ChatRepositoryImpl @Inject constructor(
    private val megaChatApiGateway: MegaChatApiGateway,
    private val megaApiGateway: MegaApiGateway,
    private val chatRequestMapper: ChatRequestMapper,
    private val localStorageGateway: MegaLocalStorageGateway,
    private val chatRoomMapper: ChatRoomMapper,
    private val combinedChatRoomMapper: CombinedChatRoomMapper,
    private val chatListItemMapper: ChatListItemMapper,
    private val megaChatPeerListMapper: MegaChatPeerListMapper,
    private val chatConnectionStatusMapper: ChatConnectionStatusMapper,
    private val connectionStateMapper: ConnectionStateMapper,
    private val chatMessageMapper: ChatMessageMapper,
    @ApplicationScope private val sharingScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val deviceEventGateway: DeviceEventGateway,
    private val appEventGateway: AppEventGateway,
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

    private fun onRequestSetOpenInviteCompleted(continuation: Continuation<Boolean>) =
        { request: MegaChatRequest, error: MegaChatError ->
            if (error.errorCode == MegaChatError.ERROR_OK) {
                continuation.resumeWith(Result.success(request.flag))
            } else {
                continuation.failWithError(error, "onRequestSetOpenInviteCompleted")
            }
        }

    override suspend fun leaveChat(chatId: Long): ChatRequest =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaChatApiGateway.leaveChat(
                    chatId,
                    OptionalMegaChatRequestListenerInterface(
                        onRequestFinish = onRequestCompleted(continuation)
                    )
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

    override suspend fun getChatFilesFolderId(): NodeId? =
        localStorageGateway.getChatFilesFolderHandle()?.let { NodeId(it) }

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

    override suspend fun checkChatLink(link: String): ChatRequest =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaChatApiGateway.checkChatLink(
                    link,
                    OptionalMegaChatRequestListenerInterface(
                        onRequestFinish = onRequestCompleted(continuation)
                    )
                )
            }
        }

    override suspend fun queryChatLink(chatId: Long): ChatRequest =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaChatApiGateway.queryChatLink(
                    chatId,
                    OptionalMegaChatRequestListenerInterface(
                        onRequestFinish = onRequestQueryChatLinkCompleted(continuation)
                    )
                )
            }
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

    override suspend fun removeFromChat(chatId: Long, handle: Long): ChatRequest =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaChatApiGateway.removeFromChat(
                    chatId, handle,
                    OptionalMegaChatRequestListenerInterface(
                        onRequestFinish = onRequestCompleted(continuation)
                    )
                )
            }
        }

    override fun monitorChatRoomUpdates(chatId: Long): Flow<ChatRoom> =
        megaChatApiGateway.getChatRoomUpdates(chatId)
            .filterIsInstance<ChatRoomUpdate.OnChatRoomUpdate>()
            .mapNotNull { it.chat }
            .map { chatRoomMapper(it) }
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
            suspendCoroutine { continuation ->
                megaChatApiGateway.clearChatHistory(
                    chatId = chatId,
                    OptionalMegaChatRequestListenerInterface(
                        onRequestFinish = { _: MegaChatRequest, error: MegaChatError ->
                            if (error.errorCode == MegaChatError.ERROR_OK) {
                                continuation.resume(Unit)
                            } else {
                                continuation.failWithError(error, "clearChatHistory")
                            }
                        }
                    )
                )
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
}
