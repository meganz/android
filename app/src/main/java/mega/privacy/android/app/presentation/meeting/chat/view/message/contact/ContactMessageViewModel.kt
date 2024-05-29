package mega.privacy.android.app.presentation.meeting.chat.view.message.contact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.usecase.contact.GetContactFromEmailUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import mega.privacy.android.domain.usecase.contact.GetUserUseCase
import mega.privacy.android.domain.usecase.contact.InviteContactWithHandleUseCase
import mega.privacy.android.domain.usecase.contact.IsContactRequestSentUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Contact attachment message view model
 *
 */
@HiltViewModel
class ContactMessageViewModel @Inject constructor(
    private val getContactFromEmailUseCase: GetContactFromEmailUseCase,
    private val getUserUseCase: GetUserUseCase,
    private val isContactRequestSentUseCase: IsContactRequestSentUseCase,
    private val inviteContactWithHandleUseCase: InviteContactWithHandleUseCase,
    private val getMyUserHandleUseCase: GetMyUserHandleUseCase,
) : ViewModel() {
    /**
     * Load contact info
     *
     * @param contactEmail Contact email
     */
    suspend fun loadContactInfo(contactEmail: String): ContactItem? {
        return runCatching {
            getContactFromEmailUseCase(contactEmail, false)
                ?.takeIf { it.contactData.fullName?.isNotEmpty() == true }
            // try to load from sdk in case contact cache not available
                ?: getContactFromEmailUseCase(contactEmail, true)
        }.onFailure {
            Timber.e(it, "Failed to get contact from email")
        }.getOrNull()
    }

    /**
     * Check user
     */
    fun checkUser(
        userHandle: Long,
        email: String,
        isContact: Boolean,
        onContactClicked: () -> Unit,
        onNonContactClicked: () -> Unit,
        onNonContactAlreadyInvitedClicked: () -> Unit,
    ) {
        viewModelScope.launch {
            val myUserHandle = getMyUserHandleUseCase()
            when {
                userHandle == myUserHandle -> {
                    return@launch
                }

                isContact -> {
                    onContactClicked()
                }

                else -> {
                    runCatching { isContactRequestSentUseCase(email) }
                        .onSuccess { isSent ->
                            if (isSent) {
                                onNonContactAlreadyInvitedClicked()
                            } else {
                                onNonContactClicked()
                            }
                        }
                        .onFailure { Timber.e(it) }
                }
            }
        }
    }

    /**
     * Invite user.
     */
    fun inviteUser(email: String, handle: Long, onInvitationSent: () -> Unit) {
        viewModelScope.launch {
            runCatching { inviteContactWithHandleUseCase(email, handle, null) }
                .onSuccess { onInvitationSent() }
                .onFailure {
                    Timber.e(it)
                }
        }
    }
}