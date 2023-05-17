package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Fetch multi factor auth setting
 *
 */
class FetchMultiFactorAuthSettingUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke
     *
     * @return
     */
    suspend operator fun invoke() = accountRepository.isMultiFactorAuthEnabled()
}