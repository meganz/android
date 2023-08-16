package mega.privacy.android.domain.usecase.inappupdate

import mega.privacy.android.domain.usecase.GetDeviceCurrentTimeUseCase
import mega.privacy.android.domain.usecase.GetInstalledVersionCodeUseCase
import javax.inject.Inject

/**
 * Update InAppUpdate Statistics Use case
 */
class UpdateInAppUpdateStatisticsUseCase @Inject constructor(
    private val setLastInAppUpdatePromptTimeUseCase: SetLastInAppUpdatePromptTimeUseCase,
    private val incrementInAppUpdatePromptCountUseCase: IncrementInAppUpdatePromptCountUseCase,
    private val setLastInAppUpdatePromptVersionUseCase: SetLastInAppUpdatePromptVersionUseCase,
    private val getInstalledVersionCodeUseCase: GetInstalledVersionCodeUseCase,
    private val getDeviceCurrentTimeUseCase: GetDeviceCurrentTimeUseCase,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke() {
        setLastInAppUpdatePromptTimeUseCase(getDeviceCurrentTimeUseCase())
        incrementInAppUpdatePromptCountUseCase()
        setLastInAppUpdatePromptVersionUseCase(getInstalledVersionCodeUseCase())
    }
}