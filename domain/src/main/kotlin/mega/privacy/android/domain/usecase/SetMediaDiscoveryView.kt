package mega.privacy.android.domain.usecase

/**
 * Set media discovery view preference
 */
fun interface SetMediaDiscoveryView {

    /**
     * Invoke
     *
     * @param state
     */
    suspend operator fun invoke(state: Int)
}