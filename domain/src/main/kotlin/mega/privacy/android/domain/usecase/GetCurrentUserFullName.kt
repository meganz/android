package mega.privacy.android.domain.usecase

/**
 * Get full name of current user
 */
fun interface GetCurrentUserFullName {

    /**
     * Invoke
     *
     * @param forceRefresh true force to load from sdk otherwise use database cache
     * @param defaultFirstName default first name when there is no user account info to use yet.
     * @param defaultLastName default last name when there is no use account info to use yet.
     * @return full name
     */
    suspend operator fun invoke(
        forceRefresh: Boolean,
        defaultFirstName: String,
        defaultLastName: String,
    ): String
}