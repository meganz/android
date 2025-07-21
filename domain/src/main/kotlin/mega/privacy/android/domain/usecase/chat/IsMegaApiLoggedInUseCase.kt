package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use case for checking if the MegaApi is logged in
 */
class IsMegaApiLoggedInUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    /**
     * Invoke
     *
     * @return true if MegaApi is logged in, false otherwise.
     */
    suspend operator fun invoke(): Boolean =
        accountRepository.isMegaApiLoggedIn()
} 