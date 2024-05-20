package mega.privacy.android.app.main.megachat.chat.explorer

import android.util.SparseBooleanArray
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.main.model.chat.explorer.ChatExplorerSearchUiState
import mega.privacy.android.app.main.model.chat.explorer.ChatExplorerUiState
import mega.privacy.android.app.presentation.contact.mapper.UserContactMapper
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.contacts.User
import mega.privacy.android.domain.entity.contacts.UserChatStatus.Busy
import mega.privacy.android.domain.entity.contacts.UserChatStatus.Invalid
import mega.privacy.android.domain.entity.contacts.UserChatStatus.Online
import mega.privacy.android.domain.entity.contacts.UserContact
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.chat.GetActiveChatListItemsUseCase
import mega.privacy.android.domain.usecase.chat.GetArchivedChatListItemsUseCase
import mega.privacy.android.domain.usecase.chat.explorer.GetVisibleContactsWithoutChatRoomUseCase
import mega.privacy.android.domain.usecase.contact.GetContactFromCacheByHandleUseCase
import mega.privacy.android.domain.usecase.contact.GetUserOnlineStatusByHandleUseCase
import mega.privacy.android.domain.usecase.contact.GetUserUseCase
import mega.privacy.android.domain.usecase.contact.RequestUserLastGreenUseCase
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import timber.log.Timber
import javax.inject.Inject

/**
 * View model class for [ChatExplorerFragment].
 *
 * @property getActiveChatListItemsUseCase Use case to get the list of active chat items.
 * @property getArchivedChatListItemsUseCase Use case to get the list of archived chat items.
 * @property getUserUseCase Use case to get [User].
 * @property getContactFromCacheByHandleUseCase Use case to get a contact from cache by its handle.
 * @property getUserOnlineStatusByHandleUseCase Get user online status from user handle.
 * @property requestUserLastGreenUseCase Request last green.
 * @property getVisibleContactsWithoutChatRoomUseCase Use case to retrieve the list of contacts that have no chat room.
 * @property userContactMapper A mapper to map [UserContact] domain entity into [ContactItemUiState].
 */
@HiltViewModel
class ChatExplorerViewModel @Inject constructor(
    private val getActiveChatListItemsUseCase: GetActiveChatListItemsUseCase,
    private val getArchivedChatListItemsUseCase: GetArchivedChatListItemsUseCase,
    private val getUserUseCase: GetUserUseCase,
    private val getContactFromCacheByHandleUseCase: GetContactFromCacheByHandleUseCase,
    private val getUserOnlineStatusByHandleUseCase: GetUserOnlineStatusByHandleUseCase,
    private val requestUserLastGreenUseCase: RequestUserLastGreenUseCase,
    private val getVisibleContactsWithoutChatRoomUseCase: GetVisibleContactsWithoutChatRoomUseCase,
    private val userContactMapper: UserContactMapper,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatExplorerUiState())

    /**
     * The public property of [ChatExplorerUiState].
     */
    val uiState = _uiState.asStateFlow()

    private val _searchUiState = MutableStateFlow(ChatExplorerSearchUiState())

    /**
     * The public property of [ChatExplorerSearchUiState].
     */
    val searchUiState = _searchUiState.asStateFlow()

    /**
     * Search items based on given query.
     *
     * @param query The search query.
     */
    fun search(query: String?) {
        if (query == null) {
            _searchUiState.update { state ->
                state.copy(
                    items = emptyList(),
                    selectedItems = SparseBooleanArray()
                )
            }
            return
        }

        viewModelScope.launch(defaultDispatcher) {
            val searchItems = mutableListOf<ChatExplorerListItem>()
            val selectedSearchItems = SparseBooleanArray()
            _uiState.value.items.forEach {
                if (!it.isHeader && it.title?.lowercase()?.contains(query.lowercase()) == true) {
                    searchItems.add(it)
                    val isItemSelected = _uiState.value.selectedItems.isNotEmpty() &&
                            _uiState.value.selectedItems.contains(it)
                    if (isItemSelected) {
                        selectedSearchItems.put(searchItems.indexOf(it), true)
                    }
                }
            }
            _searchUiState.update { state ->
                state.copy(
                    items = searchItems,
                    selectedItems = selectedSearchItems
                )
            }
        }
    }

    /**
     * Get the list of chats both active and non-active.
     */
    fun getChats() {
        _uiState.update {
            it.copy(
                items = emptyList(),
                isItemUpdated = false
            )
        }
        viewModelScope.launch(defaultDispatcher) {
            val items = buildList {
                // Add the active/recent chat rooms
                addAll(getActiveChatRooms())

                // Add the non-active chat rooms
                addAll(getNonActiveChatRooms())
            }

            _uiState.update { it.copy(items = items) }
        }
    }

    private suspend fun getActiveChatRooms(): List<ChatExplorerListItem> = buildList {
        Timber.d("Retrieving the active chat list items")
        runCatching { getActiveChatListItemsUseCase() }
            .onSuccess { activeChats ->
                activeChats.sortedByDescending { it.lastTimestamp }
                    .forEachIndexed { index, chat ->
                        if (index == 0) {
                            add(
                                ChatExplorerListItem(
                                    isRecent = true,
                                    isHeader = true
                                )
                            )
                        }

                        if (chat.ownPrivilege >= ChatRoomPermission.Standard) {
                            val contact = if (chat.isGroup) {
                                null
                            } else {
                                getContact(chat.peerHandle)
                            }
                            add(
                                ChatExplorerListItem(
                                    contactItem = contact,
                                    chat = chat,
                                    title = chat.title,
                                    id = chat.chatId.toString(),
                                    isRecent = index < RECENT_CHATS_MAX_SIZE,
                                    isSelected = _uiState.value.selectedItems.any {
                                        chat.chatId.toString() == it.id
                                    }
                                )
                            )
                        }
                    }
            }.onFailure { Timber.e("Failed to retrieve active chat list items", it) }
    }

    private suspend fun getNonActiveChatRooms(): List<ChatExplorerListItem> = buildList {
        val nonActiveChatRooms = buildList {
            addAll(getArchivedChatRooms())
            addAll(getContactsWithoutChatRooms())
        }.sortedWith(
            compareBy(
                String.CASE_INSENSITIVE_ORDER
            ) { it.title.orEmpty() }
        )
        if (nonActiveChatRooms.isNotEmpty()) {
            add(ChatExplorerListItem(isHeader = true))
            addAll(nonActiveChatRooms)
        }
    }

    private suspend fun getArchivedChatRooms(): List<ChatExplorerListItem> = buildList {
        Timber.d("Retrieving the archived chat list items")
        runCatching { getArchivedChatListItemsUseCase() }
            .onSuccess { archivedChats ->
                archivedChats.forEach { chat ->
                    if (chat.ownPrivilege >= ChatRoomPermission.Standard) {
                        val contact = if (chat.isGroup) null else getContact(chat.peerHandle)
                        add(
                            ChatExplorerListItem(
                                contactItem = contact,
                                chat = chat,
                                title = chat.title,
                                id = chat.chatId.toString(),
                                isSelected = _uiState.value.selectedItems.any {
                                    chat.chatId.toString() == it.id
                                }
                            )
                        )
                    }
                }
            }.onFailure { Timber.e("Failed to retrieve archived chat list items", it) }
    }

    private suspend fun getContact(handle: Long): ContactItemUiState? {
        Timber.d("Retrieving the cached contacts")
        return runCatching { getUserUseCase(UserId(handle)) }.getOrNull()?.let { user ->
            if (handle != MEGACHAT_INVALID_HANDLE) {
                Timber.d("Retrieving the user's online status")
                runCatching { getUserOnlineStatusByHandleUseCase(handle) }
                    .onSuccess { userStatus ->
                        if (userStatus != Online && userStatus != Busy && userStatus != Invalid) {
                            Timber.d("Requesting the user's last green")
                            runCatching { requestUserLastGreenUseCase(user.handle) }
                                .onFailure {
                                    Timber.e("Failed to request the user's last green", it)
                                }
                        }
                    }
                    .onFailure { Timber.e("Failed to retrieve user's online status", it) }
            }

            val cachedContact = runCatching {
                getContactFromCacheByHandleUseCase(handle)
            }.getOrNull()
            ContactItemUiState(
                contact = cachedContact,
                user = user
            )
        }
    }

    private suspend fun getContactsWithoutChatRooms(): List<ChatExplorerListItem> = buildList {
        Timber.d("Retrieving the visible contacts without chat rooms")
        runCatching { getVisibleContactsWithoutChatRoomUseCase() }
            .onSuccess { contacts ->
                contacts.forEach { userContact ->
                    val contactItemUiState = userContactMapper(userContact)
                    add(
                        ChatExplorerListItem(
                            contactItem = contactItemUiState,
                            title = contactItemUiState.contact?.fullName,
                            id = contactItemUiState.user?.handle?.toString(),
                            isSelected = _uiState.value.selectedItems.any {
                                contactItemUiState.user?.handle?.toString() == it.id
                            }
                        )
                    )
                }
            }.onFailure {
                Timber.e("Failed to retrieve visible contacts without chat rooms", it)
            }
    }

    /**
     * Add a new selected item.
     *
     * @param item The [ChatExplorerListItem] that needs to be added to the selected items
     */
    fun addSelectedItem(item: ChatExplorerListItem) {
        _uiState.update { uiState ->
            uiState.copy(
                selectedItems = uiState.selectedItems + item,
                isItemUpdated = true
            )
        }
    }

    /**
     * Remove a selected item.
     *
     * @param item The [ChatExplorerListItem] that needs to be removed from the selected items
     */
    fun removeSelectedItem(item: ChatExplorerListItem) {
        _uiState.update { uiState ->
            uiState.copy(
                selectedItems = uiState.selectedItems.filterNot { it == item },
                isItemUpdated = true
            )
        }
    }

    /**
     * Clear all selections.
     */
    fun clearSelections() {
        _uiState.update { uiState ->
            uiState.copy(
                items = uiState.items.map {
                    if (it.isSelected) {
                        it.copy(isSelected = false)
                    } else {
                        it
                    }
                },
                selectedItems = emptyList(),
                isItemUpdated = false
            )
        }
    }

    /**
     * Update the last green date for a specific contact item
     *
     * @param contactItem The [ContactItemUiState] that needs to be updated
     * @param date The updated last green time
     */
    fun updateItemLastGreenDateByContact(contactItem: ContactItemUiState, date: String) {
        _uiState.update {
            it.copy(
                items = it.items.map { item ->
                    if (contactItem == item.contactItem) {
                        item.copy(contactItem = item.contactItem.copy(lastGreen = date))
                    } else {
                        item
                    }
                },
                selectedItems = it.selectedItems.map { item ->
                    if (contactItem == item.contactItem) {
                        item.copy(contactItem = item.contactItem.copy(lastGreen = date))
                    } else {
                        item
                    }
                },
                isItemUpdated = true
            )
        }
    }

    companion object {
        private const val RECENT_CHATS_MAX_SIZE = 6
    }
}
