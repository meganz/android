package mega.privacy.android.feature.chat.managehistory

import mega.privacy.android.feature.chat.managehistory.model.ChatHistoryRetention
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.chat.ClearChatHistoryUseCase
import mega.privacy.android.domain.usecase.chat.GetChatRoomByUserUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatRetentionTimeUpdateUseCase
import mega.privacy.android.domain.usecase.chat.SetChatRetentionTimeUseCase
import mega.privacy.android.domain.usecase.contact.GetContactHandleUseCase
import mega.privacy.android.feature.chat.managehistory.model.ManageChatHistoryUiState
import mega.privacy.android.feature.chat.runCatchingCancellable
import timber.log.Timber

/**
 * Drives the manage chat history screen: resolves the chat room the screen was opened for, keeps
 * its message clearing time up to date, and applies the changes the user makes.
 */
@HiltViewModel(assistedFactory = ManageChatHistoryViewModel.Factory::class)
internal class ManageChatHistoryViewModel @AssistedInject constructor(
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val getChatRoomByUserUseCase: GetChatRoomByUserUseCase,
    private val getContactHandleUseCase: GetContactHandleUseCase,
    private val monitorChatRetentionTimeUpdateUseCase: MonitorChatRetentionTimeUpdateUseCase,
    private val clearChatHistoryUseCase: ClearChatHistoryUseCase,
    private val setChatRetentionTimeUseCase: SetChatRetentionTimeUseCase,
    @Assisted private val args: Args,
) : ViewModel() {

    /**
     * Creates a [ManageChatHistoryViewModel] for the chat the destination was opened with.
     */
    @AssistedFactory
    interface Factory {
        /**
         * @param args the chat the screen was opened for.
         */
        fun create(args: Args): ManageChatHistoryViewModel
    }

    /**
     * Identifies the chat room to manage, either directly or through the contact it belongs to.
     *
     * @property chatId Id of the chat room, or null when only the contact is known.
     * @property email Email of the contact whose 1:1 chat should be managed.
     */
    data class Args(val chatId: Long?, val email: String?)

    private val historyClearedEventChannel =
        Channel<StateEventWithContent<Boolean>>(Channel.BUFFERED)

    val uiState: StateFlow<ManageChatHistoryUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            monitorChatRoom(),
            historyClearedEventChannel.receiveAsFlow().onStart { emit(consumed()) },
        ) { chatRoom, historyClearedEvent ->
            if (chatRoom == null) {
                ManageChatHistoryUiState.Unavailable
            } else {
                ManageChatHistoryUiState.Data(
                    chatId = chatRoom.chatId,
                    isMeeting = chatRoom.isMeeting,
                    retention = chatRoom.retention,
                    historyClearedEvent = historyClearedEvent,
                )
            }
        }.catch { Timber.e(it, "Failed to observe the chat room") }
            .asUiStateFlow(viewModelScope, ManageChatHistoryUiState.Loading)
    }

    private fun monitorChatRoom(): Flow<ChatRoomState?> = flow {
        val chatRoom = resolveChatRoom()
        if (chatRoom == null) {
            emit(null)
            return@flow
        }
        val state = ChatRoomState(
            chatId = chatRoom.chatId,
            isMeeting = chatRoom.isMeeting,
            retention = ChatHistoryRetention.from(chatRoom.retentionTime.seconds),
        )
        emit(state)
        emitAll(
            monitorChatRetentionTimeUpdateUseCase(chatRoom.chatId)
                .map { state.copy(retention = ChatHistoryRetention.from(it.seconds)) }
        )
    }

    private suspend fun resolveChatRoom(): ChatRoom? {
        val chatId = args.chatId
        if (chatId != null) {
            return runCatchingCancellable { getChatRoomUseCase(chatId) }
                .onFailure { Timber.e(it, "Failed to get the chat room") }
                .getOrNull()
        }

        val email = args.email
        if (email.isNullOrBlank()) {
            Timber.e("Cannot manage the chat history without a chat id or a contact email")
            return null
        }

        val handle = runCatchingCancellable { getContactHandleUseCase(email) }
            .onFailure { Timber.e(it, "Failed to get the contact handle") }
            .getOrNull()
            ?: return null

        return runCatchingCancellable { getChatRoomByUserUseCase(handle) }
            .onFailure { Timber.e(it, "Failed to get the chat room of the contact") }
            .getOrNull()
    }

    /**
     * Deletes every message in the chat room and reports the outcome through the state.
     *
     * @param chatId Id of the chat room to clear.
     */
    fun onClearHistory(chatId: Long) {
        viewModelScope.launch {
            val succeeded = runCatchingCancellable { clearChatHistoryUseCase(chatId) }
                .onFailure { Timber.e(it, "Failed to clear the chat history") }
                .isSuccess
            historyClearedEventChannel.send(triggered(succeeded))
        }
    }

    /**
     * Sets how long messages are kept before being deleted.
     *
     * @param chatId Id of the chat room to change.
     * @param retention Retention to put the chat on.
     */
    fun onSetRetention(chatId: Long, retention: ChatHistoryRetention) {
        viewModelScope.launch {
            runCatchingCancellable {
                setChatRetentionTimeUseCase(chatId = chatId, period = retention.duration.inWholeSeconds)
            }.onFailure { Timber.e(it, "Failed to set the chat retention time") }
        }
    }

    /**
     * Clears the clear history outcome once it has been shown.
     */
    fun onHistoryClearedEventConsumed() {
        historyClearedEventChannel.trySend(consumed())
    }

    private data class ChatRoomState(
        val chatId: Long,
        val isMeeting: Boolean,
        val retention: ChatHistoryRetention,
    )
}
