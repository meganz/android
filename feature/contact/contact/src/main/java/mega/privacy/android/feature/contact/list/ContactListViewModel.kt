package mega.privacy.android.feature.contact.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
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
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.usecase.account.GetCurrentStorageStateUseCase
import mega.privacy.android.domain.usecase.account.contactrequest.MonitorContactRequestsUseCase
import mega.privacy.android.domain.usecase.call.GetChatCallUseCase
import mega.privacy.android.domain.usecase.call.IsParticipatingInChatCallUseCase
import mega.privacy.android.domain.usecase.call.StartCallUseCase
import mega.privacy.android.domain.usecase.chat.Get1On1ChatIdUseCase
import mega.privacy.android.domain.usecase.contact.GetContactsUseCase
import mega.privacy.android.domain.usecase.contact.RemoveContactByEmailUseCase
import mega.privacy.android.feature.contact.list.model.CallEventData
import mega.privacy.android.feature.contact.list.model.ContactListMessage
import mega.privacy.android.feature.contact.list.model.ContactListUiState
import mega.privacy.android.shared.contact.mapper.ContactItemUiStateMapper
import mega.privacy.android.shared.contact.model.ContactItemUiState
import timber.log.Timber
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * Contact list view model
 *
 * @property getContactsUseCase
 * @property get1On1ChatIdUseCase
 * @property removeContactByEmailUseCase
 * @property startCallUseCase
 * @property getChatCallUseCase
 * @property isParticipatingInChatCallUseCase
 * @property getCurrentStorageStateUseCase
 * @property monitorContactRequestsUseCase
 * @property contactItemUiStateMapper
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ContactListViewModel @Inject constructor(
    private val getContactsUseCase: GetContactsUseCase,
    private val get1On1ChatIdUseCase: Get1On1ChatIdUseCase,
    private val removeContactByEmailUseCase: RemoveContactByEmailUseCase,
    private val startCallUseCase: StartCallUseCase,
    private val getChatCallUseCase: GetChatCallUseCase,
    private val isParticipatingInChatCallUseCase: IsParticipatingInChatCallUseCase,
    private val getCurrentStorageStateUseCase: GetCurrentStorageStateUseCase,
    private val monitorContactRequestsUseCase: MonitorContactRequestsUseCase,
    private val contactItemUiStateMapper: ContactItemUiStateMapper,
) : ViewModel() {

    private val queryChannel = Channel<String?>(Channel.CONFLATED)
    private val openChatEventChannel = Channel<StateEventWithContent<Long>>(Channel.BUFFERED)
    private val startCallEventChannel =
        Channel<StateEventWithContent<CallEventData>>(Channel.BUFFERED)
    private val messageEventChannel =
        Channel<StateEventWithContent<ContactListMessage>>(Channel.BUFFERED)
    private val storageOverQuotaEventChannel = Channel<StateEvent>(Channel.BUFFERED)

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
            monitorStateEvents(),
        ) { contactData, requestCount, events ->
            ContactListUiState.Data(
                contacts = contactData.groupedContacts,
                recentlyAddedContacts = contactData.recentlyAdded,
                incomingRequestCount = requestCount,
                openChatEvent = events.openChatEvent,
                startCallEvent = events.startCallEvent,
                messageEvent = events.messageEvent,
                storageOverQuotaEvent = events.storageOverQuotaEvent,
            )
        }.catch { Timber.e(it) }
            .asUiStateFlow(viewModelScope, ContactListUiState.Loading)
    }

    private fun monitorStateEvents(): Flow<ContactListEvents> = combine(
        openChatEventChannel.receiveAsFlow().onStart { emit(consumed()) },
        startCallEventChannel.receiveAsFlow().onStart { emit(consumed()) },
        messageEventChannel.receiveAsFlow().onStart { emit(consumed()) },
        storageOverQuotaEventChannel.receiveAsFlow().onStart { emit(consumed) },
    ) { openChatEvent, startCallEvent, messageEvent, storageOverQuotaEvent ->
        ContactListEvents(
            openChatEvent = openChatEvent,
            startCallEvent = startCallEvent,
            messageEvent = messageEvent,
            storageOverQuotaEvent = storageOverQuotaEvent,
        )
    }

    private fun contactsWithSearchFlow(): Flow<ContactData> =
        combine(
            queryChannel.receiveAsFlow()
                .onStart { emit(null) },
            getContactsUseCase().map { domainList ->
                domainList.map { item ->
                    IndexedContact(
                        data = item,
                        ui = contactItemUiStateMapper(item),
                        isNew = item.isRecentlyAdded(),
                    )
                }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.ui.displayName })
            }
        ) { query, indexed ->
            if (query.isNullOrBlank()) {
                ContactData(
                    groupedContacts = indexed.map { it.ui }.groupByInitial(),
                    recentlyAdded = indexed.filter { it.isNew }.map { it.ui }.toImmutableList(),
                )
            } else {
                val filtered = indexed.filter { it.matches(query) }
                ContactData(
                    groupedContacts = filtered.map { it.ui }.groupByInitial(),
                    recentlyAdded = emptyList<ContactItemUiState>().toImmutableList(),
                )
            }
        }.catch { Timber.e(it) }

    private fun List<ContactItemUiState>.groupByInitial(): Map<String, List<ContactItemUiState>> =
        groupBy { it.displayName.trim().firstOrNull()?.uppercase() ?: "#" }

    /**
     * Set query
     *
     * @param query
     */
    fun setQuery(query: String?) {
        viewModelScope.launch { queryChannel.send(query) }
    }

    /**
     * Get chat room id
     *
     * @param userHandle
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
     * Remove contact
     *
     * @param email
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
     * Start a call with the contact, or fire the start call event for the ongoing call when one
     * already exists in the 1:1 chat. Fires the storage over quota event instead when the account
     * is in paywall state, and the ongoing call message when the user is already participating
     * in a call in another chat.
     *
     * @param userHandle
     * @param video
     * @param audio
     */
    fun onCallTap(userHandle: Long, video: Boolean, audio: Boolean) {
        viewModelScope.launch {
            if (isStorageStatePayWall()) return@launch
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
                } else if (isParticipatingInChatCallUseCase()) {
                    messageEventChannel.send(triggered(ContactListMessage.OngoingCall))
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

    private suspend fun isStorageStatePayWall(): Boolean {
        val isPayWall = runCatching { getCurrentStorageStateUseCase() }
            .getOrNull() == StorageState.PayWall
        if (isPayWall) {
            storageOverQuotaEventChannel.send(triggered)
        }
        return isPayWall
    }

    /**
     * Show the feedback message for a denied microphone permission when starting a call.
     */
    fun onCallPermissionsDenied() {
        messageEventChannel.trySend(triggered(ContactListMessage.MicrophonePermissionDenied))
    }

    /**
     * Consume the start call event.
     */
    fun onCallEventConsumed() {
        startCallEventChannel.trySend(consumed())
    }

    /**
     * Consume the message event once the screen has shown the message.
     */
    fun onMessageEventConsumed() {
        messageEventChannel.trySend(consumed())
    }

    /**
     * Consume the storage over quota event once the paywall warning has been shown.
     */
    fun onStorageOverQuotaEventConsumed() {
        storageOverQuotaEventChannel.trySend(consumed)
    }

    private fun IndexedContact.matches(query: String): Boolean {
        val q = query.lowercase()
        return ui.displayName.lowercase().contains(q)
                || data.email.lowercase().contains(q)
                || data.contactData.fullName?.lowercase()?.contains(q) == true
                || data.contactData.alias?.lowercase()?.contains(q) == true
    }

    private fun ContactItem.isRecentlyAdded(): Boolean =
        chatroomId == null && isWithinLastThreeDays(timestamp)

    private fun isWithinLastThreeDays(timestamp: Long): Boolean {
        val now = LocalDateTime.now()
        val addedTime = Instant.ofEpochSecond(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        return Duration.between(addedTime, now).toDays() < 3
    }

    /**
     * Indexed contact
     *
     * @property data
     * @property ui
     * @property isNew
     */
    private data class IndexedContact(
        val data: ContactItem,
        val ui: ContactItemUiState,
        val isNew: Boolean,
    )

    /**
     * Contact data
     *
     * @property groupedContacts
     * @property recentlyAdded
     */
    private data class ContactData(
        val groupedContacts: Map<String, List<ContactItemUiState>>,
        val recentlyAdded: ImmutableList<ContactItemUiState>,
    )

    /**
     * One-shot events combined separately to stay within the combine arity.
     *
     * @property openChatEvent
     * @property startCallEvent
     * @property messageEvent
     * @property storageOverQuotaEvent
     */
    private data class ContactListEvents(
        val openChatEvent: StateEventWithContent<Long>,
        val startCallEvent: StateEventWithContent<CallEventData>,
        val messageEvent: StateEventWithContent<ContactListMessage>,
        val storageOverQuotaEvent: StateEvent,
    )
}


