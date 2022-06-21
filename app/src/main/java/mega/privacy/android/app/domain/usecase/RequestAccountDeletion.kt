package mega.privacy.android.app.domain.usecase

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
