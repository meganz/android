package mega.privacy.android.domain.usecase

/**
 * Save the playing position histories
 */
fun interface SavePlayingPositionHistories {

    /**
     * Invoke
     *
     * @param key name of the key
     * @param value value to set
     */
    suspend operator fun invoke(key: String?, value: String?)
}