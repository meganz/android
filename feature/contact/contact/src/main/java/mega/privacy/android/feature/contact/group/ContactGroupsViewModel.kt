package mega.privacy.android.feature.contact.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.usecase.chat.CreateGroupChatRoomUseCase
import mega.privacy.android.domain.usecase.contact.group.GetContactGroupsUseCase
import mega.privacy.android.feature.contact.group.mapper.ContactGroupItemMapper
import mega.privacy.android.feature.contact.group.model.ContactGroupUiState
import mega.privacy.android.feature.contact.group.model.ContactGroupUiState.Companion.INVALID_GROUP_CHAT_ID
import timber.log.Timber
import javax.inject.Inject

/**
 * Contact groups view model
 *
 * @property createGroupChatRoomUseCase
 * @param getContactGroupsUseCase
 * @param contactGroupItemMapper
 */
@HiltViewModel
class ContactGroupsViewModel @Inject constructor(
    private val createGroupChatRoomUseCase: CreateGroupChatRoomUseCase,
    getContactGroupsUseCase: GetContactGroupsUseCase,
    contactGroupItemMapper: ContactGroupItemMapper,
) : ViewModel() {

    private val queryChannel = Channel<String?>(Channel.CONFLATED)
    private val groupChatCreatedEventChannel =
        Channel<StateEventWithContent<Long>>(Channel.CONFLATED)

    /**
     * Ui state
     */
    val uiState: StateFlow<ContactGroupUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            flow {
                runCatching {
                    getContactGroupsUseCase()
                }.onSuccess { list ->
                    emit(list.map { contactGroupItemMapper(it) })
                }.onFailure {
                    Timber.e(it)
                }
            },
            queryChannel.receiveAsFlow()
                .onStart { emit(null) },
            groupChatCreatedEventChannel.receiveAsFlow()
                .onStart { emit(consumed()) }
        ) { groups, query, groupChatEvent ->
            val filteredGroups = query.takeUnless { it.isNullOrBlank() }?.let { queryString ->
                groups.filter { it.name.contains(queryString, true) }
            } ?: groups

            ContactGroupUiState.Data(
                groups = filteredGroups,
                groupChatCreated = groupChatEvent,
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
        groupChatCreatedEventChannel.trySend(consumed())
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
                groupChatCreatedEventChannel.trySend(triggered(chatId))
            }.onFailure {
                Timber.e(it)
                groupChatCreatedEventChannel.trySend(triggered(INVALID_GROUP_CHAT_ID))
            }
        }
    }
}
