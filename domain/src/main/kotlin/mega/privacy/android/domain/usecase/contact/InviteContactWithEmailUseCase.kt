package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for inviting a contact by email.
 *
 * @property chatRepository Chat repository.
 */
class InviteContactWithEmailUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invocation method.
     *
     * @param email    User email
     * @return         Result
     */
    suspend operator fun invoke(email: String): InviteContactRequest =
        chatRepository.inviteContact(email)
}
