package mega.privacy.android.domain.usecase.inappupdate

import mega.privacy.android.domain.repository.InAppUpdateRepository
import javax.inject.Inject

/**
 * Set Last InAppUpdate Prompt Time Use case
 */
class SetLastInAppUpdatePromptTimeUseCase @Inject constructor(
    private val inAppUpdateRepository: InAppUpdateRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(time: Long) =
        inAppUpdateRepository.setLastInAppUpdatePromptTime(time)
}