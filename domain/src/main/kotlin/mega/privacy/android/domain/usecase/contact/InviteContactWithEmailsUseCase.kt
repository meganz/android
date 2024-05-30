package mega.privacy.android.domain.usecase.contact

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import javax.inject.Inject

/**
 * Use case for inviting contacts by email.
 *
 * @property inviteContactWithEmailUseCase Use case for inviting a contact by email.
 */
class InviteContactWithEmailsUseCase @Inject constructor(
    private val inviteContactWithEmailUseCase: InviteContactWithEmailUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {

    /**
     * Invocation method.
     *
     * @param emails List of emails that need to be invited.
     * @return The list of contact request results.
     */
    suspend operator fun invoke(emails: List<String>): List<InviteContactRequest> =
        withContext(defaultDispatcher) {
            val semaphore = Semaphore(8)
            val result = emails.map { email ->
                async {
                    semaphore.withPermit {
                        runCatching { inviteContactWithEmailUseCase(email) }
                    }
                }
            }.awaitAll()

            result.map { it.getOrDefault(InviteContactRequest.InvalidStatus) }
        }
}
