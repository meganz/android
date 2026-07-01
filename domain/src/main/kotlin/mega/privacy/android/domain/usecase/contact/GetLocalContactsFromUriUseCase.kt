package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.entity.contacts.LocalContact
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Use case to get local contacts from a contact picker session [UriPath].
 *
 * @property contactsRepository [ContactsRepository]
 */
class GetLocalContactsFromUriUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
) {

    /**
     * Invocation method to retrieve the local contacts from the given [UriPath].
     *
     * @param uriPath The [UriPath] returned by the contact picker.
     * @return List of [LocalContact]
     */
    suspend operator fun invoke(uriPath: UriPath): List<LocalContact> =
        contactsRepository.getLocalContactsFromUri(uriPath)
}
