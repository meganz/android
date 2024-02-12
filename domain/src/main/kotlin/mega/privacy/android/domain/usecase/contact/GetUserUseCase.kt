package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.entity.contacts.User
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Use case to get [User]
 *
 * @property contactsRepository
 */
class GetUserUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
) {
    /**
     * Invoke
     *
     * @param userId [UserId]
     * @return [User]
     */
    suspend operator fun invoke(userId: UserId) = contactsRepository.getUser(userId)
}