package mega.privacy.android.app.presentation.testpassword.model

/**
 * UI State for Test Password Feature
 * @param isCurrentPassword ui state to handle checks whether entered password is the same as the current password
 * @param isPasswordReminderNotified ui state to handle when password reminder action has been notified
 * @param isUIEnabled ui state to check whether ui is enabled
 * @param isTestPasswordMode ui state to check whether screen is currently on test password mode layout
 */
data class TestPasswordUIState(
    val isCurrentPassword: PasswordState = PasswordState.Initial,
    val isPasswordReminderNotified: PasswordState = PasswordState.Initial,
    val isUIEnabled: Boolean = true,
    val isTestPasswordMode: Boolean = false,
)

/**
 * Password State Holder to handle password related state with 3 types of state
 */
enum class PasswordState {
    /**
     * Initial state
     */
    Initial,

    /**
     * Success state and returns true
     */
    True,

    /**
     * Error state and returns false
     */
    False
}