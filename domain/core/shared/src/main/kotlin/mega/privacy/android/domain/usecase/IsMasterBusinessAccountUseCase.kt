package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.BusinessRepository
import javax.inject.Inject

/**
 * Use case that check is the account is a master business account
 *
 * @property businessRepository Repository to provide the business data
 */
class IsMasterBusinessAccountUseCase @Inject constructor(
    private val businessRepository: BusinessRepository,
) {

    /**
     * Calls the Use Case to check whether the account is a master business account
     *
     * @return true if the account is a master business account, false otherwise
     */
    suspend operator fun invoke(): Boolean = businessRepository.isMasterBusinessAccount()
}