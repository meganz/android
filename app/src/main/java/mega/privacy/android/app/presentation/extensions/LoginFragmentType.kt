package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.app.presentation.login.model.LoginFragmentType
import mega.privacy.android.app.utils.Constants

/**
 * Maps [LoginFragmentType] to its corresponding constant used in LoginActivity.
 */
internal fun LoginFragmentType.toConstant() = when (this) {
    LoginFragmentType.Login -> Constants.LOGIN_FRAGMENT
    LoginFragmentType.CreateAccount -> Constants.CREATE_ACCOUNT_FRAGMENT
    LoginFragmentType.ConfirmEmail -> Constants.CONFIRM_EMAIL_FRAGMENT
    LoginFragmentType.Tour -> Constants.TOUR_FRAGMENT
}