package mega.privacy.android.feature.contact.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.usecase.chat.GetChatRoomByUserUseCase
import mega.privacy.android.domain.usecase.contact.GetContactFromChatUseCase
import mega.privacy.android.domain.usecase.contact.GetContactFromEmailUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.feature.contact.info.model.ContactInfoUiState
import mega.privacy.android.shared.contact.extension.displayName
import timber.log.Timber

/**
 * Contact info view model. Resolves the contact either from an [email] (contact list entry point)
 * or from a [chatId] (1:1 chat entry point) and exposes the resolved contact data. When the
 * contact cannot be resolved a close event is fired so the screen can pop itself.
 *
 * @property email email of the contact, or null when entering from a chat.
 * @property chatId id of the 1:1 chat with the contact, or null when entering by email.
 * @property getContactFromEmailUseCase
 * @property getContactFromChatUseCase
 * @property getChatRoomByUserUseCase
 * @property isConnectedToInternetUseCase
 */
@HiltViewModel(assistedFactory = ContactInfoViewModel.Factory::class)
internal class ContactInfoViewModel @AssistedInject constructor(
    @Assisted private val email: String?,
    @Assisted private val chatId: Long?,
    private val getContactFromEmailUseCase: GetContactFromEmailUseCase,
    private val getContactFromChatUseCase: GetContactFromChatUseCase,
    private val getChatRoomByUserUseCase: GetChatRoomByUserUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
) : ViewModel() {

    /**
     * Factory for assisted creation, supplying the entry point arguments from the navigation key.
     */
    @AssistedFactory
    interface Factory {
        /**
         * @param email email of the contact, or null when entering from a chat.
         * @param chatId id of the 1:1 chat with the contact, or null when entering by email.
         */
        fun create(email: String?, chatId: Long?): ContactInfoViewModel
    }

    private val closeEventChannel = Channel<StateEvent>(Channel.BUFFERED)

    /**
     * Ui state
     */
    val uiState: StateFlow<ContactInfoUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            resolvedContactSource(),
            closeEventChannel.receiveAsFlow().onStart { emit(consumed) },
        ) { resolved, closeEvent ->
            resolved?.let {
                ContactInfoUiState.Data(
                    displayName = it.contact.displayName(),
                    email = it.contact.email,
                    userHandle = it.contact.handle,
                    chatRoomId = it.chatRoomId,
                    isFromContacts = it.isFromContacts,
                    closeEvent = closeEvent,
                )
            } ?: ContactInfoUiState.Loading(closeEvent = closeEvent)
        }.catch { Timber.e(it, "Failed to build contact info state") }
            .asUiStateFlow(
                viewModelScope,
                ContactInfoUiState.Loading(closeEvent = consumed),
            )
    }

    private fun resolvedContactSource(): Flow<ResolvedContact?> = flow {
        emit(null)
        val resolved = runCatching { resolveContact() }
            .onFailure { Timber.e(it, "Failed to resolve contact") }
            .getOrNull()
        if (resolved != null) {
            emit(resolved)
        } else {
            closeEventChannel.send(triggered)
        }
        awaitCancellation()
    }

    private suspend fun resolveContact(): ResolvedContact? {
        val skipCache = isConnectedToInternetUseCase()
        return when {
            chatId != null -> getContactFromChatUseCase(chatId, skipCache)?.let { contact ->
                ResolvedContact(
                    contact = contact,
                    chatRoomId = chatId,
                    isFromContacts = false,
                )
            }

            email != null -> getContactFromEmailUseCase(email, skipCache)?.let { contact ->
                ResolvedContact(
                    contact = contact,
                    chatRoomId = runCatching { getChatRoomByUserUseCase(contact.handle)?.chatId }
                        .onFailure { Timber.e(it, "Failed to get chat room for contact") }
                        .getOrNull(),
                    isFromContacts = true,
                )
            }

            else -> null
        }
    }

    /**
     * Consume the close event once the screen has navigated back.
     */
    fun onCloseEventConsumed() {
        closeEventChannel.trySend(consumed)
    }

    private data class ResolvedContact(
        val contact: ContactItem,
        val chatRoomId: Long?,
        val isFromContacts: Boolean,
    )
}
