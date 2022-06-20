package mega.privacy.android.app.domain.usecase

/**
 * Fetch nodes use case.
 */
fun interface FetchNodes {

    /**
     * Invoke.
     */
    suspend operator fun invoke()
}