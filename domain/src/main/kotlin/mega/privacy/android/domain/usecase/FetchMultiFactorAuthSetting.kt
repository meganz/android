package mega.privacy.android.domain.usecase

/**
 * Fetch multi factor auth setting
 *
 */
fun interface FetchMultiFactorAuthSetting {
    /**
     * Invoke
     *
     * @return
     */
    suspend operator fun invoke(): Boolean
}