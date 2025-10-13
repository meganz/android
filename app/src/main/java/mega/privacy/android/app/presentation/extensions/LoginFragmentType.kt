package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.app.presentation.login.model.LoginScreen
import mega.privacy.android.app.utils.Constants

/**
 * Maps [LoginScreen] to its corresponding constant used in LoginActivity.
 */
internal fun LoginScreen.toConstant() = when (this) {
    LoginScreen.LoginScreen -> Constants.LOGIN_FRAGMENT
    LoginScreen.CreateAccount -> Constants.CREATE_ACCOUNT_FRAGMENT
    LoginScreen.ConfirmEmail -> Constants.CONFIRM_EMAIL_FRAGMENT
    LoginScreen.Tour -> Constants.TOUR_FRAGMENT
}