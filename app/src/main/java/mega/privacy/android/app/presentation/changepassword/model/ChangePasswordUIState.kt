package mega.privacy.android.app.presentation.changepassword.model

import androidx.annotation.StringRes

/**
 * UI State for Change Password Activity
 * @param isPasswordReset ui state after reset password button clicked
 * @param isPasswordChanged ui state after change password button clicked
 * @param isCurrentPassword ui state to show error when the password is the same as current password
 * @param isPromptedMultiFactorAuth ui state when multi factor auth is prompted
 * @param isConnectedToNetwork ui state to handle network connectivity state
 * @param isUserLoggedIn ui state to determine whether user is logged in
 * @param isResetPasswordMode ui state to determine whether screen is on change password mode or reset password mode
 * @param isResetPasswordLinkValid ui state to determine whether link is valid when mode is password reset
 * @param isShowAlertMessage ui state to show alert dialog
 * @param passwordStrengthLevel password level strength to show password level animation
 * @param errorCode error code when navigating to other screen
 * @param snackBarMessage ui state to show SnackBar message
 * @param loadingMessage ui state to show progress loading and its message

 */
data class ChangePasswordUIState(
    val isPasswordReset: Boolean = false,
    val isPasswordChanged: Boolean = false,
    val isCurrentPassword: Boolean = false,
    val isPromptedMultiFactorAuth: Boolean = false,
    val isConnectedToNetwork: Boolean = false,
    val isUserLoggedIn: Boolean = false,
    val isResetPasswordMode: Boolean = false,
    val isResetPasswordLinkValid: Boolean = true,
    val isShowAlertMessage: Boolean = false,
    val passwordStrengthLevel: Int = -1,
    @Deprecated("Legacy screen still needs this as parameter during navigation")
    val errorCode: Int? = null,
    @StringRes val snackBarMessage: Int? = null,
    @StringRes val loadingMessage: Int? = null,
)