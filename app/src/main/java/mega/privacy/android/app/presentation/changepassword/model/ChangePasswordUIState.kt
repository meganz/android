package mega.privacy.android.app.presentation.changepassword.model

import androidx.annotation.StringRes

/**
 * UI State for Change Password Activity
 * @param isPasswordReset ui state after reset password button clicked
 * @param isPasswordChanged ui state after change password button clicked
 * @param isCurrentPassword ui state to show error when the password is the same as current password
 * @param isPromptedMultiFactorAuth ui state when multi factor auth is prompted
 * @param isConnectedToNetwork ui state to handle network connectivity state
 * @param snackBarMessage ui state to show SnackBar message
 * @param loadingMessage ui state to show progress loading and its message
 * @param isUserLoggedIn ui state to determine whether user is logged in
 * @param errorCode error code when navigating to other screen
 * @param passwordStrengthLevel password level strength to show password level animation
 */
data class ChangePasswordUIState(
    val isPasswordReset: Boolean = false,
    val isPasswordChanged: Boolean = false,
    val isCurrentPassword: Boolean = false,
    val isPromptedMultiFactorAuth: Boolean = false,
    val isConnectedToNetwork: Boolean = false,
    val isUserLoggedIn: Boolean = false,
    val passwordStrengthLevel: Int = -1,
    @Deprecated("Legacy screen still needs this as parameter during navigation")
    val errorCode: Int? = null,
    @StringRes val snackBarMessage: Int? = null,
    @StringRes val loadingMessage: Int? = null,
)