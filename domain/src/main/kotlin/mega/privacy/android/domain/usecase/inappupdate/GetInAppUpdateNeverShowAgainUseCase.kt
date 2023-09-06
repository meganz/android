package mega.privacy.android.domain.usecase.inappupdate

import mega.privacy.android.domain.repository.InAppUpdateRepository
import javax.inject.Inject

/**
 * Get InAppUpdate Never Show Again Use case
 */
class GetInAppUpdateNeverShowAgainUseCase @Inject constructor(
    private val inAppUpdateRepository: InAppUpdateRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(): Boolean = inAppUpdateRepository.getInAppUpdateNeverShowAgain()
}