package mega.privacy.android.domain.usecase.account

/**
 * Checks if 2FA info dialog should show to user
 */
fun interface Check2FADialog {

    /**
     * Initiates check 2FA Dialog
     * @param newAccount returns true only right after when user is signed up
     * @param firstLogin return true on first launch of application
     */
    suspend operator fun invoke(newAccount: Boolean, firstLogin: Boolean): Boolean
}
