package mega.privacy.android.domain.usecase.inappupdate

import mega.privacy.android.domain.repository.InAppUpdateRepository
import javax.inject.Inject

/**
 * Get Last InAppUpdate Prompt Time Use case
 */
class GetLastInAppUpdatePromptTimeUseCase @Inject constructor(
    private val inAppUpdateRepository: InAppUpdateRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(): Long = inAppUpdateRepository.getLastInAppUpdatePromptTime()
}