package mega.privacy.android.domain.usecase.contact

/**
 * Get user first name
 *
 */
fun interface GetUserFirstName {
    /**
     * Invoke
     *
     * @param handle
     * @param skipCache
     * @param shouldNotify should notify event
     */
    suspend operator fun invoke(handle: Long, skipCache: Boolean, shouldNotify: Boolean): String
}