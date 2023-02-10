package mega.privacy.android.app.presentation.changepassword.model

/**
 * UI State for Change Password Activity
 * @param isResetPassword ui state after reset password button clicked
 * @param isChangePassword ui state after change password button clicked
 * @param isCurrentPassword ui state to show error when the password is the same as current password
 * @param isMultiFactorAuthEnabled ui state when action checks for multi factor auth
 * @param isShowLoadingProgress ui state to show loading progress bar
 * @param passwordStrengthLevel password level strength to show password level animation
 */
data class ChangePasswordUIState(
    val isResetPassword: ActionResult = ActionResult.DEFAULT,
    val isChangePassword: ActionResult = ActionResult.DEFAULT,
    val isCurrentPassword: Boolean = false,
    val isMultiFactorAuthEnabled: Boolean = false,
    val isShowLoadingProgress: Boolean = false,
    val passwordStrengthLevel: Int = 0
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