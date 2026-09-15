package mega.privacy.android.feature.contact.attachment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.contacts.AttachedContact
import mega.privacy.android.domain.usecase.chat.StartConversationUseCase
import mega.privacy.android.domain.usecase.contact.GetContactsAttachedToChatMessageUseCase
import mega.privacy.android.domain.usecase.contact.InviteContactWithEmailsUseCase
import mega.privacy.android.feature.contact.attachment.model.ContactAttachmentItem
import mega.privacy.android.feature.contact.attachment.model.ContactAttachmentMessage
import mega.privacy.android.feature.contact.attachment.model.ContactAttachmentPrimaryAction
import mega.privacy.android.feature.contact.attachment.model.ContactAttachmentUiState
import timber.log.Timber

/**
 * View model backing the contact attachment screen. Resolves the contacts attached to the message
 * identified by [chatId] and [messageId] and exposes the primary invite/start-conversation action
 * along with the per-row actions.
 *
 * @property chatId Chat id containing the message.
 * @property messageId Id of the message holding the contact attachments.
 */
@HiltViewModel(assistedFactory = ContactAttachmentViewModel.Factory::class)
internal class ContactAttachmentViewModel @AssistedInject constructor(
    @Assisted("chatId") private val chatId: Long,
    @Assisted("messageId") private val messageId: Long,
    private val getContactsAttachedToChatMessageUseCase: GetContactsAttachedToChatMessageUseCase,
    private val inviteContactWithEmailsUseCase: InviteContactWithEmailsUseCase,
    private val startConversationUseCase: StartConversationUseCase,
) : ViewModel() {

    /**
     * Factory for assisted creation, supplying the message coordinates from the navigation key.
     */
    @AssistedFactory
    interface Factory {
        /**
         * @param chatId Chat id containing the message.
         * @param messageId Id of the message holding the contact attachments.
         */
        fun create(
            @Assisted("chatId") chatId: Long,
            @Assisted("messageId") messageId: Long,
        ): ContactAttachmentViewModel
    }

    private val messageEventChannel =
        Channel<StateEventWithContent<ContactAttachmentMessage>>(Channel.BUFFERED)
    private val openChatEventChannel = Channel<StateEventWithContent<Long>>(Channel.BUFFERED)
    private val openContactInfoEventChannel =
        Channel<StateEventWithContent<String>>(Channel.BUFFERED)

    /**
     * Ui state.
     */
    val uiState: StateFlow<ContactAttachmentUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            attachedContacts(),
            messageEventChannel.receiveAsFlow().onStart { emit(consumed()) },
            openChatEventChannel.receiveAsFlow().onStart { emit(consumed()) },
            openContactInfoEventChannel.receiveAsFlow().onStart { emit(consumed()) },
        ) { contacts, messageEvent, openChatEvent, openContactInfoEvent ->
            ContactAttachmentUiState.Data(
                contacts = contacts.toImmutableList(),
                primaryAction = contacts.primaryActionOrNull(),
                messageEvent = messageEvent,
                openChatEvent = openChatEvent,
                openContactInfoEvent = openContactInfoEvent,
            )
        }.asUiStateFlow(viewModelScope, ContactAttachmentUiState.Loading)
    }

    private fun attachedContacts(): Flow<List<ContactAttachmentItem>> = flow {
        emit(
            runCatching { getContactsAttachedToChatMessageUseCase(chatId, messageId) }
                .onFailure { Timber.e(it, "Failed to load attached contacts") }
                .getOrDefault(emptyList())
                .map { it.toItem() }
        )
    }

    /**
     * Handle the primary bottom action: invite every attached non-contact when at least one
     * attached user is not a contact yet, otherwise start a conversation with the attached
     * contacts.
     */
    fun onPrimaryActionClick() {
        val data = uiState.value as? ContactAttachmentUiState.Data ?: return
        when (data.primaryAction) {
            ContactAttachmentPrimaryAction.Invite ->
                invite(data.contacts.filter { !it.isMe && !it.isContact }.map { it.email })

            ContactAttachmentPrimaryAction.StartConversation -> {
                val handles = data.contacts.filterNot { it.isMe }.map { it.handle }
                startConversation(isGroup = handles.size > 1, handles = handles)
            }

            null -> Unit
        }
    }

    /**
     * Handle a row tap on the attached user identified by [handle]: open the contact info screen
     * for a visible contact, otherwise surface the "not a contact" feedback message.
     */
    fun onContactClicked(handle: Long) {
        val data = uiState.value as? ContactAttachmentUiState.Data ?: return
        val contact = data.contacts.firstOrNull { it.handle == handle } ?: return
        if (contact.isContact && contact.email.isNotBlank()) {
            openContactInfoEventChannel.trySend(triggered(contact.email))
        } else {
            messageEventChannel.trySend(triggered(ContactAttachmentMessage.UserNotAContact))
        }
    }

    /**
     * Start a one-to-one conversation with the attached contact identified by [handle].
     */
    fun onStartConversation(handle: Long) {
        startConversation(isGroup = false, handles = listOf(handle))
    }

    /**
     * Invite the attached user identified by [email] to become a contact.
     */
    fun onInviteContact(email: String) {
        invite(listOf(email))
    }

    private fun invite(emails: List<String>) {
        if (emails.isEmpty()) return
        viewModelScope.launch {
            runCatching { inviteContactWithEmailsUseCase(emails) }
                .onFailure { Timber.e(it, "Failed to invite contacts") }
            messageEventChannel.send(triggered(ContactAttachmentMessage.ContactsInvited))
        }
    }

    private fun startConversation(isGroup: Boolean, handles: List<Long>) {
        if (handles.isEmpty()) return
        viewModelScope.launch {
            runCatching { startConversationUseCase(isGroup = isGroup, userHandles = handles) }
                .onSuccess { openChatEventChannel.send(triggered(it)) }
                .onFailure {
                    Timber.e(it, "Failed to start conversation")
                    messageEventChannel.send(
                        triggered(ContactAttachmentMessage.StartConversationError)
                    )
                }
        }
    }

    /**
     * Consume the message event once the screen has shown the message.
     */
    fun onMessageEventConsumed() {
        messageEventChannel.trySend(consumed())
    }

    /**
     * Consume the open chat event once the screen has navigated to the chat.
     */
    fun onOpenChatEventConsumed() {
        openChatEventChannel.trySend(consumed())
    }

    /**
     * Consume the open contact info event once the screen has navigated to the contact info.
     */
    fun onOpenContactInfoEventConsumed() {
        openContactInfoEventChannel.trySend(consumed())
    }

    private fun AttachedContact.toItem() = ContactAttachmentItem(
        handle = handle,
        email = email,
        name = name,
        isMe = isMe,
        isContact = isContact,
        isVerified = isVerified,
        avatarColor = avatarColor,
    )

    private fun List<ContactAttachmentItem>.primaryActionOrNull(): ContactAttachmentPrimaryAction? =
        when {
            isEmpty() -> null
            any { !it.isMe && !it.isContact } -> ContactAttachmentPrimaryAction.Invite
            else -> ContactAttachmentPrimaryAction.StartConversation
        }
}
