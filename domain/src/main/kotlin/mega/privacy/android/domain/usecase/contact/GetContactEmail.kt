package mega.privacy.android.domain.usecase.contact

/**
 * Get contact email and save to local database
 *
 */
fun interface GetContactEmail {
    /**
     * Invoke
     *
     * @param handle user handle id
     */
    suspend operator fun invoke(handle: Long)
}