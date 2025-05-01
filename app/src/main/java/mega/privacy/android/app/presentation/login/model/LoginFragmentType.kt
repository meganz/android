package mega.privacy.android.app.presentation.login.model

import mega.privacy.android.app.utils.Constants

/**
 * Enum class for defining login activity fragments.
 *
 * @property value The value of the fragment type.
 */
enum class LoginFragmentType(val value: Int) {
    /**
     * Login screen.
     */
    Login(Constants.LOGIN_FRAGMENT),

    /**
     * Create account screen.
     */
    CreateAccount(Constants.CREATE_ACCOUNT_FRAGMENT),

    /**
     * Confirm email screen.
     */
    ConfirmEmail(Constants.CONFIRM_EMAIL_FRAGMENT),

    /**
     * Tour screen.
     */
    Tour(Constants.TOUR_FRAGMENT),
}