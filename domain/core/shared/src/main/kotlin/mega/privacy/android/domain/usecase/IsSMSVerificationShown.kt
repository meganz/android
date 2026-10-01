package mega.privacy.android.domain.usecase

/**
 * check whether SMS verificationShown or not
 */
fun interface IsSMSVerificationShown {

    /**
     * invoke
     * @return [Boolean] whether SMS verification shown or not
     */
    suspend operator fun invoke(): Boolean
}
