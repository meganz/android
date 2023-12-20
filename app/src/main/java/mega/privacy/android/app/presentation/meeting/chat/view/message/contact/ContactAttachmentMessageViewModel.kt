package mega.privacy.android.app.presentation.meeting.chat.view.message.contact

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.usecase.contact.GetContactFromEmailUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Contact attachment message view model
 *
 */
@HiltViewModel
class ContactAttachmentMessageViewModel @Inject constructor(
    private val getContactFromEmailUseCase: GetContactFromEmailUseCase,
) : ViewModel() {
    /**
     * Load contact info
     *
     * @param contactEmail Contact email
     */
    suspend fun loadContactInfo(contactEmail: String): ContactItem? {
        return runCatching {
            val item = getContactFromEmailUseCase(contactEmail, false)
            // try to load from sdk in case contact cache not available
            if (item != null && item.contactData.fullName.isNullOrEmpty()) {
                getContactFromEmailUseCase(contactEmail, true)
            } else {
                item
            }
        }.onFailure {
            Timber.e(it, "Failed to get contact from email")
        }.getOrNull()
    }
}