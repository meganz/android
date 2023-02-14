package mega.privacy.android.domain.usecase

/**
 * Use case for stop checking Psa.
 */
fun interface ClearPsa {

    /**
     * Invoke.
     */
    suspend operator fun invoke()
}