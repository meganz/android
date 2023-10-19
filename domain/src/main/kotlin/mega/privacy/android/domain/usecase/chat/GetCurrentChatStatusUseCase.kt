package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import mega.privacy.android.domain.entity.ConnectivityState
import mega.privacy.android.domain.entity.chat.ChatStatus
import mega.privacy.android.domain.entity.chat.ConnectionState
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.repository.ChatParticipantsRepository
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.repository.NetworkRepository
import javax.inject.Inject

/**
 * Get current chat status use case
 *
 * @property chatParticipantsRepository
 * @property contactsRepository
 * @property networkRepository
 * @property chatRepository
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetCurrentChatStatusUseCase @Inject constructor(
    private val chatParticipantsRepository: ChatParticipantsRepository,
    private val contactsRepository: ContactsRepository,
    private val networkRepository: NetworkRepository,
    private val chatRepository: ChatRepository,
) {

    /**
     * Return current chat status and subsequence updates
     */
    operator fun invoke(): Flow<ChatStatus> =
        flow {
            emit(getChatStatus())
            emitAll(
                merge(
                    monitorConnectivityChanges(),
                    monitorMyChatOnlineStatusUpdates(),
                    monitorChatConnectionStateUpdates()
                )
            )
        }

    private suspend fun getChatStatus(): ChatStatus =
        if (networkRepository.getCurrentConnectivityState() == ConnectivityState.Disconnected) {
            ChatStatus.NoNetworkConnection
        } else {
            when (chatParticipantsRepository.getCurrentStatus()) {
                UserChatStatus.Offline -> ChatStatus.Offline
                UserChatStatus.Away -> ChatStatus.Away
                UserChatStatus.Online -> ChatStatus.Online
                UserChatStatus.Busy -> ChatStatus.Busy
                UserChatStatus.Invalid -> {
                    when (chatRepository.getConnectionState()) {
                        ConnectionState.Connecting -> ChatStatus.Connecting
                        ConnectionState.Connected -> ChatStatus.Online
                        else -> ChatStatus.Reconnecting
                    }
                }
            }
        }

    private fun monitorConnectivityChanges(): Flow<ChatStatus> =
        networkRepository.monitorConnectivityChanges().mapLatest { getChatStatus() }

    private fun monitorMyChatOnlineStatusUpdates(): Flow<ChatStatus> =
        contactsRepository.monitorMyChatOnlineStatusUpdates().mapLatest { getChatStatus() }

    private fun monitorChatConnectionStateUpdates(): Flow<ChatStatus> =
        contactsRepository.monitorChatConnectionStateUpdates().mapLatest { getChatStatus() }
}
