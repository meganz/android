package mega.privacy.android.domain.usecase.inappupdate

import mega.privacy.android.domain.repository.InAppUpdateRepository
import javax.inject.Inject

/**
 * Increment InAppUpdate Prompt Count Use case
 */
class IncrementInAppUpdatePromptCountUseCase @Inject constructor(
    private val inAppUpdateRepository: InAppUpdateRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke() = inAppUpdateRepository.incrementInAppUpdatePromptCount()
}