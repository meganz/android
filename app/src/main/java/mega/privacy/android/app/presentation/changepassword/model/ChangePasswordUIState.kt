package mega.privacy.android.app.presentation.changepassword.model

import androidx.annotation.StringRes

/**
 * UI State for Change Password Activity
 * @param isResetPassword ui state after reset password button clicked
 * @param isPasswordChanged ui state after change password button clicked
 * @param isCurrentPassword ui state to show error when the password is the same as current password
 * @param isPromptedMultiFactorAuth ui state when multi factor auth is prompted
 * @param isConnectedToNetwork ui state to handle network connectivity state
 * @param snackBarMessage ui state to show SnackBar message
 * @param loadingMessage ui state to show progress loading and its message
 * @param passwordStrengthLevel password level strength to show password level animation
 */
data class ChangePasswordUIState(
    val isResetPassword: ActionResult = ActionResult.DEFAULT,
    val isPasswordChanged: Boolean = false,
    val isCurrentPassword: Boolean = false,
    val isPromptedMultiFactorAuth: Boolean = false,
    val isConnectedToNetwork: Boolean = false,
    val passwordStrengthLevel: Int = 0,
    @StringRes val snackBarMessage: Int? = null,
    @StringRes val loadingMessage: Int? = null
)

/**
 * Change Password UI State Action Result
 */
enum class ActionResult {
    /**
     * Default Action - when action hasn't been done
     */
    DEFAULT,

    /**
     * Success Action - when any action returns a success value
     */
    SUCCESS,

    /**
     * Failed Action - when any action returns an error / failure
     */
    FAILED
}