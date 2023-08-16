package mega.privacy.android.domain.usecase.inappupdate

import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Should Prompt User For Update Use Case
 */
class ShouldPromptUserForUpdateUseCase @Inject constructor(
    private val getLastInAppUpdatePromptTimeUseCase: GetLastInAppUpdatePromptTimeUseCase,
    private val getInAppUpdatePromptCountUseCase: GetInAppUpdatePromptCountUseCase,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(
        noOfDaysBeforePrompt: Int,
        isIncrementalPromptEnabled: Boolean,
        incrementalFrequencyInDays: Int,
        incrementalPromptStopCount: Int,
    ): Boolean {
        val lastInAppUpdateTime = getLastInAppUpdatePromptTimeUseCase()
        val inAppUpdatePromptCount = getInAppUpdatePromptCountUseCase()
        return if (isIncrementalPromptEnabled) {
            if (inAppUpdatePromptCount == 0) { // First time prompt
                true
            } else if (inAppUpdatePromptCount > incrementalPromptStopCount) { // stop prompting if INCREMENTAL_PROMPT_STOP_COUNT reached
                false
            } else { // Prompt initially, then after x days, then x + n days, x + 2n days, x + 3n days etc.
                val daysSinceLastCheck = getDaysSinceLastCheck(lastInAppUpdateTime)
                daysSinceLastCheck >= noOfDaysBeforePrompt + (inAppUpdatePromptCount - 1) * incrementalFrequencyInDays
            }
        } else {  // Prompt after every x days
            val daysSinceLastCheck = getDaysSinceLastCheck(lastInAppUpdateTime)
            daysSinceLastCheck >= noOfDaysBeforePrompt
        }
    }

    private fun getDaysSinceLastCheck(
        lastInAppUpdateTime: Long,
    ) = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastInAppUpdateTime)
}