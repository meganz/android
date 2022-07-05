package mega.privacy.android.domain.usecase

/**
 * Request account deletion
 *
 */
fun interface RequestAccountDeletion {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke()
}
