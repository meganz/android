package mega.privacy.android.domain.usecase.inappupdate

import javax.inject.Inject

/**
 * Reset InAppUpdate Statistics Use case
 */
class ResetInAppUpdateStatisticsUseCase @Inject constructor(
    private val setLastInAppUpdatePromptTimeUseCase: SetLastInAppUpdatePromptTimeUseCase,
    private val setInAppUpdatePromptCountUseCase: SetInAppUpdatePromptCountUseCase,
    private val setInAppUpdateNeverShowAgainUseCase: SetInAppUpdateNeverShowAgainUseCase,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke() {
        setLastInAppUpdatePromptTimeUseCase(0L)
        setInAppUpdatePromptCountUseCase(0)
        setInAppUpdateNeverShowAgainUseCase(false)
    }
}