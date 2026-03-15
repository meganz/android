package mega.privacy.android.feature.contact.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.account.contactrequest.MonitorContactRequestsUseCase
import mega.privacy.android.domain.usecase.call.GetChatCallUseCase
import mega.privacy.android.domain.usecase.call.StartCallUseCase
import mega.privacy.android.domain.usecase.chat.Get1On1ChatIdUseCase
import mega.privacy.android.domain.usecase.contact.GetContactsUseCase
import mega.privacy.android.domain.usecase.contact.RemoveContactByEmailUseCase
import mega.privacy.android.feature.contact.list.mapper.ContactItemUiModelMapper
import mega.privacy.android.feature.contact.list.model.CallEventData
import mega.privacy.android.feature.contact.list.model.ContactListUiState
import mega.privacy.android.feature.contact.list.model.ContactUiModel
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the contact list screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class ContactListViewModel @Inject constructor(
    private val getContactsUseCase: GetContactsUseCase,
    private val get1On1ChatIdUseCase: Get1On1ChatIdUseCase,
    private val removeContactByEmailUseCase: RemoveContactByEmailUseCase,
    private val startCallUseCase: StartCallUseCase,
    private val getChatCallUseCase: GetChatCallUseCase,
    private val monitorContactRequestsUseCase: MonitorContactRequestsUseCase,
    private val contactItemUiModelMapper: ContactItemUiModelMapper,
) : ViewModel() {

    private val queryChannel = Channel<String?>(Channel.CONFLATED)
    private val openChatEventChannel = Channel<StateEventWithContent<Long>>(Channel.BUFFERED)
    private val startCallEventChannel =
        Channel<StateEventWithContent<CallEventData>>(Channel.BUFFERED)

    /**
     * UI state for the contact list screen.
     */
    val uiState: StateFlow<ContactListUiState> by lazy {
        combine(
            contactsWithSearchFlow(),
            monitorContactRequestsUseCase()
                .map { it.incomingContactRequests.size }
                .catch {
                    Timber.e(it)
                    emit(0)
                },
            openChatEventChannel.receiveAsFlow()
                .onStart { emit(consumed()) },
            startCallEventChannel.receiveAsFlow()
                .onStart { emit(consumed()) },
        ) { contactData, requestCount, openChatEvent, startCallEvent ->
            ContactListUiState.Data(
                contacts = contactData.groupedContacts,
                recentlyAddedContacts = contactData.recentlyAdded,
                incomingRequestCount = requestCount,
                openChatEvent = openChatEvent,
                startCallEvent = startCallEvent,
            )
        }.catch { Timber.e(it) }
            .asUiStateFlow(viewModelScope, ContactListUiState.Loading)
    }

    private fun contactsWithSearchFlow(): Flow<ContactData> =
        combine(
            queryChannel.receiveAsFlow()
                .onStart { emit(null) },
            getContactsUseCase().map { domainList ->
                domainList.map { contactItemUiModelMapper(it) }
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayName })
            }
        ) { query, contacts ->
            if (query.isNullOrBlank()) {
                ContactData(
                    groupedContacts = contacts.groupByInitial(),
                    recentlyAdded = contacts.filter { it.isNew },
                )
            } else {
                val filtered = contacts.filter { it.matches(query) }
                ContactData(
                    groupedContacts = filtered.groupByInitial(),
                    recentlyAdded = emptyList(),
                )
            }
        }.catch { Timber.e(it) }

    private fun List<ContactUiModel>.groupByInitial(): Map<String, List<ContactUiModel>> =
        groupBy { it.displayName.trim().firstOrNull()?.uppercase() ?: "#" }

    /**
     * Set the search query.
     */
    fun setQuery(query: String?) {
        viewModelScope.launch { queryChannel.send(query) }
    }

    /**
     * Get chat room id for a user and trigger navigation.
     */
    fun getChatRoomId(userHandle: Long) {
        viewModelScope.launch {
            runCatching {
                get1On1ChatIdUseCase(userHandle)
            }.onSuccess { chatId ->
                openChatEventChannel.send(triggered(chatId))
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Consume the open chat event.
     */
    fun onChatEventConsumed() {
        openChatEventChannel.trySend(consumed())
    }

    /**
     * Remove a contact by email.
     */
    fun removeContact(email: String) {
        viewModelScope.launch {
            runCatching {
                removeContactByEmailUseCase(email)
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Handle call tap for a user.
     */
    fun onCallTap(userHandle: Long, video: Boolean, audio: Boolean) {
        viewModelScope.launch {
            runCatching {
                val chatId = get1On1ChatIdUseCase(userHandle)
                val existingCall = getChatCallUseCase(chatId)
                if (existingCall != null) {
                    startCallEventChannel.send(
                        triggered(
                            CallEventData(
                                chatId = chatId,
                                hasLocalAudio = audio,
                                hasLocalVideo = video,
                                isExistingCall = true,
                            )
                        )
                    )
                } else {
                    val call = startCallUseCase(chatId = chatId, audio = audio, video = video)
                    startCallEventChannel.send(
                        triggered(
                            CallEventData(
                                chatId = chatId,
                                hasLocalAudio = call?.hasLocalAudio ?: audio,
                                hasLocalVideo = call?.hasLocalVideo ?: video,
                                isExistingCall = false,
                            )
                        )
                    )
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Consume the start call event.
     */
    fun onCallEventConsumed() {
        startCallEventChannel.trySend(consumed())
    }

    private fun ContactUiModel.matches(query: String): Boolean {
        val q = query.lowercase()
        return displayName.lowercase().contains(q)
                || email.lowercase().contains(q)
                || fullName?.lowercase()?.contains(q) == true
                || alias?.lowercase()?.contains(q) == true
    }
}

private data class ContactData(
    val groupedContacts: Map<String, List<ContactUiModel>>,
    val recentlyAdded: List<ContactUiModel>,
)


