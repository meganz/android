package mega.privacy.android.domain.usecase.qrcode

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use case to create a contact link.
 */
class CreateContactLinkUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke method
     *
     * @param renew true to invalidate the previous contact link (if any)
     * @return Generated contact link URL. null if anything wrong.
     */
    suspend operator fun invoke(renew: Boolean): String = accountRepository.createContactLink(renew)
}