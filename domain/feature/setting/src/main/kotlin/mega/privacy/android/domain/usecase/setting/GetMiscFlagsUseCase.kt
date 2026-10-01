package mega.privacy.android.domain.usecase.setting

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use Case to get misc flags from SDK
 */
class GetMiscFlagsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke() = accountRepository.getMiscFlags()

}
