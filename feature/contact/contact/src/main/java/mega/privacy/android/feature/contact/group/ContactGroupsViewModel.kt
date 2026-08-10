package mega.privacy.android.feature.contact.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.contacts.group.ContactGroup
import mega.privacy.android.domain.usecase.chat.CreateGroupChatRoomUseCase
import mega.privacy.android.domain.usecase.contact.group.GetContactGroupsUseCase
import mega.privacy.android.feature.contact.group.mapper.ContactGroupItemMapper
import mega.privacy.android.feature.contact.group.model.ContactGroupItem
import mega.privacy.android.feature.contact.group.model.ContactGroupUiState
import timber.log.Timber
import javax.inject.Inject

/**
 * Contact groups view model
 *
 * @property createGroupChatRoomUseCase
 * @param getContactGroupsUseCase
 * @param contactGroupItemMapper
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ContactGroupsViewModel @Inject constructor(
    private val createGroupChatRoomUseCase: CreateGroupChatRoomUseCase,
    private val getContactGroupsUseCase: GetContactGroupsUseCase,
    private val contactGroupItemMapper: ContactGroupItemMapper,
) : ViewModel() {

    private val queryChannel = Channel<String?>(Channel.CONFLATED)
    private val refreshTrigger = Channel<Unit>(Channel.CONFLATED)
    private val groupChatCreatedEvents =
        MutableStateFlow<GroupChatUpdate>(GroupChatUpdate.None)

    private sealed interface GroupChatUpdate {
        val stateEvent: StateEventWithContent<Long>

        data object None : GroupChatUpdate {
            override val stateEvent: StateEventWithContent<Long> = consumed()
        }

        data object Failed : GroupChatUpdate {
            override val stateEvent: StateEventWithContent<Long> =
                triggered(ContactGroupUiState.INVALID_GROUP_CHAT_ID)
        }

        /**
         * Chat room created
         *
         * @property chatId
         */
        data class ChatRoomCreated(
            val chatId: Long,
        ) : GroupChatUpdate {
            override val stateEvent: StateEventWithContent<Long>
                get() = triggered(chatId)
        }
    }

    /**
     * Groups data, loaded once on start and only re-fetched when [refreshTrigger] fires (e.g.
     * after a new group chat is created). It does not emit while loading, so the combined
     * [uiState] keeps the previously loaded list on screen during a refresh instead of dropping
     * back to [ContactGroupUiState.Loading].
     */
    private fun contactGroups(): Flow<List<ContactGroupItem>> =
        refreshTrigger.receiveAsFlow()
            .onStart { emit(Unit) }
            .flatMapLatest {
                flow {
                    runCatching {
                        getContactGroupsUseCase()
                    }.onSuccess { list: List<ContactGroup> ->
                        emit(list.map { contactGroupItemMapper(it) })
                    }.onFailure {
                        emit(emptyList())
                        Timber.e(it)
                    }
                }
            }

    /**
     * Ui state
     */
    val uiState: StateFlow<ContactGroupUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            contactGroups(),
            queryChannel.receiveAsFlow()
                .onStart { emit(null) },
            groupChatCreatedEvents,
        ) { groups, query, groupChatUpdate ->
            val filteredGroups = query.takeUnless { it.isNullOrBlank() }?.let { queryString ->
                groups.filter { it.name.contains(queryString, true) }
            } ?: groups

            ContactGroupUiState.Data(
                groups = filteredGroups,
                groupChatCreated = groupChatUpdate.stateEvent,
            )
        }.asUiStateFlow(
            viewModelScope,
            ContactGroupUiState.Loading
        )
    }


    /**
     * Set query
     *
     * @param query
     */
    fun setQuery(query: String?) {
        queryChannel.trySend(query)
    }

    /**
     * Consume the group chat created event once it has been handled by the UI.
     */
    fun onGroupChatCreatedConsumed() {
        groupChatCreatedEvents.tryEmit(GroupChatUpdate.None)
    }

    /**
     * Create group chat
     *
     * @param participantEmails
     * @param chatTitle
     * @param allowAddParticipants
     */
    fun createGroupChat(
        participantEmails: ArrayList<String>,
        chatTitle: String?,
        allowAddParticipants: Boolean,
    ) {
        Timber.d("Create group chat called")
        viewModelScope.launch {
            runCatching {
                createGroupChatRoomUseCase(
                    participantEmails,
                    chatTitle,
                    false,
                    allowAddParticipants,
                    false
                )
            }.onSuccess { chatId ->
                Timber.d("Create group chat succeeded")
                refreshTrigger.send(Unit)
                groupChatCreatedEvents.emit(GroupChatUpdate.ChatRoomCreated(chatId))
            }.onFailure {
                Timber.e(it, "Create group chat failed")
                groupChatCreatedEvents.emit(GroupChatUpdate.Failed)
            }
        }
    }
}
