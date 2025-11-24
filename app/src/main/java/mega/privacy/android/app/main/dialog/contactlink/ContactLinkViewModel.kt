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
import mega.privacy.android.domain.entity.contacts.ContactLinkQueryResult
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.usecase.contact.ContactLinkQueryUseCase
import mega.privacy.android.domain.usecase.contact.InviteContactWithHandleUseCase
import javax.inject.Inject

@HiltViewModel
internal class ContactLinkViewModel @Inject constructor(
    private val contactLinkQueryUseCase: ContactLinkQueryUseCase,
    private val inviteContactWithHandleUseCase: InviteContactWithHandleUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val userHandle = savedStateHandle.get<Long>(ContactLinkDialogFragment.EXTRA_USER_HANDLE)
        ?: throw NullPointerException("userHandle is null")
    private val _state = MutableStateFlow(ContactLinkUiState())
    val state = _state.asStateFlow()
    private var sendInviteJob: Job? = null

    init {
        viewModelScope.launch {
            val result = runCatching { contactLinkQueryUseCase(userHandle) }
            _state.update { it.copy(contactLinkQueryResult = result) }
        }
    }

    fun sendContactInvitation(contactLinkHandle: Long, email: String) {
        if (sendInviteJob?.isActive == true) return // prevent multiple invite send
        sendInviteJob = viewModelScope.launch {
            val result =
                runCatching { inviteContactWithHandleUseCase(email, contactLinkHandle, null) }
            _state.update { it.copy(sentInviteResult = result) }
        }
    }
}

/**
 * Contact link ui state
 *
 * @property contactLinkQueryResult
 * @property sentInviteResult
 */
data class ContactLinkUiState(
    val contactLinkQueryResult: Result<ContactLinkQueryResult>? = null,
    val sentInviteResult: Result<InviteContactRequest>? = null,
)