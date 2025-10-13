package mega.privacy.android.app.presentation.login.model

import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.login.Login
import mega.privacy.android.app.presentation.login.confirmemail.ConfirmationEmailScreen
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountRoute
import mega.privacy.android.app.presentation.login.onboarding.TourScreen
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
    LoginScreen(Constants.LOGIN_FRAGMENT, Login),

    /**
     * Create account screen.
     */
    CreateAccount(Constants.CREATE_ACCOUNT_FRAGMENT, CreateAccountRoute),

    /**
     * Confirm email screen.
     */
    ConfirmEmail(Constants.CONFIRM_EMAIL_FRAGMENT, ConfirmationEmailScreen),

    /**
     * Tour screen.
     */
    Tour(Constants.TOUR_FRAGMENT, TourScreen),
}