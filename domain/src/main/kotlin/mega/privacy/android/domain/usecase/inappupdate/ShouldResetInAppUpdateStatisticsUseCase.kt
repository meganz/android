package mega.privacy.android.domain.usecase.inappupdate

import mega.privacy.android.domain.usecase.GetInstalledVersionCodeUseCase
import javax.inject.Inject

/**
 * Should Reset InAppUpdate Statistics Use Case
 */
class ShouldResetInAppUpdateStatisticsUseCase @Inject constructor(
    private val getLastInAppUpdatePromptVersionUseCase: GetLastInAppUpdatePromptVersionUseCase,
    private val getInstalledVersionCodeUseCase: GetInstalledVersionCodeUseCase,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(): Boolean {
        val lastInAppUpdatePromptVersion = getLastInAppUpdatePromptVersionUseCase()
        val installedVersion = getInstalledVersionCodeUseCase()
        return lastInAppUpdatePromptVersion > 0 && installedVersion > lastInAppUpdatePromptVersion
    }
}