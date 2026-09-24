package mega.privacy.android.domain.usecase.contact

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.chat.ChatNotificationMuteState
import mega.privacy.android.domain.entity.contacts.ContactInfoState
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.exception.ContactDoesNotExistException
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.chat.GetChatRoomByUserUseCase
import mega.privacy.android.domain.usecase.chat.MonitorCallInChatUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatNotificationMuteStateUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatRetentionTimeUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.shares.MonitorContactInSharesCountUseCase
import javax.inject.Inject

/**
 * Monitor the aggregated live information of a contact or 1:1 chat peer.
 *
 * The peer is resolved either from an email (contact list entry point) or from a chat id
 * (1:1 chat entry point). A chat peer that is not a contact resolves to a reduced state built
 * from the chat room. The resolved data is then kept up to date through the per-concern
 * monitors (contact updates, presence, notification mute state, retention time, in-shares
 * and calls).
 *
 * When the peer has no 1:1 chat room yet and one is created later (e.g. by toggling the chat
 * notifications), the chat-scoped monitors attach to the new chat room automatically by
 * listening to the chat list item updates of the peer.
 *
 * @throws ContactDoesNotExistException when neither entry point resolves to a peer.
 */
class MonitorContactInfoUseCase @Inject constructor(
    private val getContactFromEmailUseCase: GetContactFromEmailUseCase,
    private val getContactFromChatUseCase: GetContactFromChatUseCase,
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val getChatRoomByUserUseCase: GetChatRoomByUserUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val monitorContactItemUpdatesUseCase: MonitorContactItemUpdatesUseCase,
    private val monitorUserPresenceUseCase: MonitorUserPresenceUseCase,
    private val monitorChatNotificationMuteStateUseCase: MonitorChatNotificationMuteStateUseCase,
    private val monitorChatRetentionTimeUseCase: MonitorChatRetentionTimeUseCase,
    private val monitorContactInSharesCountUseCase: MonitorContactInSharesCountUseCase,
    private val monitorCallInChatUseCase: MonitorCallInChatUseCase,
    private val chatRepository: ChatRepository,
) {
    /**
     * Invoke.
     *
     * @param email  Email of the contact, or null when entering from a chat.
     * @param chatId Id of the 1:1 chat with the peer, or null when entering by email.
     * @return Flow of [ContactInfoState].
     */
    operator fun invoke(email: String?, chatId: Long?): Flow<ContactInfoState> = flow {
        val peer = resolvePeer(email, chatId) ?: throw ContactDoesNotExistException()
        emitAll(monitorPeer(peer))
    }

    private suspend fun resolvePeer(email: String?, chatId: Long?): ResolvedPeer? {
        val skipCache = isConnectedToInternetUseCase()
        return when {
            chatId != null -> {
                val chatRoom = runCatching { getChatRoomUseCase(chatId) }.getOrNull()
                val contact =
                    runCatching { getContactFromChatUseCase(chatId, skipCache) }.getOrNull()
                val userHandle = contact?.handle
                    ?: chatRoom?.peerHandlesList?.firstOrNull()
                    ?: return null
                ResolvedPeer(
                    contact = contact,
                    chatTitle = chatRoom?.title,
                    userHandle = userHandle,
                    chatRoomId = chatId,
                )
            }

            email != null -> getContactFromEmailUseCase(email, skipCache)?.let { contact ->
                val chatRoom =
                    runCatching { getChatRoomByUserUseCase(contact.handle) }.getOrNull()
                ResolvedPeer(
                    contact = contact,
                    chatTitle = null,
                    userHandle = contact.handle,
                    chatRoomId = chatRoom?.chatId,
                )
            }

            else -> null
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun monitorPeer(peer: ResolvedPeer): Flow<ContactInfoState> {
        val contactFlow = peer.contact
            ?.let { monitorContactItemUpdatesUseCase(it) }
            ?: flowOf(null)
        val chatStateFlow = monitorChatRoomId(peer).flatMapLatest { chatRoomId ->
            chatRoomId?.let { monitorChatState(it) } ?: flowOf(ChatState.NoChatRoom)
        }
        val inSharesCountFlow = contactFlow
            .map { it?.email }
            .distinctUntilChanged()
            .flatMapLatest { email ->
                email?.let { monitorContactInSharesCountUseCase(it) } ?: flowOf(0)
            }
        return combine(
            contactFlow,
            monitorUserPresenceUseCase(peer.userHandle),
            chatStateFlow,
            inSharesCountFlow,
        ) { contact, presence, chatState, inSharesCount ->
            ContactInfoState(
                contactItem = contact,
                chatRoomId = chatState.chatRoomId,
                chatTitle = peer.chatTitle,
                userHandle = peer.userHandle,
                userChatStatus = presence.status,
                lastGreenMinutes = presence.lastGreenMinutes ?: contact?.lastSeen,
                isNotificationsMuted = chatState.muteState?.isMuted,
                notificationsMutedUntilTimestamp = chatState.muteState?.mutedUntilTimestamp,
                retentionTimeSeconds = chatState.retentionTimeSeconds,
                inSharesCount = inSharesCount,
                hasOngoingCall = chatState.hasOngoingCall,
            )
        }.distinctUntilChanged()
    }

    private fun monitorChatRoomId(peer: ResolvedPeer): Flow<Long?> =
        peer.chatRoomId?.let { flowOf<Long?>(it) }
            ?: chatRepository.monitorChatListItemUpdates()
                .filter { !it.isGroup && it.peerHandle == peer.userHandle }
                .map { getChatRoomByUserUseCase(peer.userHandle)?.chatId }
                .onStart { emit(null) }
                .distinctUntilChanged()

    private fun monitorChatState(chatRoomId: Long): Flow<ChatState> = combine(
        monitorChatNotificationMuteStateUseCase(chatRoomId),
        monitorChatRetentionTimeUseCase(chatRoomId),
        monitorCallInChatUseCase(chatRoomId),
    ) { muteState, retentionTimeSeconds, call ->
        ChatState(
            chatRoomId = chatRoomId,
            muteState = muteState,
            retentionTimeSeconds = retentionTimeSeconds,
            hasOngoingCall = call?.status in ONGOING_CALL_STATUSES,
        )
    }

    private data class ResolvedPeer(
        val contact: ContactItem?,
        val chatTitle: String?,
        val userHandle: Long,
        val chatRoomId: Long?,
    )

    private data class ChatState(
        val chatRoomId: Long?,
        val muteState: ChatNotificationMuteState?,
        val retentionTimeSeconds: Long?,
        val hasOngoingCall: Boolean,
    ) {
        companion object {
            val NoChatRoom = ChatState(
                chatRoomId = null,
                muteState = null,
                retentionTimeSeconds = null,
                hasOngoingCall = false,
            )
        }
    }

    private companion object {
        val ONGOING_CALL_STATUSES = listOf(
            ChatCallStatus.Connecting,
            ChatCallStatus.Joining,
            ChatCallStatus.InProgress,
        )
    }
}
