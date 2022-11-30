package mega.privacy.android.domain.usecase

/**
 * Set SMS Verification Shown
 *
 */
fun interface SetSMSVerificationShown {

    /**
     * Invoke
     *
     * @param isShown
     */
    suspend operator fun invoke(isShown: Boolean)
}
