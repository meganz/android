package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Get specific account detail
 *
 */
class GetSpecificAccountDetailUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke
     *
     * @param storage  If true, account storage details are requested
     * @param transfer If true, account transfer details are requested
     * @param pro      If true, pro level of account is requested
     */
    suspend operator fun invoke(
        storage: Boolean,
        transfer: Boolean,
        pro: Boolean,
    ) = accountRepository.getSpecificAccountDetail(
        storage = storage,
        transfer = transfer,
        pro = pro
    )
}