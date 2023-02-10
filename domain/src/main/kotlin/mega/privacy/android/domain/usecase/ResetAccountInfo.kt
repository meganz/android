package mega.privacy.android.domain.usecase

/**
 * Use case for resetting account info.
 */
fun interface ResetAccountInfo {

    /**
     * Invoke.
     */
    suspend operator fun invoke()
}