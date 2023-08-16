package mega.privacy.android.domain.usecase.inappupdate

import mega.privacy.android.domain.repository.InAppUpdateRepository
import javax.inject.Inject

/**
 * Set Last InAppUpdate Prompt Version Use case
 */
class SetLastInAppUpdatePromptVersionUseCase @Inject constructor(
    private val inAppUpdateRepository: InAppUpdateRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(version: Int) =
        inAppUpdateRepository.setLastInAppUpdatePromptVersion(version)
}