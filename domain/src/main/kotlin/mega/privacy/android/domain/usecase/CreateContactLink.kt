package mega.privacy.android.domain.usecase

/**
 * Use case to create a contact link.
 */
fun interface CreateContactLink {
    /**
     * Invoke method
     *
     * @param renew true to invalidate the previous contact link (if any)
     * @return Generated contact link URL. null if anything wrong.
     */
    suspend operator fun invoke(renew: Boolean): String?
}