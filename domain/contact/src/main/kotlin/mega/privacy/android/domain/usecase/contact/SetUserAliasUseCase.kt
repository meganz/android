package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Set user alias name
 */
class SetUserAliasUseCase @Inject constructor(private val contactsRepository: ContactsRepository) {

    /**
     * invoke
     *
     * @param name updated nick name
     * @param userHandle user handle
     * @return [String] updated nick name
     */
    suspend operator fun invoke(name: String?, userHandle: Long) =
        contactsRepository.setUserAlias(name, userHandle)
}