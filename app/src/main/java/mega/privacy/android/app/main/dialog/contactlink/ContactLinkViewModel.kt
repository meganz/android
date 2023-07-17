package mega.privacy.android.app.main.dialog.contactlink

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.contacts.ContactLink
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.usecase.contact.GetContactLinkUseCase
import mega.privacy.android.domain.usecase.contact.InviteContactUseCase
import javax.inject.Inject

@HiltViewModel
internal class ContactLinkViewModel @Inject constructor(
    private val getContactLinkUseCase: GetContactLinkUseCase,
    private val inviteContactUseCase: InviteContactUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val userHandle = savedStateHandle.get<Long>(ContactLinkDialogFragment.EXTRA_USER_HANDLE)
        ?: throw NullPointerException("userHandle is null")
    private val _state = MutableStateFlow(ContactLinkUiState())
    val state = _state.asStateFlow()
    private var sendInviteJob: Job? = null

    init {
        viewModelScope.launch {
            val result = runCatching { getContactLinkUseCase(userHandle) }
            _state.update { it.copy(contactLinkResult = result) }
        }
    }

    fun sendContactInvitation(contactLinkHandle: Long, email: String) {
        if (sendInviteJob?.isActive == true) return // prevent multiple invite send
        sendInviteJob = viewModelScope.launch {
            val result = runCatching { inviteContactUseCase(email, contactLinkHandle, null) }
            _state.update { it.copy(sentInviteResult = result) }
        }
    }
}

/**
 * Contact link ui state
 *
 * @property contactLinkResult
 * @property sentInviteResult
 */
data class ContactLinkUiState(
    val contactLinkResult: Result<ContactLink>? = null,
    val sentInviteResult: Result<InviteContactRequest>? = null,
)