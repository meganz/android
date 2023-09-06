package mega.privacy.android.domain.usecase.inappupdate

import javax.inject.Inject

/**
 * Should Reset InAppUpdate Statistics Use Case
 */
class ShouldResetInAppUpdateStatisticsUseCase @Inject constructor(
    private val getLastInAppUpdatePromptVersionUseCase: GetLastInAppUpdatePromptVersionUseCase,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(availableVersionCode: Int): Boolean {
        val lastInAppUpdatePromptVersion = getLastInAppUpdatePromptVersionUseCase()
        return lastInAppUpdatePromptVersion > 0 && availableVersionCode > lastInAppUpdatePromptVersion
    }
}