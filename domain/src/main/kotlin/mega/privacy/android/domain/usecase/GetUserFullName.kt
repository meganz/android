package mega.privacy.android.domain.usecase

/**
 * Get current user's full name
 */
fun interface GetUserFullName {

    /**
     * Invoke
     * @param forceRefresh true force to load from sdk otherwise use database cache
     * @return full name
     */
    suspend operator fun invoke(forceRefresh: Boolean): String?
}