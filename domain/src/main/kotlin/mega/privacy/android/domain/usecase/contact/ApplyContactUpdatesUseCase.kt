package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Use-case applies the global contact updates
 */
class ApplyContactUpdatesUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
) {

    /**
     * Invoke
     *
     * @param contactItem contact info of selected user
     * @param userUpdate global update received
     * @return [ContactItem] updated contact item
     */
    suspend operator fun invoke(contactItem: ContactItem, userUpdate: UserUpdate): ContactItem {
        val selectedUserChange = userUpdate.changes[UserId(contactItem.handle)] ?: emptyList()
        var updatedContact = contactItem
        if (userUpdate.changes.containsValue(listOf(UserChanges.Alias))) {
            val alias = runCatching {
                contactsRepository.getUserAlias(contactItem.handle)
            }.getOrNull()
            updatedContact =
                updatedContact.copy(contactData = updatedContact.contactData.copy(alias = alias))
        }
        if (UserChanges.Firstname in selectedUserChange ||
            UserChanges.Lastname in selectedUserChange
        ) {
            val name = runCatching {
                contactsRepository.getUserFullName(contactItem.handle, true)
            }.getOrNull()
            updatedContact =
                updatedContact.copy(contactData = updatedContact.contactData.copy(fullName = name))
        }
        if (UserChanges.Email in selectedUserChange) {
            runCatching {
                contactsRepository.getUserEmail(contactItem.handle, true)
            }.onSuccess {
                updatedContact = updatedContact.copy(email = it)
            }
        }
        if (UserChanges.Avatar in selectedUserChange) {
            contactsRepository.deleteAvatar(contactItem.email)
            val file = contactsRepository.getAvatarUri(contactItem.email)
            updatedContact =
                updatedContact.copy(contactData = updatedContact.contactData.copy(avatarUri = file))
        }
        if (UserChanges.AuthenticationInformation in selectedUserChange) {
            runCatching {
                contactsRepository.areCredentialsVerified(contactItem.email)
            }.onSuccess {
                updatedContact = updatedContact.copy(areCredentialsVerified = it)
            }
        }
        return updatedContact
    }
}