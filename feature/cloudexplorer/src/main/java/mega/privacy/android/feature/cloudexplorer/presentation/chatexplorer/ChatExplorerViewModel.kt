package mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.chat.ChatListItem
import mega.privacy.android.domain.entity.chat.ChatListItemChanges
import mega.privacy.android.domain.usecase.MonitorChatListItemUpdates
import mega.privacy.android.domain.usecase.chat.CreateGroupChatRoomUseCase
import mega.privacy.android.domain.usecase.chat.Get1On1ChatIdUseCase
import mega.privacy.android.domain.usecase.chat.GetActiveChatListItemsUseCase
import mega.privacy.android.domain.usecase.chat.GetArchivedChatListItemsUseCase
import mega.privacy.android.domain.usecase.chat.GetNoteToSelfChatUseCase
import mega.privacy.android.domain.usecase.chat.explorer.GetVisibleContactsWithoutChatRoomUseCase
import mega.privacy.android.domain.usecase.chat.message.SendTextMessageUseCase
import mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer.ChatExplorerViewModel.Companion.RECENT_CHATS_LIMIT
import mega.privacy.android.core.coroutine.asUiStateFlow
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
    private val sendTextMessageUseCase: SendTextMessageUseCase,
    private val chatExplorerUiItemMapper: ChatExplorerUiItemMapper,
) : ViewModel() {

    private val newChatCreatedChannel = Channel<StateEventWithContent<Long>>(Channel.BUFFERED)
    private val chatsReadyToShareChannel =
        Channel<StateEventWithContent<List<Long>>>(Channel.BUFFERED)
    private val searchQueryChannel = Channel<ChatSearchInput?>(Channel.CONFLATED)

    val uiState: StateFlow<ChatExplorerUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            chatItemsFlow(),
            searchQueryChannel.receiveAsFlow().onStart { emit(null) }.distinctUntilChanged(),
            newChatCreatedChannel.receiveAsFlow().onStart { emit(consumed()) },
            chatsReadyToShareChannel.receiveAsFlow().onStart { emit(consumed()) }
        ) { chats, search, newChatCreatedEvent, chatsReadyToShareEvent ->
            ChatExplorerUiState.Data(
                items = chats,
                searchResults = if (search == null || search.query.isBlank()) {
                    ChatExplorerUiState.Items.Empty
                } else {
                    chats.matching(search)
                },
                newChatCreatedEvent = newChatCreatedEvent,
                chatsReadyToShareEvent = chatsReadyToShareEvent,
            )
        }.catch { e -> Timber.e(e, "Failed to assemble chat explorer state") }
            .asUiStateFlow(viewModelScope, ChatExplorerUiState.Loading)
    }

    /**
     * Sets the [search] whose matches [ChatExplorerUiState.Data.searchResults] exposes. Pass `null`
     * when the search is closed.
     */
    fun onSearchQuery(search: ChatSearchInput?) {
        viewModelScope.launch { searchQueryChannel.send(search) }
    }

    /**
     * A search request: the [query] text plus the localized [noteToSelfTitle], resolved by the
     * caller so the note-to-self row is matched without the ViewModel holding a Context.
     */
    data class ChatSearchInput(
        val query: String,
        val noteToSelfTitle: String,
    )

    private fun chatItemsFlow(): Flow<ChatExplorerUiState.Items> = flow {
        runCatching { getNoteToSelfChatUseCase() }.onFailure {
            Timber.e(it, "Failed to ensure note-to-self chat")
        }
        var current = loadItems()
        emit(current)
        monitorChatListItemUpdates()
            .filter { it.changes in IN_PLACE_CHANGES }
            .collect { chat ->
                current = current.applyChatUpdate(chat)
                emit(current)
            }
    }.catch { e ->
        Timber.e(e, "Failed to load chat explorer items")
    }

    private suspend fun loadItems(): ChatExplorerUiState.Items {
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
        val archivedRows = archived.map { chatExplorerUiItemMapper(it) }
        val contactRows = contactsWithoutChatRoom.mapNotNull { chatExplorerUiItemMapper(it) }
        val others = (activeOverflow + archivedRows + contactRows)
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.sortKey() })

        return ChatExplorerUiState.Items(
            noteToSelf = noteToSelf,
            recents = recents,
            others = others,
        )
    }

    private suspend fun ChatExplorerUiState.Items.applyChatUpdate(chat: ChatListItem): ChatExplorerUiState.Items {
        val mapped = chatExplorerUiItemMapper(chat)
        val dataWithoutUpdate = removeUpdatedChat(chat.chatId)

        val dataUpdated = when {
            mapped is ChatExplorerUiItem.NoteToSelf && !mapped.isArchived ->
                dataWithoutUpdate.copy(noteToSelf = mapped)

            mapped.isArchived ->
                dataWithoutUpdate.copy(others = dataWithoutUpdate.others.insertSorted(mapped))

            else -> dataWithoutUpdate.insertActiveChat(mapped)
        }
        return dataUpdated.updateRecentsIfNeeded()
    }

    private fun ChatExplorerUiState.Items.removeUpdatedChat(chatId: Long): ChatExplorerUiState.Items =
        copy(
            noteToSelf = noteToSelf?.takeIf { it.id != chatId },
            recents = recents.filterNot { it.id == chatId },
            others = others.filterNot { it.id == chatId },
        )

    /**
     * Inserts an active chat into recents when its lastTimestamp is high enough to belong there.
     * If so, and recents size is higher than RECENT_CHATS_LIMIT after the update,
     * moves the older chat into others.
     * Otherwise, inserts the chat in others.
     */
    private fun ChatExplorerUiState.Items.insertActiveChat(chat: ChatExplorerUiItem): ChatExplorerUiState.Items {
        if (recents.size < RECENT_CHATS_LIMIT) {
            return copy(recents = (recents + chat).sortedByDescending { it.lastTimestamp })
        }

        val recentsMin = recents.minBy { it.lastTimestamp }

        if (chat.lastTimestamp <= recentsMin.lastTimestamp) {
            return copy(others = others.insertSorted(chat))
        }

        val updatedRecents = (recents - recentsMin + chat).sortedByDescending { it.lastTimestamp }

        return copy(recents = updatedRecents, others = others.insertSorted(recentsMin))
    }

    /**
     * Promotes the highest-timestamp active overflow from others into recents until recents
     * reaches [RECENT_CHATS_LIMIT]. Archived chats are not eligible.
     */
    private fun ChatExplorerUiState.Items.updateRecentsIfNeeded(): ChatExplorerUiState.Items {
        if (recents.size >= RECENT_CHATS_LIMIT) return this

        val newRecent = others
            .filter { !it.isArchived }
            .maxByOrNull { it.lastTimestamp }
            ?: return this

        val updatedOthers = others.toMutableList().also { it.remove(newRecent) }
        val updatedRecents = (recents + newRecent).sortedByDescending { it.lastTimestamp }

        return copy(recents = updatedRecents, others = updatedOthers)
    }

    private fun List<ChatExplorerUiItem>.insertSorted(
        item: ChatExplorerUiItem,
    ): List<ChatExplorerUiItem> =
        (this + item).sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.sortKey() })

    private fun ChatExplorerUiState.Items.matching(search: ChatSearchInput): ChatExplorerUiState.Items =
        ChatExplorerUiState.Items(
            noteToSelf = noteToSelf?.takeIf { it.matchesQuery(search) },
            recents = recents.filter { it.matchesQuery(search) },
            others = others.filter { it.matchesQuery(search) },
        )

    private fun ChatExplorerUiItem.matchesQuery(search: ChatSearchInput): Boolean {
        val name = when (this) {
            is ChatExplorerUiItem.NoteToSelf -> search.noteToSelfTitle
            is ChatExplorerUiItem.GroupChat -> title
            is ChatExplorerUiItem.Meeting -> title
            is ChatExplorerUiItem.OneToOneChat -> contactName.orEmpty()
            is ChatExplorerUiItem.Contact -> contactName.orEmpty()
        }
        return name.contains(search.query, ignoreCase = true)
    }

    private fun ChatExplorerUiItem.sortKey(): String = when (this) {
        is ChatExplorerUiItem.NoteToSelf -> ""
        is ChatExplorerUiItem.GroupChat -> title
        is ChatExplorerUiItem.Meeting -> title
        is ChatExplorerUiItem.OneToOneChat -> contactName.orEmpty()
        is ChatExplorerUiItem.Contact -> contactName.orEmpty()
    }

    fun onContactsSelectedForGroupChat(newGroupChatResult: CreateGroupChatNavKey.NewGroupChatResult) {
        with(newGroupChatResult) {
            if (emails.isEmpty()) return

            viewModelScope.launch {
                runCatching {
                    createGroupChatRoomUseCase(
                        emails = emails,
                        title = title,
                        isEkr = isEkr,
                        addParticipants = allowAddParticipants,
                        chatLink = isChatLink,
                    )
                }.onFailure { e ->
                    Timber.e(e, "Failed to create chat from contact selection")
                }.getOrNull()?.let { chatId ->
                    newChatCreatedChannel.send(triggered(chatId))
                }
            }
        }
    }

    fun onNewChatCreatedConsumed() {
        newChatCreatedChannel.trySend(consumed())
    }

    /**
     * Resolves [selectedIds] (a mix of chat ids and [ChatExplorerUiItem.Contact] user handles) into
     * concrete chat ids, creating a 1:1 chat for each contact when one doesn't exist yet. When
     * [message] is non-null, it is sent to every resolved chat id via [sendTextMessageUseCase]
     * before emitting the resolved ids through [ChatExplorerUiState.Data.chatsReadyToShareEvent].
     */
    fun prepareChatsForSharing(selectedIds: List<Long>, message: String?) {
        if (selectedIds.isEmpty()) return

        val chats = (uiState.value as? ChatExplorerUiState.Data)?.items ?: return

        viewModelScope.launch {
            val itemsById = (listOfNotNull(chats.noteToSelf) + chats.recents + chats.others)
                .associateBy { it.id }
            val chatIds = selectedIds.mapNotNull { id ->
                when (val item = itemsById[id]) {
                    is ChatExplorerUiItem.Contact -> runCatching { get1On1ChatIdUseCase(id) }
                        .onFailure { Timber.e(it, "Failed to create 1:1 chat for handle=$id") }
                        .getOrNull()

                    null -> {
                        Timber.w("Selected id $id not found in chat explorer items")
                        null
                    }

                    else -> item.id
                }
            }
            if (chatIds.isEmpty()) return@launch

            if (message != null) {
                chatIds.forEach { chatId ->
                    runCatching { sendTextMessageUseCase(chatId = chatId, message = message) }
                        .onFailure { Timber.e(it, "Failed to send message to chat=$chatId") }
                }
            }
            chatsReadyToShareChannel.send(triggered(chatIds))
        }
    }

    fun onChatsReadyToShareConsumed() {
        chatsReadyToShareChannel.trySend(consumed())
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
