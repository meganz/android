package mega.privacy.android.app.domain.usecase

/**
 * Set chat logs enabled
 *
 */
fun interface SetChatLogsEnabled {
    /**
     * Invoke
     *
     * @param enabled
     */
    suspend operator fun invoke(enabled: Boolean)
}
