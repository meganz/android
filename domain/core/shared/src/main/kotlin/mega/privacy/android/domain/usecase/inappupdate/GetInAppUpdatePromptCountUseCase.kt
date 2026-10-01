package mega.privacy.android.domain.usecase.inappupdate

import mega.privacy.android.domain.repository.InAppUpdateRepository
import javax.inject.Inject

/**
 * Get InAppUpdate Prompt Count Use case
 */
class GetInAppUpdatePromptCountUseCase @Inject constructor(
    private val inAppUpdateRepository: InAppUpdateRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(): Int = inAppUpdateRepository.getInAppUpdatePromptCount()
}