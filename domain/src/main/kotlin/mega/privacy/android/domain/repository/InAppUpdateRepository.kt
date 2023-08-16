package mega.privacy.android.domain.repository

/**
 * InAppUpdate repository
 *
 */
interface InAppUpdateRepository {
    /**
     * Set Last InAppUpdate Prompt time
     */
    suspend fun setLastInAppUpdatePromptTime(time: Long)

    /**
     * Get Last InAppUpdate Prompt time
     */
    suspend fun getLastInAppUpdatePromptTime(): Long

    /**
     * Increment InAppUpdate Prompt count
     */
    suspend fun incrementInAppUpdatePromptCount()

    /**
     * Get InAppUpdate Prompt count
     */
    suspend fun getInAppUpdatePromptCount(): Int


    /**
     * Set InAppUpdate Prompt count
     */
    suspend fun setInAppUpdatePromptCount(count: Int)


    /**
     * Get Last InAppUpdate Prompt version
     */
    suspend fun getLastInAppUpdatePromptVersion(): Int


    /**
     * Set Last InAppUpdate Prompt version
     */
    suspend fun setLastInAppUpdatePromptVersion(version: Int)
}
