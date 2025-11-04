package mega.privacy.android.app.presentation.login.model

import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.login.LoginNavKey
import mega.privacy.android.app.presentation.login.confirmemail.ConfirmationEmailNavKey
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountNavKey
import mega.privacy.android.app.presentation.login.onboarding.TourNavKey
import mega.privacy.android.app.utils.Constants

/**
 * Enum class for defining login activity fragments.
 *
 * @property value The value of the fragment type.
 */
enum class LoginScreen(val value: Int, val navKey: NavKey) {
    /**
     * Login screen.
     */
    LoginScreen(Constants.LOGIN_FRAGMENT, LoginNavKey),

    /**
     * Create account screen.
     */
    CreateAccount(Constants.CREATE_ACCOUNT_FRAGMENT, CreateAccountNavKey),

    /**
     * Confirm email screen.
     */
    ConfirmEmail(Constants.CONFIRM_EMAIL_FRAGMENT, ConfirmationEmailNavKey),

    /**
     * Tour screen.
     */
    Tour(Constants.TOUR_FRAGMENT, TourNavKey),
}