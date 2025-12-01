package mega.privacy.android.app.presentation.contact.link

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.contact.link.dialog.ContactLinkDialogNavKey
import mega.privacy.android.app.presentation.contact.link.dialog.ContactLinkDialogUiState
import mega.privacy.android.domain.entity.contacts.ContactLinkQueryResult
import mega.privacy.android.domain.usecase.avatar.GetUserAvatarColorUseCase
import mega.privacy.android.domain.usecase.contact.GetAvatarFromBase64StringUseCase
import mega.privacy.android.domain.usecase.contact.InviteContactWithHandleUseCase
import timber.log.Timber

@HiltViewModel(assistedFactory = ContactLinkDialogViewModel.Factory::class)
internal class ContactLinkDialogViewModel @AssistedInject constructor(
    private val inviteContactWithHandleUseCase: InviteContactWithHandleUseCase,
    private val getAvatarFromBase64StringUseCase: GetAvatarFromBase64StringUseCase,
    private val getUserAvatarColorUseCase: GetUserAvatarColorUseCase,
    @Assisted private val navKey: ContactLinkDialogNavKey,
) : ViewModel() {
    private val args = navKey
    private val _uiState = MutableStateFlow(ContactLinkDialogUiState())
    val uiState = _uiState.asStateFlow()
    private var inviteContactJob: Job? = null

    init {
        _uiState.update { state -> state.copy(contactLinkQueryResult = args.contactLinkQueryResult) }
        getAvatar(args.contactLinkQueryResult)
    }

    private fun getAvatar(contactLinkQueryResult: ContactLinkQueryResult) {
        viewModelScope.launch {
            contactLinkQueryResult.avatarFileInBase64
                ?.takeUnless { it == "none" }?.let { base64String ->
                    runCatching {
                        getAvatarFromBase64StringUseCase(
                            userHandle = contactLinkQueryResult.contactHandle,
                            base64String = base64String,
                        )
                    }.onFailure {
                        Timber.w(it)
                    }.onSuccess {
                        _uiState.update { state -> state.copy(avatarFile = it) }
                    }
                }
            runCatching {
                getUserAvatarColorUseCase(
                    userHandle = contactLinkQueryResult.contactHandle,
                )
            }.onFailure { Timber.w(it) }.getOrNull()?.let {
                val color = Color(it)
                _uiState.update { state -> state.copy(avatarColor = color) }
            }
        }
    }

    fun inviteContact() {
        if (inviteContactJob?.isActive == true) return // prevent multiple invite send
        uiState.value.contactLinkQueryResult?.let { result ->
            result.email?.let { email ->
                inviteContactJob = viewModelScope.launch {
                    runCatching {
                        inviteContactWithHandleUseCase(
                            email = email,
                            handle = result.contactHandle,
                            message = null,
                        )
                    }.let { inviteContactResult ->
                        _uiState.update { state ->
                            state.copy(inviteContactResult = inviteContactResult)
                        }
                    }
                }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(navKey: ContactLinkDialogNavKey): ContactLinkDialogViewModel
    }
}