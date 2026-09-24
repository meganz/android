package mega.privacy.android.domain.usecase.qrcode

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use case to reset contact link
 */
class ResetContactLinkUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    /**
     * invoke method
     *
     * @return new contact link
     */
    suspend operator fun invoke(): String = accountRepository.createContactLink(renew = true)
}