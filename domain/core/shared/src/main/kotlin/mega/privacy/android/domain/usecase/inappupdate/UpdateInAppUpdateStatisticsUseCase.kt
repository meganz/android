package mega.privacy.android.domain.usecase.inappupdate

import mega.privacy.android.domain.usecase.GetDeviceCurrentTimeUseCase
import javax.inject.Inject

/**
 * Update InAppUpdate Statistics Use case
 */
class UpdateInAppUpdateStatisticsUseCase @Inject constructor(
    private val setLastInAppUpdatePromptTimeUseCase: SetLastInAppUpdatePromptTimeUseCase,
    private val incrementInAppUpdatePromptCountUseCase: IncrementInAppUpdatePromptCountUseCase,
    private val setLastInAppUpdatePromptVersionUseCase: SetLastInAppUpdatePromptVersionUseCase,
    private val getDeviceCurrentTimeUseCase: GetDeviceCurrentTimeUseCase,
    private val setInAppUpdateNeverShowAgainUseCase: SetInAppUpdateNeverShowAgainUseCase,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(neverShowAgain: Boolean, availableVersionCode: Int) {
        setLastInAppUpdatePromptTimeUseCase(getDeviceCurrentTimeUseCase())
        incrementInAppUpdatePromptCountUseCase()
        setLastInAppUpdatePromptVersionUseCase(availableVersionCode)
        setInAppUpdateNeverShowAgainUseCase(neverShowAgain)
    }
}