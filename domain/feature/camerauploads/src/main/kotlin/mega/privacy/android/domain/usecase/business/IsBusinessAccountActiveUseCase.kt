package mega.privacy.android.domain.usecase.business

import mega.privacy.android.domain.repository.BusinessRepository
import javax.inject.Inject

/**
 * Use case that checks whether the Business Account is Active or not
 */
class IsBusinessAccountActiveUseCase @Inject constructor(
    private val businessRepository: BusinessRepository,
) {

    /**
     * Invocation function
     *
     * @return true if the Business Account is active
     */
    suspend operator fun invoke() = businessRepository.isBusinessAccountActive()
}