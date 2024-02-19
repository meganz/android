package mega.privacy.android.app.presentation.meeting.chat.view.message.contact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.usecase.contact.GetContactFromEmailUseCase
import mega.privacy.android.domain.usecase.contact.GetUserUseCase
import mega.privacy.android.domain.usecase.contact.InviteContactUseCase
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
    private val inviteContactUseCase: InviteContactUseCase,
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
        onContactClicked: (String) -> Unit,
        onNonContactClicked: () -> Unit,
        onNonContactAlreadyInvitedClicked: () -> Unit,
    ) {
        viewModelScope.launch {
            runCatching {
                getUserUseCase(UserId(userHandle))
            }.onSuccess { it ->
                it?.takeIf { it.visibility == UserVisibility.Visible }?.let { user ->
                    onContactClicked(user.email)
                } ?: run {
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
            }.onFailure {
                Timber.d(it)
            }
        }
    }

    /**
     * Invite user.
     */
    fun inviteUser(email: String, handle: Long, onInvitationSent: () -> Unit) {
        viewModelScope.launch {
            runCatching { inviteContactUseCase(email, handle, null) }
                .onSuccess { onInvitationSent() }
                .onFailure {
                    Timber.e(it)
                }
        }
    }
}