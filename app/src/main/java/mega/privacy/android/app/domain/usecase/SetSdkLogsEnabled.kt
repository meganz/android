package mega.privacy.android.app.domain.usecase

/**
 * Set sdk logs enabled
 *
 */
interface SetSdkLogsEnabled {
    /**
     * Invoke
     *
     * @param enabled
     */
    suspend operator fun invoke(enabled: Boolean)
}
