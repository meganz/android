package mega.privacy.android.domain.usecase

/**
 * Set sdk logs enabled
 *
 */
fun interface SetSdkLogsEnabled {
    /**
     * Invoke
     *
     * @param enabled
     */
    suspend operator fun invoke(enabled: Boolean)
}
