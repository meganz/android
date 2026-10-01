package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.repository.BusinessRepository
import javax.inject.Inject

/**
 * Get Business Account Status Use Case
 */
class GetBusinessStatusUseCase @Inject constructor(
    private val businessRepository: BusinessRepository
) {
    /**
     * Invoke
     * @return business account status as [BusinessAccountStatus]
     */
    suspend operator fun invoke(): BusinessAccountStatus = businessRepository.getBusinessStatus()
}