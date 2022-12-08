package mega.privacy.android.domain.usecase

/**
 * Logout use case
 */
fun interface Logout {

    /**
     * Invoke
     */
    suspend operator fun invoke()
}
