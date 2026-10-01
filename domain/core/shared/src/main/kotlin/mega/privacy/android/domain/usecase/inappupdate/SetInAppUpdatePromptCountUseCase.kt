package mega.privacy.android.domain.usecase.inappupdate

import mega.privacy.android.domain.repository.InAppUpdateRepository
import javax.inject.Inject

/**
 * Set InAppUpdate Prompt Count Use case
 */
class SetInAppUpdatePromptCountUseCase @Inject constructor(
    private val inAppUpdateRepository: InAppUpdateRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(count: Int) = inAppUpdateRepository.setInAppUpdatePromptCount(count)
}