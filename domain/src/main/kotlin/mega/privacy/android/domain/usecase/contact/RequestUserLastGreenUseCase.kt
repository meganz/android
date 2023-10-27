package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Request last green
 *
 * @property contactsRepository
 * @constructor Create empty Request last green
 */
class RequestUserLastGreenUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
) {

    /**
     * Invoke.
     *
     * @param userHandle User handle.
     */
    suspend operator fun invoke(userHandle: Long) = contactsRepository.requestLastGreen(userHandle)
}