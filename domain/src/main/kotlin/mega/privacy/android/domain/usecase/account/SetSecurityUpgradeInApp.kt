package mega.privacy.android.domain.usecase.account

/**
 * Set account security upgrade in app
 *
 */
fun interface SetSecurityUpgradeInApp {
    /**
     * Invoke
     *
     * @param isSecurityUpgrade
     */
    suspend operator fun invoke(isSecurityUpgrade: Boolean)
}
