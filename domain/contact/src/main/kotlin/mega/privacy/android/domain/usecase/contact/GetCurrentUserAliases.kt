package mega.privacy.android.domain.usecase.contact

/**
 * Get current user aliases
 *
 */
fun interface GetCurrentUserAliases {
    /**
     * Invoke
     *
     * @return the map of key is user handle and value is user nick name
     */
    suspend operator fun invoke(): Map<Long, String>
}