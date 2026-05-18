package mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.chat.ChatListItemChanges
import mega.privacy.android.domain.usecase.MonitorChatListItemUpdates
import mega.privacy.android.domain.usecase.chat.CreateGroupChatRoomUseCase
import mega.privacy.android.domain.usecase.chat.Get1On1ChatIdUseCase
import mega.privacy.android.domain.usecase.chat.GetActiveChatListItemsUseCase
import mega.privacy.android.domain.usecase.chat.GetArchivedChatListItemsUseCase
import mega.privacy.android.domain.usecase.chat.GetNoteToSelfChatUseCase
import mega.privacy.android.domain.usecase.chat.explorer.GetVisibleContactsWithoutChatRoomUseCase
import mega.privacy.android.domain.usecase.contact.GetContactHandleUseCase
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import mega.privacy.android.navigation.destination.CreateGroupChatNavKey
import mega.privacy.android.shared.chats.model.ChatExplorerUiItem
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ChatExplorerViewModel @Inject constructor(
    private val getActiveChatListItemsUseCase: GetActiveChatListItemsUseCase,
    private val getArchivedChatListItemsUseCase: GetArchivedChatListItemsUseCase,
    private val getVisibleContactsWithoutChatRoomUseCase: GetVisibleContactsWithoutChatRoomUseCase,
    private val getNoteToSelfChatUseCase: GetNoteToSelfChatUseCase,
    private val monitorChatListItemUpdates: MonitorChatListItemUpdates,
    private val createGroupChatRoomUseCase: CreateGroupChatRoomUseCase,
    private val get1On1ChatIdUseCase: Get1On1ChatIdUseCase,
    private val getContactHandleUseCase: GetContactHandleUseCase,
    private val chatExplorerUiItemMapper: ChatExplorerUiItemMapper,
) : ViewModel() {

    val uiState: StateFlow<ChatExplorerUiState> by lazy(LazyThreadSafetyMode.NONE) {
        chatItemsFlow()
            .map<ChatExplorerUiState.Data, ChatExplorerUiState> { it }
            .catch { e -> Timber.e(e, "Failed to assemble chat explorer state") }
            .asUiStateFlow(viewModelScope, ChatExplorerUiState.Loading)
    }

    private fun chatItemsFlow(): Flow<ChatExplorerUiState.Data> = flow {
        runCatching { getNoteToSelfChatUseCase() }.onFailure {
            Timber.e(it, "Failed to ensure note-to-self chat")
        }
        emit(loadItems())
        monitorChatListItemUpdates().collect {
            emit(loadItems())
        }
    }.catch { e ->
        Timber.e(e, "Failed to load chat explorer items")
    }

    private suspend fun loadItems(): ChatExplorerUiState.Data {
        val active = runCatching { getActiveChatListItemsUseCase() }
            .onFailure { Timber.e(it, "Failed to load active chats") }
            .getOrDefault(emptyList())
        val archived = runCatching { getArchivedChatListItemsUseCase() }
            .onFailure { Timber.e(it, "Failed to load archived chats") }
            .getOrDefault(emptyList())
        val contactsWithoutChatRoom = runCatching { getVisibleContactsWithoutChatRoomUseCase() }
            .onFailure { Timber.e(it, "Failed to load contacts without chat rooms") }
            .getOrDefault(emptyList())
        val noteToSelf = active
            .firstOrNull { it.isNoteToSelf }?.let { chatExplorerUiItemMapper(it) }
        val activeByRecency = active
            .filter { !it.isNoteToSelf }
            .sortedByDescending { it.lastTimestamp }
        val recents = activeByRecency.take(RECENT_CHATS_LIMIT).map { chatExplorerUiItemMapper(it) }
        val activeOverflow = activeByRecency
            .drop(RECENT_CHATS_LIMIT).map { chatExplorerUiItemMapper(it) }
        val archivedRows = archived.filter { !it.isNoteToSelf }.map { chatExplorerUiItemMapper(it) }
        val contactRows = contactsWithoutChatRoom.mapNotNull { chatExplorerUiItemMapper(it) }
        val others = (activeOverflow + archivedRows + contactRows)
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.sortKey() })

        return ChatExplorerUiState.Data(
            noteToSelf = noteToSelf,
            recents = recents,
            others = others,
        )
    }

    private fun ChatExplorerUiItem.sortKey(): String = when (this) {
        is ChatExplorerUiItem.NoteToSelf -> ""
        is ChatExplorerUiItem.GroupChat -> title
        is ChatExplorerUiItem.Meeting -> title
        is ChatExplorerUiItem.OneToOneChat -> contactName.orEmpty()
        is ChatExplorerUiItem.Contact -> contactName.orEmpty()
    }

    fun onContactsSelectedForGroupChat(selection: CreateGroupChatNavKey.NewGroupChatResult) {
        if (selection.emails.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                if (selection.emails.size == 1) {
                    val handle = getContactHandleUseCase(selection.emails.first())
                        ?: error("Unable to resolve handle for ${selection.emails.first()}")
                    get1On1ChatIdUseCase(handle)
                } else {
                    createGroupChatRoomUseCase(
                        emails = selection.emails,
                        title = selection.title,
                        isEkr = selection.isEkr,
                        addParticipants = selection.allowAddParticipants,
                        chatLink = selection.isChatLink,
                    )
                }
            }.onFailure { e ->
                Timber.e(e, "Failed to create chat from contact selection")
            }
        }
    }

    private companion object {
        private const val RECENT_CHATS_LIMIT = 5

        private val IN_PLACE_CHANGES = setOf(
            ChatListItemChanges.Status,
            ChatListItemChanges.OwnPrivilege,
            ChatListItemChanges.Participants,
            ChatListItemChanges.Title,
            ChatListItemChanges.Closed,
            ChatListItemChanges.LastTS,
            ChatListItemChanges.Archive,
            ChatListItemChanges.Deleted,
        )
    }
}
